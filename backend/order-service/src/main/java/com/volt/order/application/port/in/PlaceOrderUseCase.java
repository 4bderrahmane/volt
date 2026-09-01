package com.volt.order.application.port.in;

import com.volt.order.domain.model.Order;

import java.util.UUID;

public interface PlaceOrderUseCase {

    Order placeOrder(UUID customerId);
}
