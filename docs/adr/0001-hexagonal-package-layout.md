# ADR-0001: Hexagonal architecture with an enforced package layout

**Status:** Accepted
**Date:** 2026-08-14
**Deciders:** Project maintainers

## Context

The specification (§4) imposes hexagonal architecture and lists five non-negotiable
rules. It does not say *why*, and a layout followed without understanding
degrades into three folders that all import each other.

The reason hexagonal architecture is worth the ceremony on this project is
narrow and concrete: the order service's most interesting logic — computing
totals, applying VAT, deciding whether a cart is valid — must be testable
without a database, without HTTP, and without a running catalog service. Every
rule in §4 exists to protect that property. If a rule does not serve it, the
rule is cargo cult.

The mental model: `domain` is a library that knows nothing about the outside
world. `application` orchestrates domain objects and declares, as interfaces,
what it needs from the world. `infrastructure` supplies those interfaces using
Spring, JPA, and HTTP. Dependencies point inward only, so the inner layers can
be compiled and tested with no framework on the classpath.

**A port is a need, not a technology.** `CatalogClientPort` is named for what the
application needs (product data), not how it gets it (REST). If the port name
mentions HTTP, JPA, or Kafka, the abstraction has leaked.

## Decision

Both services use the identical package layout below, and it is enforced by
ArchUnit rather than by discipline.

```
com.volt.<service>
├── domain/
│   ├── model/          Aggregates, value objects, enums. Plain Java.
│   ├── exception/      Business failures. Extend RuntimeException.
│   └── service/        Pure logic spanning several aggregates.
│
├── application/
│   ├── port/
│   │   ├── in/         Use case interfaces. One per business intent.
│   │   └── out/        What the use case needs from the world.
│   └── usecase/        Implements port.in, depends only on port.out.
│
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   ├── web/
    │   │   │   ├── controller/     @RestController, and nothing else
    │   │   │   ├── dto/request/    Inbound payloads + bean validation
    │   │   │   ├── dto/response/   The published output contract
    │   │   │   └── advice/         @RestControllerAdvice → RFC 7807
    │   │   └── scheduling/         @Scheduled driving adapters
    │   └── out/
    │       ├── persistence/        *PersistenceAdapter (implements the out-port)
    │       │   ├── entity/         *JpaEntity — the table shape
    │       │   ├── repository/     *JpaRepository — Spring Data
    │       │   └── mapper/         Explicit entity ⇄ domain translation
    │       └── client/             REST clients implementing out-ports
    └── config/         Spring @Configuration, security, bean wiring
```

### Why the adapters have internal structure

Both adapters started flat: every controller, request DTO, response DTO and the
exception handler side by side in `adapter/in/web`, and entities, repositories,
mappers and adapters together in `adapter/out/persistence`. That is fine at six
files. At sixteen it stops being a package and becomes a pile — the API surface
is no longer visible without reading filenames one at a time.

The sub-packages sit **inside** `adapter.in.web` and `adapter.out.persistence`
rather than at the top of the service, and that placement is the whole point.
Every rule above — controllers are driving adapters, `jakarta.persistence` is
confined to one place, dependencies point inward — is written against those two
prefixes, and ArchUnit's `..` matches sub-packages. So the tree can be organised
for readability without loosening a single boundary. A top-level
`com.volt.<service>.controller` package would read as tidier and would quietly
delete the rule that keeps controllers out of the domain.

`HexagonalArchitectureTest` asserts the internal shape too: `*Request` in
`dto.request`, `*Response` in `dto.response`, `@Entity` in `entity`,
`*JpaRepository` in `repository`, `*PersistenceMapper` in `mapper`, and no
dependency at all from the web adapter onto persistence entities. Grouping that
is merely agreed drifts back within a month, because each individual file is
easier to drop next to its neighbour than to file correctly.

Three additions to §4, all load-bearing:

**`infrastructure/config`** — the specification's tree has no home for `SecurityConfig`
or `RestClient` bean definitions. Without it those land in `application`, which
puts `@Configuration` one package away from the use cases and makes rule 5 hard
to enforce.

**`package-info.java` in every package** — a one-paragraph statement of what
belongs there. When the placement of a class is unclear later, the distinction
between a domain service and a use case is already documented.

**Entities are classes; value objects are records.** Rule of thumb: if it has an
`id` column, it is a class.

| Kind | Examples | Form |
|---|---|---|
| Entity / aggregate root | `Product`, `Reservation`, `Order`, `Cart`, `OrderLine` | class |
| Value object | `OrderTotals`, `VatRate`, `PagedResult`, `ProductSearchCriteria`, `StockShortage`, `ProductSnapshot` | record |
| Command / result message | the records nested inside port interfaces | record |

An entity has an identity that outlives its field values; a record's generated
`equals` compares every component. Three consequences follow, and each is a real
bug rather than a matter of taste:

1. **Stale copies stop being the same object.** Two snapshots of order 42 taken
   either side of a status change compare as different orders, so a `HashSet`
   containing one cannot find the other.
2. **Every state change becomes a new instance.** `order.confirm()` on a record
   returns a second `Order` while the first stays reachable and still says
   `CREATED` — two answers to "what is the status of order 42". For a
   single-use token like `Reservation`, that is a token that can be spent twice,
   which is exactly what ADR-0003 exists to prevent.
