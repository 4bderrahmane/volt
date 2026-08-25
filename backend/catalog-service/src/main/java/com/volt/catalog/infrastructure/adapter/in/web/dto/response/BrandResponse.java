package com.volt.catalog.infrastructure.adapter.in.web.dto.response;

import com.volt.catalog.domain.model.Brand;

public record BrandResponse(Long id, String name) {

    public static BrandResponse from(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getName());
    }
}
