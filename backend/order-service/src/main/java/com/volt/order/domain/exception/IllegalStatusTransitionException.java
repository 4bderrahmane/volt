package com.volt.order.domain.exception;

import com.volt.order.domain.model.OrderStatus;

import java.io.Serial;

/**
 * Specification §6.2 exposes PATCH /orders/{id}/status to ADMIN with no stated
 * constraints. Without a transition table an admin can move a DELIVERED order
 * back to CREATED. Maps to HTTP 409.
 */
public class IllegalStatusTransitionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public IllegalStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot move an order from " + from + " to " + to);
    }
}