3. **An aggregate cannot protect its own state.** Records expose every
   component, so `Cart` cannot hand out its lines without handing out the
   ability to bypass the one-line-per-product invariant.

Entities therefore define `equals`/`hashCode` **by id alone**, with a *constant*
hash rather than `Objects.hash(id)`. The id is null before persistence and
assigned by the database afterwards; a hash derived from it would change while
the object sits in a set, making it unfindable in the very collection holding
it. Two unsaved instances are equal only if they are the same object, since
neither has a persistent identity yet.

Where records are right, they are kept. `OrderTotals` and `VatRate` have no
identity — two instances with the same amounts are interchangeable —
component-wise equality is the correct semantic, and rewriting them as classes
would be boilerplate for nothing.

This convention is documented, not enforced by ArchUnit. A rule of the form
"no record in `domain.model` may declare a field named `id`" is expressible with
a custom `ArchCondition`, but it needs API this project has not yet compiled
against, and an architecture test that fails to compile is worse than a
convention written down. Add it once the suite runs green — it is a genuinely
mechanical check, and it is the kind of thing that decays without one.

`catalog-service` keeps `adapter/out/client` even though it calls nobody today;
it is empty and marked as such. Delete it if it remains unused once service
integration is complete.

## Options considered

### Option A: Layout as specified in §4, enforced by ArchUnit

| Dimension | Assessment |
|---|---|
| Complexity | Medium — many small packages, explicit mappers |
| Cost | ~2 extra mapper classes per aggregate |
| Scalability | High — boundaries remain explicit as the domain grows |
| Team familiarity | Low — mitigated by package documentation and ArchUnit |

**Pros:** Meets §13's ArchUnit acceptance criterion. Domain is unit-testable
with no Spring context, which is what makes the 70% coverage target (§9)
achievable at all. The mapping boundary keeps persistence concerns out of the
domain even though both sides now use English identifiers.

**Cons:** Three representations of the same concept (domain model, JPA entity,
web DTO) plus two mappers. For `Category` — two fields — this is pure overhead.

### Option B: Layered architecture (controller / service / repository)

| Dimension | Assessment |
|---|---|
| Complexity | Low |
| Cost | Near zero |
| Scalability | Poor past a few aggregates |
| Team familiarity | High |

**Pros:** Half the classes. Faster initial implementation.

**Cons:** Fails §13 outright. JPA entities leak into controllers, so domain
tests need a Spring context, which makes them slower and puts the 70% coverage
target at risk. Rejected because it removes the isolation that §4 explicitly
requires.

### Option C: Hexagonal for `order-service`, layered for `catalog-service`

| Dimension | Assessment |
|---|---|
| Complexity | Medium |
| Cost | Low |
| Scalability | Medium |
| Team familiarity | Medium |

**Pros:** Honest about where the logic actually is. The catalog service is
mostly CRUD with a search query; hexagonal ceremony buys little there. The order
service has real invariants and deserves it.

**Cons:** Rejected, but not because it is technically unsound — it is arguably
the right choice for a standalone CRUD service. It is rejected because §13
makes a passing ArchUnit test an acceptance criterion and because two layouts in
one repository increase maintenance and review cost. This ADR records the
trade-off rather than treating the hexagonal layout as free.

## Trade-off analysis

The real cost is mapping. Three representations per aggregate means a field
addition touches six files. The mitigation is to write mappers as plain static
methods in a `*Mapper` final class with a private constructor — no MapStruct, no
reflection. It is more typing and less magic, and when a mapping is wrong the
stack trace points at an implementation line.

The real benefit is test speed. A `TotalCalculationService` test runs in
milliseconds with no container. Fast isolated tests make the 70% coverage
target maintainable throughout development instead of a late cleanup task.

Where hexagonal genuinely does not pay: `Category` and `Brand` are reference
data with no behaviour. Using the same three-class ceremony there is overhead
with no return. It is accepted here for uniformity and because ArchUnit checks
are all-or-nothing. This ADR records that overhead explicitly rather than
claiming the pattern is free.

## Consequences

**Easier**
- Domain and use case tests run without Spring, so they are fast enough to run
  constantly.
- Swapping the catalog REST client for a fake in order-service tests is a
  one-line constructor argument.
- Database and Java identifiers share one English vocabulary while remaining
  separate models.

**Harder**
- Every new field crosses three classes and two mappers.
- Pagination is awkward: `Page<T>` is a Spring type and must not appear in an
  out-port signature. The port returns a domain `PagedResult<T>` record and the
  persistence adapter converts. This is the first place people break rule 1.

**To revisit**
- If `catalog-service` remains pure CRUD after service integration, reassess
  whether hexagonal architecture was over-applied there and update this ADR.

## Action items

1. [x] Create the package tree and `package-info.java` files for both services
2. [x] Write ArchUnit tests enforcing rules 1 and 5 (ADR: see §9 of specification)
3. [x] Entities as classes with id-based equality; value objects as records
4. [ ] Write mappers as static-method final classes — no MapStruct
5. [ ] Introduce `PagedResult<T>` in domain before the first paginated query
6. [ ] Once the suite is green, add the ArchUnit rule forbidding a record with
   an `id` field in `domain.model`
