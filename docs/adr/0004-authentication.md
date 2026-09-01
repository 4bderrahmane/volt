# ADR-0004: Keycloak as the OIDC issuer; services are resource servers

**Status:** Accepted
**Date:** 2026-08-14
**Deciders:** Project maintainers

## Context

§F10 requires authentication and `CLIENT` / `ADMIN` roles, and assigns it to a
**cross-cutting service** — a service that does not exist. The §3.1 architecture
diagram contains no identity component. §5.2 gives `order` a `customer_id`
column with no table behind it and no owner. §12 requires test accounts in
the README and §13 requires a working end-to-end flow.

So identity is required, verifiable, and homeless.

There is also a second, quieter requirement that §6.1 states without addressing:
`/internal/v1/stock/**` is marked role `service`. Under `docker compose`, catalog
publishes port 8081 to the host, so `/internal/**` is reachable from anywhere on
the developer's machine — including from the browser running the SPA. An
unauthenticated internal endpoint that mutates stock is the most serious
security hole in the specification, and it is currently a table cell reading
"service" with no mechanism behind it.

## Decision

Add **Keycloak** to `docker-compose.yml` as the OIDC issuer. Both Spring services
are pure OAuth2 **resource servers** — they validate JWTs against Keycloak's
JWKS endpoint and issue no tokens themselves. This matches the
`spring-boot-starter-security-oauth2-resource-server` dependency already present
in both poms.

- **Realm:** `volt`, imported from `infra/keycloak/realm-volt.json` at startup so
  `docker compose up --build` still satisfies §13 on a clean machine.
- **Roles:** realm roles `CLIENT` and `ADMIN`.
- **SPA client:** `volt-web`, public, authorization-code + PKCE.
- **Service client:** `volt-order`, confidential, **client-credentials grant** —
  this is how order-service authenticates to `/internal/v1/stock/**`.
- **`customer_id`** in §5.2 is the Keycloak user's `sub` claim (a UUID). Store it
  as `customer_id UUID NOT NULL`. Order-service never trusts a `customerId` from a
  request body — it reads `sub` from the validated JWT. Otherwise any
  authenticated user can read anyone's orders by changing a field.
- **Test accounts** (`client@volt.test` / `admin@volt.test`, seeded in the realm
  export) go in the README per §12.2.

Two implementation details are required for correct integration:

1. A `JwtAuthenticationConverter` mapping Keycloak's `realm_access.roles` claim
   onto Spring's `ROLE_` authorities. Keycloak does not emit
   `SCOPE_`/`ROLE_`-shaped claims, so without this every `@PreAuthorize` fails
   and the usual reaction is to disable security.
2. Order-service obtaining a client-credentials token and attaching it to the
   catalog call, so `/internal/**` requires `ROLE_SERVICE` and the browser-facing
   port cannot touch it.

## Options considered

### Option A: Keycloak container (chosen)

| Dimension | Assessment |
|---|---|
| Complexity | Low in code, medium in configuration |
| Cost | ~450 MB image, one container, ~30 s startup |
| Scalability | Not a concern here |
| Team familiarity | Low — mitigated by versioned realm configuration |

**Pros:** Zero authentication code to write and maintain. Uses the standard OIDC
model — realms, clients, audiences, scopes vs roles, JWKS rotation, and client
credentials — instead of collapsing authentication into a shared secret. It is
not a deviation from "two microservices": Keycloak is infrastructure in the same
sense as PostgreSQL. It fits the existing authentication and admin-UI
workstream.

**Cons:** Realm configuration is fiddly and largely managed through the admin
UI. The export must be committed and kept in sync with any console changes, or
the clean-machine criterion in §13 silently breaks. Compose startup gets ~30 s
slower. Debugging a 401 means reading a JWT rather than a stack trace.

### Option B: A third, hand-written `auth-service`

| Dimension | Assessment |
|---|---|
| Complexity | High |
| Cost | 1–2 weeks done properly |
| Scalability | Implementation-dependent |
| Team familiarity | Medium — auth in a monolith is already known |

