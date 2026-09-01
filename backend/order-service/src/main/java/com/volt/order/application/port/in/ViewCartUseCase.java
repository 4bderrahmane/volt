package com.volt.order.application.port.in;

import com.volt.order.domain.model.CartLine;
import com.volt.order.domain.model.OrderTotals;
import com.volt.order.domain.model.ProductSnapshot;

import java.util.List;
import java.math.BigDecimal;
import java.util.UUID;

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
