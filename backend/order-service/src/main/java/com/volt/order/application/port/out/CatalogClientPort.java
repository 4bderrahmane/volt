package com.volt.order.application.port.out;

import com.volt.order.domain.exception.CatalogUnavailableException;
import com.volt.order.domain.exception.InsufficientStockException;
import com.volt.order.domain.model.ProductSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * What this service needs from the catalog, in domain vocabulary (ADR-0005).
 *
 * <p>Note the absences: no URL, no status code, no {@code ResponseEntity}, no
 * {@code RestClient}. That REST is the transport is a fact about
 * {@code CatalogRestClientAdapter} and must not be inferable from this file.
 * Two consequences follow: use case tests can implement this interface as a
 * plain fake with no HTTP stubbing, and replacing REST with messaging later
 * touches one class.
 *
 * <p>Implementations must translate transport failures into the domain
 * exceptions declared below. A {@code RestClientResponseException} escaping the
 * adapter means the application layer has started knowing about HTTP.
 */
public interface CatalogClientPort {

    /**
     * Phase one of ADR-0003: reserve stock and read back the authoritative
     * prices in a single call.
     *
     * @param orderRef idempotency key; a retry with the same reference must not
     *                 create a second reservation
     * @throws InsufficientStockException   catalog returned 409 (do not retry)
     * @throws CatalogUnavailableException  timeout or 5xx after retries
     */
    Reservation reserve(String orderRef, List<RequestedLine> lines);

    /** Phase two. Idempotent. */
    void confirmReservation(long reservationId);

    /** Releases an unconfirmed reservation early. Idempotent. */
    void releaseReservation(long reservationId);

    /** Returns stock for a confirmed order, deriving immutable lines from its reservation. Idempotent. */
    void restock(String orderRef);

    /** Batch price lookup for cart rendering (ADR-0002) — one call, not N. */
    List<ProductSnapshot> findProducts(Collection<Long> productIds);

    record RequestedLine(Long productId, int quantity) {
    }

    record Reservation(Long reservationId, Instant expiresAt, List<ReservedLine> lines) {

        public Reservation {
            lines = List.copyOf(lines);
        }
    }

    /** The priced snapshot copied into order lines (specification §3.3). */
    record ReservedLine(
            Long productId,
            String reference,
            String label,
            BigDecimal unitPriceExclVat,
            int quantity) {
    }
}
