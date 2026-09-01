package com.volt.order.application.port.out;

import com.volt.order.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {

    Optional<Order> findById(long orderId);

    // Acquires a pessimistic lock held by the caller's transaction.
    Optional<Order> findByIdForUpdate(long orderId);

    Optional<Order> findByIdAndCustomerId(long orderId, UUID customerId);

    List<Order> findByCustomerId(UUID customerId);

    Order save(Order order);
}
