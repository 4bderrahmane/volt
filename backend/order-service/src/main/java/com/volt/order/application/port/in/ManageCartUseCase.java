package com.volt.order.application.port.in;

import com.volt.order.domain.model.Cart;

import java.util.UUID;

public interface ManageCartUseCase {

    Cart addLine(UUID customerId, long productId, int quantity);

    Cart updateLineQuantity(UUID customerId, long cartLineId, int quantity);

    Cart removeLine(UUID customerId, long cartLineId);
}
