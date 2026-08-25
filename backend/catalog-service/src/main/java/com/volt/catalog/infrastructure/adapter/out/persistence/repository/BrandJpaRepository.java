package com.volt.catalog.infrastructure.adapter.out.persistence.repository;

import com.volt.catalog.infrastructure.adapter.out.persistence.entity.BrandJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandJpaRepository extends JpaRepository<BrandJpaEntity, Long> {
}
