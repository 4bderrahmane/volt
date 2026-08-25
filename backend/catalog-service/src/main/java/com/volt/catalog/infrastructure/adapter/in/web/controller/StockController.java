package com.volt.catalog.infrastructure.adapter.in.web.controller;

import com.volt.catalog.application.port.in.ManageReservationUseCase;
import com.volt.catalog.application.port.in.ReserveStockUseCase;
import com.volt.catalog.infrastructure.adapter.in.web.dto.request.ReserveStockRequest;
import com.volt.catalog.infrastructure.adapter.in.web.dto.request.RestockRequest;
import com.volt.catalog.infrastructure.adapter.in.web.dto.response.ReservationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Validated
@RestController
@RequestMapping("/internal/v1/stock")
public class StockController {

    private final ReserveStockUseCase reserveStock;
    private final ManageReservationUseCase manageReservations;

    public StockController(ReserveStockUseCase reserveStock, ManageReservationUseCase manageReservations) {
        this.reserveStock = reserveStock;
        this.manageReservations = manageReservations;
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveStockRequest request) {
        ReserveStockUseCase.ReservationResult result = reserveStock.reserve(request.toCommand());
        return ResponseEntity
                .created(URI.create("/internal/v1/stock/reservations/" + result.reservationId()))
                .body(ReservationResponse.from(result));
    }

    @PostMapping("/reservations/{reservationId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable @Positive long reservationId) {
        manageReservations.confirm(reservationId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<Void> release(@PathVariable @Positive long reservationId) {
        manageReservations.release(reservationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restock")
    public ResponseEntity<Void> restock(@Valid @RequestBody RestockRequest request) {
        manageReservations.restock(request.orderRef());
        return ResponseEntity.noContent().build();
    }
}
