package com.volt.order.domain.exception;

/** Maps to HTTP 404. */
public class OrderNotFoundException extends RuntimeException {

    /** Exceptions are Serializable by inheritance; pin the id so a
     *  refactor cannot silently change it. Never actually serialised here. */
    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(long orderId) {
        super("No order with id " + orderId);
    }
}
