package com.volt.catalog.infrastructure.adapter.in.web.controller;

import com.volt.catalog.application.usecase.ManageProductService;
import com.volt.catalog.application.usecase.ProductQueryService;
import com.volt.catalog.infrastructure.adapter.in.web.advice.GlobalExceptionHandler;
import com.volt.catalog.infrastructure.adapter.out.memory.InMemoryProductRepositoryAdapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the HTTP input adapter without starting the complete application.
 *
 * <p>The test connects the controller to real use-case objects and the small
 * in-memory output adapter. This checks JSON mapping and the complete port flow
 * while avoiding a database and a Spring application context.
 */
class ProductControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InMemoryProductRepositoryAdapter repository = new InMemoryProductRepositoryAdapter();
        ProductQueryService queryService = new ProductQueryService(repository);
        Clock clock = Clock.fixed(Instant.parse("2026-08-14T10:00:00Z"), ZoneOffset.UTC);
        ManageProductService manageService = new ManageProductService(repository, clock);
        ProductController controller = new ProductController(queryService, manageService, queryService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsTheProductResponseDto() throws Exception {
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reference").value("DEMO-001"))
                .andExpect(jsonPath("$.unit").value("ITEM"));
    }

    @Test
    void returnsAStablePagedSearchEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("q", "demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    /**
     * Specification §F2. The persistence side of filtering is proved against real
     * SQL in {@code ProductSearchFilterIntegrationTest}; what is at stake here is
     * the wiring — that {@code ?categoryId=} and {@code ?brandId=} bind at all and
     * reach the use case, rather than being silently dropped and returning the
     * unfiltered catalog with a 200.
     */
    @Test
    void bindsCategoryAndBrandFiltersFromQueryParameters() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reference": "LGT-001",
                                  "label": "Bench lamp",
                                  "priceExclVat": 19.90,
                                  "unit": "ITEM",
                                  "initialStock": 4,
                                  "categoryId": 2,
                                  "brandId": 2
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products").param("categoryId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].reference").value("LGT-001"));

        mockMvc.perform(get("/api/v1/products").param("brandId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].reference").value("DEMO-001"));

        // Both filters must intersect. Were they ORed, this would return two.
        mockMvc.perform(get("/api/v1/products")
                        .param("categoryId", "1")
                        .param("brandId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsANonPositiveFilterId() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("categoryId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/invalid-value"));
    }

    @Test
    void returnsBatchProductsInRequestedOrderAndOmitsMissingIds() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("ids", "999,1,1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void createsAProductFromARequestDto() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reference": "BOX-001",
                                  "label": "Cable box",
                                  "description": "A product created through the input adapter",
                                  "priceExclVat": 29.90,
                                  "unit": "BOX",
                                  "initialStock": 5,
                                  "categoryId": 1,
                                  "brandId": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/products/2"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.reference").value("BOX-001"));
    }

    @Test
    void returnsAValidationProblemForAnInvalidRequestDto() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialStock\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://volt.local/problems/validation-error"));
    }
}
