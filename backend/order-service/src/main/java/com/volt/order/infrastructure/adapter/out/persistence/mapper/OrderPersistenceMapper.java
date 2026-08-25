package com.volt.order.infrastructure.adapter.out.persistence.mapper;

import com.volt.order.domain.model.Order;
import com.volt.order.domain.model.OrderLine;
import com.volt.order.domain.model.OrderTotals;
import com.volt.order.infrastructure.adapter.out.persistence.entity.OrderJpaEntity;
import com.volt.order.infrastructure.adapter.out.persistence.entity.OrderLineJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderPersistenceMapper {
    public Order toDomain(OrderJpaEntity entity) {
        List<OrderLine> lines = entity.getLines().stream()
                .map(line -> OrderLine.rehydrate(
                        line.getId(), line.getProductId(), line.getProductReference(), line.getProductLabel(),
                        line.getUnitPriceExclVat(), line.getQuantity(), line.getLineTotalExclVat()))
                .toList();
        return Order.rehydrate(
                entity.getId(), entity.getNumber(), entity.getCustomerId(), entity.getStatus(), entity.getReservationId(),
                new OrderTotals(entity.getTotalExclVat(), entity.getVatAmount(), entity.getTotalInclVat()),
                lines, entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public OrderJpaEntity newEntity(Order order) {
        OrderJpaEntity entity = OrderJpaEntity.builder()
                .number(order.getNumber())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .reservationId(order.getReservationId())
                .totalExclVat(order.getTotals().totalExclVat())
                .vatAmount(order.getTotals().vatAmount())
                .totalInclVat(order.getTotals().totalInclVat())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
        for (OrderLine line : order.getLines()) {
            entity.getLines().add(OrderLineJpaEntity.builder()
                    .order(entity)
                    .productId(line.getProductId())
                    .productReference(line.getProductReference())
                    .productLabel(line.getProductLabel())
                    .unitPriceExclVat(line.getUnitPriceExclVat())
                    .quantity(line.getQuantity())
                    .lineTotalExclVat(line.getLineTotalExclVat())
                    .build());
        }
        return entity;
    }

    public void update(Order order, OrderJpaEntity entity) {
        entity.setStatus(order.getStatus());
        entity.setReservationId(order.getReservationId());
        entity.setUpdatedAt(order.getUpdatedAt());
    }
}
