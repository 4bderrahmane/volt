package com.volt.catalog.infrastructure.adapter.out.persistence.mapper;

import com.volt.catalog.domain.model.Brand;
import com.volt.catalog.domain.model.Category;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.BrandJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.CategoryJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ReferenceDataPersistenceMapper {

    public Category toDomain(CategoryJpaEntity entity) {
        return new Category(entity.getId(), entity.getCode(), entity.getLabel());
    }

    public Brand toDomain(BrandJpaEntity entity) {
        return new Brand(entity.getId(), entity.getName());
    }
}
