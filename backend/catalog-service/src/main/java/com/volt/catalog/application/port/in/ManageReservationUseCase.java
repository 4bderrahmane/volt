package com.volt.catalog.application.port.in;

/**
 * Phases two and three of ADR-0003, plus the restock path the specification omits.
 *
 * <p>Every method here is idempotent, which is the precondition that makes the
 * retry mandated by specification §3.4 correct instead of a double-decrement bug.
 */
public interface ManageReservationUseCase {

    /**
     * Commits the reservation: physical stock is decremented and the
     * reservation moves to CONFIRMED.
     *
     * <p>Idempotent — confirming an already-confirmed reservation returns
     * normally and decrements nothing. Confirming an expired one throws
     * {@link com.volt.catalog.domain.exception.ReservationExpiredException},
     * because silently succeeding would oversell.
     */
    void confirm(long reservationId);

    /** Releases an active reservation early. Idempotent. */
    void release(long reservationId);

    /**
     * Returns stock for an order cancelled after confirmation.
     *
     * <p>Closes a real gap: specification §F9 defines a CANCELLED status and §F5 lists
     * only "read and decrement", so as specified a cancellation
     * destroys stock permanently.
     */
    void restock(String orderRef);
}
