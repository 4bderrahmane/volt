package com.volt.order.infrastructure.adapter.out.persistence.mapper;

import com.volt.order.domain.model.Cart;
import com.volt.order.domain.model.CartLine;
import com.volt.order.infrastructure.adapter.out.persistence.entity.CartJpaEntity;
import com.volt.order.infrastructure.adapter.out.persistence.entity.CartLineJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CartPersistenceMapper {
    public Cart toDomain(CartJpaEntity entity) {
        List<CartLine> lines = entity.getLines().stream()
                .map(line -> CartLine.rehydrate(line.getId(), line.getProductId(), line.getQuantity()))
                .toList();
        return Cart.rehydrate(entity.getId(), entity.getCustomerId(), lines, entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public CartJpaEntity newEntity(Cart cart) {
        CartJpaEntity entity = CartJpaEntity.builder()
                .customerId(cart.getCustomerId())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
        replaceLines(cart, entity);
        return entity;
    }

    public void update(Cart cart, CartJpaEntity entity) {
        entity.setCustomerId(cart.getCustomerId());
        entity.setCreatedAt(cart.getCreatedAt());
        entity.setUpdatedAt(cart.getUpdatedAt());
        replaceLines(cart, entity);
    }

    private void replaceLines(Cart cart, CartJpaEntity entity) {
        Map<Long, CartLineJpaEntity> existing = new HashMap<>();
        for (CartLineJpaEntity line : entity.getLines()) {
            if (line.getId() != null) existing.put(line.getId(), line);
        }
        List<CartLineJpaEntity> replacement = new ArrayList<>();
        for (CartLine line : cart.getLines()) {
            // The no-arg constructor is reserved for Hibernate; the builder is public.
            CartLineJpaEntity mapped = line.getId() == null
                    ? CartLineJpaEntity.builder().build()
                    : existing.get(line.getId());
            if (mapped == null) throw new IllegalStateException("cart line " + line.getId() + " is not owned by cart " + cart.getId());
            mapped.setCart(entity);
            mapped.setProductId(line.getProductId());
            mapped.setQuantity(line.getQuantity());
            replacement.add(mapped);
        }
        entity.getLines().clear();
        entity.getLines().addAll(replacement);
    }
}
