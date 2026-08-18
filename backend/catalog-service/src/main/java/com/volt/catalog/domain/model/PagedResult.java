package com.volt.catalog.domain.model;

import java.util.List;

/**
 * Pagination in domain terms.
 *
 * <p>This type exists so that {@code org.springframework.data.domain.Page} never
 * appears in an out-port signature. Returning Spring's {@code Page} from a port
 * is the most common way this codebase would break specification §4 rule 1, because it
 * looks harmless and compiles fine.
 */
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
