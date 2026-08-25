package com.volt.order.application.port.in;

import com.volt.order.domain.model.CartLine;
import com.volt.order.domain.model.OrderTotals;
import com.volt.order.domain.model.ProductSnapshot;

import java.util.List;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Specification §F6 — cart display.
 *
 * <p>The view pairs stored lines with prices fetched live from the catalog
 * (ADR-0002). The totals shown here are indicative: the authoritative ones are
 * computed at checkout from the reservation response, so a price change between
 * viewing and validating is reflected in the order rather than hidden.
 */
public interface ViewCartUseCase {

    CartView view(UUID customerId);

    record CartView(List<Item> items, OrderTotals indicativeTotals) {

        public CartView {
            items = List.copyOf(items);
        }
    }

    record Item(CartLine line, ProductSnapshot product, BigDecimal lineTotalExclVat) {
    }
}
