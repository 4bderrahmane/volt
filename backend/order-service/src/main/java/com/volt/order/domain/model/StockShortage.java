package com.volt.order.domain.model;

/** A line the catalog could not reserve, parsed from its 409 problem body. */
public record StockShortage(Long productId, String reference, int requested, int available) {
}
