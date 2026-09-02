package com.volt.order.infrastructure.adapter.out.client.dto.response;

import java.math.BigDecimal;

public record ReservedLineResponse(Long productId, String reference, String label,
                                   BigDecimal unitPriceExclVat, int quantity) {
}
