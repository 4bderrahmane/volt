package com.volt.order.application.port.out;

import com.volt.order.domain.model.ProductSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface CatalogClientPort {

    Reservation reserve(String orderRef, List<RequestedLine> lines);

    // Idempotent
    void confirmReservation(long reservationId);


    // Releases an unconfirmed reservation early. Idempotent.
    void releaseReservation(long reservationId);


    // Returns stock for a confirmed order, deriving immutable lines from its reservation. Idempotent.
    void restock(String orderRef);


    // Fetches prices for all cart products in one call.
    List<ProductSnapshot> findProducts(Collection<Long> productIds);

    record RequestedLine(Long productId, int quantity) {
    }

    record Reservation(Long reservationId, Instant expiresAt, List<ReservedLine> lines) {

        public Reservation {
            lines = List.copyOf(lines);
        }
    }


    // The immutable checkout snapshot copied into an order line.
    record ReservedLine(
            Long productId,
            String reference,
            String label,
            BigDecimal unitPriceExclVat,
            int quantity) {
    }
}
