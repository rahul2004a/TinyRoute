---
name: add-endpoint
description: Scaffold a new TinyRoute HTTP endpoint (controller, service, repository/store interface, Jpa*/Redis* adapter, domain types, and a test stub) following the LLD layering in docs/architecture/architecture.md. Use when adding or changing an API endpoint on the Spring Boot backend.
---

# Add endpoint

Scaffold a new HTTP endpoint for TinyRoute's Spring Boot backend, matching
the layering and naming already locked in
[docs/architecture/architecture.md](../../../docs/architecture/architecture.md)
and the project rules in [AGENTS.md](../../../AGENTS.md).

## Before you start

1. Identify the requirement this endpoint implements. It must cite a
   `FR-*` ID from
   [docs/requirements/Functional.md](../../../docs/requirements/Functional.md)
   (and any related `NFR-*` IDs from
   [docs/requirements/Non-Functional.md](../../../docs/requirements/Non-Functional.md)).
   If no ID is given or findable, stop and ask, or run the `trace-requirement`
   skill (`$trace`) first — do not invent an endpoint that isn't in scope.
2. Confirm the endpoint's priority/release (Must/Should/Could, MVP/V1/Future)
   is in scope per `AGENTS.md`. Do not build V1/Future endpoints (destination
   blocklist, API keys, admin console, safe-browsing) unless the user
   explicitly asked.
3. Re-read the "Layers (LLD)" and "HTTP boundary (MVP)" sections of
   architecture.md so naming matches exactly (e.g. `LinkController`,
   `AuthController`, `RedirectController`, `HealthController`).

## Steps

1. **Controller** — add or extend the matching `*Controller`
   (`web` package). HTTP translation only: request/response mapping,
   status codes, calling exactly one application service method. No business
   logic, no direct repository or cache access.
2. **Service** — add or extend the matching `*Service`
   (`application` package) that owns policy and the transaction boundary.
   Depends on repository/store **interfaces** only, never on `Jpa*`/`Redis*`
   concrete types directly.
3. **Domain types** — add or reuse types from the `domain` package (e.g.
   `Link`, `LinkStatus`, `ShortCode`, `DestinationUrl`, `User`,
   `RefreshSession`). Keep validation rules here, not scattered across
   controller/service.
4. **Repository / store interface** — if new persistence or cache access is
   needed, add an interface in `persistence` (JPA-backed) or `cache`
   (Redis-backed) — never let the service depend on `Jpa*`/`Redis*` types
   directly.
5. **Adapter implementation** — implement that interface with a `Jpa*`
   adapter (PostgreSQL) or `Redis*` adapter (cache/session/rate-limit/
   revocation), matching the key formats and TTLs already defined in the
   "Data" section of architecture.md. Do not introduce a new Redis key
   pattern without documenting it there.
6. **Security** — if the endpoint is not on the public redirect path, verify
   it goes through `JwtAuthenticationFilter` and, for mutations,
   `CsrfProtection`. If it's an owner-only operation, route ownership checks
   through `OwnershipGuard` (single query, `id AND owner_id`, missing and
   non-owned both 404 per NFR-SEC-04).
7. **Rate limiting** — if the endpoint creates/authenticates, wire it through
   `RateLimitService` / `RateLimitStore` using the correct Redis key prefix
   (`rl:auth:`, `rl:create:`, `rl:redirect:`) per the architecture doc.
8. **Tests** — add a test stub covering the happy path plus the specific
   failure modes called out in the requirement (unknown/disabled/deleted/
   expired/case-mismatch for redirects; ownership/ not-owned/missing for
   management endpoints; rate-limit exceeded where applicable). Reference
   NFR-TST-01 for which paths are critical.
9. **Fail closed** — for anything touching redirect resolution or auth
   state, never return a guessed result when the datastore state is unknown;
   return a safe error instead (NFR-REL-02).
10. **Design check** — keep each new type cohesive; depend on interfaces only
    at persistence, cache, and external-system boundaries; prefer composition
    over inheritance; and add a pattern such as Strategy or Factory only when
    the endpoint has a real interchangeable behavior or construction problem.

## Output

Summarize which files were added/changed, the `FR-*`/`NFR-*` IDs covered, and
any open question that needs the user's decision before merging.
