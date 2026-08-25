package com.volt.order.application.usecase;

import com.volt.order.application.port.out.CartRepositoryPort;
import com.volt.order.application.port.out.CatalogClientPort;
import com.volt.order.domain.exception.ProductUnavailableException;
import com.volt.order.domain.model.Cart;
import com.volt.order.domain.model.ProductSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartServiceTest {
    @Test
    void rejectsUnknownProductBeforeTheCartCanBeSaved() {
        FakeCarts carts = new FakeCarts();
        CartService service = new CartService(carts, new FakeCatalog(List.of()),
                Clock.fixed(Instant.parse("2026-08-17T10:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(() -> service.addLine(UUID.randomUUID(), 99L, 1))
                .isInstanceOf(ProductUnavailableException.class);
        assertThat(carts.saves).isZero();
        assertThat(carts.cart).isNull();
    }

    @Test
    void savesAnActiveCatalogProduct() {
        FakeCarts carts = new FakeCarts();
        ProductSnapshot product = new ProductSnapshot(7L, "REF-7", "Cable", new BigDecimal("10.00"), 3, true);
        CartService service = new CartService(carts, new FakeCatalog(List.of(product)),
                Clock.fixed(Instant.parse("2026-08-17T10:00:00Z"), ZoneOffset.UTC));

        service.addLine(UUID.randomUUID(), 7L, 2);

        assertThat(carts.saves).isEqualTo(1);
        assertThat(carts.cart.getLines()).singleElement().satisfies(line -> assertThat(line.getProductId()).isEqualTo(7L));
    }

    private static final class FakeCarts implements CartRepositoryPort {
        private Cart cart;
        private int saves;
        @Override public Optional<Cart> findByCustomerId(UUID customerId) { return Optional.ofNullable(cart); }
        @Override public Cart save(Cart cart) { this.cart = cart; saves++; return cart; }
        @Override public void deleteByCustomerId(UUID customerId) { cart = null; }
    }

    private static final class FakeCatalog implements CatalogClientPort {
        private final List<ProductSnapshot> products;
        private FakeCatalog(List<ProductSnapshot> products) { this.products = products; }
        @Override public Reservation reserve(String orderRef, List<RequestedLine> lines) { throw new UnsupportedOperationException(); }
        @Override public void confirmReservation(long reservationId) { throw new UnsupportedOperationException(); }
        @Override public void releaseReservation(long reservationId) { throw new UnsupportedOperationException(); }
        @Override public void restock(String orderRef) { throw new UnsupportedOperationException(); }
        @Override public List<ProductSnapshot> findProducts(Collection<Long> productIds) { return products; }
    }
}
