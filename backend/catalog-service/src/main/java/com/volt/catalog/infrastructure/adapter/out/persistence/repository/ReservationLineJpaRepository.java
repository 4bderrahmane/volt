package com.volt.catalog.infrastructure.adapter.out.persistence.repository;

import com.volt.catalog.domain.model.ReservationStatus;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.ReservationLineJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ReservationLineJpaRepository extends JpaRepository<ReservationLineJpaEntity, Long> {

    @Query("select line.product.id as productId, sum(line.quantity) as quantity "
            + "from ReservationLineJpaEntity line "
            + "where line.product.id in :productIds "
            + "and line.reservation.status = :status "
            + "and line.reservation.expiresAt > :now "
            + "group by line.product.id")
    List<ReservedQuantityView> reservedQuantities(
            @Param("productIds") Collection<Long> productIds,
            @Param("status") ReservationStatus status,
            @Param("now") Instant now);

    interface ReservedQuantityView {
        Long getProductId();

        Long getQuantity();
    }
}
