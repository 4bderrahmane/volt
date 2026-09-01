# ADR-0003: Stock consistency via reservation → confirmation with TTL

**Status:** Accepted — **supersedes technical specification §3.4**
**Date:** 2026-08-14
**Deciders:** Project maintainers

## Context

Specification §3.4 specifies:

> **Consistency:** if the stock decrement fails, the order is not created (the
> transaction is rolled back in `order-service`).

**This is not implementable.** A `@Transactional` rollback in order-service acts
on order-service's database connection. It has no authority over a row that
catalog-service already committed in a different PostgreSQL instance. The
sentence describes a distributed transaction while prescribing a local one.

The failure it fails to handle is not exotic:

```
1. order-service → POST /internal/v1/stock/decrement   → catalog COMMITS
2. catalog's HTTP response is lost (timeout, pod restart, network blip)
3. order-service rolls back its own transaction
4. Result: stock decremented, no order exists. Inventory silently wrong.
```

Nothing in §3.4 detects or repairs step 4. Worse, §3.4 also mandates a `@Retry`
on this call. Retrying a non-idempotent decrement after an ambiguous timeout
decrements *twice*. The specification requires a mechanism that manufactures the
corruption it claims to prevent.

Two further gaps in the same area:

- **Price TOCTOU.** §3.3 requires copying the unit price into the order line.
  Reading prices (`GET /products/{id}`) and decrementing stock are separate
  calls; an ADMIN price change between them means the persisted price was never
  the one that was validated.
- **No restock path.** §F9 has a `CANCELLED` status and §F5 lists only
  "read and decrement". Cancelling an order therefore destroys stock
  permanently. There is no endpoint to give it back.

## Decision

Replace the single decrement with a **two-phase reservation protocol, made safe
by expiry rather than by compensation.**

```
┌─ order-service ──────────────────┐        ┌─ catalog-service ──────────────┐
│                                  │        │                                │
│ 1. POST /internal/v1/stock/reservations ─▶ │ ONE local transaction:         │
│    { orderRef, lines[] }         │        │  · SELECT ... FOR UPDATE       │
│                                  │        │  · available = stock − reserved│
│                                  │        │  · if short → 409, change      │
│                                  │        │    nothing (all-or-nothing)    │
│                                  │        │  · INSERT reservation lines,   │
│                                  │        │    expires_at = now + 15 min   │
│    ◀── 201 { reservationId,      │        │  · return AUTHORITATIVE prices │
│           lines[{ productId,     │        │                                │
│             reference, label,    │        │                                │
│             unitPriceExclVat }] }│        │                                │
│                                  │        │                                │
│ 2. LOCAL TX: persist order using │        │                                │
│    the returned prices,          │        │                                │
│    status = CREATED,             │        │                                │
│    store reservationId. COMMIT.  │        │                                │
│    ↳ if this fails: do nothing.  │        │                                │
│      The reservation expires.    │        │                                │
│                                  │        │                                │
│ 3. POST /reservations/{id}/confirm ──────▶ │ ONE local transaction:         │
│                                  │        │  · stock_quantity −= qty       │
│                                  │        │  · delete reservation          │
│    ◀── 200                       │        │  · idempotent: confirming an   │
│    order → CONFIRMED             │        │    already-confirmed id → 200  │
│    ↳ if this never happens:      │        │                                │
│      the sweeper releases it.    │        │ @Scheduled every 60s:          │
│                                  │        │  DELETE FROM reservation       │
│                                  │        │  WHERE expires_at < now()      │
└──────────────────────────────────┘        └────────────────────────────────┘
```

**The property that makes this correct:** every repair action happens inside
catalog-service, in a local transaction, against the same database that holds
the stock. No cross-service coordination is required to reach a consistent
state, and no participant needs to survive a crash in order for the system to
recover. If order-service is destroyed between steps 1 and 3, stock returns to
normal within 15 minutes with no human involvement.

**Available stock** is `stock_quantity − SUM(active reservations)`. It is
computed, never stored, so it cannot drift.

