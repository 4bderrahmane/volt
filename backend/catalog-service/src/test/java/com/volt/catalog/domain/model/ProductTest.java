package com.volt.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure domain test. It calls business behaviour directly and therefore needs
 * no controller, use case, adapter, Spring context, or database.
 */
class ProductTest {

    @Test
    void checksAvailableStockWithoutAnyFramework() {
        Product product = product(10);

        assertTrue(product.canFulfil(6, 4));
        assertFalse(product.canFulfil(7, 4));
    }

    @Test
    void rejectsNegativeStock() {
        assertThrows(IllegalArgumentException.class, () -> product(-1));
    }

    @Test
    void changesStockWithoutAllowingUnderflow() {
        Product product = product(10);
        Instant later = Instant.parse("2026-08-14T10:01:00Z");

        product.decreaseStock(4, later);
        product.increaseStock(2, later.plusSeconds(1));

        assertEquals(8, product.getStockQuantity());
        assertThrows(IllegalArgumentException.class, () -> product.decreaseStock(9, later.plusSeconds(2)));
    }

    @Test
    void rejectsANullDeactivationTime() {
        Product product = product(10);

        assertThrows(NullPointerException.class, () -> product.deactivate(null));
        assertTrue(product.isActive());
    }

    private static Product product(int stockQuantity) {
        Instant now = Instant.parse("2026-08-14T10:00:00Z");
        return Product.create(
                "REF-001",
                "Test product",
                null,
                new BigDecimal("12.50"),
                Unit.ITEM,
                stockQuantity,
                1L,
                1L,
                now);
    }
}
