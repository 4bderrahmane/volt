# ADR-0006: Flyway is the only schema authority, tests included

**Status:** Accepted
**Date:** 2026-08-14
**Deciders:** Project maintainers

## Context

§5 requires Flyway with versioned scripts and permits `ddl-auto: update` only
in tests.

Read literally, that permits Hibernate to generate the schema in tests while
Flyway generates it in production. That arrangement is backwards: **it means the
migrations are the only part of the schema that is never tested.** A migration
with a typo, a missing index, or a constraint Hibernate would have inferred
differently passes CI and fails on the first deployment.

It also produces a second, quieter failure: tests pass against a Hibernate-shaped
schema that differs from the Flyway one (nullability, column types, cascade
behaviour), so green tests stop being evidence about production.

## Decision

**Flyway builds the schema everywhere — production, local, and every test.**
`spring.jpa.hibernate.ddl-auto` is `validate` in all profiles, never `update`,
never `create-drop`.

`validate` is the useful setting: Hibernate compares its mappings against the
Flyway-built schema at startup and fails loudly if they disagree. That turns
"the entity and the migration drifted apart" from a runtime mystery into a
startup error, which is the ideal failure point.

Testcontainers starts PostgreSQL 18, Flyway migrates it, tests run against it.
This satisfies §9 (no test depends on a locally installed database) and
makes every CI run validate the migration path.

**Schema conventions**

- Table names, columns, constraints, indexes, Java identifiers, HTTP paths, and
  JSON fields use one English vocabulary. The explicit JPA column annotations
  make the database contract visible without maintaining two translations.
- Money is `NUMERIC(10,2)` mapped to `BigDecimal`. Never `double`. Never `float`.
  VAT at 20% on a `double` total produces immediately visible rounding errors.
- Timestamps are `TIMESTAMPTZ`, mapped to `Instant`. Not `TIMESTAMP`, not
  `LocalDateTime` — a container in UTC and a laptop in `Africa/Casablanca` will
  otherwise disagree about when an order was placed.
- Every foreign key gets an index. PostgreSQL does not create one automatically,
  and `order_line.order_id` is on the hot path of §F8.
- `V{n}__snake_case_description.sql`. Applied migrations are immutable; fix
  forward with a new version. Flyway checksums enforce this, and the error
  message for editing an applied migration is not immediately obvious.

**Migration layout**

| Service | Version | Contents |
|---|---|---|
| catalog | `V1__init.sql` | `category`, `brand`, `product`, `reservation`, `reservation_line` |
| catalog | `V2__demo_data.sql` | §12.5 — 40+ products across 5 categories |
| order | `V1__init.sql` | `cart`, `cart_line`, `orders`, `order_line` |

The demo dataset is a **migration, not a `CommandLineRunner`**. §12.5 requires it
loaded automatically at startup; a runner has to be idempotent by hand and
usually is not, so it duplicates rows on every restart. A versioned migration
runs exactly once, for free.

Put it behind a Flyway location that only the `demo` and `dev` profiles include,
so an empty production schema stays possible:
`spring.flyway.locations=classpath:db/migration,classpath:db/demo`.

**JPA specifics that will otherwise bite**

- `@Version` optimistic locking on `product`. Two concurrent reservations for the
  last unit is the exact race in ADR-0003; the `SELECT ... FOR UPDATE` handles it
  inside the reservation transaction, but `@Version` catches the case where an
  ADMIN edit and a reservation collide.
- `FetchType.LAZY` on every `@ManyToOne`. JPA defaults `@ManyToOne` to EAGER,
  which is how a product listing turns into 3N queries.
- `order_line` is loaded with a `join fetch` when reading order detail (§F8).
  Without it, listing 20 orders issues 21 queries.

## Options considered

### Option A: Flyway everywhere, `ddl-auto: validate` (chosen)

**Pros:** Migrations are exercised on every CI run. Tests run against the real
schema, so green means something. `validate` catches entity/schema drift at
startup. One schema definition, one source of truth.

**Cons:** Writing a migration before running a test adds friction during early
model iteration. Testcontainers startup grows by the
migration time (a second or two, cached across tests via a singleton container).

### Option B: Flyway in prod, `ddl-auto: create-drop` in tests (the literal §5 reading)

**Pros:** Fastest iteration early on — change the entity, run the test.

**Cons:** Migrations are never executed by CI, so the acceptance criterion
"`docker compose up --build` on a clean machine" is validated for the first time
by hand, late. Tests pass against a schema production never has. Rejected — this
is the specific thing this ADR exists to correct.

### Option C: `ddl-auto: update` in dev, Flyway in prod

**Pros:** Fastest possible local loop.

**Cons:** All of Option B's problems plus schema drift between developer
machines. `update` never drops or alters columns, so a renamed field leaves the
old column behind forever, and it silently does nothing for many changes.
Rejected.

## Trade-off analysis

The cost is real during early model iteration: with `create-drop`, an entity can
be changed and tested immediately; with Flyway, a migration comes first. This
adds roughly two minutes per model change while the schema is most volatile.

The mitigation is to accept a coarse `V1` while a service's schema is still
moving, and only start adding `V2`, `V3` once another component
depends on it. Editing `V1` before anyone else has run it is fine — Flyway only
objects once a checksum has been recorded, and a `docker compose down -v` resets
that. **Stop doing this the moment the migration is pushed to `main`.**

What the friction buys: the §13 clean-machine startup criterion is verified
continuously instead of being discovered late.
The migration path is a common deployment failure point,
and this makes it the single most-executed code path in CI.

## Consequences

**Easier**
- CI proves the clean-machine startup criterion on every push.
- `validate` turns entity/schema drift into a startup failure with a clear message.
- Test data is a migration, so it is versioned, reviewable, and runs once.
- Money and time behave correctly because the types were chosen deliberately.

**Harder**
- Schema changes need a migration, which adds friction during early schema development.
- Testcontainers must reuse a singleton container, or each test class pays a
  fresh Postgres startup and the suite can become too slow for frequent local use.
- Two migration sets to keep straight, one per database.

**To revisit**
- If the demo dataset makes tests slow, split it out of the default profile.
- If the suite exceeds ~2 minutes, look at container reuse before anything else.

## Action items

1. [x] `V1__init.sql` for both databases
2. [x] `V2__demo_data.sql` for catalog (§12.5: 40+ products, 5 categories)
3. [ ] `ddl-auto: validate` in every profile — verify no `update` anywhere
4. [ ] Singleton Testcontainer shared across test classes
5. [ ] `@Version` on `product`; `FetchType.LAZY` on every `@ManyToOne`
6. [ ] `NUMERIC(10,2)` → `BigDecimal`, `TIMESTAMPTZ` → `Instant`, no exceptions
7. [ ] Index every foreign key
