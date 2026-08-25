package com.volt.catalog.infrastructure.adapter.out.persistence.repository;

import com.volt.catalog.infrastructure.adapter.out.persistence.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {
}
