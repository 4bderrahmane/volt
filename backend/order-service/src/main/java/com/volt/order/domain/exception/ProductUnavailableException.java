package com.volt.order.domain.exception;

/** A cart references a product the catalog can no longer return. */
public class ProductUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ProductUnavailableException(long productId) {
        super("Product " + productId + " is unavailable");
    }
}
