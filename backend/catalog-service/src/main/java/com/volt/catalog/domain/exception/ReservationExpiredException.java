package com.volt.catalog.domain.exception;

/**
 * Raised when confirming a reservation the expiry sweep has already released
 * (ADR-0003). Distinct from {@link ReservationNotFoundException}: the caller
 * did nothing wrong, it simply took too long, and the SPA should say so rather
 * than reporting a missing resource.
 *
 * <p>Maps to HTTP 410 Gone.
 */
public class ReservationExpiredException extends RuntimeException {

    /** Exceptions are Serializable by inheritance; pin the id so a
     *  refactor cannot silently change it. Never actually serialised here. */
    private static final long serialVersionUID = 1L;

    public ReservationExpiredException(Long reservationId) {
        super("Reservation " + reservationId + " has expired");
    }
}
