package com.volt.catalog.domain.model;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(of = {"id", "name"})
public final class Brand {

    private final Long id;
    private String name;

    public Brand(Long id, String name) {
        if (id != null && id < 1) {
            throw new IllegalArgumentException("id must be positive");
        }
        this.id = id;
        this.name = requireName(name);
    }

    public static Brand create(String name) {
        return new Brand(null, name);
    }

    public void rename(String newName) {
        this.name = requireName(newName);
    }

    private static String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > 128) {
            throw new IllegalArgumentException("name must be at most 128 characters");
        }
        return normalized;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Brand that)) {
            return false;
        }

        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
