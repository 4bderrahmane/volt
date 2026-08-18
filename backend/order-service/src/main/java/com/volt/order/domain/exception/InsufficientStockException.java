package com.volt.order.domain.exception;

import com.volt.order.domain.model.StockShortage;

import java.util.List;

/**
 * The catalog refused the reservation. Maps to HTTP 409, which specification §13
 * requires along with a clear message — hence the shortage list: the SPA
 * needs to name which product ran out, not just report that something did.
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
