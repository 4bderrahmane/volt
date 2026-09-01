package com.volt.catalog.infrastructure.adapter.in.web.dto.response;

import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.Unit;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
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

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getReference(),
                product.getLabel(),
                product.getDescription(),
                product.getPriceExclVat(),
                product.getUnit(),
                product.getStockQuantity(),
                product.isActive(),
                product.getCategoryId(),
                product.getBrandId(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
