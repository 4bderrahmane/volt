package com.volt.order.infrastructure.adapter.in.web.dto.request;

import com.volt.order.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeOrderStatusRequest(@NotNull OrderStatus status) { }
