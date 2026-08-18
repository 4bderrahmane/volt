package com.volt.order.domain.exception;

/**
 * The catalog could not be reached, or failed, after retries (ADR-0005).
 *
 * <p>Distinct from {@link InsufficientStockException}: that one means the
 * customer cannot have the goods, this one means we do not know. Collapsing the
 * two would tell a customer their order failed for lack of stock when in fact a
 * container was restarting. Maps to HTTP 503.
 */
public class CatalogUnavailableException extends RuntimeException {

    /** Exceptions are Serializable by inheritance; pin the id so a
     *  refactor cannot silently change it. Never actually serialised here. */
    private static final long serialVersionUID = 1L;

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
