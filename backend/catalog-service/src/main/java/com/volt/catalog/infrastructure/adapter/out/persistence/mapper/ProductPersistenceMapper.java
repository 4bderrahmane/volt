package com.volt.catalog.infrastructure.adapter.out.persistence.mapper;

import com.volt.catalog.domain.model.Product;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.BrandJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.CategoryJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.ProductJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductPersistenceMapper {

    public Product toDomain(ProductJpaEntity entity) {
        return new Product(
                entity.getId(),
                entity.getReference(),
                entity.getLabel(),
                entity.getDescription(),
                entity.getPriceExclVat(),
                entity.getUnit(),
                entity.getStockQuantity(),
                entity.isActive(),
                entity.getCategory().getId(),
                entity.getBrand().getId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ProductJpaEntity toNewEntity(
            Product product,
            CategoryJpaEntity category,
            BrandJpaEntity brand) {
        return ProductJpaEntity.builder()
                .reference(product.getReference())
                .label(product.getLabel())
                .description(product.getDescription())
                .priceExclVat(product.getPriceExclVat())
                .unit(product.getUnit())
                .stockQuantity(product.getStockQuantity())
                .active(product.isActive())
                .category(category)
                .brand(brand)
                .version(0L)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public void updateEntity(Product product, ProductJpaEntity entity, CategoryJpaEntity category, BrandJpaEntity brand) {
        entity.setLabel(product.getLabel());
        entity.setDescription(product.getDescription());
        entity.setPriceExclVat(product.getPriceExclVat());
        entity.setUnit(product.getUnit());
        entity.setStockQuantity(product.getStockQuantity());
        entity.setActive(product.isActive());
        entity.setCategory(category);
        entity.setBrand(brand);
        entity.setUpdatedAt(product.getUpdatedAt());
    }
}
