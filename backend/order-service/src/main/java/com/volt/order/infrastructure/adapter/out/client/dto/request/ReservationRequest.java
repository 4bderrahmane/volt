package com.volt.order.infrastructure.adapter.out.client.dto.request;

import com.volt.order.application.port.out.CatalogClientPort.RequestedLine;

import java.util.List;

public record ReservationRequest(String orderRef, List<RequestedLine> lines) {
}
