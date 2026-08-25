/**
 * Implementations of the in-ports. Each depends only on out-ports and domain
 * types, never on a Spring type beyond {@code @Service} and
 * {@code @Transactional}.
 *
 * <p>Constructor injection via Lombok's {@code @RequiredArgsConstructor} over
 * {@code private final} fields — never {@code @Autowired} on a field, which
 * {@code HexagonalArchitectureTest} rejects. Final fields keep the class
 * instantiable in a plain unit test with hand-written fakes and no Spring:
 *
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * @Slf4j
 * class PlaceOrderService implements PlaceOrderUseCase {
 *
 *     private final OrderRepositoryPort orders;
 *     private final CatalogClientPort catalog;
 *     private final Clock clock;          // inject the clock; never Instant.now()
 *     ...
 * }
 * }</pre>
 *
 * <p>Keep {@code @Transactional} scoped to the local database work only. In
 * particular do not wrap the three phases of ADR-0003 in a single transactional
 * method: a local transaction has no authority over the catalog's database, and
 * believing otherwise is the exact mistake specification §3.4 makes.
 *
 * <p>Note {@code @RequiredArgsConstructor} generates a constructor over the
 * final fields in declaration order. That is safe as long as the fields are of
 * distinct types — which they are here, being ports — but it is the reason the
 * domain forbids {@code @AllArgsConstructor} (ADR-0008).
 *
 * <p>The flow lives here; the rules live in {@code domain}. If a use case is
 * making a business decision rather than sequencing one, it belongs on an
 * aggregate.
 */
package com.volt.order.application.usecase;
