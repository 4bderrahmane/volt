package com.volt.order.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderMoneyTest {
    @Test
    void normalizesPriceThenCalculatesLineAndVatTotals() {
        OrderLine line = OrderLine.of(9L, "REF-9", "Breaker", new BigDecimal("10.005"), 2);
        OrderTotals totals = OrderTotals.calculate(List.of(line), VatRate.STANDARD);

        assertThat(line.getUnitPriceExclVat()).isEqualByComparingTo("10.01");
        assertThat(line.getLineTotalExclVat()).isEqualByComparingTo("20.02");
        assertThat(totals.vatAmount()).isEqualByComparingTo("4.00");
        assertThat(totals.totalInclVat()).isEqualByComparingTo("24.02");
    }

    @Test
    void rejectsForgedLineAndOrderTotals() {
        assertThatThrownBy(() -> OrderLine.rehydrate(
                1L, 9L, "REF-9", "Breaker", new BigDecimal("10.00"), 2, new BigDecimal("19.99")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line total");
        assertThatThrownBy(() -> new OrderTotals(
                new BigDecimal("10.00"), new BigDecimal("2.00"), new BigDecimal("11.99")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must equal");
    }
}
