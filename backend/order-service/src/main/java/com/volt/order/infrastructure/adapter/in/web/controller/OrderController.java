package com.volt.order.infrastructure.adapter.in.web.controller;

import com.volt.order.application.port.in.ChangeOrderStatusUseCase;
import com.volt.order.application.port.in.PlaceOrderUseCase;
import com.volt.order.application.port.in.ViewOrdersUseCase;
import com.volt.order.domain.model.Order;
import com.volt.order.infrastructure.adapter.in.web.dto.request.ChangeOrderStatusRequest;
import com.volt.order.infrastructure.adapter.in.web.dto.response.OrderResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final PlaceOrderUseCase placeOrder;
    private final ViewOrdersUseCase viewOrders;
    private final ChangeOrderStatusUseCase changeStatus;

    public OrderController(PlaceOrderUseCase placeOrder, ViewOrdersUseCase viewOrders,
                           ChangeOrderStatusUseCase changeStatus) {
        this.placeOrder = placeOrder;
        this.viewOrders = viewOrders;
        this.changeStatus = changeStatus;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal Jwt jwt) {
        Order order = placeOrder.placeOrder(customer(jwt));
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.getId())).body(OrderResponse.from(order));
    }

    @GetMapping
    public List<OrderResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return viewOrders.listForCustomer(customer(jwt)).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive long orderId) {
        return OrderResponse.from(viewOrders.getForCustomer(customer(jwt), orderId));
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse changeStatus(@PathVariable @Positive long orderId,
                                      @Valid @RequestBody ChangeOrderStatusRequest request) {
        return OrderResponse.from(changeStatus.changeStatus(orderId, request.status()));
    }

    private static UUID customer(Jwt jwt) {
        if (jwt == null) throw new IllegalArgumentException("authenticated customer is required");
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException invalidSubject) {
            throw new IllegalArgumentException("authenticated subject must be a UUID", invalidSubject);
        }
    }
}
