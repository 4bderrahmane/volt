# ADR-0005: Synchronous REST with explicit timeouts and idempotency keys

**Status:** Accepted
**Date:** 2026-08-14
**Deciders:** Project maintainers

## Context

§3.4 mandates synchronous REST (`RestClient` / `WebClient`), a 3-second timeout,
a simple `@Retry`, and offers Resilience4j circuit breaking as a bonus. The
protocol choice is fixed and not relitigated here.

What §3.4 leaves undefined is what makes the retry safe. **Retry is not a
resilience feature; it is a correctness hazard applied to a non-idempotent
operation.** A retried `POST /stock/decrement` after an ambiguous timeout
decrements twice. The specification prescribes the retry without prescribing the
property that makes it survivable.

Three further omissions:

- **No timeout distinction.** "3 s" is one number for two different things:
  connect timeout (is catalog reachable?) and read timeout (is catalog slow?).
  Collapsing them means a dead host and a slow query fail identically.
- **No error contract between services.** §6.3 defines RFC 7807 for the public
  API but says nothing about how catalog reports "insufficient stock" to order
  in a way order can act on. A 409 with a free-text message forces string
  matching.
- **No authentication on the outbound call.** See ADR-0004.

## Decision

**`RestClient`, not `WebClient`.** Both services are `spring-boot-starter-webmvc`
on virtual threads (Java 25). `WebClient` drags in the reactive stack for a
blocking call and produces stack traces that are painful to read. `RestClient`
is the synchronous, blocking client, which is what this actually is.

**Two timeouts, not one:**

| Setting | Value | Rationale |
|---|---|---|
| Connect timeout | 1 s | Catalog is one hop away on a compose network. Slower than this means unreachable. |
| Read timeout | 3 s | Per §3.4. A reservation is a `SELECT FOR UPDATE` + inserts; over 3 s means contention or a missing index. |

**Retry only on the retryable half.** Retrying a 409 is pointless; retrying a
timeout is the point.

Implement it as a loop inside `CatalogRestClientAdapter`, not as an annotation.
Two attempts over a single call site is about ten lines with no dependency, and
it avoids the trap that catches everyone once: annotation-driven retry is
proxy-based, so a call from one method of a bean to another method of the same
bean bypasses the proxy and retries nothing — silently, with no warning
anywhere. Spring Framework 7 does ship `@Retryable` in
`org.springframework.resilience.annotation` if annotation style is preferred; note
that `org.springframework.retry:spring-retry` is a different, older artifact
that the Spring Boot BOM does not manage.

| Outcome | Retry? | Order-service behaviour |
|---|---|---|
| Connect failure / read timeout | Yes, 2 attempts, 200 ms backoff | Then fail the checkout with 503 |
| 5xx | Yes, 2 attempts | Then 503 |
| 409 insufficient stock | **No** | 409 to the client, per §6.3 |
| 4xx other | **No** | 502 — this is our bug, not the user's |

**Every mutating internal endpoint is idempotent** (ADR-0003). Reservation
creation carries an `Idempotency-Key` header set to the order reference;
`confirm` and `restock` are idempotent by construction because the reservation
row is a single-use token. This is the precondition that makes the mandated
retry safe rather than dangerous, and it is the single most important line in
this ADR.

**Typed error contract.** Catalog returns RFC 7807 `application/problem+json` on
`/internal/**` as well as on the public API, with a stable machine-readable
`type` URI:

```json
{
  "type": "https://volt.local/problems/insufficient-stock",
  "title": "Insufficient stock",
  "status": 409,
  "detail": "Insufficient stock for 3 of 5 requested lines",
  "shortages": [ { "productId": 42, "requested": 10, "available": 3 } ]
}
```

Order-service switches on `type`, never on `detail`. The `shortages` array lets
the SPA say *which* product ran out — §13 requires a clear message, and
"insufficient stock" alone is not clear when the cart has five lines.

**The port stays technology-free.** `CatalogClientPort` speaks in domain terms
(`reserve(...)`, `confirm(...)`) and throws domain exceptions
(`InsufficientStockException`). `RestClientResponseException` never escapes
`CatalogRestClientAdapter`. If order-service's use cases can tell that catalog is
reached over HTTP, ADR-0001 rule 5 has been broken.

