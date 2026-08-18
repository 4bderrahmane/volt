package com.volt.order.domain.model;

import com.volt.order.domain.exception.CartLineNotFoundException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@ToString(of = {"id", "customerId"})
public final class Cart {
    private final Long id;
    private final UUID customerId;
    @Getter(AccessLevel.NONE)
    private final List<CartLine> lines = new ArrayList<>();
    private final Instant createdAt;
    private Instant updatedAt;

    private Cart(Long id, UUID customerId, List<CartLine> lines, Instant createdAt, Instant updatedAt) {
        this.id = validId(id);
        this.customerId = Objects.requireNonNull(customerId, "customer id is required");
        Objects.requireNonNull(lines, "cart lines are required");
        this.lines.addAll(lines);
        requireUniqueProducts(this.lines);
        this.createdAt = Objects.requireNonNull(createdAt, "cart creation time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "cart update time is required");
        if (updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("cart update time cannot precede creation time");
    }

    public static Cart empty(UUID customerId, Instant now) {
        Instant timestamp = Objects.requireNonNull(now, "cart creation time is required");
        return new Cart(null, customerId, List.of(), timestamp, timestamp);
    }

    public static Cart rehydrate(Long id, UUID customerId, List<CartLine> lines, Instant createdAt, Instant updatedAt) {
        if (id == null) throw new IllegalArgumentException("persisted cart id is required");
        return new Cart(id, customerId, lines, createdAt, updatedAt);
    }

    public List<CartLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public void addLine(Long productId, int quantity, Instant now) {
        Instant timestamp = nextTimestamp(now);
        lines.stream().filter(line -> line.getProductId().equals(productId)).findFirst()
                .ifPresentOrElse(line -> line.addQuantity(quantity), () -> lines.add(CartLine.create(productId, quantity)));
        updatedAt = timestamp;
    }

    public void changeLineQuantity(Long cartLineId, int quantity, Instant now) {
        Instant timestamp = nextTimestamp(now);
        line(cartLineId).changeQuantity(quantity);
        updatedAt = timestamp;
    }

    public void removeLine(Long cartLineId, Instant now) {
        Instant timestamp = nextTimestamp(now);
        lines.remove(line(cartLineId));
        updatedAt = timestamp;
    }

    public void clear(Instant now) {
        Instant timestamp = nextTimestamp(now);
        lines.clear();
        updatedAt = timestamp;
    }

    private CartLine line(Long cartLineId) {
        if (cartLineId == null || cartLineId < 1) throw new IllegalArgumentException("cart line id must be positive");
        return lines.stream().filter(candidate -> cartLineId.equals(candidate.getId())).findFirst()
                .orElseThrow(() -> new CartLineNotFoundException(cartLineId));
    }

    private Instant nextTimestamp(Instant now) {
        Instant timestamp = Objects.requireNonNull(now, "cart update time is required");
        if (timestamp.isBefore(updatedAt)) throw new IllegalArgumentException("cart update time cannot move backwards");
        return timestamp;
    }

    private static void requireUniqueProducts(List<CartLine> lines) {
        Set<Long> productIds = new HashSet<>();
        for (CartLine line : lines) {
            Objects.requireNonNull(line, "cart line is required");
            if (!productIds.add(line.getProductId())) throw new IllegalArgumentException("cart cannot contain duplicate products");
        }
    }

    private static Long validId(Long id) {
        if (id != null && id < 1) throw new IllegalArgumentException("cart id must be positive when present");
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof Cart that && id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
