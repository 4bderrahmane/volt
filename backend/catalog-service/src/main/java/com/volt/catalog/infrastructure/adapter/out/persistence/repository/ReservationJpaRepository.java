package com.volt.catalog.infrastructure.adapter.out.persistence.repository;

import com.volt.catalog.domain.model.ReservationStatus;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.ReservationJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {

    Optional<ReservationJpaEntity> findByOrderRef(String orderRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from ReservationJpaEntity reservation where reservation.orderRef = :orderRef")
    Optional<ReservationJpaEntity> lockByOrderRef(@Param("orderRef") String orderRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from ReservationJpaEntity reservation where reservation.id = :id")
    Optional<ReservationJpaEntity> lockById(@Param("id") long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from ReservationJpaEntity reservation "
            + "where reservation.status = :status and reservation.expiresAt <= :now "
            + "order by reservation.id")
    List<ReservationJpaEntity> lockExpired(
            @Param("status") ReservationStatus status,
            @Param("now") Instant now);
}
