package com.volt.catalog.application.port.in;

import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.Unit;

import java.math.BigDecimal;

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
