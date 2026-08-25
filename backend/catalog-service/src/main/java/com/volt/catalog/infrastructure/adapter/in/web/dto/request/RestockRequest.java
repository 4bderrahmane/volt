package com.volt.catalog.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestockRequest(@NotBlank @Size(max = 64) String orderRef) {
}
