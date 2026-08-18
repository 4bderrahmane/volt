package com.volt.catalog.domain.exception;

public class ProductNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProductNotFoundException(long productId) {
        super("No product with id " + productId);
    }
}
