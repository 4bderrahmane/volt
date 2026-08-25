package com.volt.catalog.domain.model;

import com.volt.catalog.domain.exception.ReservationExpiredException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationTest {

    private static final Instant NOW = Instant.parse("2026-08-14T10:00:00Z");

    @Test
    void confirmsOnce() {
        Reservation reservation = persistedReservation(ReservationStatus.ACTIVE, NOW.plusSeconds(60));

        assertTrue(reservation.confirm(NOW.plusSeconds(1)));
        assertFalse(reservation.confirm(NOW.plusSeconds(2)));
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }

    @Test
    void refusesConfirmationAtOrAfterExpiry() {
        Reservation reservation = persistedReservation(ReservationStatus.ACTIVE, NOW.plusSeconds(60));

        assertThrows(ReservationExpiredException.class, () -> reservation.confirm(NOW.plusSeconds(60)));
        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
    }

    @Test
    void releasesAndExpiresIdempotently() {
        Reservation released = persistedReservation(ReservationStatus.ACTIVE, NOW.plusSeconds(60));
        Reservation expired = persistedReservation(ReservationStatus.ACTIVE, NOW.plusSeconds(60));

        assertTrue(released.release(NOW.plusSeconds(1)));
        assertFalse(released.release(NOW.plusSeconds(2)));
        assertTrue(expired.expire(NOW.plusSeconds(60)));
        assertFalse(expired.expire(NOW.plusSeconds(61)));
    }

    @Test
    void restocksAConfirmedReservationOnce() {
        Reservation reservation = persistedReservation(ReservationStatus.CONFIRMED, NOW.plusSeconds(60));

        assertTrue(reservation.restock(NOW.plusSeconds(1)));
        assertFalse(reservation.restock(NOW.plusSeconds(2)));
        assertEquals(ReservationStatus.RESTOCKED, reservation.getStatus());
    }

    @Test
    void rejectsDuplicateProducts() {
        List<ReservationLine> duplicates = List.of(
                ReservationLine.create(1L, 1),
                ReservationLine.create(1L, 2));

        assertThrows(IllegalArgumentException.class,
                () -> Reservation.open("ORD-1", duplicates, NOW, NOW.plusSeconds(60)));
    }

    private static Reservation persistedReservation(ReservationStatus status, Instant expiresAt) {
        return new Reservation(
                1L,
                "ORD-1",
                status,
                expiresAt,
                NOW,
                NOW,
                List.of(new ReservationLine(1L, 10L, 2)));
    }
}
