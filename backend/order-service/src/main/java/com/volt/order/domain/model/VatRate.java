package com.volt.order.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record VatRate(BigDecimal rate) {
    public static final VatRate STANDARD = new VatRate(new BigDecimal("0.20"));

    public VatRate {
        Objects.requireNonNull(rate, "VAT rate is required");
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("VAT rate must be between zero and one");
        }
        rate = rate.stripTrailingZeros();
    }

    public BigDecimal vatOn(BigDecimal amountExclVat) {
        BigDecimal amount = OrderLine.money(amountExclVat, "amount excluding VAT");
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
