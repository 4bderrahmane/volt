package com.volt.catalog;

import com.volt.catalog.application.port.in.ManageProductUseCase;
import com.volt.catalog.application.port.in.ManageReservationUseCase;
import com.volt.catalog.application.port.in.ReserveStockUseCase;
import com.volt.catalog.application.port.out.ProductRepositoryPort;
import com.volt.catalog.application.port.out.ReservationRepositoryPort;
import com.volt.catalog.domain.exception.ReservationNotFoundException;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.ReservationStatus;
import com.volt.catalog.domain.model.Unit;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.BrandJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.BrandJpaRepository;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.CategoryJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.CategoryJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Restock idempotency against a real PostgreSQL, deliberately <b>not</b>
 * {@code @Transactional}.
 *
 * <p>A rolled-back test transaction cannot demonstrate this property at all:
 * the competing threads would run on their own connections and never see the
 * uncommitted rows, so the race under test could not occur. Everything here
 * commits, which is why the reference and category codes are unique per run.
 */
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest
class CatalogRestockConcurrencyIntegrationTest {

    private static final int RESTOCK_THREADS = 8;

    @Autowired private CategoryJpaRepository categories;
    @Autowired private BrandJpaRepository brands;
    @Autowired private ManageProductUseCase manageProducts;
    @Autowired private ReserveStockUseCase reserveStock;
    @Autowired private ManageReservationUseCase manageReservations;
    @Autowired private ProductRepositoryPort products;
    @Autowired private ReservationRepositoryPort reservations;

    /**
     * Catalog demarcates transactions in the application layer, so
     * {@code ReservationService} is what opens one in production. This test
     * reaches past that layer to inspect a reservation directly, and its lines
     * are a lazy association — so it has to supply the session the application
     * layer would normally have opened. Using the adapter bare would report a
     * lazy-initialization error rather than the state under test.
     */
    @Autowired private TransactionTemplate transactions;

    @Test
    void concurrentRestocksReturnStockExactlyOnce() throws Exception {
        String orderRef = "ORD-RESTOCK-RACE";
        Product product = productWithStock("RACE-001", 10);
        long reservationId = reserveStock.reserve(new ReserveStockUseCase.ReserveStockCommand(
                orderRef,
                List.of(new ReserveStockUseCase.RequestedLine(product.getId(), 3)))).reservationId();
        manageReservations.confirm(reservationId);
        assertThat(stockOf(product)).isEqualTo(7);

        AtomicInteger succeeded = new AtomicInteger();
        List<Throwable> failures = race(RESTOCK_THREADS, () -> {
            manageReservations.restock(orderRef);
            succeeded.incrementAndGet();
        });

        assertThat(failures).as("a repeated restock is a no-op, never an error").isEmpty();
        assertThat(succeeded.get()).isEqualTo(RESTOCK_THREADS);
        assertThat(stockOf(product))
                .as("%d concurrent restocks must add the reserved quantity once, not %d times",
                        RESTOCK_THREADS, RESTOCK_THREADS)
                .isEqualTo(10);
        assertThat(statusOf(reservationId)).isEqualTo(ReservationStatus.RESTOCKED);
    }

    @Test
    void repeatedSequentialRestocksAreAlsoASingleIncrement() {
        String orderRef = "ORD-RESTOCK-REPEAT";
        Product product = productWithStock("RACE-002", 12);
        long reservationId = reserveStock.reserve(new ReserveStockUseCase.ReserveStockCommand(
                orderRef,
                List.of(new ReserveStockUseCase.RequestedLine(product.getId(), 5)))).reservationId();
        manageReservations.confirm(reservationId);

        manageReservations.restock(orderRef);
        manageReservations.restock(orderRef);
        manageReservations.restock(orderRef);

        assertThat(stockOf(product)).isEqualTo(12);
        assertThat(statusOf(reservationId)).isEqualTo(ReservationStatus.RESTOCKED);
    }

    @Test
    void restockingAnUnknownOrderReferenceIsReportedAsNotFound() {
        assertThatThrownBy(() -> manageReservations.restock("ORD-NEVER-EXISTED"))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    /**
     * {@code RESTOCKED} is a terminal state, so the order reference cannot be
     * recycled into a fresh hold. Reusing it would let a cancelled order's
     * reference silently reserve stock again.
     */
    @Test
    void aRestockedOrderReferenceCannotBeReservedAgain() {
        String orderRef = "ORD-RESTOCK-TERMINAL";
        Product product = productWithStock("RACE-003", 6);
        long reservationId = reserveStock.reserve(new ReserveStockUseCase.ReserveStockCommand(
                orderRef,
                List.of(new ReserveStockUseCase.RequestedLine(product.getId(), 2)))).reservationId();
        manageReservations.confirm(reservationId);
        manageReservations.restock(orderRef);

        assertThatThrownBy(() -> reserveStock.reserve(new ReserveStockUseCase.ReserveStockCommand(
                orderRef,
                List.of(new ReserveStockUseCase.RequestedLine(product.getId(), 2)))))
                .isInstanceOf(com.volt.catalog.domain.exception.ReservationExpiredException.class);

        assertThat(stockOf(product)).isEqualTo(6);
    }

    // ------------------------------------------------------------------ helpers

    private Product productWithStock(String reference, int stock) {
        CategoryJpaEntity category = categories.save(CategoryJpaEntity.builder()
                .code(reference)
                .label("Race category " + reference)
                .build());
        BrandJpaEntity brand = brands.save(BrandJpaEntity.builder().name("Race brand " + reference).build());
        return manageProducts.create(new ManageProductUseCase.CreateProductCommand(
                reference, "Race product " + reference, null, new BigDecimal("9.99"),
                Unit.ITEM, stock, category.getId(), brand.getId()));
    }

    private ReservationStatus statusOf(long reservationId) {
        return transactions.execute(status ->
                reservations.findById(reservationId).orElseThrow().getStatus());
    }

    private int stockOf(Product product) {
        return products.findById(product.getId()).orElseThrow().getStockQuantity();
    }

    private static List<Throwable> race(int threads, Runnable action) throws Exception {
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<Throwable>> attempts = new ArrayList<>();
            for (int attempt = 0; attempt < threads; attempt++) {
                attempts.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        action.run();
                        return null;
                    } catch (Throwable failure) {
                        return failure;
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            List<Throwable> failures = new ArrayList<>();
            for (Future<Throwable> attempt : attempts) {
                Throwable failure = attempt.get(60, TimeUnit.SECONDS);
                if (failure != null) {
                    failures.add(failure);
                }
            }
            return failures;
        }
    }
}
