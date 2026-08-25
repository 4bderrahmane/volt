package com.volt.order.application.port.in;

import com.volt.order.domain.model.Order;
import com.volt.order.domain.model.OrderStatus;

/**
 * Specification §F9, §6.2 — ADMIN status changes.
 *
 * <p>Cancellation has a stock consequence the specification does not mention:
 * cancelling a CREATED order releases its reservation, cancelling a CONFIRMED
 * one calls restock. Without that, every cancellation destroys inventory.
 */
public interface ChangeOrderStatusUseCase {

    Order changeStatus(long orderId, OrderStatus target);
}
