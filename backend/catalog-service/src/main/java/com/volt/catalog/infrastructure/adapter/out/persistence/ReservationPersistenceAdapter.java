package com.volt.catalog.infrastructure.adapter.out.persistence;

import com.volt.catalog.application.port.out.ReservationRepositoryPort;
import com.volt.catalog.domain.model.Reservation;
import com.volt.catalog.domain.model.ReservationStatus;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.ReservationJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.mapper.ReservationPersistenceMapper;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.ProductJpaRepository;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.ReservationJpaRepository;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.ReservationLineJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class ReservationPersistenceAdapter implements ReservationRepositoryPort {

    private final ReservationJpaRepository reservations;
    private final ReservationLineJpaRepository lines;
    private final ProductJpaRepository products;
    private final ReservationPersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    public void lockOrderRef(String orderRef) {
        entityManager.createNativeQuery(
                        "SELECT 1 FROM (SELECT pg_advisory_xact_lock(" + "hashtextextended(CAST(:orderRef AS TEXT), 0))) held")
                .setParameter("orderRef", orderRef)
                .getSingleResult();
    }

    @Override
    public Optional<Reservation> findById(long reservationId) {
        return reservations.findById(reservationId).map(mapper::toDomain);
    }

    @Override
    public Optional<Reservation> lockById(long reservationId) {
        return reservations.lockById(reservationId).map(mapper::toDomain);
    }

    @Override
    public Optional<Reservation> findByOrderRef(String orderRef) {
        return reservations.findByOrderRef(orderRef).map(mapper::toDomain);
    }

    @Override
    public Optional<Reservation> lockByOrderRef(String orderRef) {
        return reservations.lockByOrderRef(orderRef).map(mapper::toDomain);
    }

    @Override
    public Reservation save(Reservation reservation) {
        ReservationJpaEntity entity;
        if (reservation.getId() == null) {
            entity = mapper.toNewEntity(reservation, products::getReferenceById);
        } else {
            entity = reservations.findById(reservation.getId())
                    .orElseThrow(() -> new IllegalStateException("reservation disappeared during transaction"));
            mapper.updateEntity(reservation, entity);
        }
        return mapper.toDomain(reservations.save(entity));
    }

    @Override
    public Map<Long, Integer> reservedQuantities(List<Long> productIds, Instant now) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (ReservationLineJpaRepository.ReservedQuantityView row
                : lines.reservedQuantities(productIds, ReservationStatus.ACTIVE, now)) {
            result.put(row.getProductId(), Math.toIntExact(row.getQuantity()));
        }
        return result;
    }

    @Override
    public List<Reservation> findExpired(Instant now) {
        return reservations.lockExpired(ReservationStatus.ACTIVE, now).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
