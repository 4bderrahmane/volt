package com.volt.order.infrastructure.adapter.out.persistence.repository;

import com.volt.order.infrastructure.adapter.out.persistence.entity.CartJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartJpaRepository extends JpaRepository<CartJpaEntity, Long> {
    @EntityGraph(attributePaths = "lines")
    Optional<CartJpaEntity> findByCustomerId(UUID customerId);

    void deleteByCustomerId(UUID customerId);
}
