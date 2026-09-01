package com.volt.order.application.usecase;

import com.volt.order.application.port.out.CartRepositoryPort;
import com.volt.order.application.port.out.CatalogClientPort;
import com.volt.order.application.port.out.OrderRepositoryPort;
import com.volt.order.domain.model.Cart;
import com.volt.order.domain.model.CartLine;
import com.volt.order.domain.model.Order;
import com.volt.order.domain.model.OrderLine;
import com.volt.order.domain.model.OrderStatus;
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

class PlaceOrderServiceTest {
    @Test
    void reservesPersistsConfirmsAndClearsTheCart() {
        UUID customer = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-17T10:00:00Z");
        FakeCarts carts = new FakeCarts(Cart.rehydrate(
                1L, customer, List.of(CartLine.rehydrate(10L, 7L, 2)), now, now));
        FakeOrders orders = new FakeOrders();
        FakeCatalog catalog = new FakeCatalog(now);
        Clock clock = Clock.fixed(now.plusSeconds(1), ZoneOffset.UTC);
        PlaceOrderService service = new PlaceOrderService(carts, orders, () -> "ORD-2026-000001", catalog,
                new ChangeOrderStatusService(orders, catalog, clock), clock);

        Order result = service.placeOrder(customer);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getTotals().totalInclVat()).isEqualByComparingTo("24.00");
        assertThat(carts.cart.isEmpty()).isTrue();
        assertThat(catalog.confirmedReservation).isEqualTo(55L);
        assertThat(orders.saves).isEqualTo(2);
        // Checkout confirmation uses the same locked transition path as administrators.
        assertThat(orders.locks).isEqualTo(1);
    }

    @Test
    void rejectsAndReleasesAReservationThatDoesNotMatchTheCart() {
        UUID customer = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-17T10:00:00Z");
        FakeCarts carts = new FakeCarts(Cart.rehydrate(
                1L, customer, List.of(CartLine.rehydrate(10L, 7L, 2)), now, now));
        FakeOrders orders = new FakeOrders();
        FakeCatalog catalog = new FakeCatalog(now);
        catalog.reservedProductId = 8L;
        Clock clock = Clock.fixed(now.plusSeconds(1), ZoneOffset.UTC);
        PlaceOrderService service = new PlaceOrderService(carts, orders, () -> "ORD-2026-000001", catalog,
                new ChangeOrderStatusService(orders, catalog, clock), clock);

        assertThatThrownBy(() -> service.placeOrder(customer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not match");

        assertThat(catalog.releasedReservation).isEqualTo(55L);
        assertThat(orders.saves).isZero();
        assertThat(orders.locks).isZero();
        assertThat(carts.cart.isEmpty()).isFalse();
    }

    private static final class FakeCarts implements CartRepositoryPort {
        private Cart cart;

        private FakeCarts(Cart cart) { this.cart = cart; }
        @Override public Optional<Cart> findByCustomerId(UUID customerId) { return Optional.ofNullable(cart); }
        @Override public Cart save(Cart cart) { this.cart = cart; return cart; }
        @Override public void deleteByCustomerId(UUID customerId) { cart = null; }
    }

    private static final class FakeOrders implements OrderRepositoryPort {
        private int saves;
        private int locks;
        private Order stored;

        @Override public Optional<Order> findById(long orderId) { return Optional.ofNullable(stored); }

        @Override
        public Optional<Order> findByIdForUpdate(long orderId) {
            locks++;
            return Optional.ofNullable(stored);
        }

        @Override public Optional<Order> findByIdAndCustomerId(long orderId, UUID customerId) { return Optional.empty(); }
        @Override public List<Order> findByCustomerId(UUID customerId) { return List.of(); }

        @Override
        public Order save(Order order) {
            saves++;
            if (order.getId() != null) {
                stored = order;
                return order;
            }
            List<OrderLine> lines = order.getLines().stream().map(line -> OrderLine.rehydrate(
                    100L, line.getProductId(), line.getProductReference(), line.getProductLabel(),
                    line.getUnitPriceExclVat(), line.getQuantity(), line.getLineTotalExclVat())).toList();
            stored = Order.rehydrate(42L, order.getNumber(), order.getCustomerId(), order.getStatus(), order.getReservationId(),
                    order.getTotals(), lines, order.getCreatedAt(), order.getUpdatedAt());
            return stored;
        }
    }

    private static final class FakeCatalog implements CatalogClientPort {
        private final Instant now;
        private long confirmedReservation;
        private long releasedReservation;
        private long reservedProductId = 7L;

        private FakeCatalog(Instant now) { this.now = now; }
        @Override public Reservation reserve(String orderRef, List<RequestedLine> lines) {
            return new Reservation(55L, now.plusSeconds(900),
                    List.of(new ReservedLine(reservedProductId, "REF-7", "Cable", new BigDecimal("10.00"), 2)));
        }
        @Override public void confirmReservation(long reservationId) { confirmedReservation = reservationId; }
        @Override public void releaseReservation(long reservationId) { releasedReservation = reservationId; }
        @Override public void restock(String orderRef) { }
        @Override public List<com.volt.order.domain.model.ProductSnapshot> findProducts(Collection<Long> productIds) { return List.of(); }
    }
}
