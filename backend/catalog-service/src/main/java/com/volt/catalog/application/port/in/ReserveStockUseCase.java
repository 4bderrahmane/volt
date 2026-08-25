package com.volt.catalog.application.port.in;

import com.volt.catalog.domain.exception.InsufficientStockException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Phase one of ADR-0003. Reserves stock and returns the authoritative prices in
 * a single transaction.
 *
 * <p>Returning the prices is not a convenience. Specification §3.3 requires the order
 * line to copy the unit price, and reading the price in a separate earlier call
 * leaves a window in which an ADMIN edit changes it — so the price persisted on
 * the order would be one that was never validated. Reading availability and
 * price in the same transaction closes that window.
 *
 * <p>All-or-nothing: if any line is short, nothing is reserved and
 * {@link InsufficientStockException} carries every shortage.
 */
public interface ReserveStockUseCase {

    ReservationResult reserve(ReserveStockCommand command);

    /**
     * @param orderRef caller-supplied idempotency key; a repeated call with the
     *                 same reference must return the existing reservation
     *                 rather than creating a second one, which is what makes
     *                 the retry mandated by specification §3.4 safe.
     */
    record ReserveStockCommand(String orderRef, List<RequestedLine> lines) {

        public ReserveStockCommand {
            if (orderRef == null || orderRef.isBlank()) {
                throw new IllegalArgumentException("orderRef must not be blank");
            }
            orderRef = orderRef.trim();
            if (orderRef.length() > 64) {
                throw new IllegalArgumentException("orderRef must be at most 64 characters");
            }
            lines = List.copyOf(lines);
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("cannot reserve an empty set of lines");
            }
            Set<Long> productIds = new HashSet<>();
            for (RequestedLine line : lines) {
                if (!productIds.add(line.productId())) {
                    throw new IllegalArgumentException("duplicate productId " + line.productId());
                }
            }
        }
    }

    record RequestedLine(Long productId, int quantity) {
        public RequestedLine {
            if (productId == null || productId < 1) {
                throw new IllegalArgumentException("productId must be positive");
            }
            if (quantity < 1) {
                throw new IllegalArgumentException("quantity must be positive");
            }
        }
    }

    record ReservationResult(Long reservationId, Instant expiresAt, List<ReservedLine> lines) {

        public ReservationResult {
            lines = List.copyOf(lines);
        }
    }

    /** The snapshot order-service copies into its order lines (specification §5.2). */
    record ReservedLine(
            Long productId,
            String reference,
            String label,
            BigDecimal unitPriceExclVat,
            int quantity) {
    }
}
