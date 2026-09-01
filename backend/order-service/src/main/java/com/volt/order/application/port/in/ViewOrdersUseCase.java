package com.volt.order.application.port.in;

import com.volt.order.domain.model.Order;

import java.util.List;
import java.util.UUID;

public interface ViewOrdersUseCase {

    List<Order> listForCustomer(UUID customerId);

    Order getForCustomer(UUID customerId, long orderId);
}
