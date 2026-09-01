package com.volt.order.application.port.out;

import com.volt.order.domain.model.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartRepositoryPort {

    Optional<Cart> findByCustomerId(UUID customerId);

    Cart save(Cart cart);

    void deleteByCustomerId(UUID customerId);
}
