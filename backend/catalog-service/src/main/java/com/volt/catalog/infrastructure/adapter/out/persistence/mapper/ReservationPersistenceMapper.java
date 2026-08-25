package com.volt.catalog.infrastructure.adapter.out.persistence.mapper;

import com.volt.catalog.domain.model.Reservation;
import com.volt.catalog.domain.model.ReservationLine;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.ProductJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.ReservationJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.ReservationLineJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
public class ReservationPersistenceMapper {

    public Reservation toDomain(ReservationJpaEntity entity) {
        List<ReservationLine> lines = entity.getLines().stream()
                .map(line -> new ReservationLine(line.getId(), line.getProduct().getId(), line.getQuantity()))
                .toList();
        return new Reservation(
                entity.getId(),
                entity.getOrderRef(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                lines);
    }

    public ReservationJpaEntity toNewEntity(
            Reservation reservation,
            Function<Long, ProductJpaEntity> productReference) {
        ReservationJpaEntity entity = ReservationJpaEntity.builder()
                .orderRef(reservation.getOrderRef())
                .status(reservation.getStatus())
                .expiresAt(reservation.getExpiresAt())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
        List<ReservationLineJpaEntity> lines = reservation.getLines().stream()
                .map(line -> ReservationLineJpaEntity.builder()
                        .reservation(entity)
                        .product(productReference.apply(line.getProductId()))
                        .quantity(line.getQuantity())
                        .build())
                .toList();
        entity.getLines().addAll(lines);
        return entity;
    }

    public void updateEntity(Reservation reservation, ReservationJpaEntity entity) {
        entity.setStatus(reservation.getStatus());
        entity.setUpdatedAt(reservation.getUpdatedAt());
    }
}
