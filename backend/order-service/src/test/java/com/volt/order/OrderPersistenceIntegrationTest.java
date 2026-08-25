package com.volt.order;

import com.volt.order.application.port.out.CartRepositoryPort;
import com.volt.order.application.port.out.OrderNumberGeneratorPort;
import com.volt.order.application.port.out.OrderRepositoryPort;
import com.volt.order.domain.model.Cart;
import com.volt.order.domain.model.CartLine;
import com.volt.order.domain.model.Order;
import com.volt.order.domain.model.OrderLine;
import com.volt.order.domain.model.OrderStatus;
import com.volt.order.domain.model.OrderTotals;
import com.volt.order.domain.model.VatRate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderPersistenceIntegrationTest {
    @Autowired private CartRepositoryPort carts;
    @Autowired private OrderRepositoryPort orders;
    @Autowired private OrderNumberGeneratorPort orderNumbers;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void bootsWithJacksonThreeAndRoundTripsAggregatesThroughTheV1Schema() {
        assertThat(objectMapper.getClass().getName()).startsWith("tools.jackson.databind");

        UUID customerId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-18T09:00:00Z");
        Cart cart = Cart.empty(customerId, createdAt);
        cart.addLine(7L, 2, createdAt.plusSeconds(1));
        cart.addLine(8L, 1, createdAt.plusSeconds(2));

        Cart savedCart = carts.save(cart);
        CartLine retainedLine = savedCart.getLines().stream()
                .filter(line -> line.getProductId().equals(7L))
                .findFirst()
                .orElseThrow();
        CartLine removedLine = savedCart.getLines().stream()
                .filter(line -> line.getProductId().equals(8L))
                .findFirst()
                .orElseThrow();
        savedCart.changeLineQuantity(retainedLine.getId(), 3, createdAt.plusSeconds(3));
        savedCart.removeLine(removedLine.getId(), createdAt.plusSeconds(4));
        carts.save(savedCart);

        Cart reloadedCart = carts.findByCustomerId(customerId).orElseThrow();
        assertThat(reloadedCart.getId()).isNotNull();
        assertThat(reloadedCart.getLines())
                .singleElement()
                .satisfies(line -> {
                    assertThat(line.getProductId()).isEqualTo(7L);
                    assertThat(line.getQuantity()).isEqualTo(3);
                });

        List<OrderLine> lines = List.of(OrderLine.of(
                7L, "REF-7", "Cable", new BigDecimal("10.00"), 3));
        String number = orderNumbers.nextOrderNumber();
        Order createdOrder = Order.place(number, customerId, 55L,
                OrderTotals.calculate(lines, VatRate.STANDARD), lines, createdAt.plusSeconds(5));

        Order savedOrder = orders.save(createdOrder);
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getLines()).singleElement().extracting(OrderLine::getId).isNotNull();
        assertThat(orders.findByIdAndCustomerId(savedOrder.getId(), customerId)).isPresent();

        savedOrder.confirm(createdAt.plusSeconds(6));
        orders.save(savedOrder);

        Order reloadedOrder = orders.findById(savedOrder.getId()).orElseThrow();
        assertThat(reloadedOrder.getNumber()).isEqualTo(number);
        assertThat(reloadedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(reloadedOrder.getReservationId()).isNull();
        assertThat(reloadedOrder.getTotals().totalInclVat()).isEqualByComparingTo("36.00");
        assertThat(reloadedOrder.getLines()).singleElement().satisfies(line -> {
            assertThat(line.getProductReference()).isEqualTo("REF-7");
            assertThat(line.getLineTotalExclVat()).isEqualByComparingTo("30.00");
        });
    }
}
