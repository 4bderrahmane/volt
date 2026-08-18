package com.volt.catalog.domain.model;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(of = {"id", "code"})
public final class Category {

    private final Long id;
    private final String code;
    private String label;

    public Category(Long id, String code, String label) {
        if (id != null && id < 1) {
            throw new IllegalArgumentException("id must be positive");
        }
        this.id = id;
        this.code = requireText(code, "code", 32);
        this.label = requireText(label, "label", 128);
    }

    public static Category create(String code, String label) {
        return new Category(null, code, label);
    }

    public void relabel(String newLabel) {
        this.label = requireText(newLabel, "label", 128);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof Category that)) {
            return false;
        }

        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
