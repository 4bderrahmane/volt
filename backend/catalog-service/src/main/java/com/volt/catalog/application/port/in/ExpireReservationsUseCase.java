package com.volt.catalog.application.port.in;

/**
 * The expiry sweep of ADR-0003 — the mechanism that makes the whole design
 * self-healing. Every other repair path in a distributed checkout requires the
 * caller to survive the failure it is repairing; this one runs inside the
 * service that owns the stock, as a local transaction.
 *
 * <p>Driven by a scheduled adapter in {@code infrastructure}, not by an
 * {@code @Scheduled} annotation on the use case, so it stays unit-testable.
 *
 * <p>Expose the returned count and the age of the oldest unexpired-but-overdue
 * reservation via Actuator: a sweep that silently stops running starves
 * inventory, and nothing else in the system will notice.
 */
public interface ExpireReservationsUseCase {

    /** @return how many reservations were released */
    int expireOverdueReservations();
}
