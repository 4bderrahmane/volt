package com.volt.catalog.application.port.out;

import com.volt.catalog.domain.model.PagedResult;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.ProductSearchCriteria;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {

    Optional<Product> findById(long productId);

    Optional<Product> findByReference(String reference);

    List<Product> findAllByIds(Collection<Long> productIds);

    PagedResult<Product> search(ProductSearchCriteria criteria);

    Product save(Product product);

    /** Rows locked FOR UPDATE; the caller must already hold a transaction. */
    List<Product> lockForUpdate(Collection<Long> productIds);
}
