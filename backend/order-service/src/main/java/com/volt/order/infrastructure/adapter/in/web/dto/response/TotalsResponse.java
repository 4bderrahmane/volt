package com.volt.order.infrastructure.adapter.in.web.dto.response;

import com.volt.order.domain.model.OrderTotals;

import java.math.BigDecimal;

public record TotalsResponse(BigDecimal totalExclVat, BigDecimal vatAmount, BigDecimal totalInclVat) {
    static TotalsResponse from(OrderTotals totals) {
        return new TotalsResponse(totals.totalExclVat(), totals.vatAmount(), totals.totalInclVat());
    }
}
