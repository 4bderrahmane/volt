package com.volt.catalog.application.usecase;

import com.volt.catalog.application.port.in.GetProductUseCase;
import com.volt.catalog.application.port.in.SearchProductsUseCase;
import com.volt.catalog.application.port.out.ProductRepositoryPort;
import com.volt.catalog.domain.exception.ProductNotFoundException;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.PagedResult;
import com.volt.catalog.domain.model.ProductSearchCriteria;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Application use-case implementation for reading products.
 *
 * <p>A use case coordinates one user intention. It depends on the incoming
 * {@code GetProductUseCase} contract and the outgoing {@code ProductRepositoryPort}
 * contract, while remaining unaware of controllers and storage technology.
 */
@Service
@Transactional(readOnly = true)
public class ProductQueryService implements GetProductUseCase, SearchProductsUseCase {

    private final ProductRepositoryPort productRepository;

    public ProductQueryService(ProductRepositoryPort productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product getById(long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Override
    public List<Product> getAll(Collection<Long> productIds) {
        return productRepository.findAllByIds(productIds);
    }

    @Override
    public PagedResult<Product> search(ProductSearchCriteria criteria) {
        return productRepository.search(criteria);
    }
}
