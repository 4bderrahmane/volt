package com.volt.order.application.port.out;

/**
 * Generates the human-facing order number (specification §5.2:
 * {@code ORD-2026-000123}, unique).
 *
 * <p>A port rather than a helper because the only correct implementation uses a
 * database sequence — generating it in Java means two concurrent checkouts can
 * produce the same number, and the unique constraint then fails one of them
 * after the reservation has already been taken.
 */
public interface OrderNumberGeneratorPort {

    String nextOrderNumber();
}
