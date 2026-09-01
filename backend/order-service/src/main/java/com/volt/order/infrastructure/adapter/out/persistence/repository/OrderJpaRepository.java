package com.volt.order.infrastructure.adapter.out.persistence.repository;

import com.volt.order.infrastructure.adapter.out.persistence.entity.OrderJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {
    @Override
    @EntityGraph(attributePaths = "lines")
    Optional<OrderJpaEntity> findById(Long id);

    /**
     * No {@code @EntityGraph}: PostgreSQL rejects {@code FOR UPDATE} on the
     * nullable side of the resulting outer join. Lock only the aggregate root.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT orderEntity FROM OrderJpaEntity orderEntity WHERE orderEntity.id = :id")
    Optional<OrderJpaEntity> lockById(@Param("id") long id);

    @EntityGraph(attributePaths = "lines")
    List<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @EntityGraph(attributePaths = "lines")
    Optional<OrderJpaEntity> findByIdAndCustomerId(Long id, UUID customerId);
}
