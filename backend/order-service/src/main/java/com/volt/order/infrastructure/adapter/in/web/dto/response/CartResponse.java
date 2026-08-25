package com.volt.order.infrastructure.adapter.in.web.dto.response;

import com.volt.order.application.port.in.ViewCartUseCase;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<Item> items, TotalsResponse totals) {
    public CartResponse {
        items = List.copyOf(items);
    }

    public static CartResponse from(ViewCartUseCase.CartView view) {
        return new CartResponse(view.items().stream().map(Item::from).toList(), TotalsResponse.from(view.indicativeTotals()));
    }

    public record Item(Long lineId, Long productId, String reference, String label,
                       BigDecimal unitPriceExclVat, int quantity, BigDecimal lineTotalExclVat,
                       int availableQuantity, boolean active) {
        static Item from(ViewCartUseCase.Item item) {
            return new Item(item.line().getId(), item.line().getProductId(), item.product().reference(),
                    item.product().label(), item.product().unitPriceExclVat(), item.line().getQuantity(),
                    item.lineTotalExclVat(), item.product().availableQuantity(), item.product().active());
        }
    }
}
