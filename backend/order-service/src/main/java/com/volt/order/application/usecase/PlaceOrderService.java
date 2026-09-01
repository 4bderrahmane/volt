package com.volt.order.application.usecase;

import com.volt.order.application.port.in.ChangeOrderStatusUseCase;
import com.volt.order.application.port.in.PlaceOrderUseCase;
import com.volt.order.application.port.out.CartRepositoryPort;
import com.volt.order.application.port.out.CatalogClientPort;
import com.volt.order.application.port.out.OrderNumberGeneratorPort;
import com.volt.order.application.port.out.OrderRepositoryPort;
import com.volt.order.domain.exception.EmptyCartException;
import com.volt.order.domain.model.Cart;
import com.volt.order.domain.model.Order;
import com.volt.order.domain.model.OrderLine;
import com.volt.order.domain.model.OrderStatus;
import com.volt.order.domain.model.OrderTotals;
import com.volt.order.domain.model.VatRate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PlaceOrderService implements PlaceOrderUseCase {
    private final CartRepositoryPort carts;
    private final OrderRepositoryPort orders;
    private final OrderNumberGeneratorPort numbers;
    private final CatalogClientPort catalog;
    private final ChangeOrderStatusUseCase statusChanges;
    private final Clock clock;

    public PlaceOrderService(CartRepositoryPort carts, OrderRepositoryPort orders,
                             OrderNumberGeneratorPort numbers, CatalogClientPort catalog,
                             ChangeOrderStatusUseCase statusChanges, Clock clock) {
        this.carts = carts;
        this.orders = orders;
        this.numbers = numbers;
        this.catalog = catalog;
        this.statusChanges = statusChanges;
        this.clock = clock;
    }

    @Override
    public Order placeOrder(UUID customerId) {
        Cart cart = carts.findByCustomerId(customerId).filter(candidate -> !candidate.isEmpty())
                .orElseThrow(EmptyCartException::new);
        String number = numbers.nextOrderNumber();
        List<CatalogClientPort.RequestedLine> requested = cart.getLines().stream()
                .map(line -> new CatalogClientPort.RequestedLine(line.getProductId(), line.getQuantity()))
                .toList();
        CatalogClientPort.Reservation reservation = catalog.reserve(number, requested);
        List<OrderLine> lines;
        try {
            requireMatchingReservation(requested, reservation.lines());
            lines = reservation.lines().stream()
                    .map(line -> OrderLine.of(line.productId(), line.reference(), line.label(),
                            line.unitPriceExclVat(), line.quantity()))
                    .toList();
        } catch (RuntimeException invalidResponse) {
            releaseQuietly(reservation.reservationId());
            throw invalidResponse;
        }
        Order created = Order.place(number, customerId, reservation.reservationId(),
                OrderTotals.calculate(lines, VatRate.STANDARD), lines, clock.instant());
        try {
            created = orders.save(created);
        } catch (RuntimeException failure) {
            releaseQuietly(reservation.reservationId());
            throw failure;
        }

        // The CREATED order must commit before confirmation; statusChanges owns
        // the separate locking transaction.
        Order confirmed = statusChanges.changeStatus(created.getId(), OrderStatus.CONFIRMED);
        cart.clear(clock.instant());
        carts.save(cart);
        return confirmed;
    }

    private static void requireMatchingReservation(List<CatalogClientPort.RequestedLine> requested,
                                                   List<CatalogClientPort.ReservedLine> reserved) {
        Map<Long, Integer> expected = new HashMap<>();
        for (CatalogClientPort.RequestedLine line : requested) {
            if (expected.put(line.productId(), line.quantity()) != null) {
                throw new IllegalStateException("checkout request contained duplicate products");
            }
        }
        Map<Long, Integer> actual = new HashMap<>();
        for (CatalogClientPort.ReservedLine line : reserved) {
            if (actual.put(line.productId(), line.quantity()) != null) {
                throw new IllegalStateException("catalog reservation response contained duplicate products");
            }
        }
        if (!actual.equals(expected)) {
            throw new IllegalStateException("catalog reservation response did not match every requested line");
        }
    }

    private void releaseQuietly(long reservationId) {
        try {
            catalog.releaseReservation(reservationId);
        } catch (RuntimeException ignored) {
            // The catalog reservation expires; preserve the original local failure.
        }
    }
}
