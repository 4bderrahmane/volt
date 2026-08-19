package com.volt.catalog.domain.exception;

import java.io.Serial;

public class ReservationExpiredException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ReservationExpiredException(Long reservationId) {
        super("Reservation " + reservationId + " has expired");
    }
}
