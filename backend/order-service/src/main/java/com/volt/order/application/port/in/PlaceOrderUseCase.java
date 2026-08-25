package com.volt.order.application.port.in;

import com.volt.order.domain.model.Order;

import java.util.UUID;

/**
 * Specification §F7 — the checkout. The one operation in this project where getting
 * the ordering wrong produces silent data corruption.
 *
 * <p>Required sequence (ADR-0003), and the reason for each step:
 * <ol>
 *   <li><b>Reserve</b> against the catalog. Returns authoritative prices, so the
 *       price persisted below is the one that was validated.</li>
 *   <li><b>Persist</b> the order as CREATED in a local transaction, storing the
 *       reservation id. If this fails, do nothing at all: the reservation
 *       expires on its own. There is deliberately no compensating call here —
 *       a compensation issued by a process that may have just died is not a
 *       recovery mechanism.</li>
     *   <li><b>Confirm</b> the reservation, then move the order to CONFIRMED. If
     *       confirmation fails, the order remains CREATED for explicit operational
     *       follow-up; catalog-side expiry prevents a permanent stock hold.</li>
 * </ol>
 *
 * <p>Do not wrap steps 1–3 in a single {@code @Transactional} method. A local
 * transaction has no authority over the catalog's database, which is exactly the
 * mistake specification §3.4 makes.
 */
public interface PlaceOrderUseCase {

    Order placeOrder(UUID customerId);
}
