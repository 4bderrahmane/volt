package com.volt.catalog.domain.model;

import java.util.List;

public record PagedResult<T>(List<T> content, int page, int size, long totalElements) {

    public PagedResult {
        content = List.copyOf(content);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return (long) (page + 1) * size < totalElements;
    }
}
