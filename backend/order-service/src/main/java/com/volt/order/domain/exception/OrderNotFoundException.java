package com.volt.order.domain.exception;

public class OrderNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(long orderId) {
        super("No order with id " + orderId);
    }
}
