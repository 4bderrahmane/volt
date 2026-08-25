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

/**
 * The single serialization point for every order status transition (ADR-0003).
 *
 * <p>Both the administrative transitions of §F9 and checkout's own
 * {@code CREATED -> CONFIRMED} finalization run through here, so there is
 * exactly one place where an order's status and its catalog side effect are
 * decided together.
 */
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
     * Locks the order, then performs its catalog side effect and stores the new
     * status in one local transaction.
     *
     * <p>The lock is taken <em>before</em> the catalog call, not after, and that
     * ordering is the whole point. Reading the status outside a lock leaves a
     * window in which a concurrent cancellation commits between the read and
     * the catalog call: both callers see {@code CONFIRMED}, one restocks and one
     * ships, and stock no longer matches the order. Holding the row until
     * commit forces the second caller to re-read the committed status and be
     * rejected by {@link OrderStatus#canTransitionTo} instead.
     *
     * <p>This does hold a database row across an HTTP call, which is normally
     * worth avoiding. It is accepted here because the lock is per-order — never
     * contended except by concurrent operators on the same order — and because
     * the alternative, releasing it first, reintroduces exactly the race the
     * lock exists to close. The catalog client's timeout bounds how long the
     * row can stay held.
     */
    @Override
    @Transactional
    public Order changeStatus(long orderId, OrderStatus target) {
        Order order = orders.findByIdForUpdate(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderStatus previous = order.getStatus();
        if (!previous.canTransitionTo(target)) {
            // Fail before touching the catalog: an illegal transition must not
            // leave a confirmed reservation or a restock behind it.
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
