package com.volt.catalog.application.port.out;

import com.volt.catalog.domain.model.PagedResult;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.ProductSearchCriteria;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Outgoing port describing the product storage needed by the application.
 *
 * <p>An outgoing port is owned by the core and implemented by an output adapter.
 * It uses domain and Java types only: no Spring Data {@code Page}, JPA entity,
 * SQL query, or HTTP response leaks into the application layer.
 */
public interface ProductRepositoryPort {

    Optional<Product> findById(long productId);

    Optional<Product> findByReference(String reference);

    List<Product> findAllByIds(Collection<Long> productIds);

    PagedResult<Product> search(ProductSearchCriteria criteria);

    Product save(Product product);

    /** Loads products exclusively when a future database adapter supports locking. */
    List<Product> lockForUpdate(Collection<Long> productIds);
}
