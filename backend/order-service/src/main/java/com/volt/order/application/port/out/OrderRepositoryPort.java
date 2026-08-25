package com.volt.order.application.port.out;

import com.volt.order.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Order persistence. No Spring Data types cross this boundary. */
public interface OrderRepositoryPort {

    Optional<Order> findById(long orderId);

    /**
     * Reads the order under a pessimistic row lock held until the surrounding
     * transaction commits.
     *
     * <p>Every status change has a catalog side effect — release, confirm or
     * restock — and those side effects are not commutative. Two administrators
     * cancelling and shipping the same order concurrently would otherwise both
     * read {@code CONFIRMED}, both pass the transition check, and issue two
     * conflicting catalog calls against one reservation. The lock makes the
     * read-decide-write cycle serial, so the second caller observes the first
     * one's outcome and fails the transition instead of racing it.
     */
    Optional<Order> findByIdForUpdate(long orderId);

    Optional<Order> findByIdAndCustomerId(long orderId, UUID customerId);

    List<Order> findByCustomerId(UUID customerId);

    Order save(Order order);
}
