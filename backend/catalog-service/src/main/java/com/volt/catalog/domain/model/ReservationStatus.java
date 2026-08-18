package com.volt.catalog.domain.model;

/** Lifecycle of a stock reservation (ADR-0003). */
public enum ReservationStatus {
    /** Holding stock, not yet committed. Expires at {@code expiresAt}. */
    ACTIVE,
    /** Confirmed: physical stock has been decremented. Terminal. */
    CONFIRMED,
    /** Released explicitly by the caller. Terminal. */
    RELEASED,
    /** Released by the expiry sweep because it was never confirmed. Terminal. */
    EXPIRED,
    /** Stock from a confirmed reservation has been returned. Terminal. */
    RESTOCKED
}
