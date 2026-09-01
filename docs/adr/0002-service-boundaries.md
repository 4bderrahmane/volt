# ADR-0002: Two services, database per service

**Status:** Accepted
**Date:** 2026-08-14
**Deciders:** Project maintainers

## Context

The specification (§3.1) fixes two services and one database each, with the rule that
no service touches the other's database. That decision is already made, so this
ADR does not relitigate it — it records where the boundary actually falls,
because the specification leaves three questions open that will cause bugs if
they are not answered now.

**1. Who owns price?** §3.3 says order-service copies the unit price into the
order line so a past order does not change when the catalog changes. So catalog
owns *current* price; order owns *historical* price. Both are correct and they
are different facts. The order line is not a cache of the product — it is a
record of a commercial agreement.

**2. Who owns the customer?** §5.2 gives `order` a `customer_id` column with no
table behind it and no service that owns customers. See ADR-0004.

**3. What is the cart, really?** §F6 puts the cart in order-service, persisted
in PostgreSQL against a `customer_id`. That makes the cart a durable server-side
resource, which is a real decision with real consequences (see below), not an
implementation detail.

## Decision

The boundary is drawn by **who is the source of truth for a fact**, not by which
service happens to need it:

| Fact | Owner | Other service's access |
|---|---|---|
| Product reference, label, description | catalog | reads via REST, copies at order time |
| Current price excluding VAT | catalog | reads via REST, copies at order time |
| Physical stock | catalog | mutates only via `/internal/v1/stock/**` |
| Stock reservations | catalog | creates and confirms via `/internal/v1/stock/**` |
| Historical order-line price | order | catalog never sees it |
| Cart contents | order | catalog never sees it |
| Order status | order | catalog never sees it |
| Identity, roles | Keycloak | both validate JWTs (ADR-0004) |

**Reservations live in catalog, not order.** This is the non-obvious one.
Reserved stock is a fact about a product's availability, and availability is
catalog's concern. If reservations lived in order-service, catalog could not
answer "how many of this can I sell?" without calling order-service — an inward
dependency from owner to consumer, which inverts the boundary. It also makes the
expiry sweep (ADR-0003) a local transaction in the same database as the stock it
protects.

**The cart is durable and server-side**, per §F6. Accepted, with the
consequences written down rather than discovered:

- A cart line stores `product_id` and `quantity` only — **not** price or label.
  Prices in a cart must be *displayed live* from the catalog, because a cart is
  a shopping intent, not an agreement. Copying the price into the cart line
  would show a customer a stale price for as long as the cart lives.
- Copying happens exactly once, at `POST /api/v1/orders`, from the
  authoritative prices returned by the reservation call (ADR-0003).
- Carts accumulate forever. Add a cleanup for carts untouched for 30 days, or
  accept unbounded growth and say so.

**Catalog does not call order.** The dependency graph is acyclic:
`order → catalog`, both `→ Keycloak`. Nothing calls order-service except the
SPA. Preserve this; a cycle between two services is how a two-service system
becomes a distributed monolith.

## Options considered

### Option A: Boundary by data ownership (chosen)

**Pros:** Each fact has exactly one owner. Ownership questions have answers.
Acyclic dependency graph. Reservations sit next to the stock they constrain.

**Cons:** Order-service cannot render a cart without calling catalog for prices,
so cart display is N products per page load. Mitigated by a single batch
endpoint rather than N calls.

### Option B: Order-service caches a product read-model

Order-service keeps a local `product_cache` table, updated on read, so carts and
listings render without calling catalog.

**Pros:** Cart rendering is a single local query. Catalog outage degrades order
gracefully.

**Cons:** Two copies of price with no invalidation strategy — the exact bug §3.3
exists to prevent, reintroduced through the back door. Without events (there is
no broker in scope) the cache is stale by construction. Rejected: it trades a
correctness property for a latency problem the project does not have.

### Option C: Reservations owned by order-service

Order-service tracks what it has reserved; catalog only ever does absolute
decrements.

**Pros:** Catalog stays simpler — no reservation tables, no sweeper.

**Cons:** Catalog can no longer compute true availability alone, so it must ask
order-service, creating a cycle. Expiry cleanup becomes a cross-service
operation instead of a local transaction. Rejected.

## Trade-off analysis

The cost of Option A is a chatty cart. Rendering a 5-line cart requires one
extra HTTP hop to catalog. At this scale that is a few milliseconds and it is
the correct trade: the alternative is caching prices, and cached prices are the
specific failure §3.3 names.

The subtle win is that "where does this field live?" stops being a judgement
call. `unit_price_excl_vat` on `order_line` is not duplication — it is a
different fact from `product.price_excl_vat`, recorded at a different time for a
different reason. Keeping that distinction explicit here shows why
database-per-service is not just about avoiding shared tables.

## Consequences

**Easier**
- Every field has one owner, so schema changes stay inside one service.
- Catalog can be developed and verified with order-service switched off.
- No distributed joins, no shared schema, no coupling through the database.

**Harder**
- Cart rendering needs a batch price lookup endpoint in catalog that the specification
  does not list (`GET /api/v1/products?ids=`). Add it.
- Order-service is unusable if catalog is down. Acceptable for this scope, but
  say so explicitly rather than pretending resilience exists.
- Carts grow without bound until a cleanup job exists.

**To revisit**
- If the SPA ends up making three round trips to render one page, an API gateway
  or backend-for-frontend is the answer — not moving data across the boundary.

## Action items

1. [ ] Add `GET /api/v1/products?ids=1,2,3` to catalog for batch cart rendering
2. [ ] Confirm cart lines store only `product_id` + `quantity`
3. [ ] Decide: cart TTL cleanup job, or document unbounded growth
4. [ ] Keep the dependency graph acyclic — catalog must never call order
