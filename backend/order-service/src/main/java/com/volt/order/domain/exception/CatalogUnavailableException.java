package com.volt.order.domain.exception;

public class CatalogUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
