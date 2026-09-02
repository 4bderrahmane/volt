package com.volt.order.infrastructure.adapter.out.client.dto.response;

import java.time.Instant;
import java.util.List;

public record ReservationResponse(Long reservationId, Instant expiresAt, List<ReservedLineResponse> lines) {
}
