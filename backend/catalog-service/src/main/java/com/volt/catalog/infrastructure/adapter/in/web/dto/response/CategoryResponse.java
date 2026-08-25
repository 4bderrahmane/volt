package com.volt.catalog.infrastructure.adapter.in.web.dto.response;

import com.volt.catalog.domain.model.Category;

public record CategoryResponse(Long id, String code, String label) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getCode(), category.getLabel());
    }
}
