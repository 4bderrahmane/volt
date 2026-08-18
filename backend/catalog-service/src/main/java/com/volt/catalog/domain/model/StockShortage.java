package com.volt.catalog.domain.model;

/**
 * One line that could not be reserved. Carried in the RFC 7807 problem body so
 * the SPA can name the product that ran out (specification §13 requires a clear
 * message; "insufficient stock" is not clear for a five-line cart).
 */
public record StockShortage(Long productId, String reference, int requested, int available) {
}
