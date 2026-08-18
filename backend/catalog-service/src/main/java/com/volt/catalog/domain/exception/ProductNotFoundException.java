package com.volt.catalog.domain.exception;

/**
 * Business failure raised when no product matches an identifier.
 *
 * <p>It contains no HTTP status; the input adapter decides how to present the
 * failure to its caller.
 */
public class ProductNotFoundException extends RuntimeException {

    /** Exceptions are Serializable by inheritance; pin the id so a
     *  refactor cannot silently change it. Never actually serialised here. */
    private static final long serialVersionUID = 1L;

    public ProductNotFoundException(long productId) {
        super("No product with id " + productId);
    }
}
