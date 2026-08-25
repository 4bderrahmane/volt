/**
 * Spring MVC controllers, and nothing else.
 *
 * <p>A controller's whole job is translation: bind and validate the request,
 * call one incoming port, map the result to a response DTO. It holds no business
 * rules — anything resembling a decision here belongs in a use case, where it can
 * be tested without a servlet.
 *
 * <p>Controllers depend on {@code application.port.in} interfaces, never on the
 * {@code application.usecase} implementations. {@code HexagonalArchitectureTest}
 * enforces both that and the fact that {@code @RestController} appears nowhere
 * outside this package.
 */
package com.volt.order.infrastructure.adapter.in.web.controller;
