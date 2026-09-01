package com.volt.catalog.infrastructure.adapter.in.web.dto.request;

import com.volt.catalog.application.port.in.ManageProductUseCase;
import com.volt.catalog.domain.model.Unit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 64) String reference,
        @NotBlank @Size(max = 255) String label,
        String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal priceExclVat,
        @NotNull Unit unit,
        @NotNull @PositiveOrZero Integer initialStock,
        @NotNull @Positive Long categoryId,
        @NotNull @Positive Long brandId) {

    public ManageProductUseCase.CreateProductCommand toCommand() {
        return new ManageProductUseCase.CreateProductCommand(
                reference,
                label,
                description,
                priceExclVat,
                unit,
                initialStock,
                categoryId,
                brandId);
    }
}
