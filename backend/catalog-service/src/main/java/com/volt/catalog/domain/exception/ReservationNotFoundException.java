package com.volt.catalog.domain.exception;

import java.io.Serial;

public class ReservationNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ReservationNotFoundException(long reservationId) {
        super("No reservation with id " + reservationId);
    }

    public ReservationNotFoundException(String orderRef) {
        super("No reservation for order reference " + orderRef);
    }
}
