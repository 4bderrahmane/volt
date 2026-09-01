package com.volt.order.application.usecase;

import com.volt.order.application.port.in.ChangeOrderStatusUseCase;
import com.volt.order.application.port.out.CatalogClientPort;
import com.volt.order.application.port.out.OrderRepositoryPort;
import com.volt.order.domain.exception.OrderNotFoundException;
import com.volt.order.domain.model.Order;
import com.volt.order.domain.model.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Shared serialization point for checkout and administrative status transitions. */
@Service
public class ChangeOrderStatusService implements ChangeOrderStatusUseCase {
    private final OrderRepositoryPort orders;
    private final CatalogClientPort catalog;
    private final Clock clock;

    public ChangeOrderStatusService(OrderRepositoryPort orders, CatalogClientPort catalog, Clock clock) {
        this.orders = orders;
        this.catalog = catalog;
        this.clock = clock;
    }

    /**
     * Holds the row lock across the catalog side effect and status update so
     * concurrent, non-commutative transitions cannot both succeed. This
     * intentionally holds a database row across an HTTP call; the catalog
     * timeout bounds the lock duration.
     */
    @Override
    @Transactional
    public Order changeStatus(long orderId, OrderStatus target) {
        Order order = orders.findByIdForUpdate(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderStatus previous = order.getStatus();
        if (!previous.canTransitionTo(target)) {
            order.changeStatus(target, clock.instant());
        }
        if (target == OrderStatus.CONFIRMED && order.getReservationId() != null) {
            catalog.confirmReservation(order.getReservationId());
        } else if (target == OrderStatus.CANCELLED && previous == OrderStatus.CREATED) {
            catalog.releaseReservation(order.getReservationId());
        } else if (target == OrderStatus.CANCELLED && previous == OrderStatus.CONFIRMED) {
            catalog.restock(order.getNumber());
        }
        order.changeStatus(target, clock.instant());
        return orders.save(order);
    }
}