**Circuit breaker: skip it, and say why.** §3.4 lists it as optional. A circuit
breaker protects a caller from wasting resources on a service that is reliably
down. With a synchronous checkout that has no fallback path — if catalog is
down, no order can be placed, full stop — the breaker changes a 3-second failure
into a fast failure and nothing else. Defer it unless telemetry shows repeated
failures or catalog gains a degraded mode; this ADR records that omission as
deliberate.

## Options considered

### Option A: `RestClient` + tuned retry + idempotency keys (chosen)

**Pros:** Matches §3.4. Blocking client for a blocking call. Retry is safe
because every target is idempotent. Typed errors mean no string matching.

**Cons:** Idempotency requires catalog to deduplicate, which means either a key
column with a unique constraint or reservation-as-token. ADR-0003 chose the
latter, so this is nearly free.

### Option B: `WebClient` with reactive composition

**Pros:** Non-blocking; the natural choice if the stack were WebFlux.

**Cons:** The stack is not WebFlux. `.block()` on a `Mono` inside an MVC
controller is worse than a blocking client, and the stack traces are much harder
to read under incident pressure. Rejected.

### Option C: Retry everything uniformly, including 4xx

**Pros:** One rule, no classification code.

**Cons:** Retrying a 409 costs three round trips to receive the same "no". Worse,
retrying a 400 hides a contract bug behind latency instead of surfacing it.
Rejected.

### Option D: Asynchronous messaging (RabbitMQ / Kafka)

**Pros:** Decouples availability; checkout survives a catalog outage.

**Cons:** §3.4 mandates synchronous REST, and the SPA needs a synchronous answer
to "did my order go through?". Adding eventual consistency means adding an
order-pending UI state. Out of scope; recorded here as a future alternative.

## Trade-off analysis

The important shift is where safety comes from. §3.4 locates it in the retry
("if it fails, try again") and in a transaction rollback that has no authority
across a service boundary (ADR-0003). This ADR locates it in **idempotency** —
the retry is then merely an optimisation over waiting for the next request.

That reframing generalises: in a distributed system a caller cannot know whether a
timed-out call was applied. The only two honest responses are to make retrying
harmless, or to make the state self-correcting. ADR-0003 does the second;
this ADR does the first. Together they mean no failure mode requires a human.

Splitting connect and read timeouts costs one line of configuration and buys
diagnosability: a connect failure means "catalog is not there", a read timeout
means "catalog is struggling". Those need different responses from an operator,
so they should not look identical in the logs.

## Consequences

**Easier**
- Retry, and later a circuit breaker, can be added without correctness analysis.
- Order-service maps a typed 409 to a precise user-facing message naming the
  product that ran short (§13).
- Swapping REST for messaging later touches one adapter class, because the port
  is technology-free.

**Harder**
- Every internal endpoint must be designed idempotent up front. This constrains
  API design in a way the specification does not anticipate.
- Order-service needs an outbound client-credentials token (ADR-0004), so the
  `RestClient` needs an interceptor.
- Tests need a stubbed catalog. Use a hand-written fake implementing
  `CatalogClientPort` for use case tests (fast, no HTTP), and MockRestServiceServer
  or WireMock for the adapter's own tests. Do not start a real catalog in tests.

**To revisit**
- If checkout latency becomes visible, the two calls in ADR-0003 are the first
  thing to look at — they are sequential by necessity, so the fix is a faster
  reservation query, not parallelism.
- Revisit the circuit breaker if catalog ever gains a degraded read-only mode.

## Action items

1. [ ] `RestClient` bean with connect 1 s / read 3 s and a client-credentials interceptor
2. [ ] `CatalogClientPort` in `application/port/out` — domain vocabulary only
3. [ ] `CatalogRestClientAdapter` translates HTTP → domain exceptions
4. [ ] Retry on timeouts and 5xx only, never on 4xx. **Not via spring-retry** —
   it is not in the Spring Boot BOM, so it needs a hand-pinned version. Prefer a
   plain loop in the adapter (ten lines, no dependency, no proxy semantics to
   reason about); Spring Framework 7's `@Retryable` in
   `org.springframework.resilience.annotation` is the annotation alternative.
5. [ ] Stable `type` URIs for problem responses; switch on `type`, never `detail`
6. [ ] `shortages[]` in the 409 body so the SPA can name the product
7. [ ] Hand-written `FakeCatalogClient` for use case tests
