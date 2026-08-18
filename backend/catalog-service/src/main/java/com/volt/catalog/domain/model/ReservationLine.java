package com.volt.catalog.domain.model;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(of = {"productId", "quantity"})
public final class ReservationLine {

    private final Long id;
    private final Long productId;
    private final int quantity;

    public ReservationLine(Long id, Long productId, int quantity) {
        if (id != null && id < 1) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (productId == null || productId < 1) {
            throw new IllegalArgumentException("productId must be positive");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("reservation quantity must be positive");
        }
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
    }

    public static ReservationLine create(Long productId, int quantity) {
        return new ReservationLine(null, productId, quantity);
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ReservationLine that)) return false;

        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
