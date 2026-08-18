package com.volt.order.domain.model;

import lombok.Getter;
import lombok.ToString;

/** A quantity of one catalog product owned by a {@link Cart}. */
@Getter
@ToString(of = {"productId", "quantity"})
public final class CartLine {
    private final Long id;
    private final Long productId;
    private int quantity;

    private CartLine(Long id, Long productId, int quantity) {
        this.id = validId(id, "cart line id");
        this.productId = positive(productId, "product id");
        this.quantity = positive(quantity, "cart line quantity");
    }

    public static CartLine create(Long productId, int quantity) {
        return new CartLine(null, productId, quantity);
    }

    public static CartLine rehydrate(Long id, Long productId, int quantity) {
        if (id == null) throw new IllegalArgumentException("persisted cart line id is required");
        return new CartLine(id, productId, quantity);
    }

    void changeQuantity(int quantity) {
        this.quantity = positive(quantity, "cart line quantity");
    }

    void addQuantity(int extra) {
        if (extra < 1) throw new IllegalArgumentException("added quantity must be positive");
        this.quantity = Math.addExact(this.quantity, extra);
    }

    private static int positive(int value, String name) {
        if (value < 1) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private static Long positive(Long value, String name) {
        if (value == null || value < 1) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private static Long validId(Long value, String name) {
        if (value != null && value < 1) throw new IllegalArgumentException(name + " must be positive when present");
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof CartLine that && id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
