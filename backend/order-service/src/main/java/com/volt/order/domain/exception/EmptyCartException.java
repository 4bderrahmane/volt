package com.volt.order.domain.exception;

/** Checkout of an empty cart (specification §F7). Maps to HTTP 422. */
public class EmptyCartException extends RuntimeException {

    /** Exceptions are Serializable by inheritance; pin the id so a
     *  refactor cannot silently change it. Never actually serialised here. */
    private static final long serialVersionUID = 1L;

    public EmptyCartException() {
        super("Cannot place an order from an empty cart");
    }
}
