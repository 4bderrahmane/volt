package com.volt.order.domain.exception;

public class EmptyCartException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmptyCartException() {
        super("Cannot place an order from an empty cart");
    }
}
