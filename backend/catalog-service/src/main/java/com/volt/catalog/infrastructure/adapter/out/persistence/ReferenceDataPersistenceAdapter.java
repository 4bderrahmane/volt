package com.volt.catalog.infrastructure.adapter.out.persistence;

import com.volt.catalog.application.port.out.ReferenceDataRepositoryPort;
import com.volt.catalog.domain.model.Brand;
import com.volt.catalog.domain.model.Category;
import com.volt.catalog.infrastructure.adapter.out.persistence.mapper.ReferenceDataPersistenceMapper;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.BrandJpaRepository;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.CategoryJpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ReferenceDataPersistenceAdapter implements ReferenceDataRepositoryPort {

    private final CategoryJpaRepository categories;
    private final BrandJpaRepository brands;
    private final ReferenceDataPersistenceMapper mapper;

    public ReferenceDataPersistenceAdapter(
            CategoryJpaRepository categories,
            BrandJpaRepository brands,
            ReferenceDataPersistenceMapper mapper) {
        this.categories = categories;
        this.brands = brands;
        this.mapper = mapper;
    }

    @Override
    public List<Category> findAllCategories() {
        return categories.findAll(Sort.by("label")).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Brand> findAllBrands() {
        return brands.findAll(Sort.by("name")).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Category> findCategoryById(long categoryId) {
        return categories.findById(categoryId).map(mapper::toDomain);
    }

    @Override
    public Optional<Brand> findBrandById(long brandId) {
        return brands.findById(brandId).map(mapper::toDomain);
    }
}
