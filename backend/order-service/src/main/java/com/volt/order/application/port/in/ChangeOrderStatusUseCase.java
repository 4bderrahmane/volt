package com.volt.order.application.port.in;

import com.volt.order.domain.model.Order;
import com.volt.order.domain.model.OrderStatus;

public interface ChangeOrderStatusUseCase {

    Order changeStatus(long orderId, OrderStatus target);
}
