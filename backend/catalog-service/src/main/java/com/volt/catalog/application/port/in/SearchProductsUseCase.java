package com.volt.catalog.application.port.in;

import com.volt.catalog.domain.model.PagedResult;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.ProductSearchCriteria;

/** Specification §F1, §F2 — paginated listing with search and filters. */
public interface SearchProductsUseCase {

    PagedResult<Product> search(ProductSearchCriteria criteria);
}
