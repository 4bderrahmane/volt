package com.volt.catalog.application.usecase;

import com.volt.catalog.application.port.in.ManageProductUseCase;
import com.volt.catalog.application.port.out.ProductRepositoryPort;
import com.volt.catalog.domain.exception.DuplicateProductReferenceException;
import com.volt.catalog.domain.exception.ProductNotFoundException;
import com.volt.catalog.domain.model.Product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class ManageProductService implements ManageProductUseCase {

    private final ProductRepositoryPort productRepository;
    private final Clock clock;

    public ManageProductService(ProductRepositoryPort productRepository, Clock clock) {
        this.productRepository = productRepository;
        this.clock = clock;
    }

    @Override
    public Product create(CreateProductCommand command) {
        Product product = Product.create(
                command.reference(),
                command.label(),
                command.description(),
                command.priceExclVat(),
                command.unit(),
                command.initialStock(),
                command.categoryId(),
                command.brandId(),
                clock.instant());
        if (productRepository.findByReference(product.getReference()).isPresent()) {
            throw new DuplicateProductReferenceException(product.getReference());
        }
        return productRepository.save(product);
    }

    @Override
    public Product update(long productId, UpdateProductCommand command) {
        Product product = findProduct(productId);
        product.applyUpdate(
                command.label(),
                command.description(),
                command.priceExclVat(),
                command.unit(),
                command.categoryId(),
                command.brandId(),
                clock.instant());
        return productRepository.save(product);
    }

    @Override
    public void deactivate(long productId) {
        Product product = findProduct(productId);
        product.deactivate(clock.instant());
        productRepository.save(product);
    }

    private Product findProduct(long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
