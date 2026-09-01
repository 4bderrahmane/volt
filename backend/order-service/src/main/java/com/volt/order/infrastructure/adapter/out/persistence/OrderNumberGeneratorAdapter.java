package com.volt.order.infrastructure.adapter.out.persistence;

import com.volt.order.application.port.out.OrderNumberGeneratorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Year;

@Component
@RequiredArgsConstructor
public class OrderNumberGeneratorAdapter implements OrderNumberGeneratorPort {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Override
    public String nextOrderNumber() {
        Long sequence = jdbc.queryForObject("SELECT nextval('order_number_seq')", Long.class);

        if (sequence == null) {
            throw new IllegalStateException("order number sequence returned no value");
        }
        return "ORD-%d-%06d".formatted(Year.now(clock).getValue(), sequence);
    }
}
