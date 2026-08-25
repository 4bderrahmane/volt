package com.volt.order.infrastructure.adapter.in.web.dto.response;

import com.volt.order.domain.model.Order;
import com.volt.order.domain.model.OrderLine;
import com.volt.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, String number, OrderStatus status, TotalsResponse totals,
                            List<Line> lines, Instant createdAt, Instant updatedAt) {
    public OrderResponse {
        lines = List.copyOf(lines);
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getNumber(), order.getStatus(), TotalsResponse.from(order.getTotals()),
                order.getLines().stream().map(Line::from).toList(), order.getCreatedAt(), order.getUpdatedAt());
    }

    public record Line(Long id, Long productId, String productReference, String productLabel,
                       BigDecimal unitPriceExclVat, int quantity, BigDecimal lineTotalExclVat) {
        static Line from(OrderLine line) {
            return new Line(line.getId(), line.getProductId(), line.getProductReference(), line.getProductLabel(),
                    line.getUnitPriceExclVat(), line.getQuantity(), line.getLineTotalExclVat());
        }
    }
}
