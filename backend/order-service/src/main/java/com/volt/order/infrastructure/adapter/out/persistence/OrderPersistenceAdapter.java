package com.volt.order.infrastructure.adapter.out.persistence;

import com.volt.order.application.port.out.OrderRepositoryPort;
import com.volt.order.domain.model.Order;
import com.volt.order.infrastructure.adapter.out.persistence.entity.OrderJpaEntity;
import com.volt.order.infrastructure.adapter.out.persistence.mapper.OrderPersistenceMapper;
import com.volt.order.infrastructure.adapter.out.persistence.repository.OrderJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderPersistenceAdapter implements OrderRepositoryPort {
    private final OrderJpaRepository repository;
    private final OrderPersistenceMapper mapper;

    public OrderPersistenceAdapter(OrderJpaRepository repository, OrderPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(long orderId) {
        return repository.findById(orderId).map(mapper::toDomain);
    }

    /**
     * {@code MANDATORY} on purpose. A row lock taken in its own transaction is
     * released the instant this method returns, so the caller would hold
     * nothing while still believing it had exclusive access — the most
     * expensive kind of no-op. Requiring an inherited transaction turns that
     * mistake into a startup-time failure instead of a race in production.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Order> findByIdForUpdate(long orderId) {
        return repository.lockById(orderId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findByIdAndCustomerId(long orderId, UUID customerId) {
        return repository.findByIdAndCustomerId(orderId, customerId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(UUID customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Order save(Order order) {
        OrderJpaEntity entity;
        if (order.getId() == null) {
            entity = mapper.newEntity(order);
        } else {
            entity = repository.findById(order.getId())
                    .orElseThrow(() -> new IllegalStateException("Order disappeared while being updated"));
            mapper.update(order, entity);
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }
}
