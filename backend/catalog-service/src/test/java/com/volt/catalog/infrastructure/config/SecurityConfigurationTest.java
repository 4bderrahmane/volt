package com.volt.catalog.infrastructure.config;

import com.volt.catalog.application.port.in.GetProductUseCase;
import com.volt.catalog.application.port.in.ManageReservationUseCase;
import com.volt.catalog.application.port.in.ManageProductUseCase;
import com.volt.catalog.application.port.in.ReserveStockUseCase;
import com.volt.catalog.application.port.in.SearchProductsUseCase;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.Unit;
import com.volt.catalog.infrastructure.adapter.in.web.controller.ProductController;
import com.volt.catalog.infrastructure.adapter.in.web.controller.StockController;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the security gate together with a real HTTP controller.
 *
 * <p>The role converter has its own focused test. This test checks the next
 * responsibility: whether anonymous, CLIENT, and ADMIN requests are actually
 * allowed or rejected at the intended endpoint. It starts only the MVC slice,
 * so no database or running Keycloak instance is required.
 */
@WebMvcTest({ProductController.class, StockController.class})
@Import({SecurityConfiguration.class, KeycloakRealmRoleConverter.class})
class SecurityConfigurationTest {

    private static final String VALID_PRODUCT_JSON = """
            {
              "reference": "SEC-001",
              "label": "Secured product",
              "description": "Created by an administrator",
              "priceExclVat": 19.90,
              "unit": "ITEM",
              "initialStock": 3,
              "categoryId": 1,
              "brandId": 1
            }
            """;

    private static final String VALID_UPDATE_JSON = """
            {
              "label": "Updated product",
              "description": "Updated by an administrator",
              "priceExclVat": 24.90,
              "unit": "ITEM",
              "categoryId": 1,
              "brandId": 1
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetProductUseCase getProduct;

    @MockitoBean
    private ManageProductUseCase manageProduct;

    @MockitoBean
    private SearchProductsUseCase searchProducts;

    @MockitoBean
    private ReserveStockUseCase reserveStock;

    @MockitoBean
    private ManageReservationUseCase manageReservations;

    /** Satisfies resource-server startup; jwt() installs test authentication directly. */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void provideControllerResponses() {
        Product product = new Product(
                1L,
                "SEC-001",
                "Secured product",
                "Created by an administrator",
                new BigDecimal("19.90"),
                Unit.ITEM,
                3,
                true,
                1L,
                1L,
                Instant.parse("2026-08-14T10:00:00Z"),
                Instant.parse("2026-08-14T10:00:00Z"));
        when(getProduct.getById(1L)).thenReturn(product);
        when(manageProduct.create(any())).thenReturn(product);
        when(manageProduct.update(anyLong(), any())).thenReturn(product);
    }

    @Test
    void allowsAnonymousUsersToReadProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAnonymousProductCreation() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAClientRoleFromProductMutations() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/products/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/products/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAnAdminRoleToManageProducts() throws Exception {
        var admin = new SimpleGrantedAuthority("ROLE_ADMIN");

        mockMvc.perform(post("/api/v1/products")
                        .with(jwt().authorities(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/products/1")
                        .with(jwt().authorities(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/products/1")
                        .with(jwt().authorities(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void internalStockOperationsRequireTheServiceRole() throws Exception {
        mockMvc.perform(post("/internal/v1/stock/reservations/1/confirm")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/internal/v1/stock/reservations/1/confirm")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE"))))
                .andExpect(status().isOk());
    }
}
