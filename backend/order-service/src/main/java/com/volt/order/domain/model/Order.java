package com.volt.order.domain.model;

import com.volt.order.domain.exception.IllegalStatusTransitionException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Aggregate root for an immutable set of checkout snapshots. */
@Getter
@ToString(of = {"id", "number", "status"})
public final class Order {
    private final Long id;
    private final String number;
    private final UUID customerId;
    private OrderStatus status;
    private Long reservationId;
    private final OrderTotals totals;
    @Getter(AccessLevel.NONE)
    private final List<OrderLine> lines = new ArrayList<>();
    private final Instant createdAt;
    private Instant updatedAt;

    private Order(Long id, String number, UUID customerId, OrderStatus status, Long reservationId,
                  OrderTotals totals, List<OrderLine> lines, Instant createdAt, Instant updatedAt) {
        this.id = validId(id, "order id");
        this.number = text(number, 32, "order number");
        this.customerId = Objects.requireNonNull(customerId, "customer id is required");
        this.status = Objects.requireNonNull(status, "order status is required");
        this.reservationId = validId(reservationId, "reservation id");
        this.totals = Objects.requireNonNull(totals, "order totals are required");
        Objects.requireNonNull(lines, "order lines are required");
        if (lines.isEmpty()) throw new IllegalArgumentException("order must contain at least one line");
        this.lines.addAll(lines);
        validateLines(this.lines, totals);
        this.createdAt = Objects.requireNonNull(createdAt, "order creation time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "order update time is required");
        if (updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("order update time cannot precede creation time");
        validateReservationState();
    }

    public static Order place(String number, UUID customerId, Long reservationId,
                              OrderTotals totals, List<OrderLine> lines, Instant now) {
        Instant timestamp = Objects.requireNonNull(now, "order creation time is required");
        return new Order(null, number, customerId, OrderStatus.CREATED, reservationId, totals, lines, timestamp, timestamp);
    }

    public static Order rehydrate(Long id, String number, UUID customerId, OrderStatus status,
                                  Long reservationId, OrderTotals totals, List<OrderLine> lines,
                                  Instant createdAt, Instant updatedAt) {
        if (id == null) throw new IllegalArgumentException("persisted order id is required");
        return new Order(id, number, customerId, status, reservationId, totals, lines, createdAt, updatedAt);
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public boolean isOwnedBy(UUID candidate) {
        return customerId.equals(candidate);
    }

    public void confirm(Instant now) {
        transition(OrderStatus.CONFIRMED, now);
        reservationId = null;
    }

    public void changeStatus(OrderStatus target, Instant now) {
        if (target == OrderStatus.CONFIRMED) {
            confirm(now);
            return;
        }
        transition(target, now);
        if (target == OrderStatus.CANCELLED) reservationId = null;
    }

    private void transition(OrderStatus target, Instant now) {
        if (!status.canTransitionTo(target)) throw new IllegalStatusTransitionException(status, target);
        Instant timestamp = Objects.requireNonNull(now, "order update time is required");
        if (timestamp.isBefore(updatedAt)) throw new IllegalArgumentException("order update time cannot move backwards");
        status = target;
        updatedAt = timestamp;
    }

    private void validateReservationState() {
        if (status == OrderStatus.CREATED && reservationId == null) {
            throw new IllegalArgumentException("created order requires a reservation id");
        }
        if (status != OrderStatus.CREATED && reservationId != null) {
            throw new IllegalArgumentException("only a created order may retain a reservation id");
        }
    }

    private static void validateLines(List<OrderLine> lines, OrderTotals totals) {
        Set<Long> products = new HashSet<>();
        BigDecimal sum = new BigDecimal("0.00");
        for (OrderLine line : lines) {
            Objects.requireNonNull(line, "order line is required");
            if (!products.add(line.getProductId())) throw new IllegalArgumentException("order cannot contain duplicate products");
            sum = sum.add(line.getLineTotalExclVat());
        }
        if (sum.compareTo(totals.totalExclVat()) != 0) {
            throw new IllegalArgumentException("order total excluding VAT must equal the sum of its lines");
        }
    }

    private static String text(String value, int max, String name) {
        Objects.requireNonNull(value, name + " is required");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > max) throw new IllegalArgumentException(name + " must contain 1 to " + max + " characters");
        return normalized;
    }

    private static Long validId(Long id, String name) {
        if (id != null && id < 1) throw new IllegalArgumentException(name + " must be positive when present");
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof Order that && id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
