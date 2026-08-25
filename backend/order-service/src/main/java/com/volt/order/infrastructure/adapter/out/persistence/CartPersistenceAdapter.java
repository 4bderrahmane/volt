package com.volt.order.infrastructure.adapter.out.persistence;

import com.volt.order.application.port.out.CartRepositoryPort;
import com.volt.order.domain.model.Cart;
import com.volt.order.infrastructure.adapter.out.persistence.entity.CartJpaEntity;
import com.volt.order.infrastructure.adapter.out.persistence.mapper.CartPersistenceMapper;
import com.volt.order.infrastructure.adapter.out.persistence.repository.CartJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class CartPersistenceAdapter implements CartRepositoryPort {
    private final CartJpaRepository repository;
    private final CartPersistenceMapper mapper;

    public CartPersistenceAdapter(CartJpaRepository repository, CartPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cart> findByCustomerId(UUID customerId) {
        return repository.findByCustomerId(customerId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Cart save(Cart cart) {
        CartJpaEntity entity;
        if (cart.getId() == null) {
            entity = mapper.newEntity(cart);
        } else {
            entity = repository.findById(cart.getId())
                    .orElseThrow(() -> new IllegalStateException("Cart disappeared while being updated"));
            mapper.update(cart, entity);
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void deleteByCustomerId(UUID customerId) {
        repository.deleteByCustomerId(customerId);
    }
}
