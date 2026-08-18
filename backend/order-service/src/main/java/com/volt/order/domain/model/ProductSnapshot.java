package com.volt.order.domain.model;

import java.math.BigDecimal;

public record ProductSnapshot(
        Long productId,
        String reference,
        String label,
        BigDecimal unitPriceExclVat,
        int availableQuantity,
        boolean active) {
}
