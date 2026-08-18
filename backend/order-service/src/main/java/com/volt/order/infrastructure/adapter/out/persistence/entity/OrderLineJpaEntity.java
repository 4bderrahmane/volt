package com.volt.order.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Persistence representation of an immutable order-line snapshot. */
@Entity
@Table(name = "order_line")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_reference", nullable = false, length = 64)
    private String productReference;

    @Column(name = "product_label", nullable = false, length = 255)
    private String productLabel;

    @Column(name = "unit_price_excl_vat", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceExclVat;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "line_total_excl_vat", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotalExclVat;
}
