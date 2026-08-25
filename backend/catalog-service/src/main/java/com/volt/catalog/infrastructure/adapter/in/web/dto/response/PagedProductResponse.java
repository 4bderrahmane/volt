package com.volt.catalog.infrastructure.adapter.in.web.dto.response;

import com.volt.catalog.domain.model.PagedResult;
import com.volt.catalog.domain.model.Product;

import java.util.List;

public record PagedProductResponse(
        List<ProductResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public PagedProductResponse {
        content = List.copyOf(content);
    }

    public static PagedProductResponse from(PagedResult<Product> result) {
        return new PagedProductResponse(
                result.content().stream().map(ProductResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
