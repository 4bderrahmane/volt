package com.volt.catalog.infrastructure.adapter.out.persistence;

import com.volt.catalog.application.port.out.ProductRepositoryPort;
import com.volt.catalog.domain.exception.ProductNotFoundException;
import com.volt.catalog.domain.model.PagedResult;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.ProductSearchCriteria;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.BrandJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.CategoryJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.ProductJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.mapper.ProductPersistenceMapper;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.BrandJpaRepository;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.CategoryJpaRepository;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.ProductJpaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductPersistenceAdapter implements ProductRepositoryPort {

    private final ProductJpaRepository products;
    private final CategoryJpaRepository categories;
    private final BrandJpaRepository brands;
    private final ProductPersistenceMapper mapper;

    @Override
    public Optional<Product> findById(long productId) {
        return products.findById(productId).map(mapper::toDomain);
    }

    @Override
    public Optional<Product> findByReference(String reference) {
        return products.findByReference(reference).map(mapper::toDomain);
    }

    @Override
    public List<Product> findAllByIds(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        return products.findAllByIdIn(productIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PagedResult<Product> search(ProductSearchCriteria criteria) {
        PageRequest pageable = PageRequest.of(
                criteria.page(),
                criteria.size(),
                Sort.by(Sort.Order.asc("label"), Sort.Order.asc("id")));
        Page<ProductJpaEntity> result = products.findAll(specification(criteria), pageable);
        return new PagedResult<>(
                result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    @Override
    public Product save(Product product) {
        CategoryJpaEntity category = categories.findById(product.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("unknown categoryId " + product.getCategoryId()));
        BrandJpaEntity brand = brands.findById(product.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("unknown brandId " + product.getBrandId()));

        ProductJpaEntity entity;
        if (product.getId() == null) {
            entity = mapper.toNewEntity(product, category, brand);
        } else {
            entity = products.findById(product.getId())
                    .orElseThrow(() -> new ProductNotFoundException(product.getId()));
            mapper.updateEntity(product, entity, category, brand);
        }
        return mapper.toDomain(products.save(entity));
    }

    @Override
    public List<Product> lockForUpdate(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        return products.lockAllByIds(productIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    private static Specification<ProductJpaEntity> specification(ProductSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.activeOnly()) {
                predicates.add(builder.isTrue(root.get("active")));
            }
            if (criteria.categoryId() != null) {
                predicates.add(builder.equal(root.get("category").get("id"), criteria.categoryId()));
            }
            if (criteria.brandId() != null) {
                predicates.add(builder.equal(root.get("brand").get("id"), criteria.brandId()));
            }
            if (criteria.query() != null && !criteria.query().isBlank()) {
                String pattern = "%" + escapeLike(criteria.query().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("label")), pattern, '\\'),
                        builder.like(builder.lower(root.get("reference")), pattern, '\\')));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
