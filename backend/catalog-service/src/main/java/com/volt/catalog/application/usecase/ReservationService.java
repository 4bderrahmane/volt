package com.volt.catalog.application.usecase;

import com.volt.catalog.application.port.in.ExpireReservationsUseCase;
import com.volt.catalog.application.port.in.ManageReservationUseCase;
import com.volt.catalog.application.port.in.ReserveStockUseCase;
import com.volt.catalog.application.port.out.ProductRepositoryPort;
import com.volt.catalog.application.port.out.ReservationRepositoryPort;
import com.volt.catalog.domain.exception.InsufficientStockException;
import com.volt.catalog.domain.exception.ProductNotFoundException;
import com.volt.catalog.domain.exception.ReservationExpiredException;
import com.volt.catalog.domain.exception.ReservationNotFoundException;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.Reservation;
import com.volt.catalog.domain.model.ReservationLine;
import com.volt.catalog.domain.model.ReservationStatus;
import com.volt.catalog.domain.model.StockShortage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationService
        implements ReserveStockUseCase, ManageReservationUseCase, ExpireReservationsUseCase {

    private final ProductRepositoryPort products;
    private final ReservationRepositoryPort reservations;
    private final Clock clock;
    private final Duration reservationTtl;

    public ReservationService(
            ProductRepositoryPort products,
            ReservationRepositoryPort reservations,
            Clock clock,
            @Value("${volt.reservation.ttl:PT15M}") Duration reservationTtl) {
        if (reservationTtl.isNegative() || reservationTtl.isZero()) {
            throw new IllegalArgumentException("reservation TTL must be positive");
        }
        this.products = products;
        this.reservations = reservations;
        this.clock = clock;
        this.reservationTtl = reservationTtl;
    }

    @Override
    public ReservationResult reserve(ReserveStockCommand command) {
        Instant now = clock.instant();
        reservations.lockOrderRef(command.orderRef());
        Reservation existing = reservations.findByOrderRef(command.orderRef()).orElse(null);
        if (existing != null) {
            return existingResult(existing);
        }

        List<Long> productIds = command.lines().stream()
                .map(RequestedLine::productId)
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = byId(products.lockForUpdate(productIds));
        requireEveryProduct(productIds, lockedProducts);

        // Product locks serialize competing requests for the same order lines.
        // Re-checking after the lock makes an ordinary retry return the row that
        // the preceding transaction committed while this one was waiting.
        existing = reservations.findByOrderRef(command.orderRef()).orElse(null);
        if (existing != null) {
            return result(existing, lockedProducts);
        }

        Map<Long, Integer> reserved = reservations.reservedQuantities(productIds, now);
        List<StockShortage> shortages = new ArrayList<>();
        for (RequestedLine requested : command.lines()) {
            Product product = lockedProducts.get(requested.productId());
            int alreadyReserved = reserved.getOrDefault(requested.productId(), 0);
            if (!product.canFulfil(requested.quantity(), alreadyReserved)) {
                int available = product.isActive()
                        ? Math.max(0, product.getStockQuantity() - alreadyReserved)
                        : 0;
                shortages.add(new StockShortage(
                        product.getId(), product.getReference(), requested.quantity(), available));
            }
        }
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }

        List<ReservationLine> lines = command.lines().stream()
                .map(line -> ReservationLine.create(line.productId(), line.quantity()))
                .toList();
        Reservation saved = reservations.save(
                Reservation.open(command.orderRef(), lines, now, now.plus(reservationTtl)));
        return result(saved, lockedProducts);
    }

    @Override
    public void confirm(long reservationId) {
        Instant now = clock.instant();
        Reservation reservation = lockedReservation(reservationId);
        if (!reservation.confirm(now)) {
            return;
        }

        List<Long> productIds = reservation.getLines().stream()
                .map(ReservationLine::getProductId)
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = byId(products.lockForUpdate(productIds));
        requireEveryProduct(productIds, lockedProducts);
        for (ReservationLine line : reservation.getLines()) {
            Product product = lockedProducts.get(line.getProductId());
            product.decreaseStock(line.getQuantity(), now);
            products.save(product);
        }
        reservations.save(reservation);
    }

    @Override
    public void release(long reservationId) {
        Reservation reservation = lockedReservation(reservationId);
        if (reservation.release(clock.instant())) {
            reservations.save(reservation);
        }
    }

    @Override
    public void restock(String orderRef) {
        if (orderRef == null || orderRef.isBlank()) {
            throw new IllegalArgumentException("orderRef must not be blank");
        }
        String normalizedOrderRef = orderRef.trim();
        if (normalizedOrderRef.length() > 64) {
            throw new IllegalArgumentException("orderRef must be at most 64 characters");
        }
        Reservation reservation = reservations.lockByOrderRef(normalizedOrderRef)
                .orElseThrow(() -> new ReservationNotFoundException(normalizedOrderRef));
        Instant now = clock.instant();
        if (!reservation.restock(now)) {
            return;
        }
        List<Long> productIds = reservation.getLines().stream()
                .map(ReservationLine::getProductId)
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = byId(products.lockForUpdate(productIds));
        requireEveryProduct(productIds, lockedProducts);
        for (ReservationLine line : reservation.getLines()) {
            Product product = lockedProducts.get(line.getProductId());
            product.increaseStock(line.getQuantity(), now);
            products.save(product);
        }
        reservations.save(reservation);
    }

    @Override
    public int expireOverdueReservations() {
        Instant now = clock.instant();
        int expired = 0;
        for (Reservation reservation : reservations.findExpired(now)) {
            if (reservation.expire(now)) {
                reservations.save(reservation);
                expired++;
            }
        }
        return expired;
    }

    private Reservation lockedReservation(long reservationId) {
        return reservations.lockById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }

    private ReservationResult existingResult(Reservation reservation) {
        List<Long> productIds = reservation.getLines().stream()
                .map(ReservationLine::getProductId)
                .toList();
        Map<Long, Product> currentProducts = byId(products.findAllByIds(productIds));
        requireEveryProduct(productIds, currentProducts);
        return result(reservation, currentProducts);
    }

    private static ReservationResult result(Reservation reservation, Map<Long, Product> products) {
        if (reservation.getStatus() == ReservationStatus.EXPIRED
                || reservation.getStatus() == ReservationStatus.RELEASED
                || reservation.getStatus() == ReservationStatus.RESTOCKED) {
            throw new ReservationExpiredException(reservation.getId());
        }
        List<ReservedLine> lines = reservation.getLines().stream()
                .map(line -> {
                    Product product = products.get(line.getProductId());
                    return new ReservedLine(
                            product.getId(),
                            product.getReference(),
                            product.getLabel(),
                            product.getPriceExclVat(),
                            line.getQuantity());
                })
                .toList();
        return new ReservationResult(reservation.getId(), reservation.getExpiresAt(), lines);
    }

    private static Map<Long, Product> byId(List<Product> products) {
        return products.stream().collect(Collectors.toMap(
                Product::getId,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private static void requireEveryProduct(List<Long> productIds, Map<Long, Product> products) {
        productIds.stream()
                .distinct()
                .filter(id -> !products.containsKey(id))
                .min(Comparator.naturalOrder())
                .ifPresent(missing -> {
                    throw new ProductNotFoundException(missing);
                });
    }
}
