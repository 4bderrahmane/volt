/**
 * Implementations of incoming ports. Use cases coordinate domain objects and
 * outgoing ports: they describe the steps of an operation while business rules
 * remain inside the domain entities.
 *
 * <p>Constructor injection keeps these classes usable in plain unit tests with
 * fake port implementations and no Spring application context.
 */
package com.volt.catalog.application.usecase;
