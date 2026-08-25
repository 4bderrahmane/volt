package com.volt.catalog.application.port.out;

import com.volt.catalog.domain.model.Brand;
import com.volt.catalog.domain.model.Category;

import java.util.List;
import java.util.Optional;

/** Categories and brands (specification §5.1). */
public interface ReferenceDataRepositoryPort {

    List<Category> findAllCategories();

    List<Brand> findAllBrands();

    Optional<Category> findCategoryById(long categoryId);

    Optional<Brand> findBrandById(long brandId);
}
