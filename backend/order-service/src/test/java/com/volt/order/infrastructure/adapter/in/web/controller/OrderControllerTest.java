package com.volt.order.infrastructure.adapter.in.web.controller;

import com.volt.order.application.port.in.ChangeOrderStatusUseCase;
import com.volt.order.application.port.in.PlaceOrderUseCase;
import com.volt.order.application.port.in.ViewOrdersUseCase;
import com.volt.order.domain.exception.CatalogUnavailableException;
import com.volt.order.domain.exception.EmptyCartException;
import com.volt.order.domain.exception.IllegalStatusTransitionException;
import com.volt.order.domain.exception.InsufficientStockException;
import com.volt.order.domain.exception.OrderNotFoundException;
import com.volt.order.domain.model.Order;
import com.volt.order.domain.model.OrderLine;
import com.volt.order.domain.model.OrderStatus;
import com.volt.order.domain.model.OrderTotals;
import com.volt.order.domain.model.StockShortage;
import com.volt.order.domain.model.VatRate;
import com.volt.order.infrastructure.adapter.in.web.advice.GlobalExceptionHandler;
import com.volt.order.infrastructure.config.KeycloakRealmRoleConverter;
import com.volt.order.infrastructure.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises order identity and role rules through the real security filter chain. */
@WebMvcTest(OrderController.class)
@Import({SecurityConfiguration.class, KeycloakRealmRoleConverter.class, GlobalExceptionHandler.class})
class OrderControllerTest {

