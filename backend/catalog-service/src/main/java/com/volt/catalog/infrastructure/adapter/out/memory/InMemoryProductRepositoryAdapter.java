package com.volt.catalog.infrastructure.adapter.out.memory;

import com.volt.catalog.application.port.out.ProductRepositoryPort;
import com.volt.catalog.domain.model.PagedResult;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.ProductSearchCriteria;
import com.volt.catalog.domain.model.Unit;

import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("memory")
public class InMemoryProductRepositoryAdapter implements ProductRepositoryPort {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(2);

    public InMemoryProductRepositoryAdapter() {
        Instant now = Instant.parse("2026-08-14T00:00:00Z");
        products.put(1L, new Product(
                1L,
                "DEMO-001",
                "Demonstration product",
                "Stored by the in-memory output adapter",
                new BigDecimal("49.90"),
                Unit.ITEM,
                20,
                true,
                1L,
                1L,
                now,
                now));
    }

    @Override
    public Optional<Product> findById(long productId) {
        return Optional.ofNullable(products.get(productId));
    }

    @Override
    public Optional<Product> findByReference(String reference) {
        return products.values().stream()
                .filter(product -> product.getReference().equals(reference))
                .findFirst();
    }

    @Override
    public List<Product> findAllByIds(Collection<Long> productIds) {
        return productIds.stream().map(products::get).filter(product -> product != null).toList();
    }

    @Override
    public PagedResult<Product> search(ProductSearchCriteria criteria) {
        List<Product> matches = products.values().stream()
                .filter(product -> !criteria.activeOnly() || product.isActive())
                .filter(product -> criteria.categoryId() == null
                        || criteria.categoryId().equals(product.getCategoryId()))
                .filter(product -> criteria.brandId() == null
                        || criteria.brandId().equals(product.getBrandId()))
                .filter(product -> matchesQuery(product, criteria.query()))
                .sorted(Comparator.comparing(Product::getLabel))
                .toList();
        int from = Math.min(criteria.page() * criteria.size(), matches.size());
        int to = Math.min(from + criteria.size(), matches.size());
        return new PagedResult<>(matches.subList(from, to), criteria.page(), criteria.size(), matches.size());
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            product = withId(product, nextId.getAndIncrement());
        }
        products.put(product.getId(), product);
        return product;
    }

    @Override
    public List<Product> lockForUpdate(Collection<Long> productIds) {
        return findAllByIds(productIds);
    }

    private static boolean matchesQuery(Product product, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return product.getLabel().toLowerCase(Locale.ROOT).contains(normalized)
                || product.getReference().toLowerCase(Locale.ROOT).contains(normalized);
    }

    private static Product withId(Product product, long id) {
        return new Product(
                id,
                product.getReference(),
                product.getLabel(),
                product.getDescription(),
                product.getPriceExclVat(),
                product.getUnit(),
                product.getStockQuantity(),
                product.isActive(),
                product.getCategoryId(),
                product.getBrandId(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
