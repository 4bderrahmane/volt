package com.volt.order.domain.model;

import java.math.BigDecimal;

/**
 * What this service knows about a catalog product: enough to render a cart and
 * to build an order line, and nothing more.
 *
 * <p>Deliberately not a copy of catalog's {@code Product}. Each service owns its
 * own view of shared concepts; a shared DTO module between the two services
 * would couple their release cycles and quietly undo the split the whole project
 * is built around.
 */
public record ProductSnapshot(
        Long productId,
        String reference,
        String label,
        BigDecimal unitPriceExclVat,
        int availableQuantity,
        boolean active) {
}
