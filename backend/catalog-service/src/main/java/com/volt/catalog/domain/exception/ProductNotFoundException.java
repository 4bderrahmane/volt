package com.volt.catalog.domain.exception;

import java.io.Serial;

public class ProductNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ProductNotFoundException(long productId) {
        super("No product with id " + productId);
    }
}
