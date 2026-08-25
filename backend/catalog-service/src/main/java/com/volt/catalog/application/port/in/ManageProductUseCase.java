package com.volt.catalog.application.port.in;

import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.Unit;

import java.math.BigDecimal;

/**
 * Incoming port describing the product-changing operations offered by the core.
 *
 * <p>The nested command records contain application input, not HTTP details.
 * A controller maps request DTOs into these commands before calling the port.
 */
public interface ManageProductUseCase {

    Product create(CreateProductCommand command);

    Product update(long productId, UpdateProductCommand command);

    void deactivate(long productId);

    record CreateProductCommand(
            String reference,
            String label,
            String description,
            BigDecimal priceExclVat,
            Unit unit,
            int initialStock,
            Long categoryId,
            Long brandId) {
    }

    record UpdateProductCommand(
            String label,
            String description,
            BigDecimal priceExclVat,
            Unit unit,
            Long categoryId,
            Long brandId) {
    }
}
