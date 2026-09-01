package com.volt.order.application.usecase;

import com.volt.order.application.port.out.CatalogClientPort;
import com.volt.order.application.port.out.OrderRepositoryPort;
import com.volt.order.domain.exception.IllegalStatusTransitionException;
import com.volt.order.domain.exception.OrderNotFoundException;
import com.volt.order.domain.model.Order;
import com.volt.order.domain.model.OrderLine;
import com.volt.order.domain.model.OrderStatus;
import com.volt.order.domain.model.OrderTotals;
import com.volt.order.domain.model.ProductSnapshot;
import com.volt.order.domain.model.VatRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangeOrderStatusServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-18T09:00:00Z");

    @Test
    void cancellingAConfirmedOrderRestocksItsReservationByOrderReference() {
        FakeOrders orders = new FakeOrders(order(OrderStatus.CONFIRMED, null));
        FakeCatalog catalog = new FakeCatalog();

        Order result = service(orders, catalog).changeStatus(42L, OrderStatus.CANCELLED);

        assertThat(catalog.restockedOrderRef).isEqualTo("ORD-2026-000001");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orders.saved).isSameAs(result);
    }

    @Test
    void cancellingACreatedOrderReleasesItsReservationInstead() {
        FakeOrders orders = new FakeOrders(order(OrderStatus.CREATED, 55L));
        FakeCatalog catalog = new FakeCatalog();

        Order result = service(orders, catalog).changeStatus(42L, OrderStatus.CANCELLED);

        assertThat(catalog.releasedReservation).isEqualTo(55L);
        assertThat(catalog.restockedOrderRef).isNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result.getReservationId()).isNull();
    }

    @Test
    void confirmingACreatedOrderCommitsItsReservation() {
        FakeOrders orders = new FakeOrders(order(OrderStatus.CREATED, 55L));
        FakeCatalog catalog = new FakeCatalog();

        Order result = service(orders, catalog).changeStatus(42L, OrderStatus.CONFIRMED);

        assertThat(catalog.confirmedReservation).isEqualTo(55L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getReservationId()).isNull();
    }

    /** The row lock must precede external side effects so they use the serialized status. */
    @Test
    void locksTheOrderRowBeforeAnyCatalogSideEffect() {
        FakeOrders orders = new FakeOrders(order(OrderStatus.CONFIRMED, null));
        FakeCatalog catalog = new FakeCatalog();
        List<String> calls = new ArrayList<>();
        orders.calls = calls;
        catalog.calls = calls;

        service(orders, catalog).changeStatus(42L, OrderStatus.CANCELLED);

        assertThat(calls).containsExactly("lock", "restock", "save");
        assertThat(orders.unlockedReads).isZero();
    }

    @Test
    void rejectsAnIllegalTransitionWithoutTouchingTheCatalog() {
        FakeOrders orders = new FakeOrders(order(OrderStatus.DELIVERED, null));
        FakeCatalog catalog = new FakeCatalog();

        assertThatThrownBy(() -> service(orders, catalog).changeStatus(42L, OrderStatus.CANCELLED))
                .isInstanceOf(IllegalStatusTransitionException.class);

        assertThat(catalog.restockedOrderRef).isNull();
        assertThat(catalog.releasedReservation).isZero();
        assertThat(orders.saved).isNull();
    }

    @Test
    void reportsAnUnknownOrderAsNotFound() {
        FakeOrders orders = new FakeOrders(null);

        assertThatThrownBy(() -> service(orders, new FakeCatalog()).changeStatus(42L, OrderStatus.CANCELLED))
                .isInstanceOf(OrderNotFoundException.class);
    }

    private static ChangeOrderStatusService service(FakeOrders orders, FakeCatalog catalog) {
        return new ChangeOrderStatusService(orders, catalog, Clock.fixed(CREATED_AT.plusSeconds(1), ZoneOffset.UTC));
    }

    private static Order order(OrderStatus status, Long reservationId) {
        OrderLine line = OrderLine.rehydrate(
                10L, 7L, "REF-7", "Cable", new BigDecimal("10.00"), 2, new BigDecimal("20.00"));
        return Order.rehydrate(42L, "ORD-2026-000001", UUID.randomUUID(), status, reservationId,
                OrderTotals.calculate(List.of(line), VatRate.STANDARD), List.of(line), CREATED_AT, CREATED_AT);
    }

    private static final class FakeOrders implements OrderRepositoryPort {
        private final Order found;
        private Order saved;
        private int unlockedReads;
        private List<String> calls = new ArrayList<>();

        private FakeOrders(Order found) {
            this.found = found;
        }

        @Override public Optional<Order> findById(long orderId) {
            unlockedReads++;
            calls.add("findById");
            return Optional.ofNullable(found);
        }

        @Override public Optional<Order> findByIdForUpdate(long orderId) {
            calls.add("lock");
            return Optional.ofNullable(found);
        }

        @Override public Optional<Order> findByIdAndCustomerId(long orderId, UUID customerId) { return Optional.empty(); }
        @Override public List<Order> findByCustomerId(UUID customerId) { return List.of(); }

        @Override public Order save(Order order) {
            calls.add("save");
            saved = order;
            return order;
        }
    }

    private static final class FakeCatalog implements CatalogClientPort {
        private String restockedOrderRef;
        private long releasedReservation;
        private long confirmedReservation;
        private List<String> calls = new ArrayList<>();

        @Override public Reservation reserve(String orderRef, List<RequestedLine> lines) { throw new UnsupportedOperationException(); }

        @Override public void confirmReservation(long reservationId) {
            calls.add("confirm");
            confirmedReservation = reservationId;
        }

        @Override public void releaseReservation(long reservationId) {
            calls.add("release");
            releasedReservation = reservationId;
        }

        @Override public void restock(String orderRef) {
            calls.add("restock");
            restockedOrderRef = orderRef;
        }

        @Override public List<ProductSnapshot> findProducts(Collection<Long> productIds) { return List.of(); }
    }
}
