package com.volt.order.domain.model;

import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Getter
@ToString(of = {"productReference", "quantity"})
public final class OrderLine {
    private final Long id;
    private final Long productId;
    private final String productReference;
    private final String productLabel;
    private final BigDecimal unitPriceExclVat;
    private final int quantity;
    private final BigDecimal lineTotalExclVat;

    private OrderLine(Long id, Long productId, String reference, String label,
                      BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
        this.id = validId(id);
        this.productId = positive(productId, "product id");
        this.productReference = text(reference, 64, "product reference");
        this.productLabel = text(label, 255, "product label");
        this.unitPriceExclVat = money(unitPrice, "unit price");
        if (quantity < 1) throw new IllegalArgumentException("order line quantity must be positive");
        this.quantity = quantity;
        BigDecimal expected = this.unitPriceExclVat.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        this.lineTotalExclVat = money(lineTotal, "line total");
        if (this.lineTotalExclVat.compareTo(expected) != 0) {
            throw new IllegalArgumentException("line total must equal unit price multiplied by quantity");
        }
    }

    public static OrderLine of(Long productId, String reference, String label, BigDecimal unitPrice, int quantity) {
        BigDecimal normalizedPrice = money(unitPrice, "unit price");
        BigDecimal total = normalizedPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        return new OrderLine(null, productId, reference, label, normalizedPrice, quantity, total);
    }

    public static OrderLine rehydrate(Long id, Long productId, String reference, String label,
                                      BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
        if (id == null) throw new IllegalArgumentException("persisted order line id is required");
        return new OrderLine(id, productId, reference, label, unitPrice, quantity, lineTotal);
    }

    static BigDecimal money(BigDecimal value, String name) {
        Objects.requireNonNull(value, name + " is required");
        if (value.signum() < 0) throw new IllegalArgumentException(name + " must not be negative");
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String text(String value, int max, String name) {
        Objects.requireNonNull(value, name + " is required");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > max) {
            throw new IllegalArgumentException(name + " must contain 1 to " + max + " characters");
        }
        return normalized;
    }

    private static Long positive(Long value, String name) {
        if (value == null || value < 1) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private static Long validId(Long id) {
        if (id != null && id < 1) throw new IllegalArgumentException("order line id must be positive when present");
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof OrderLine that && id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
