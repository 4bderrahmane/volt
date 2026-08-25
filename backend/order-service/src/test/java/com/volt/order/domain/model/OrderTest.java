package com.volt.order.domain.model;

import com.volt.order.domain.exception.IllegalStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {
    private static final Instant NOW = Instant.parse("2026-08-17T10:00:00Z");

    @Test
    void followsTheHappyPathAndMakesTerminalStatesFinal() {
        OrderLine line = OrderLine.of(5L, "REF-5", "Cable", new BigDecimal("12.50"), 2);
        Order order = Order.place("ORD-2026-000001", UUID.randomUUID(), 99L,
                OrderTotals.calculate(List.of(line), VatRate.STANDARD), List.of(line), NOW);

        order.confirm(NOW.plusSeconds(1));
        order.changeStatus(OrderStatus.SHIPPED, NOW.plusSeconds(2));
        order.changeStatus(OrderStatus.DELIVERED, NOW.plusSeconds(3));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getReservationId()).isNull();
        assertThatThrownBy(() -> order.changeStatus(OrderStatus.CREATED, NOW.plusSeconds(4)))
                .isInstanceOf(IllegalStatusTransitionException.class);
    }
}
