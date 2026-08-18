package com.volt.catalog.domain.exception;

import com.volt.catalog.domain.model.StockShortage;

import java.util.List;

public class InsufficientStockException extends RuntimeException {

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
