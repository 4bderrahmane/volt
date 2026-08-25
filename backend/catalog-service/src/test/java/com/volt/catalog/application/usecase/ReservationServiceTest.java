package com.volt.catalog.application.usecase;

import com.volt.catalog.application.port.in.ReserveStockUseCase;
import com.volt.catalog.application.port.out.ReservationRepositoryPort;
import com.volt.catalog.domain.exception.InsufficientStockException;
import com.volt.catalog.domain.exception.ReservationExpiredException;
import com.volt.catalog.domain.model.Reservation;
import com.volt.catalog.domain.model.ReservationLine;
import com.volt.catalog.domain.model.ReservationStatus;
import com.volt.catalog.infrastructure.adapter.out.memory.InMemoryProductRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationServiceTest {

    private MutableClock clock;
    private InMemoryProductRepositoryAdapter products;
    private FakeReservationRepository reservations;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-08-14T10:00:00Z"));
        products = new InMemoryProductRepositoryAdapter();
        reservations = new FakeReservationRepository();
        service = new ReservationService(products, reservations, clock, Duration.ofMinutes(15));
    }

    @Test
    void reservesAndConfirmsExactlyOnce() {
        ReserveStockUseCase.ReservationResult result = service.reserve(command("ORD-1", 4));

        service.confirm(result.reservationId());
        service.confirm(result.reservationId());

        assertEquals(16, products.findById(1L).orElseThrow().getStockQuantity());
        assertEquals(ReservationStatus.CONFIRMED,
                reservations.findById(result.reservationId()).orElseThrow().getStatus());
    }

    @Test
    void returnsTheSameReservationForAnOrderRetry() {
        ReserveStockUseCase.ReservationResult first = service.reserve(command("ORD-1", 4));
        ReserveStockUseCase.ReservationResult retry = service.reserve(command("ORD-1", 4));

        assertEquals(first.reservationId(), retry.reservationId());
        assertEquals(1, reservations.rows.size());
    }

    @Test
    void restocksAConfirmedOrderExactlyOnce() {
        ReserveStockUseCase.ReservationResult result = service.reserve(command("ORD-1", 4));
        service.confirm(result.reservationId());

        service.restock("ORD-1");
        service.restock("ORD-1");

        assertEquals(20, products.findById(1L).orElseThrow().getStockQuantity());
        assertEquals(ReservationStatus.RESTOCKED,
                reservations.findById(result.reservationId()).orElseThrow().getStatus());
        assertThrows(ReservationExpiredException.class, () -> service.reserve(command("ORD-1", 4)));
    }

    @Test
    void rejectsInsufficientStockWithoutCreatingAReservation() {
        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> service.reserve(command("ORD-1", 21)));

        assertEquals(20, exception.shortages().getFirst().available());
        assertEquals(0, reservations.rows.size());
    }

    @Test
    void expiresAnAbandonedReservation() {
        ReserveStockUseCase.ReservationResult result = service.reserve(command("ORD-1", 4));
        clock.advance(Duration.ofMinutes(16));

        assertEquals(1, service.expireOverdueReservations());
        assertThrows(ReservationExpiredException.class, () -> service.confirm(result.reservationId()));
    }

    private static ReserveStockUseCase.ReserveStockCommand command(String orderRef, int quantity) {
        return new ReserveStockUseCase.ReserveStockCommand(
                orderRef,
                List.of(new ReserveStockUseCase.RequestedLine(1L, quantity)));
    }

    private static final class FakeReservationRepository implements ReservationRepositoryPort {

        private final Map<Long, Reservation> rows = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public void lockOrderRef(String orderRef) {
        }

        @Override
        public Optional<Reservation> findById(long reservationId) {
            return Optional.ofNullable(rows.get(reservationId));
        }

        @Override
        public Optional<Reservation> lockById(long reservationId) {
            return findById(reservationId);
        }

        @Override
        public Optional<Reservation> findByOrderRef(String orderRef) {
            return rows.values().stream().filter(row -> row.getOrderRef().equals(orderRef)).findFirst();
        }

        @Override
        public Optional<Reservation> lockByOrderRef(String orderRef) {
            return findByOrderRef(orderRef);
        }

        @Override
        public Reservation save(Reservation reservation) {
            if (reservation.getId() == null) {
                reservation = new Reservation(
                        nextId++,
                        reservation.getOrderRef(),
                        reservation.getStatus(),
                        reservation.getExpiresAt(),
                        reservation.getCreatedAt(),
                        reservation.getUpdatedAt(),
                        reservation.getLines());
            }
            rows.put(reservation.getId(), reservation);
            return reservation;
        }

        @Override
        public Map<Long, Integer> reservedQuantities(List<Long> productIds, Instant now) {
            Map<Long, Integer> totals = new LinkedHashMap<>();
            rows.values().stream()
                    .filter(Reservation::isActive)
                    .filter(row -> !row.hasExpired(now))
                    .flatMap(row -> row.getLines().stream())
                    .filter(line -> productIds.contains(line.getProductId()))
                    .forEach(line -> totals.merge(line.getProductId(), line.getQuantity(), Integer::sum));
            return totals;
        }

        @Override
        public List<Reservation> findExpired(Instant now) {
            return rows.values().stream()
                    .filter(Reservation::isActive)
                    .filter(row -> row.hasExpired(now))
                    .toList();
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
