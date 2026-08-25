package com.volt.order.application.port.in;

import com.volt.order.domain.model.Cart;

import java.util.UUID;

/**
 * Specification §F6 — cart mutation.
 *
 * <p>Every method takes the customer id explicitly. It arrives from the JWT
 * {@code sub} claim, and passing it as an argument rather than reading a
 * SecurityContext inside the use case keeps the application layer free of Spring
 * Security and keeps these methods unit-testable (specification §4 rule 5).
 */
public interface ManageCartUseCase {

    Cart addLine(UUID customerId, long productId, int quantity);

    Cart updateLineQuantity(UUID customerId, long cartLineId, int quantity);

    Cart removeLine(UUID customerId, long cartLineId);
}
