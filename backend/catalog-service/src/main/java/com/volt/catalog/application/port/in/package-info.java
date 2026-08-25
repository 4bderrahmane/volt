/**
 * Driving ports: one interface per business intent, named after the intent
 * (ReserveStockUseCase), plus the command records it accepts. The web adapter
 * depends on these; it never depends on a usecase implementation class.
 */
package com.volt.catalog.application.port.in;
