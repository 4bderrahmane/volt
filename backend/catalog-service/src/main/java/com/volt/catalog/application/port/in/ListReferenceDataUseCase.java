package com.volt.catalog.application.port.in;

import com.volt.catalog.domain.model.Brand;
import com.volt.catalog.domain.model.Category;

import java.util.List;

/** Specification §F2, §6.1 — categories and brands for the filter controls. */
public interface ListReferenceDataUseCase {

    List<Category> listCategories();

    List<Brand> listBrands();
}
