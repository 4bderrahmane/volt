package com.volt.catalog.domain.exception;

import com.volt.catalog.domain.model.StockShortage;

import java.util.List;

/**
 * Raised when a reservation cannot be satisfied. Reservation is all-or-nothing
 * (ADR-0003): if any line is short, nothing is reserved, so this exception
 * carries every shortage rather than only the first one.
 *
 * <p>Maps to HTTP 409 with problem type
 * {@code https://volt.local/problems/insufficient-stock}.
 */
public class InsufficientStockException extends RuntimeException {

    /** Exceptions are Serializable by inheritance; pin the id so a
     *  refactor cannot silently change it. Never actually serialised here. */
    private static final long serialVersionUID = 1L;

    private final transient List<StockShortage> shortages;

    public InsufficientStockException(List<StockShortage> shortages) {
        super("Insufficient stock for " + shortages.size() + " line(s)");
        this.shortages = List.copyOf(shortages);
    }

    public List<StockShortage> shortages() {
        return shortages;
    }
}
