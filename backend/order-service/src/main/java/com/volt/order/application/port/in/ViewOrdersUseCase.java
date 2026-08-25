package com.volt.order.application.port.in;

import com.volt.order.domain.model.Order;

import java.util.List;
import java.util.UUID;

/**
 * Specification §F8 — history and detail.
 *
 * <p>{@code customerId} is a filter, not a convenience: loading an order by id
 * alone and checking ownership afterwards is how one customer reads another's
 * orders when someone later forgets the check.
 */
public interface ViewOrdersUseCase {

    List<Order> listForCustomer(UUID customerId);

    Order getForCustomer(UUID customerId, long orderId);
}
