package com.volt.order.application.usecase;

import com.volt.order.application.port.in.ViewOrdersUseCase;
import com.volt.order.application.port.out.OrderRepositoryPort;
import com.volt.order.domain.exception.OrderNotFoundException;
import com.volt.order.domain.model.Order;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderQueryService implements ViewOrdersUseCase {
    private final OrderRepositoryPort orders;

    public OrderQueryService(OrderRepositoryPort orders) {
        this.orders = orders;
    }

    @Override
    public List<Order> listForCustomer(UUID customerId) {
        return orders.findByCustomerId(customerId);
    }

    @Override
    public Order getForCustomer(UUID customerId, long orderId) {
        return orders.findByIdAndCustomerId(orderId, customerId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
