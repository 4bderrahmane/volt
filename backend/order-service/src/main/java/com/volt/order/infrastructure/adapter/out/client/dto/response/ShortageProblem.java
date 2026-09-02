package com.volt.order.infrastructure.adapter.out.client.dto.response;

import com.volt.order.domain.model.StockShortage;

import java.util.List;

public record ShortageProblem(List<StockShortage> shortages) {
}