**Pros:** Full control over token issuance. No external identity component is a
black box.

**Cons:** Rejected on two grounds, and the second matters more than the first.

*Time:* password hashing, token issuance, refresh and rotation, revocation, key
management, and a JWKS endpoint is a project, not a task. The authentication
and admin-UI workstream already carries several deliverables.

*Protocol coverage:* a hand-written service tends toward an HS256 shared secret,
which omits audience validation, JWKS rotation, PKCE, and the client-credentials
grant — the parts of OAuth2 that are load-bearing in a multi-service system.
Minting a JWT with `Jwts.builder()` covers token construction but not the
resource-server and service-to-service responsibilities that Option A handles.

*Scope:* it also makes the system three services against a specification that
specifies two, creating an explicit scope deviation and an additional service to
operate.

### Option C: Static symmetric JWT, no issuer

| Dimension | Assessment |
|---|---|
| Complexity | Trivial |
| Cost | An afternoon |
| Scalability | None |
| Team familiarity | High |

**Pros:** Fastest. No extra container.

**Cons:** §13 requires a verifiable login flow and §12.2 requires test accounts;
tokens minted by a test fixture satisfy neither. A shared secret
distributed to every service means any service can forge any other's tokens.
Rejected.

### Option D: Spring Authorization Server embedded in catalog-service

**Pros:** Standards-compliant, no extra container, real JWKS.

**Cons:** Makes catalog-service both the product catalog and the identity
provider — two unrelated reasons to change in one deployable, which is precisely
the coupling the microservice split exists to avoid. Rejected, though it is the
most interesting rejected option and is recorded here for completeness.

## Trade-off analysis

The important tension is protocol coverage versus implementation cost. Option B
focuses on how a token is signed. Option A covers how a distributed system
agrees on identity — issuer, audience, key rotation, propagation, and machine
credentials. The latter is the requirement that matters at the service
boundary; token construction itself is a library call.

The real cost of Option A is configuration opacity. When a request returns 401
there is no breakpoint to set, only claims to inspect. Budget half a day for the
first working token and make decoded-claim inspection part of the debugging
workflow.

One risk to name explicitly: realm configuration done by clicking in the admin
console and never re-exported. The clean-machine criterion in §13 fails silently,
often only appearing during clean-start validation. **Re-export the realm after
every console change, and treat `realm-volt.json` as source code.**

## Consequences

**Easier**
- No authentication code to write, test, or maintain.
- Both services get identical, standard security configuration.
- `/internal/**` gets a real mechanism (`ROLE_SERVICE` via client credentials)
  instead of a table cell that says "service".
- Adding a third service later requires no identity work.

**Harder**
- +450 MB and ~30 s to compose startup; §8's <400 MB limit applies to the *Java
  service images*, not to Keycloak, but state that explicitly in the README so
  it does not look like a missed requirement.
- Realm export must stay in version control and in sync.
- Tests need `@WithMockJwt`-style helpers; controller tests must not require a
  live Keycloak. Use `@WebMvcTest` + mocked `JwtDecoder`, not a Testcontainer.
- 401/403 debugging is claim inspection, not stack traces.

**To revisit**
- If Keycloak startup makes the compose stack painful to iterate on, add a
  `dev` Spring profile that permits all requests — but keep it out of the CI
  path and off by default, or it can leak into shared environments.

## Action items

1. [ ] Add Keycloak service + `realm-volt.json` import to `docker-compose.yml`
2. [ ] `SecurityConfig` in both services: `oauth2ResourceServer().jwt()`
3. [ ] `KeycloakRoleConverter` mapping `realm_access.roles` → `ROLE_*`
4. [ ] `/internal/**` requires `ROLE_SERVICE`; everything else per §6 table
5. [ ] Order-service: client-credentials token on outbound catalog calls
6. [ ] Read `customer_id` from the JWT `sub` claim — never from the request body
7. [ ] Test accounts documented in README (§12.2)
8. [ ] Re-export the realm after every admin-console change
