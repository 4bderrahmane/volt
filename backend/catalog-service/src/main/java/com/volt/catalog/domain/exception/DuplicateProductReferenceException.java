package com.volt.catalog.domain.exception;

import java.io.Serial;

/**
 * Business failure raised when a product reference is already in use.
 * The web exception adapter currently translates it into an HTTP conflict.
 */
public class DuplicateProductReferenceException extends RuntimeException {

    /** Exceptions are Serializable by inheritance; pin the id so a
     *  refactor cannot silently change it. Never actually serialised here. */
    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateProductReferenceException(String reference) {
        super("Product reference already exists: " + reference);
    }
}
