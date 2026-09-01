package com.volt.order.application.port.out;

/** Generates unique human-facing order numbers from a database sequence. */
public interface OrderNumberGeneratorPort {

    String nextOrderNumber();
}
