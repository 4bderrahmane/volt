package com.volt.catalog.infrastructure.adapter.in.web.dto.request;

import com.volt.catalog.application.port.in.ReserveStockUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReserveStockRequest(@NotBlank @Size(max = 64) String orderRef, @NotEmpty List<@Valid RequestedLine> lines) {

    public ReserveStockUseCase.ReserveStockCommand toCommand() {
        return new ReserveStockUseCase.ReserveStockCommand(
                orderRef,
                lines.stream().map(RequestedLine::toCommand).toList());
    }

    public record RequestedLine(@NotNull @Positive Long productId, @Positive int quantity) {
        ReserveStockUseCase.RequestedLine toCommand() {
            return new ReserveStockUseCase.RequestedLine(productId, quantity);
        }
    }
}
