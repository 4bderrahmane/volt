package com.volt.catalog.domain.exception;

import java.io.Serial;

public class DuplicateProductReferenceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateProductReferenceException(String reference) {
        super("Product reference already exists: " + reference);
    }
}
