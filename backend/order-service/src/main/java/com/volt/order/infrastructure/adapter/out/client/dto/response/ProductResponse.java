package com.volt.order.infrastructure.adapter.out.client.dto.response;

import java.math.BigDecimal;

public record ProductResponse(Long id, String reference, String label, BigDecimal priceExclVat,
                              int stockQuantity, boolean active) {
}
