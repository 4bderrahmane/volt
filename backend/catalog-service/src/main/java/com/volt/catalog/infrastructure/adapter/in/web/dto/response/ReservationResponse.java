package com.volt.catalog.infrastructure.adapter.in.web.dto.response;

import com.volt.catalog.application.port.in.ReserveStockUseCase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReservationResponse(
        Long reservationId,
        Instant expiresAt,
        List<ReservedLineResponse> lines) {

    public ReservationResponse {
        lines = List.copyOf(lines);
    }

    public static ReservationResponse from(ReserveStockUseCase.ReservationResult result) {
        return new ReservationResponse(
                result.reservationId(),
                result.expiresAt(),
                result.lines().stream().map(ReservedLineResponse::from).toList());
    }

    public record ReservedLineResponse(
            Long productId,
            String reference,
            String label,
            BigDecimal unitPriceExclVat,
            int quantity) {

        static ReservedLineResponse from(ReserveStockUseCase.ReservedLine line) {
            return new ReservedLineResponse(
                    line.productId(),
                    line.reference(),
                    line.label(),
                    line.unitPriceExclVat(),
                    line.quantity());
        }
    }
}
