package com.volt.order.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {
    private static final UUID CUSTOMER = UUID.fromString("8fe20245-6121-4d3c-92d0-05cc3271d3d1");
    private static final Instant NOW = Instant.parse("2026-08-17T10:00:00Z");

    @Test
    void mergesRepeatedProductsAndKeepsTheCollectionEncapsulated() {
        Cart cart = Cart.empty(CUSTOMER, NOW);

        cart.addLine(7L, 2, NOW.plusSeconds(1));
        cart.addLine(7L, 3, NOW.plusSeconds(2));

        assertThat(cart.getLines()).singleElement().satisfies(line -> {
            assertThat(line.getProductId()).isEqualTo(7L);
            assertThat(line.getQuantity()).isEqualTo(5);
        });
        assertThat(cart.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThatThrownBy(() -> cart.getLines().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void changesAndRemovesOnlyLinesOwnedByTheCart() {
        Cart cart = Cart.rehydrate(1L, CUSTOMER, List.of(CartLine.rehydrate(11L, 7L, 2)), NOW, NOW);

        cart.changeLineQuantity(11L, 4, NOW.plusSeconds(1));
        assertThat(cart.getLines().getFirst().getQuantity()).isEqualTo(4);
        cart.removeLine(11L, NOW.plusSeconds(2));
        assertThat(cart).extracting(Cart::isEmpty).isEqualTo(true);
    }

    @Test
    void rejectsDuplicateProductsDuringRehydration() {
        assertThatThrownBy(() -> Cart.rehydrate(1L, CUSTOMER,
                List.of(CartLine.rehydrate(11L, 7L, 1), CartLine.rehydrate(12L, 7L, 1)), NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate products");
    }
}
