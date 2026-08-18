package com.volt.catalog.domain.model;

/**
 * Search parameters for specification §F1/§F2. A domain type rather than a bag of
 * method arguments, so adding a filter does not change every signature between
 * the controller and the repository.
 */
public record ProductSearchCriteria(
        String query,
        Long categoryId,
        Long brandId,
        boolean activeOnly,
        int page,
        int size) {

    public static final int MAX_PAGE_SIZE = 100;

    public ProductSearchCriteria {
        if (categoryId != null && categoryId < 1) {
            throw new IllegalArgumentException("categoryId must be positive");
        }
        if (brandId != null && brandId < 1) {
            throw new IllegalArgumentException("brandId must be positive");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
