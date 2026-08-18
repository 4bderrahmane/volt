package com.volt.order.domain.exception;

import com.volt.order.domain.model.OrderStatus;

import java.io.Serial;

public class IllegalStatusTransitionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public IllegalStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot move an order from " + from + " to " + to);
    }
}
