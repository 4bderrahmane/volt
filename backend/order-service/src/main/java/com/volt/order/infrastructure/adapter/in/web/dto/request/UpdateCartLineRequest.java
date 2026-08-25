package com.volt.order.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.Positive;

public record UpdateCartLineRequest(@Positive int quantity) { }
