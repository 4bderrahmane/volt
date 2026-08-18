package com.volt.order.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record OrderTotals(BigDecimal totalExclVat, BigDecimal vatAmount, BigDecimal totalInclVat) {
    public OrderTotals {
        totalExclVat = OrderLine.money(totalExclVat, "total excluding VAT");
        vatAmount = OrderLine.money(vatAmount, "VAT amount");
        totalInclVat = OrderLine.money(totalInclVat, "total including VAT");
        if (totalInclVat.compareTo(totalExclVat.add(vatAmount)) != 0) {
            throw new IllegalArgumentException("total including VAT must equal excluding VAT plus VAT");
        }
    }

    public static OrderTotals calculate(List<OrderLine> lines, VatRate vatRate) {
        Objects.requireNonNull(lines, "order lines are required");
        Objects.requireNonNull(vatRate, "VAT rate is required");
        BigDecimal excluding = lines.stream()
                .map(line -> Objects.requireNonNull(line, "order line is required").getLineTotalExclVat())
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        BigDecimal vat = vatRate.vatOn(excluding);
        return new OrderTotals(excluding, vat, excluding.add(vat));
    }

    public static OrderTotals zero() {
        return new OrderTotals(new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"));
    }
}
