package com.volt.order;

import com.volt.order.application.port.in.ChangeOrderStatusUseCase;
import com.volt.order.application.port.out.CatalogClientPort;
import com.volt.order.application.port.out.OrderNumberGeneratorPort;
import com.volt.order.application.port.out.OrderRepositoryPort;
import com.volt.order.domain.exception.IllegalStatusTransitionException;
import com.volt.order.domain.model.Order;
import com.volt.order.domain.model.OrderLine;
import com.volt.order.domain.model.OrderStatus;
import com.volt.order.domain.model.OrderTotals;
import com.volt.order.domain.model.ProductSnapshot;
import com.volt.order.domain.model.VatRate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.IllegalTransactionStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves against PostgreSQL that the pessimistic read in
 * {@link com.volt.order.application.usecase.ChangeOrderStatusService} actually
 * serializes competing status changes.
 *
 * <p>Without the lock both tests below pass their assertions on status while
 * silently calling the catalog twice, which is exactly the stock/order mismatch
 * the lock exists to prevent — so the catalog call counts, not the statuses,
 * are what make this test worth running.
 */
@Import({TestcontainersConfiguration.class, OrderStatusConcurrencyIntegrationTest.SlowCountingCatalog.class})
@SpringBootTest
class OrderStatusConcurrencyIntegrationTest {

    @Autowired private ChangeOrderStatusUseCase changeStatus;
    @Autowired private OrderRepositoryPort orders;
    @Autowired private OrderNumberGeneratorPort orderNumbers;
    @Autowired private CountingCatalogClient catalog;

    @Test
    void twoSimultaneousCancellationsCancelOnceAndRestockOnce() throws Exception {
        long orderId = persistConfirmedOrder();
        catalog.reset();

        List<Throwable> failures = race(2, () -> changeStatus.changeStatus(orderId, OrderStatus.CANCELLED));

        assertThat(catalog.restocks.get())
                .as("a cancelled order must return its stock exactly once")
                .isEqualTo(1);
        assertThat(failures)
                .singleElement()
                .isInstanceOf(IllegalStatusTransitionException.class);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancellationAndShipmentCannotBothWin() throws Exception {
        long orderId = persistConfirmedOrder();
        catalog.reset();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<OrderStatus> cancelling = pool.submit(() -> {
                ready.countDown();
                go.await();
                return changeStatus.changeStatus(orderId, OrderStatus.CANCELLED).getStatus();
            });
            Future<OrderStatus> shipping = pool.submit(() -> {
                ready.countDown();
                go.await();
                return changeStatus.changeStatus(orderId, OrderStatus.SHIPPED).getStatus();
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            OrderStatus survivor = winnerOf(cancelling, shipping);
            assertThat(survivor).isIn(OrderStatus.CANCELLED, OrderStatus.SHIPPED);
            assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(survivor);
            // A shipped order must not have been restocked, and a cancelled one
            // must have been restocked once. Never both, which is what an
            // unlocked read would allow.
            assertThat(catalog.restocks.get()).isEqualTo(survivor == OrderStatus.CANCELLED ? 1 : 0);
        }
    }

    /**
     * The lock is only real while a transaction is open, so the adapter declares
     * {@code Propagation.MANDATORY}. This asserts that guard is live rather than
     * decorative.
     */
    @Test
    void aPessimisticReadOutsideATransactionIsRefused() {
        long orderId = persistConfirmedOrder();

        assertThatThrownBy(() -> orders.findByIdForUpdate(orderId))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    // ------------------------------------------------------------------ helpers

    private long persistConfirmedOrder() {
        Instant now = Instant.now();
        List<OrderLine> lines = List.of(OrderLine.of(7L, "REF-7", "Cable", new BigDecimal("10.00"), 3));
        Order created = Order.place(orderNumbers.nextOrderNumber(), UUID.randomUUID(), 55L,
                OrderTotals.calculate(lines, VatRate.STANDARD), lines, now);
        Order saved = orders.save(created);
        saved.confirm(now.plusMillis(1));
        return orders.save(saved).getId();
    }

    /** Runs {@code action} on {@code threads} threads released together, returning whatever failed. */
    private static List<Throwable> race(int threads, ThrowingRunnable action) throws Exception {
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<Throwable>> attempts = java.util.stream.IntStream.range(0, threads)
                    .mapToObj(ignored -> pool.<Throwable>submit(() -> {
                        ready.countDown();
                        go.await();
                        try {
                            action.run();
                            return null;
                        } catch (Throwable failure) {
                            return failure;
                        }
                    }))
                    .toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            List<Throwable> failures = new java.util.ArrayList<>();
            for (Future<Throwable> attempt : attempts) {
                Throwable failure = attempt.get(30, TimeUnit.SECONDS);
                if (failure != null) {
                    failures.add(failure);
                }
            }
            return failures;
        }
    }

    private static OrderStatus winnerOf(Future<OrderStatus> first, Future<OrderStatus> second) throws Exception {
        OrderStatus firstResult = resultOrNull(first);
        OrderStatus secondResult = resultOrNull(second);
        assertThat(List.of(firstResult == null, secondResult == null))
                .as("exactly one of two competing transitions may succeed")
                .containsExactlyInAnyOrder(true, false);
        return firstResult != null ? firstResult : secondResult;
    }

    private static OrderStatus resultOrNull(Future<OrderStatus> attempt) throws Exception {
        try {
            return attempt.get(30, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException rejected) {
            assertThat(rejected).hasRootCauseInstanceOf(IllegalStatusTransitionException.class);
            return null;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SlowCountingCatalog {

        /**
         * Deliberately slow. A catalog call that returns instantly would let the
         * losing thread arrive after the winner has already committed, so the
         * test would pass with or without the lock. The delay guarantees the
         * second transaction reaches the row while the first still holds it.
         */
        @Bean
        @Primary
        CountingCatalogClient countingCatalogClient() {
            return new CountingCatalogClient();
        }
    }

    static final class CountingCatalogClient implements CatalogClientPort {
        private final AtomicInteger restocks = new AtomicInteger();
        private final AtomicInteger confirms = new AtomicInteger();
        private final AtomicInteger releases = new AtomicInteger();

        void reset() {
            restocks.set(0);
            confirms.set(0);
            releases.set(0);
        }

        @Override
        public Reservation reserve(String orderRef, List<RequestedLine> lines) {
            throw new UnsupportedOperationException("checkout is not exercised by this test");
        }

        @Override
        public void confirmReservation(long reservationId) {
            confirms.incrementAndGet();
            pause();
        }

        @Override
        public void releaseReservation(long reservationId) {
            releases.incrementAndGet();
            pause();
        }

        @Override
        public void restock(String orderRef) {
            restocks.incrementAndGet();
            pause();
        }

        @Override
        public List<ProductSnapshot> findProducts(Collection<Long> productIds) {
            return List.of();
        }

        private static void pause() {
            try {
                Thread.sleep(300);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
