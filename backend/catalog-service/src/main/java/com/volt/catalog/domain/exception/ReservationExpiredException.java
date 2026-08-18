package com.volt.catalog.domain.exception;

public class ReservationExpiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservationExpiredException(Long reservationId) {
        super("Reservation " + reservationId + " has expired");
    }
}
