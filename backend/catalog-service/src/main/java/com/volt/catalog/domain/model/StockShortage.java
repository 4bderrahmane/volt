package com.volt.catalog.domain.model;

public record StockShortage(Long productId, String reference, int requested, int available) {
}