    private static final UUID CUSTOMER = UUID.fromString("8fe20245-6121-4d3c-92d0-05cc3271d3d1");
    private static final UUID OTHER_CUSTOMER = UUID.fromString("2b0d0a37-2a4a-4a58-9a1a-b3c4d5e6f708");
    private static final Instant NOW = Instant.parse("2026-08-17T10:00:00Z");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PlaceOrderUseCase placeOrder;
    @MockitoBean private ViewOrdersUseCase viewOrders;
    @MockitoBean private ChangeOrderStatusUseCase changeStatus;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void checkoutReturns201WithALocationHeaderAndThePricedOrder() throws Exception {
        when(placeOrder.placeOrder(CUSTOMER)).thenReturn(order(42L, OrderStatus.CONFIRMED, CUSTOMER));

        mockMvc.perform(post("/api/v1/orders").with(client(CUSTOMER)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/orders/42"))
                .andExpect(jsonPath("$.number").value("ORD-2026-000001"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totals.totalInclVat").value(24.00))
                .andExpect(jsonPath("$.lines[0].productReference").value("REF-7"));
    }

    @Test
    void checkoutOfAnEmptyCartIsA422ProblemDetail() throws Exception {
        when(placeOrder.placeOrder(CUSTOMER)).thenThrow(new EmptyCartException());

        mockMvc.perform(post("/api/v1/orders").with(client(CUSTOMER)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/empty-cart"))
                .andExpect(jsonPath("$.title").value("Empty cart"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("Cannot place an order from an empty cart"));
    }

    @Test
    void checkoutWithoutStockIsA409CarryingTheShortages() throws Exception {
        when(placeOrder.placeOrder(CUSTOMER))
                .thenThrow(new InsufficientStockException(List.of(new StockShortage(7L, "REF-7", 4, 2))));

        mockMvc.perform(post("/api/v1/orders").with(client(CUSTOMER)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/insufficient-stock"))
                .andExpect(jsonPath("$.shortages[0].productId").value(7))
                .andExpect(jsonPath("$.shortages[0].reference").value("REF-7"))
                .andExpect(jsonPath("$.shortages[0].requested").value(4))
                .andExpect(jsonPath("$.shortages[0].available").value(2));
    }

    @Test
    void anUnreachableCatalogIsA503RatherThanA500() throws Exception {
        when(placeOrder.placeOrder(CUSTOMER)).thenThrow(new CatalogUnavailableException("catalog timed out", null));

        mockMvc.perform(post("/api/v1/orders").with(client(CUSTOMER)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/catalog-unavailable"));
    }

    @Test
    void historyIsScopedToTheAuthenticatedSubject() throws Exception {
        when(viewOrders.listForCustomer(CUSTOMER)).thenReturn(List.of(order(42L, OrderStatus.CONFIRMED, CUSTOMER)));

        mockMvc.perform(get("/api/v1/orders").with(client(CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(42));

        verify(viewOrders).listForCustomer(CUSTOMER);
    }

    @Test
    void anotherCustomersOrderIsNotFoundRatherThanForbidden() throws Exception {
        when(viewOrders.getForCustomer(OTHER_CUSTOMER, 42L)).thenThrow(new OrderNotFoundException(42L));

        mockMvc.perform(get("/api/v1/orders/42").with(client(OTHER_CUSTOMER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/not-found"))
                .andExpect(jsonPath("$.detail").value("No order with id 42"));

        // 404 rather than 403 on purpose: a 403 would confirm that order 42
        // exists, which is an ownership oracle for anyone enumerating ids.
        verify(viewOrders).getForCustomer(OTHER_CUSTOMER, 42L);
    }

    @Test
    void theOwnerReadsTheirOwnOrderDetail() throws Exception {
        when(viewOrders.getForCustomer(CUSTOMER, 42L)).thenReturn(order(42L, OrderStatus.CONFIRMED, CUSTOMER));

        mockMvc.perform(get("/api/v1/orders/42").with(client(CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.lines.length()").value(1));
    }

    @Test
    void aNonPositiveOrderIdIsRejectedBeforeReachingTheUseCase() throws Exception {
        mockMvc.perform(get("/api/v1/orders/0").with(client(CUSTOMER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/invalid-value"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(viewOrders);
    }

    @Test
    void aTokenWhoseSubjectIsNotAUuidIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/orders").with(jwt()
                        .jwt(token -> token.subject("not-a-uuid"))
                        .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/invalid-value"));

        verifyNoInteractions(viewOrders);
    }

    @Test
    void statusChangesRequireAdminAndRejectClientsAndAnonymousCallers() throws Exception {
        String body = "{\"status\":\"SHIPPED\"}";

        mockMvc.perform(statusRequest(body)).andExpect(status().isUnauthorized());
        mockMvc.perform(statusRequest(body).with(client(CUSTOMER))).andExpect(status().isForbidden());

        verifyNoInteractions(changeStatus);
    }

    @Test
    void anAdminMayShipAnOrder() throws Exception {
        when(changeStatus.changeStatus(42L, OrderStatus.SHIPPED)).thenReturn(order(42L, OrderStatus.SHIPPED, CUSTOMER));

        mockMvc.perform(statusRequest("{\"status\":\"SHIPPED\"}").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void anIllegalTransitionIsA409ProblemDetail() throws Exception {
        doThrow(new IllegalStatusTransitionException(OrderStatus.DELIVERED, OrderStatus.CANCELLED))
                .when(changeStatus).changeStatus(anyLong(), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(statusRequest("{\"status\":\"CANCELLED\"}").with(admin()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/illegal-status-transition"));
    }

    @Test
    void aMissingStatusFieldIsA400ValidationProblem() throws Exception {
        mockMvc.perform(statusRequest("{}").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Request validation failed"));

        verifyNoInteractions(changeStatus);
    }

    @Test
    void anUnknownStatusValueIsRejected() throws Exception {
        mockMvc.perform(statusRequest("{\"status\":\"TELEPORTED\"}").with(admin()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(changeStatus);
    }

    private static MockHttpServletRequestBuilder statusRequest(String body) {
        return patch("/api/v1/orders/42/status").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor client(UUID subject) {
        return jwt().jwt(token -> token.subject(subject.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"));
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor admin() {
        return jwt().jwt(token -> token.subject(CUSTOMER.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static Order order(long id, OrderStatus status, UUID customerId) {
        OrderLine line = OrderLine.rehydrate(10L, 7L, "REF-7", "Cable",
                new BigDecimal("10.00"), 2, new BigDecimal("20.00"));
        return Order.rehydrate(id, "ORD-2026-000001", customerId, status, null,
                OrderTotals.calculate(List.of(line), VatRate.STANDARD), List.of(line), NOW, NOW);
    }
}
