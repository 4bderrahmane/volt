package com.volt.catalog.infrastructure.adapter.in.web.controller;

import com.volt.catalog.application.port.in.ManageReservationUseCase;
import com.volt.catalog.application.port.in.ReserveStockUseCase;
import com.volt.catalog.domain.exception.InsufficientStockException;
import com.volt.catalog.domain.model.StockShortage;
import com.volt.catalog.infrastructure.adapter.in.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockControllerTest {

    @Test
    void reservesStockAndReturnsTheAuthoritativeSnapshot() throws Exception {
        MockMvc mockMvc = mvc(new SuccessfulStockUseCase());

        mockMvc.perform(post("/internal/v1/stock/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderRef":"ORD-1","lines":[{"productId":1,"quantity":2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/internal/v1/stock/reservations/7"))
                .andExpect(jsonPath("$.reservationId").value(7))
                .andExpect(jsonPath("$.lines[0].unitPriceExclVat").value(12.50));
    }

    @Test
    void returnsA409WithStructuredShortages() throws Exception {
        MockMvc mockMvc = mvc(command -> {
            throw new InsufficientStockException(List.of(new StockShortage(1L, "REF-1", 4, 2)));
        });

        mockMvc.perform(post("/internal/v1/stock/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderRef":"ORD-1","lines":[{"productId":1,"quantity":4}]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/insufficient-stock"))
                .andExpect(jsonPath("$.shortages[0].reference").value("REF-1"))
                .andExpect(jsonPath("$.shortages[0].available").value(2));
    }

    @Test
    void restocksByOrderReference() throws Exception {
        MockMvc mockMvc = mvc(new SuccessfulStockUseCase());

        mockMvc.perform(post("/internal/v1/stock/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderRef\":\"ORD-1\"}"))
                .andExpect(status().isNoContent());
    }

    private static MockMvc mvc(ReserveStockUseCase reserve) {
        return MockMvcBuilders
                .standaloneSetup(new StockController(reserve, new NoOpManageReservations()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final class SuccessfulStockUseCase implements ReserveStockUseCase {
        @Override
        public ReservationResult reserve(ReserveStockCommand command) {
            return new ReservationResult(
                    7L,
                    Instant.parse("2026-08-14T10:15:00Z"),
                    List.of(new ReservedLine(1L, "REF-1", "Product", new BigDecimal("12.50"), 2)));
        }
    }

    private static final class NoOpManageReservations implements ManageReservationUseCase {
        @Override
        public void confirm(long reservationId) {
        }

        @Override
        public void release(long reservationId) {
        }

        @Override
        public void restock(String orderRef) {
        }
    }
}
