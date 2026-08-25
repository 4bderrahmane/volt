package com.volt.order.infrastructure.adapter.in.web.controller;

import com.volt.order.application.port.in.ManageCartUseCase;
import com.volt.order.application.port.in.ViewCartUseCase;
import com.volt.order.infrastructure.adapter.in.web.dto.request.AddCartLineRequest;
import com.volt.order.infrastructure.adapter.in.web.dto.request.UpdateCartLineRequest;
import com.volt.order.infrastructure.adapter.in.web.dto.response.CartResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final ManageCartUseCase manageCart;
    private final ViewCartUseCase viewCart;

    public CartController(ManageCartUseCase manageCart, ViewCartUseCase viewCart) {
        this.manageCart = manageCart;
        this.viewCart = viewCart;
    }

    @GetMapping
    public CartResponse view(@AuthenticationPrincipal Jwt jwt) {
        return CartResponse.from(viewCart.view(customer(jwt)));
    }

    @PostMapping("/lines")
    public CartResponse add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddCartLineRequest request) {
        UUID customerId = customer(jwt);
        manageCart.addLine(customerId, request.productId(), request.quantity());
        return CartResponse.from(viewCart.view(customerId));
    }

    @PutMapping("/lines/{lineId}")
    public CartResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive long lineId,
                               @Valid @RequestBody UpdateCartLineRequest request) {
        UUID customerId = customer(jwt);
        manageCart.updateLineQuantity(customerId, lineId, request.quantity());
        return CartResponse.from(viewCart.view(customerId));
    }

    @DeleteMapping("/lines/{lineId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive long lineId) {
        manageCart.removeLine(customer(jwt), lineId);
        return ResponseEntity.noContent().build();
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
