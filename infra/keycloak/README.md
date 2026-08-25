# Keycloak realm

`realm-volt.json` is imported at container start (`start-dev --import-realm`),
so `docker compose up --build` works on a clean machine — specification §13.

## Treat this file as source code

The failure mode to avoid: you change something in the admin console at
`http://localhost:8080`, it works locally, you never re-export, and the change
exists only in a container volume. Weeks later a clean checkout no longer starts
correctly and the cause is invisible in the diff.

**After any change made through the console, re-export:**

```bash
docker compose exec keycloak \
  /opt/keycloak/bin/kc.sh export --dir /tmp/export --realm volt --users realm_file

docker compose cp keycloak:/tmp/export/volt-realm.json infra/keycloak/realm-volt.json
```

Then diff it before committing — the export is verbose and includes generated
ids that create noise.

## What is configured

| Item | Value |
|---|---|
| Realm | `volt` |
| Realm roles | `CLIENT`, `ADMIN`, `SERVICE` |
| SPA client | `volt-web` — public, authorization code + PKCE |
| Service client | `volt-order` — confidential, client-credentials, holds `SERVICE` |
| Audience client | `volt-api` — bearer-only, exists so `aud` can be validated |
| Access token TTL | 5 minutes |

## Test accounts (specification §12.2)

| Username | Password | Roles |
|---|---|---|
| `client@volt.test` | `client` | `CLIENT` |
| `admin@volt.test` | `admin` | `ADMIN`, `CLIENT` |

Development credentials for a stack that binds to localhost. They are committed
deliberately so the project starts with no manual step; nothing outside this
compose network is reachable with them.

## The two failures everyone hits

**1. `401` on every request, with no useful message.**

Almost always an issuer mismatch. The browser obtains a token from
`http://localhost:8080`, so `iss` says `localhost`; the service resolves
Keycloak at `http://keycloak:8080` on the compose network and rejects the claim.

The fix is already applied in `docker-compose.yml` and `application.yaml`:
`KC_HOSTNAME` pins the public hostname, services validate against
`issuer-uri: http://localhost:8080/...`, and fetch keys from
`jwk-set-uri: http://keycloak:8080/...`. Paste the token into jwt.io and compare
`iss` against the service's `issuer-uri` before looking anywhere else.

**2. `403` where `401` was expected.**

The token is valid; the roles are not where Spring looks for them. Keycloak puts
realm roles in `realm_access.roles`, and Spring's default converter reads
`scope`. Without a `JwtAuthenticationConverter` mapping the former to `ROLE_*`
authorities, every `@PreAuthorize("hasRole('ADMIN')")` fails for a genuine
admin. Write the converter; do not disable method security.
