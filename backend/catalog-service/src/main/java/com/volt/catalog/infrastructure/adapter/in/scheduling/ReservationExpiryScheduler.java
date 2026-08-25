package com.volt.catalog.infrastructure.adapter.in.scheduling;

import com.volt.catalog.application.port.in.ExpireReservationsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiryScheduler {

    private final ExpireReservationsUseCase expireReservations;

    public ReservationExpiryScheduler(ExpireReservationsUseCase expireReservations) {
        this.expireReservations = expireReservations;
    }

    @Scheduled(fixedDelayString = "${volt.reservation.sweep-interval:PT1M}")
    public void expireOverdueReservations() {
        expireReservations.expireOverdueReservations();
    }
}
