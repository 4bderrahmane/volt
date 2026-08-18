package com.volt.catalog.domain.exception;

/**
 * The reservation never existed.
 *
 * <p>Maps to HTTP 404 in the web adapter.
 */
public class ReservationNotFoundException extends RuntimeException {

    /** Exceptions are Serializable by inheritance; pin the id so a
     *  refactor cannot silently change it. Never actually serialised here. */
    private static final long serialVersionUID = 1L;

    public ReservationNotFoundException(long reservationId) {
        super("No reservation with id " + reservationId);
    }

    public ReservationNotFoundException(String orderRef) {
        super("No reservation for order reference " + orderRef);
    }
}
