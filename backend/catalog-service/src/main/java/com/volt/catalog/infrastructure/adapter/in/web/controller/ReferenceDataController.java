package com.volt.catalog.infrastructure.adapter.in.web.controller;

import com.volt.catalog.application.port.in.ListReferenceDataUseCase;
import com.volt.catalog.infrastructure.adapter.in.web.dto.response.BrandResponse;
import com.volt.catalog.infrastructure.adapter.in.web.dto.response.CategoryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReferenceDataController {

    private final ListReferenceDataUseCase referenceData;

    public ReferenceDataController(ListReferenceDataUseCase referenceData) {
        this.referenceData = referenceData;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return referenceData.listCategories().stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/brands")
    public List<BrandResponse> brands() {
        return referenceData.listBrands().stream().map(BrandResponse::from).toList();
    }
}
