# ADR-0007: React + Vite instead of Angular

**Status:** **Proposed — specification exception pending**
**Date:** 2026-08-14
**Deciders:** Project maintainers

> This is the only decision in this set that deviates from the specification as a
> *preference* rather than as a correction. ADR-0003 and ADR-0004 fix things that
> are broken or missing. This one substitutes a different tool for a working one.
> It therefore requires an explicit exception rather than being treated as an
> implicit implementation detail.

## Context

§7 specifies Angular 17+ with standalone components, `inject()`, and `@if`/`@for`
control flow. §15 repeats Angular 17+ in the reference stack. §1 lists
"Develop an Angular interface that consumes the REST APIs" as an explicit
objective. Angular is therefore a named constraint, not an interchangeable
implementation detail.

The repository is currently scaffolded with React 19 + Vite 8 + TypeScript 7.

The relevant compliance and delivery considerations are:

**The specification is unambiguous.** Angular is named in both the objectives
and the reference stack. Its opinionated dependency injection, module
boundaries, and RxJS model also align conceptually with the Spring backend.
Using React is a deliberate exception to §7 and §15.

**The technical case for equivalence:** §13's acceptance criteria do not mention
Angular. They test
behaviour: compose starts, catalog browsable, cart fills, order persists, stock
decrements, prices freeze, CI green, coverage met, no secrets. Every one of those
is framework-agnostic. The framework appears in the objectives and the reference
stack, not in the behavioural acceptance gate.

**Existing scaffolding is not a justification.** Three files and a
`package.json` represent negligible sunk cost. The decision must stand on
delivery risk, maintainability, and requirement coverage.

## Decision

Keep React 19 + Vite only under an explicitly documented exception to §7 and
§15. Resolve the exception before frontend feature implementation. If strict
conformance is required, replace the scaffold with Angular before feature work
begins.

Rationale, in descending order of weight:

1. Every §13 acceptance criterion is framework-independent.
2. The §1 objectives that dominate — hexagonal architecture, service
   decomposition, inter-service consistency, containerisation, CI — are all
   backend, and none is affected.
3. Existing React fluency concentrates delivery time on auth flow, guards, error
   states, and admin CRUD rather than framework migration.

Every §7 structural requirement has a React equivalent. The mapping below
establishes behavioural and architectural coverage; it does not remove the
framework exception:

| §7 requirement | React implementation |
|---|---|
| `core/` / `shared/` / `features/` | Same directory structure, unchanged |
| Typed HTTP services | Typed fetch wrapper per feature, shared error envelope |
| URLs from `environment.ts` | `import.meta.env` + `src/config/env.ts` |
| Auth interceptor (token injection) | Wrapper around `fetch` attaching the bearer token |
| Global error interceptor | Same wrapper mapping RFC 7807 → typed errors + error boundary |
| Route guards (auth + admin) | `<RequireAuth>` / `<RequireRole role="ADMIN">` route elements |
| Lazy loading | `React.lazy` + `Suspense` per route |
| Explicit loading/error states | TanStack Query `isPending` / `isError`, no silent empty screens |
| Single UI library, used consistently | Tailwind only — not Tailwind plus a component kit |
| Responsive | Tailwind breakpoints |
| Tests (§9: "key services and components") | Vitest + Testing Library, already configured |

**If the exception is declined, switch before feature implementation.** For an
engineer familiar with TypeScript and dependency injection, the migration is
estimated at roughly one engineer-week, and the directory structure above ports
almost directly. Resolving the exception early keeps that rework bounded.

## Options considered

### Option A: Switch to Angular 17+

| Dimension | Assessment |
|---|---|
| Complexity | Medium — new framework |
| Cost | ~1 engineer-week |
| Scalability | Fine |
| Specification compliance | **Full** |

**Pros:** Requires no exception. Angular's DI and module boundaries align with
the backend's explicit dependency boundaries. Removes the largest compliance
risk in the project.

**Cons:** Consumes roughly one engineer-week that could otherwise go into
behaviour covered directly by §13.

### Option B: Keep React under a documented exception (chosen)

| Dimension | Assessment |
|---|---|
| Complexity | Low |
| Cost | Zero |
| Scalability | Fine |
| Specification compliance | **Exception required** |

**Pros:** Delivery time remains focused on behaviour covered by §13. The
deviation is explicit, bounded, and supported by a requirement mapping.

**Cons:** Until the exception is resolved, the implementation carries a known
compliance risk. A late reversal increases rework and delays frontend delivery.

### Option C: Keep React without recording the deviation

| Dimension | Assessment |
|---|---|
| Complexity | Low |
| Cost | Zero up front |
| Scalability | Fine |
| Specification compliance | **None** |

**Pros:** No up-front governance work.

**Cons:** Rejected. It silently violates §7 and §15, removes traceability, and
defers discovery until corrective work is most expensive. Passing the
framework-agnostic acceptance checks does not cancel an explicit stack
constraint.

## Trade-off analysis

The technical question is nearly empty: React and Angular both build this SPA,
and every §7 requirement maps cleanly. The material issue is whether behavioural
equivalence is sufficient to justify an exception to a named technology
constraint.

The strongest case for keeping React is that §13 — the definition of success the
document itself provides — is untouched. The strongest case against is that §1,
§7, and §15 name Angular explicitly; framework-agnostic acceptance criteria do
not invalidate those constraints.

That asymmetry makes the decision conditional: keep React only with a documented
exception, otherwise switch before building feature-specific code.

Note also that this deviation is not free of interaction effects: ADR-0004 also
departs from the §3.1 diagram. Two structural deviations are defensible when both
have explicit rationale and verification. Additional deviations increase the
burden of proving specification coverage and must be reviewed independently.

## Consequences

**Easier**
- No framework migration; delivery time goes into auth, guards, and error states.
- Vite dev server and build are fast, and Vitest is already wired.

**Harder**
- The §7 and §15 exception must remain explicit and traceable.
- No framework-provided interceptor or guard abstractions — the fetch wrapper and
  route guards are hand-written, and §7 requires them, so they must be
  deliberately built rather than assumed.
- The nginx image (§8) must serve an SPA with `try_files ... /index.html`, or
  deep links 404 after refresh. This is the standard SPA-on-nginx trap.

**To revisit**
- Before frontend feature implementation or whenever the specification
  constraint changes. If the exception is not accepted, switch to Angular. This
  ADR remains `Proposed` until the exception is resolved.

## Action items

1. [ ] Record explicit acceptance or rejection of the §7 and §15 exception
2. [ ] Update this ADR to `Accepted` or `Rejected` when resolved
3. [ ] Set up `core/` / `shared/` / `features/` to mirror §7 exactly
4. [ ] Hand-write the fetch wrapper (token + RFC 7807 mapping) and route guards
5. [ ] Tailwind only — no second UI library (§7 requires consistency)
6. [ ] nginx `try_files $uri $uri/ /index.html` for SPA deep links
