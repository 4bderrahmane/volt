package com.volt.order.infrastructure.adapter.in.web.controller;

import com.volt.order.application.port.in.ManageCartUseCase;
import com.volt.order.application.port.in.ViewCartUseCase;
import com.volt.order.domain.exception.CartLineNotFoundException;
import com.volt.order.domain.exception.CatalogUnavailableException;
import com.volt.order.domain.model.CartLine;
import com.volt.order.domain.model.OrderTotals;
import com.volt.order.domain.model.ProductSnapshot;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract for {@code /api/v1/cart}, through the real filter chain.
 *
 * <p>The cart has no id in its URLs — every route resolves to the cart of the
 * JWT subject — so the tests that matter most are the ones asserting the
 * customer id reaching the use case came from the token.
 */
@WebMvcTest(CartController.class)
@Import({SecurityConfiguration.class, KeycloakRealmRoleConverter.class, GlobalExceptionHandler.class})
class CartControllerTest {

    private static final UUID CUSTOMER = UUID.fromString("8fe20245-6121-4d3c-92d0-05cc3271d3d1");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ManageCartUseCase manageCart;
    @MockitoBean private ViewCartUseCase viewCart;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void anAnonymousCallerCannotTouchTheCart() throws Exception {
        mockMvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/cart/lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":7,\"quantity\":1}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(manageCart, viewCart);
    }

    /**
     * The chain grants {@code /api/v1/**} to ROLE_CLIENT. A valid token without
     * that role is authenticated but not authorized, and must not fall through
     * to the controller.
     */
    @Test
    void aTokenWithoutTheClientRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/cart").with(jwt().jwt(token -> token.subject(CUSTOMER.toString()))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(viewCart);
    }

    @Test
    void rendersTheCartOfTheAuthenticatedSubjectWithPricedLines() throws Exception {
        when(viewCart.view(CUSTOMER)).thenReturn(cartWithOneLine());

        mockMvc.perform(get("/api/v1/cart").with(client()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].lineId").value(10))
                .andExpect(jsonPath("$.items[0].productId").value(7))
                .andExpect(jsonPath("$.items[0].reference").value("REF-7"))
                .andExpect(jsonPath("$.items[0].unitPriceExclVat").value(10.00))
                .andExpect(jsonPath("$.items[0].lineTotalExclVat").value(20.00))
                .andExpect(jsonPath("$.items[0].availableQuantity").value(5))
                .andExpect(jsonPath("$.items[0].active").value(true))
                .andExpect(jsonPath("$.totals.totalInclVat").value(24.00));

        verify(viewCart).view(CUSTOMER);
    }

    @Test
    void addingALineReturnsTheRecalculatedCart() throws Exception {
        when(viewCart.view(CUSTOMER)).thenReturn(cartWithOneLine());

        mockMvc.perform(post("/api/v1/cart/lines").with(client())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":7,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        verify(manageCart).addLine(CUSTOMER, 7L, 2);
    }

    @Test
    void updatingALineQuantityReturnsTheRecalculatedCart() throws Exception {
        when(viewCart.view(CUSTOMER)).thenReturn(cartWithOneLine());

        mockMvc.perform(put("/api/v1/cart/lines/10").with(client())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk());

        verify(manageCart).updateLineQuantity(CUSTOMER, 10L, 3);
    }

    @Test
    void removingALineReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/cart/lines/10").with(client()))
                .andExpect(status().isNoContent());

        verify(manageCart).removeLine(CUSTOMER, 10L);
    }

    @Test
    void aLineBelongingToAnotherCartIsA404ProblemDetail() throws Exception {
        org.mockito.Mockito.doThrow(new CartLineNotFoundException(99L))
                .when(manageCart).removeLine(CUSTOMER, 99L);

        mockMvc.perform(delete("/api/v1/cart/lines/99").with(client()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/not-found"))
                .andExpect(jsonPath("$.detail").value("No cart line with id 99"));
    }

    @Test
    void aNonPositiveQuantityIsA400ValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/cart/lines").with(client())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":7,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/validation-error"));

        verifyNoInteractions(manageCart);
    }

    @Test
    void aMissingProductIdIsA400ValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/cart/lines").with(client())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/validation-error"));

        verifyNoInteractions(manageCart);
    }

    @Test
    void aNonPositiveLineIdIsRejectedBeforeReachingTheUseCase() throws Exception {
        mockMvc.perform(delete("/api/v1/cart/lines/0").with(client()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/invalid-value"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(manageCart);
    }

    /**
     * Cart rendering asks the catalog for live prices, so it inherits the
     * catalog's availability. A 503 keeps that distinguishable from an empty
     * cart, which would otherwise look identical to the SPA.
     */
    @Test
    void anUnreachableCatalogWhileRenderingIsA503() throws Exception {
        when(viewCart.view(CUSTOMER)).thenThrow(new CatalogUnavailableException("catalog timed out", null));

        mockMvc.perform(get("/api/v1/cart").with(client()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/catalog-unavailable"));
    }

    // -------------------------------------------------------------- helpers

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor client() {
        return jwt().jwt(token -> token.subject(CUSTOMER.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"));
    }

    private static ViewCartUseCase.CartView cartWithOneLine() {
        ViewCartUseCase.Item item = new ViewCartUseCase.Item(
                CartLine.rehydrate(10L, 7L, 2),
                new ProductSnapshot(7L, "REF-7", "Cable", new BigDecimal("10.00"), 5, true),
                new BigDecimal("20.00"));
        return new ViewCartUseCase.CartView(List.of(item), new OrderTotals(
                new BigDecimal("20.00"), new BigDecimal("4.00"), new BigDecimal("24.00")));
    }
}
