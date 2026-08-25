package com.volt.catalog.application.port.out;

import com.volt.catalog.domain.model.Reservation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reservation persistence (ADR-0003). */
public interface ReservationRepositoryPort {

    /** Serializes creation attempts that carry the same idempotency key. */
    void lockOrderRef(String orderRef);

    Optional<Reservation> findById(long reservationId);

    /** Serializes confirm/release for a single reservation. */
    Optional<Reservation> lockById(long reservationId);

    /** Supports idempotent reservation: same order reference, same reservation. */
    Optional<Reservation> findByOrderRef(String orderRef);

    /** Serializes restock retries for one order. */
    Optional<Reservation> lockByOrderRef(String orderRef);

    Reservation save(Reservation reservation);

    /**
     * Quantity currently held by ACTIVE, unexpired reservations, per product.
     *
     * <p>Available stock is {@code product.stockQuantity()} minus this. It is
     * computed rather than stored so it cannot drift out of step with the
     * reservation rows.
     */
    Map<Long, Integer> reservedQuantities(List<Long> productIds, Instant now);

    /** ACTIVE reservations whose {@code expiresAt} is in the past. */
    List<Reservation> findExpired(Instant now);
}
