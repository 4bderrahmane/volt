package com.volt.order.infrastructure.adapter.out.client;

import tools.jackson.databind.ObjectMapper;
import com.volt.order.application.port.out.CatalogClientPort;
import com.volt.order.domain.exception.CatalogUnavailableException;
import com.volt.order.domain.exception.InsufficientStockException;
import com.volt.order.domain.model.ProductSnapshot;
import com.volt.order.domain.model.StockShortage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

@Component
public class CatalogRestClientAdapter implements CatalogClientPort {
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;
    private final Duration backoff;

    public CatalogRestClientAdapter(
            @Qualifier("catalogRestClient") RestClient client,
            ObjectMapper objectMapper,
            @Value("${volt.catalog.retry.max-attempts:2}") int maxAttempts,
            @Value("${volt.catalog.retry.backoff:PT0.2S}") Duration backoff) {
        if (maxAttempts < 1) throw new IllegalArgumentException("catalog max attempts must be positive");
        this.client = client;
        this.objectMapper = objectMapper;
        this.maxAttempts = maxAttempts;
        this.backoff = backoff;
    }

    @Override
    public Reservation reserve(String orderRef, List<RequestedLine> lines) {
        ReservationResponse response = execute("reserve stock", () -> client.post()
                .uri("/internal/v1/stock/reservations")
                .body(new ReservationRequest(orderRef, lines))
                .retrieve()
                .body(ReservationResponse.class));
        if (response == null) throw unavailable("Catalog returned an empty reservation response", null);
        return new Reservation(response.reservationId(), response.expiresAt(), response.lines().stream()
                .map(line -> new ReservedLine(line.productId(), line.reference(), line.label(),
                        line.unitPriceExclVat(), line.quantity()))
                .toList());
    }

    @Override
    public void confirmReservation(long reservationId) {
        execute("confirm reservation", () -> {
            client.post().uri("/internal/v1/stock/reservations/{id}/confirm", reservationId)
                    .retrieve().toBodilessEntity();
            return null;
        });
    }

    @Override
    public void releaseReservation(long reservationId) {
        execute("release reservation", () -> {
            client.delete().uri("/internal/v1/stock/reservations/{id}", reservationId)
                    .retrieve().toBodilessEntity();
            return null;
        });
    }

    @Override
    public void restock(String orderRef) {
        execute("restock products", () -> {
            client.post().uri("/internal/v1/stock/restock").body(new RestockRequest(orderRef))
                    .retrieve().toBodilessEntity();
            return null;
        });
    }

    @Override
    public List<ProductSnapshot> findProducts(Collection<Long> productIds) {
        if (productIds.isEmpty()) return List.of();
        String ids = productIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        List<ProductResponse> response = execute("load products", () -> client.get()
                .uri(builder -> builder.path("/api/v1/products").queryParam("ids", ids).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductResponse>>() { }));
        if (response == null) throw unavailable("Catalog returned an empty product response", null);
        return response.stream().map(product -> new ProductSnapshot(
                product.id(), product.reference(), product.label(), product.priceExclVat(),
                product.stockQuantity(), product.active())).toList();
    }

    private <T> T execute(String operation, Supplier<T> request) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return request.get();
            } catch (RestClientResponseException response) {
                if (response.getStatusCode().value() == 409) throw shortage(response);
                if (!response.getStatusCode().is5xxServerError()) {
                    throw unavailable("Catalog rejected request to " + operation + " with " + response.getStatusCode(), response);
                }
                last = response;
            } catch (ResourceAccessException transport) {
                last = transport;
            }
            if (attempt < maxAttempts) pause();
        }
        throw unavailable("Catalog unavailable while attempting to " + operation, last);
    }

    private InsufficientStockException shortage(RestClientResponseException response) {
        try {
            ShortageProblem problem = objectMapper.readValue(response.getResponseBodyAsByteArray(), ShortageProblem.class);
            return new InsufficientStockException(problem.shortages() == null ? List.of() : problem.shortages());
        } catch (Exception parseFailure) {
            throw unavailable("Catalog returned an unreadable stock-conflict response", parseFailure);
        }
    }

    private void pause() {
        try {
            Thread.sleep(backoff.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw unavailable("Interrupted while retrying catalog request", interrupted);
        }
    }

    private static CatalogUnavailableException unavailable(String message, Throwable cause) {
        return new CatalogUnavailableException(message, cause);
    }

    private record ReservationRequest(String orderRef, List<RequestedLine> lines) { }
    private record RestockRequest(String orderRef) { }
    private record ReservationResponse(Long reservationId, Instant expiresAt, List<ReservedLineResponse> lines) { }
    private record ReservedLineResponse(Long productId, String reference, String label,
                                        BigDecimal unitPriceExclVat, int quantity) { }
    private record ProductResponse(Long id, String reference, String label, BigDecimal priceExclVat,
                                   int stockQuantity, boolean active) { }
    private record ShortageProblem(List<StockShortage> shortages) { }
}
