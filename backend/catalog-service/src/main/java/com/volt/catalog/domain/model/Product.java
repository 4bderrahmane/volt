package com.volt.catalog.domain.model;

import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Getter
@ToString(of = {"id", "reference"})
public final class Product {

    private final Long id;
    private final String reference;
    private String label;
    private String description;
    private BigDecimal priceExclVat;
    private Unit unit;
    private int stockQuantity;
    private boolean active;
    private Long categoryId;
    private Long brandId;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * Full constructor, used to reconstitute a product from persistence. The
     * persistence adapter is its only caller; application code creating a new
     * product uses {@link #create}.
     *
     * <p>Deliberately not {@code @AllArgsConstructor}: a generated constructor
     * silently changes its parameter order when a field is reordered, and every
     * call site keeps compiling because the types line up. Writing it out means
     * a field reorder is a compile error, not a data-corruption bug.
     */
    public Product(
            Long id,
            String reference,
            String label,
            String description,
            BigDecimal priceExclVat,
            Unit unit,
            int stockQuantity,
            boolean active,
            Long categoryId,
            Long brandId,
            Instant createdAt,
            Instant updatedAt) {

        if (id != null && id < 1) throw new IllegalArgumentException("id must be positive");
        validateEditableFields(label, priceExclVat, unit, categoryId, brandId);
        if (stockQuantity < 0) throw new IllegalArgumentException("stock quantity must not be negative");
        Instant requiredCreatedAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        Instant requiredUpdatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (requiredUpdatedAt.isBefore(requiredCreatedAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }

        this.id = id;
        this.reference = requireText(reference, "reference", 64);
        this.label = label.trim();
        this.description = normalizeDescription(description);
        this.priceExclVat = priceExclVat;
        this.unit = unit;
        this.stockQuantity = stockQuantity;
        this.active = active;
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.createdAt = requiredCreatedAt;
        this.updatedAt = requiredUpdatedAt;
    }

    /**
     * Creates a product that has not been persisted yet.
     */
    public static Product create(
            String reference,
            String label,
            String description,
            BigDecimal priceExclVat,
            Unit unit,
            int initialStock,
            Long categoryId,
            Long brandId,
            Instant now) {
        return new Product(null, reference, label, description, priceExclVat, unit, initialStock, true, categoryId, brandId, now, now);
    }

    public void deactivate(Instant now) {
        requireMutationTime(now);
        this.active = false;
        this.updatedAt = now;
    }

    /**
     * Validates all new values before mutating. The reference is deliberately
     * absent because it is the immutable business key.
     */
    public void applyUpdate(
            String newLabel,
            String newDescription,
            BigDecimal newPriceExclVat,
            Unit newUnit,
            Long newCategoryId,
            Long newBrandId,
            Instant now) {
        validateEditableFields(newLabel, newPriceExclVat, newUnit, newCategoryId, newBrandId);
        requireMutationTime(now);

        this.label = newLabel.trim();
        this.description = normalizeDescription(newDescription);
        this.priceExclVat = newPriceExclVat;
        this.unit = newUnit;
        this.categoryId = newCategoryId;
        this.brandId = newBrandId;
        this.updatedAt = now;
    }

    public boolean canFulfil(int requested, int alreadyReserved) {
        if (requested < 1) throw new IllegalArgumentException("requested quantity must be positive");
        if (alreadyReserved < 0) throw new IllegalArgumentException("reserved quantity must not be negative");

        return active && (long) stockQuantity - alreadyReserved >= requested;
    }

    public void decreaseStock(int quantity, Instant now) {
        requirePositiveQuantity(quantity);
        requireMutationTime(now);
        if (stockQuantity < quantity) {
            throw new IllegalArgumentException("stock quantity must not become negative");
        }
        stockQuantity -= quantity;
        updatedAt = now;
    }

    public void increaseStock(int quantity, Instant now) {
        requirePositiveQuantity(quantity);
        requireMutationTime(now);
        try {
            stockQuantity = Math.addExact(stockQuantity, quantity);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("stock quantity is too large", exception);
        }
        updatedAt = now;
    }

    private static void validateEditableFields(
            String label,
            BigDecimal priceExclVat,
            Unit unit,
            Long categoryId,
            Long brandId) {
        requireText(label, "label", 255);
        Objects.requireNonNull(priceExclVat, "priceExclVat must not be null");
        if (priceExclVat.signum() < 0) {
            throw new IllegalArgumentException("priceExclVat must not be negative");
        }
        if (priceExclVat.scale() > 2 || priceExclVat.precision() - priceExclVat.scale() > 8) {
            throw new IllegalArgumentException("priceExclVat must fit NUMERIC(10,2)");
        }
        Objects.requireNonNull(unit, "unit must not be null");
        requirePositive(categoryId, "categoryId");
        requirePositive(brandId, "brandId");
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void requirePositiveQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    private void requireMutationTime(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (now.isBefore(createdAt)) {
            throw new IllegalArgumentException("now must not be before createdAt");
        }
    }

    private static String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Product that)) return false;

        return id != null && id.equals(that.id);
    }


    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
