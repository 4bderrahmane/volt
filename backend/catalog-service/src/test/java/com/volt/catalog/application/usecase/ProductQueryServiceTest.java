package com.volt.catalog.application.usecase;

import com.volt.catalog.application.port.out.ProductRepositoryPort;
import com.volt.catalog.domain.exception.ProductNotFoundException;
import com.volt.catalog.domain.model.PagedResult;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.ProductSearchCriteria;
import com.volt.catalog.domain.model.Unit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for the read use case using a hand-written fake output port.
 *
 * <p>The fake proves that application tests do not need Spring, HTTP, or a
 * database. Only the port contract matters to the use-case implementation.
 */
class ProductQueryServiceTest {

    private final ProductQueryService service = new ProductQueryService(new FakeProductRepository());

    @Test
    void getsAProductThroughTheOutputPort() {
        Product product = service.getById(1L);

        assertEquals("REF-001", product.getReference());
    }

    @Test
    void reportsAnUnknownProduct() {
        assertThrows(ProductNotFoundException.class, () -> service.getById(999L));
    }

    /** Minimal test implementation of the outgoing repository port. */
    private static final class FakeProductRepository implements ProductRepositoryPort {

        private final Product product;

        private FakeProductRepository() {
            Instant now = Instant.parse("2026-08-14T10:00:00Z");
            product = new Product(
                    1L,
                    "REF-001",
                    "Test product",
                    null,
                    new BigDecimal("12.50"),
                    Unit.ITEM,
                    10,
                    true,
                    1L,
                    1L,
                    now,
                    now);
        }

        @Override
        public Optional<Product> findById(long productId) {
            return productId == product.getId() ? Optional.of(product) : Optional.empty();
        }

        @Override
        public Optional<Product> findByReference(String reference) {
            return Optional.empty();
        }

        @Override
        public List<Product> findAllByIds(Collection<Long> productIds) {
            return productIds.contains(product.getId()) ? List.of(product) : List.of();
        }

        @Override
        public PagedResult<Product> search(ProductSearchCriteria criteria) {
            return new PagedResult<>(List.of(product), criteria.page(), criteria.size(), 1);
        }

        @Override
        public Product save(Product product) {
            return product;
        }

        @Override
        public List<Product> lockForUpdate(Collection<Long> productIds) {
            return findAllByIds(productIds);
        }
    }
}
