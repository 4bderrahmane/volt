# Architecture Decision Records

Each ADR records one decision, the alternatives that were rejected, and what the
decision costs us later. They are numbered; changes to an accepted decision are
recorded by superseding it, while editorial clarifications may be applied in
place.

| ADR | Title | Status |
|---|---|---|
| [0001](0001-hexagonal-package-layout.md) | Hexagonal architecture with an enforced package layout | Accepted |
| [0002](0002-service-boundaries.md) | Two services, database per service | Accepted |
| [0003](0003-stock-consistency.md) | Stock consistency via reservation → confirmation with TTL | Accepted (supersedes specification §3.4) |
| [0004](0004-authentication.md) | Keycloak as the OIDC issuer; services are resource servers | Accepted |
| [0005](0005-inter-service-communication.md) | Synchronous REST with explicit timeouts and idempotency keys | Accepted |
| [0006](0006-persistence-and-migrations.md) | Flyway is the only schema authority, tests included | Accepted |
| [0007](0007-frontend-framework.md) | React + Vite instead of Angular | Proposed — specification exception pending |
| [0008](0008-lombok-and-jakarta-annotations.md) | Lombok and jakarta annotations, scoped by layer | Accepted |

## Deviations from the technical specification

Three decisions here diverge from the specification. Their rationale and
mitigations are recorded below so each deviation remains explicit and
reviewable.

1. **ADR-0003** contradicts specification §3.4. The specification's consistency mechanism
   is not implementable as written; see the ADR for why.
2. **ADR-0004** adds a Keycloak container that does not appear in the §3.1
   diagram, because §F10 assigns authentication to no service at all.
3. **ADR-0007** replaces Angular with React, contradicting §7 and §15. This is
   the only deviation that is a preference rather than a correction, and it is
   the one most likely to be challenged.

ADR-0008 is the one place where a rule is enforced by something other than
ArchUnit. Lombok's annotations are erased before bytecode, so ArchUnit cannot
see them at all; `lombok.config` runs inside the compiler and can. A rule
guarded by the wrong mechanism passes forever while guarding nothing.

The original specification used a different identifier vocabulary. This
repository now uses English consistently across service names, packages,
database schemas, HTTP paths, and JSON fields. JPA remains a separate mapping
layer because the domain must stay independent of persistence, not because
translation is required.
