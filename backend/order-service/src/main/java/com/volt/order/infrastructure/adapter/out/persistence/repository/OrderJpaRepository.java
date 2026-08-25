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
     * Deliberately without {@code @EntityGraph}: joining {@code order_line}
     * here would emit {@code FOR UPDATE} against the nullable side of an outer
     * join, which PostgreSQL rejects outright. The lock belongs on the
     * aggregate root row alone; lines are loaded lazily inside the same
     * transaction, which is where the mapper reads them.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select orderEntity from OrderJpaEntity orderEntity where orderEntity.id = :id")
    Optional<OrderJpaEntity> lockById(@Param("id") long id);

    @EntityGraph(attributePaths = "lines")
    List<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @EntityGraph(attributePaths = "lines")
    Optional<OrderJpaEntity> findByIdAndCustomerId(Long id, UUID customerId);
}
