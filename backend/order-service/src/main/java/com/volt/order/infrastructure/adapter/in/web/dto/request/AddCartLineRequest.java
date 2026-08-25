package com.volt.order.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartLineRequest(@NotNull @Positive Long productId, @Positive int quantity) { }
