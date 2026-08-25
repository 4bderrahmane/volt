package com.volt.order.infrastructure.adapter.out.client;

import com.volt.order.application.port.out.CatalogClientPort;
import com.volt.order.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatalogRestClientAdapterTest {
    private MockRestServiceServer server;
    private CatalogRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://catalog.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new CatalogRestClientAdapter(
                builder.build(), JsonMapper.builder().findAndAddModules().build(), 1, Duration.ZERO);
    }

    @Test
    void reservesUsingTheDocumentedContract() {
        server.expect(once(), requestTo("http://catalog.test/internal/v1/stock/reservations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"orderRef":"ORD-2026-000001","lines":[{"productId":7,"quantity":2}]}
                        """))
                .andRespond(withSuccess("""
                        {"reservationId":55,"expiresAt":"2026-08-17T10:15:00Z","lines":[
                          {"productId":7,"reference":"REF-7","label":"Cable","unitPriceExclVat":10.00,"quantity":2}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        CatalogClientPort.Reservation result = adapter.reserve(
                "ORD-2026-000001", List.of(new CatalogClientPort.RequestedLine(7L, 2)));

        assertThat(result.reservationId()).isEqualTo(55L);
        assertThat(result.lines()).singleElement().satisfies(line ->
                assertThat(line.unitPriceExclVat()).isEqualByComparingTo("10.00"));
        server.verify();
    }

    @Test
    void translatesStockConflictWithEveryShortage() {
        server.expect(requestTo("http://catalog.test/internal/v1/stock/reservations"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"type":"https://volt.local/problems/insufficient-stock","status":409,
                                 "shortages":[{"productId":7,"reference":"REF-7","requested":5,"available":3}]}
                                """));

        assertThatThrownBy(() -> adapter.reserve(
                "ORD-2026-000001", List.of(new CatalogClientPort.RequestedLine(7L, 5))))
                .isInstanceOf(InsufficientStockException.class)
                .satisfies(error -> assertThat(((InsufficientStockException) error).shortages())
                        .singleElement().satisfies(shortage -> assertThat(shortage.available()).isEqualTo(3)));
        server.verify();
    }

    @Test
    void loadsProductsInOneBatch() {
        server.expect(requestTo("http://catalog.test/api/v1/products?ids=7,8"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":7,"reference":"REF-7","label":"Cable","priceExclVat":10.00,
                          "stockQuantity":3,"active":true}]
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.findProducts(List.of(7L, 8L))).singleElement().satisfies(product -> {
            assertThat(product.productId()).isEqualTo(7L);
            assertThat(product.availableQuantity()).isEqualTo(3);
        });
        server.verify();
    }

    @Test
    void restocksByOrderReference() {
        server.expect(once(), requestTo("http://catalog.test/internal/v1/stock/restock"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"orderRef":"ORD-2026-000001"}
                        """))
                .andRespond(withSuccess());

        adapter.restock("ORD-2026-000001");

        server.verify();
    }
}