**Endpoints** (replacing §6.1's single `/internal/v1/stock/decrement`):

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/internal/v1/stock/reservations` | Reserve + return authoritative prices |
| POST | `/internal/v1/stock/reservations/{id}/confirm` | Commit the decrement |
| DELETE | `/internal/v1/stock/reservations/{id}` | Release early (cart abandoned) |
| POST | `/internal/v1/stock/restock` | Return stock on CANCELLED — closes the §F5/F9 gap |

All four are idempotent, which is what makes the mandated `@Retry` safe.
`confirm` is naturally idempotent because the reservation row is the token: once
consumed, a repeat call finds it gone-and-confirmed and returns 200 rather than
decrementing again.

**Order status gains meaning**, which §F9 currently lacks:

| Status | Meaning |
|---|---|
| `CREATED` | Order persisted, stock reserved, not yet committed |
| `CONFIRMED` | Reservation confirmed, stock decremented |
| `SHIPPED` / `DELIVERED` | Per §F9, no stock effect |
| `CANCELLED` | If from `CREATED`: release reservation. If from `CONFIRMED`: call restock |

Under the specification's design `CREATED → CONFIRMED` was a state change with no
referent. Here it is the visible half of the commit.

## Options considered

### Option A: Reserve → confirm with TTL expiry (chosen)

| Dimension | Assessment |
|---|---|
| Complexity | Medium — 2 tables, 1 scheduled job, 4 endpoints |
| Cost | ~150 LOC over the naive version |
| Scalability | Good — row locks scoped to products in one order |
| Team familiarity | Low — requires clear operational documentation |

**Pros:** Self-healing without a compensation path. Solves price TOCTOU for free
because availability and price are read in the same transaction. Collapses
N price lookups + 1 decrement into 1 call. Gives the §F9 state machine semantics.
Idempotent throughout, so §3.4's `@Retry` becomes safe rather than dangerous.

**Cons:** Two round trips on the happy path. Reserved-but-unconfirmed stock is
invisible to other buyers for up to 15 minutes. A sweeper that stops running
silently starves inventory — needs an Actuator health indicator and an alert on
`COUNT(*) WHERE expires_at < now())`.

### Option B: Decrement last, with compensating release

Persist the order `PENDING`, commit, then decrement with an `Idempotency-Key`;
on failure call a release endpoint and mark the order `FAILED`.

| Dimension | Assessment |
|---|---|
| Complexity | Low-medium — 1 idempotency table, 2 endpoints |
| Cost | ~60 LOC over naive |
| Scalability | Good |
| Team familiarity | Medium |

**Pros:** One round trip. No reservation tables, no sweeper, no invisible stock.
Strictly better than the specification's design.

**Cons — the decisive one:** the compensating call is issued by order-service,
but the failures it compensates for are order-service crashing or the network
partitioning. The recovery path is unavailable in precisely the scenarios that
require it — *who compensates the compensator?* Recovery becomes a manual
reconciliation job. Also retains the price TOCTOU, since prices are still read
in a separate earlier call. Rejected.

### Option C: Implement §3.4 literally

| Dimension | Assessment |
|---|---|
| Complexity | Low |
| Cost | Near zero |
| Scalability | Irrelevant |
| Team familiarity | High |

**Pros:** Matches the specification exactly and requires no documented exception.

**Cons:** Known stock-corruption bug, made reachable by the retry the same
section mandates. Rejected. Note that §13's acceptance criterion — "stock is
correctly decremented" — cannot honestly be checked by an implementation with a
known decrement bug, so following §3.4 literally fails §13.

### Option D: Event-driven saga over a broker

**Pros:** A common design at scale. No synchronous coupling.

**Cons:** Adds Kafka or RabbitMQ, an outbox table, consumer idempotency, and
eventual consistency the UI must render. §3.4 mandates synchronous REST and the
additional infrastructure exceeds the current delivery scope. Rejected here;
this ADR records it as the likely direction at larger scale.

## Trade-off analysis

A and B are both correct in the happy path and both fix the specification's bug. They
diverge on **who is responsible for cleanup after an unplanned failure.**

Option B assigns cleanup to the participant most likely to have died. Option A
assigns it to a timer running inside the database that owns the data, where the
repair is a single local `DELETE` that cannot partially fail. That difference —
compensation versus expiry — is the whole reason to prefer A, and it generalises
far beyond this project: *a recovery mechanism that requires the failed
component to still be alive is not a recovery mechanism.*

The price of A is 15 minutes of pessimism. Stock reserved by an abandoned
checkout is unsellable until it expires. At the current catalog size and expected
traffic this is negligible; at real volume the TTL becomes a tuning parameter
balancing oversell risk against lost sales. Start at 15 minutes and document the
rationale.

The second-order win is that A makes the mandated `@Retry` correct instead of
harmful. Under §3.4 as written, retry is the mechanism that doubles the
decrement. Under A, every endpoint is idempotent, so retry is what it is
supposed to be — a way to survive a lost packet.

## Consequences

**Easier**
- No compensation logic and no reconciliation job. Expiry handles every crash.
- One call returns availability *and* authoritative prices, so §3.3's
  requirement is satisfied without a TOCTOU window.
- `CREATED` vs `CONFIRMED` becomes observable in API behaviour and integration
  tests rather than remaining decorative.
- Retry, timeout, and circuit breaking are all safe to add.

**Harder**
- Two extra tables (`reservation`, `reservation_line`) and a `@Scheduled` sweep.
- Two round trips per checkout instead of one.
- The sweeper is a new silent-failure mode. Expose reservation lag via Actuator.
- Integration tests must cover: confirm-after-expiry (→ 410 Gone, order
  `CANCELLED`), double-confirm (→ 200, decrement once), and concurrent
  reservations racing for the last unit (→ one 201, one 409).

**To revisit**
- TTL is a guess. Instrument how long checkout actually takes, then set it.
- If reservation contention ever shows up in profiling, the `FOR UPDATE` scope
  is the first thing to look at.

## Action items

1. [x] Flyway `V1` — `reservation` and `reservation_line` in `db_catalog`
2. [ ] `ReserveStockUseCase` — `SELECT ... FOR UPDATE`, all-or-nothing, 409 on shortfall
3. [ ] `ConfirmReservationUseCase` — idempotent, 410 on expired
4. [ ] `@Scheduled(fixedDelay = 60s)` expiry sweep + Actuator health indicator
5. [ ] `POST /internal/v1/stock/restock` for `CANCELLED` after `CONFIRMED`
6. [ ] Tests: expired-confirm, double-confirm, concurrent last-unit race
7. [x] Record the §3.4 analysis and reservation rationale in this ADR
