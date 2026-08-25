package com.volt.catalog.infrastructure.adapter.in.web.dto.request;

import com.volt.catalog.application.port.in.ManageProductUseCase;
import com.volt.catalog.domain.model.Unit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * HTTP request DTO for editing the mutable part of a product.
 *
 * <p>The product reference is absent on purpose: the domain treats it as an
 * immutable business key, so the web API must not offer a way to change it.
 */
public record UpdateProductRequest(
        @NotBlank @Size(max = 255) String label,
        String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal priceExclVat,
        @NotNull Unit unit,
        @NotNull @Positive Long categoryId,
        @NotNull @Positive Long brandId) {

    public ManageProductUseCase.UpdateProductCommand toCommand() {
        return new ManageProductUseCase.UpdateProductCommand(
                label,
                description,
                priceExclVat,
                unit,
                categoryId,
                brandId);
    }
}
