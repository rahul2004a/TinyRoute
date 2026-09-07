---
name: add-endpoint
description: Implement or change a TinyRoute Spring Boot endpoint, adding only the layers required by an approved feature spec. Explicit-only; invoke $add-endpoint followed by a spec slug.
---

# Add endpoint

Implement or change an HTTP endpoint for TinyRoute's Spring Boot backend, matching
the layering and naming already locked in
[docs/architecture/architecture.md](../../../docs/architecture/architecture.md)
and the project rules in [AGENTS.md](../../../AGENTS.md).

User input: the spec slug after `$add-endpoint`, matching
`.codex/spec/<slug>.md`.

## Plan Mode

Perform read-only prerequisite checks and return a decision-complete execution
plan only. Do not create or edit files until Plan Mode has ended.

## Before you start

1. Verify `backend/pom.xml`, the Spring Boot application, and the relevant
   package structure exist. If the backend has not been bootstrapped, stop and
   report that prerequisite; do not expand this skill into application setup.
2. Read `.codex/spec/<slug>.md`. If the slug is missing or the file does not
   exist, stop with `Usage: $add-endpoint <spec-slug>` or the missing path.
3. Identify the requirement this endpoint implements. It must cite a
   `FR-*` ID from
   [docs/requirements/Functional.md](../../../docs/requirements/Functional.md)
   (and any related `NFR-*` IDs from
   [docs/requirements/Non-Functional.md](../../../docs/requirements/Non-Functional.md)).
   If no ID is findable, stop and ask the user to run `$trace`; do not invoke
   an explicit-only skill on their behalf or invent an endpoint.
4. Confirm the endpoint's priority/release (Must/Should/Could, MVP/V1/Future)
   is in scope per `AGENTS.md`. Do not build V1/Future endpoints (destination
   blocklist, API keys, admin console, safe-browsing) unless the user
   explicitly asked.
5. Confirm a read-only `spec-guardian` review exists for the final spec and has
   no blocking findings. If absent, spawn it and stop implementation until its
   result is incorporated.
6. Re-read the "Layers (LLD)", "Component interactions", and "HTTP boundary
   (MVP)" sections of
   architecture.md so naming matches exactly (e.g. `LinkController`,
   `AuthController`, `RedirectController`, `HealthController`).

## Steps

1. **Controller** — add or extend the matching `*Controller`
   (`web` package). HTTP translation only: request/response mapping,
   status codes, and the calls explicitly documented for the flow. Rate-limited
   flows call `RateLimitService` before the primary application service; other
   flows call the primary service only. No inline business logic and no direct
   repository or cache access.
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
6. **Security** — apply the route-specific controls from the "HTTP boundary
   (MVP)" section. Redirect and health are public; registration, login, reset,
   and OAuth use their documented CSRF/state controls without an access JWT;
   refresh authenticates with the refresh cookie; `/me`, logout, account
   deletion, and owner APIs use `JwtAuthenticationFilter`. Apply
   `OwnershipGuard` only to owner-scoped link operations (single query,
   `id AND owner_id`, missing and non-owned both 404 per NFR-SEC-04).
7. **Rate limiting** — for create, auth, and redirect flows, have the
   controller call `RateLimitService` before the primary service.
   `RateLimitService` uses `RateLimitStore` with the documented Redis key
   prefix (`rl:auth:`, `rl:create:`, or `rl:redirect:`); the controller never
   accesses the store.
8. **Tests** — do not create placeholder, disabled, or empty tests. After the
   endpoint is implemented, tell the user to invoke `$test <spec-slug>` for
   complete executable tests covering NFR-TST-01 and applicable failure modes.
9. **Fail closed** — for anything touching redirect resolution or auth
   state, never return a guessed result when the datastore state is unknown;
   return a safe error instead (NFR-REL-02).
10. **Design check** — keep each new type cohesive; depend on interfaces only
    at persistence, cache, and external-system boundaries; prefer composition
    over inheritance; and add a pattern such as Strategy or Factory only when
    the endpoint has a real interchangeable behavior or construction problem.

## Output

Summarize which files were added/changed, the `FR-*`/`NFR-*` IDs covered, and
any open question that needs the user's decision. Include the exact `$test`
command to run next.
