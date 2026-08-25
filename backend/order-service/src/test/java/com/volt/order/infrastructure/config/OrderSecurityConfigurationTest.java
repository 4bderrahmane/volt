package com.volt.order.infrastructure.config;

import com.volt.order.application.port.in.ChangeOrderStatusUseCase;
import com.volt.order.application.port.in.ManageCartUseCase;
import com.volt.order.application.port.in.PlaceOrderUseCase;
import com.volt.order.application.port.in.ViewCartUseCase;
import com.volt.order.application.port.in.ViewOrdersUseCase;
import com.volt.order.domain.model.Order;
import com.volt.order.domain.model.OrderLine;
import com.volt.order.domain.model.OrderStatus;
import com.volt.order.domain.model.OrderTotals;
import com.volt.order.domain.model.VatRate;
import com.volt.order.infrastructure.adapter.in.web.controller.CartController;
import com.volt.order.infrastructure.adapter.in.web.advice.GlobalExceptionHandler;
import com.volt.order.infrastructure.adapter.in.web.controller.OrderController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CartController.class, OrderController.class})
@Import({SecurityConfiguration.class, KeycloakRealmRoleConverter.class, GlobalExceptionHandler.class})
class OrderSecurityConfigurationTest {
    private static final UUID CUSTOMER = UUID.fromString("8fe20245-6121-4d3c-92d0-05cc3271d3d1");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ManageCartUseCase manageCart;
    @MockitoBean private ViewCartUseCase viewCart;
    @MockitoBean private PlaceOrderUseCase placeOrder;
    @MockitoBean private ViewOrdersUseCase viewOrders;
    @MockitoBean private ChangeOrderStatusUseCase changeStatus;
    @MockitoBean private JwtDecoder jwtDecoder;

    @BeforeEach
    void responses() {
        when(viewCart.view(CUSTOMER)).thenReturn(new ViewCartUseCase.CartView(List.of(), OrderTotals.zero()));
        when(changeStatus.changeStatus(1L, OrderStatus.SHIPPED)).thenReturn(order(OrderStatus.SHIPPED));
    }

    @Test
    void protectsClientRoutesAndUsesJwtSubjectIdentity() throws Exception {
        mockMvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/cart").with(jwt()
                        .jwt(token -> token.subject(CUSTOMER.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totals.totalInclVat").value(0.00));
    }

    @Test
    void permitsOnlyAdminToChangeStatus() throws Exception {
        String body = "{\"status\":\"SHIPPED\"}";
        mockMvc.perform(patch("/api/v1/orders/1/status")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/orders/1/status")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    private static Order order(OrderStatus status) {
        Instant now = Instant.parse("2026-08-17T10:00:00Z");
        OrderLine line = OrderLine.rehydrate(10L, 7L, "REF-7", "Cable",
                new BigDecimal("10.00"), 1, new BigDecimal("10.00"));
        return Order.rehydrate(1L, "ORD-2026-000001", CUSTOMER, status, null,
                OrderTotals.calculate(List.of(line), VatRate.STANDARD), List.of(line), now, now);
    }
}
