# TinyRoute — Codex project guidance

Keep this file small. Full detail lives in the docs it points to — read those
before designing LLD, APIs, classes, or implementation, and before adding any
feature.

## Stack (locked)

- Backend is **Spring Boot** (Java). Controllers, services, filters, JPA,
  Redis adapters.
- Frontend is **Next.js**. HTTP client only; no ownership or redirect policy
  in the UI.
- PostgreSQL is the system of record. Redis is refresh sessions, JWT
  revocation, redirect cache, and rate limits.

Source of truth: [docs/architecture/architecture.md](docs/architecture/architecture.md).
Do not invent a different stack.

Use Spring names from that doc: `*Controller`, `*Service`,
`JwtAuthenticationFilter`, `Jpa*` / `Redis*` adapters, repository interfaces.
Never propose Express, Nest, Django, or Next.js API routes as the backend.
Controllers translate HTTP only; services own policy and transactions;
repositories own persistence; Redis access goes through `Redis*` adapters
behind interfaces (`RedirectCache`, `RefreshSessionStore`,
`JwtRevocationStore`, `RateLimitStore`).

## Backend design practices

- Keep controllers thin and services cohesive; split a service only when its
  responsibilities genuinely differ.
- Depend on interfaces at persistence, cache, and external-system boundaries;
  do not create interfaces for simple local helpers.
- Prefer composition over inheritance. Keep domain invariants on domain types
  and use immutable value objects where practical.
- Reuse the existing Controller–Service–Repository/Store–Adapter patterns.
  Introduce Strategy, Factory, or other patterns only for a real
  interchangeable behavior or construction problem.
- Avoid premature abstractions, unnecessary framework additions, and new
  dependencies unless the requirement calls for them.

## Requirements and scope guardrails

Source of truth (do not duplicate; read when implementing or designing):

- [docs/requirements/Functional.md](docs/requirements/Functional.md)
- [docs/requirements/Non-Functional.md](docs/requirements/Non-Functional.md)

- Portfolio URL shortener for one developer; keep it small and deployable.
- Redirects are public (no account). Create/manage requires a signed-in
  account.
- Prefer MVP over V1 over Future.
- MVP scope is **Must, Should, and Could**. Design and implement all of them
  unless the user narrows the request.
- Do not implement V1 or Future work unless the user explicitly asks:
  destination blocklist, public API / API keys, safe-browsing checks, admin
  console.

### MVP (build this)

**Must** — email/password and Google OAuth register; sign in/out;
authenticated HTTPS-only creation with unique codes and a copyable result;
expiry at create; public redirection with correct unknown, disabled, deleted,
case-mismatched, and expired handling; owner-only list, disable, re-enable,
delete, and destination edit; click counts; 30-day click trend;
referrer/device/OS/browser/geo (country and city); rate limits on creation
and auth.

**Should** — session persistence; password reset; self-reference rejection;
custom aliases; search/filter of own links; redirect throttling.

**Could** — account deletion (all of that user's short URLs stop
redirecting).

### Key NFRs (MVP)

- Redirect p95 under 150 ms, p99 under 300 ms server-side at ~100 rps
  sustained
- Create/list/manage p95 under 500 ms
- Fail closed: never guess a destination when state is unknown
- Health check endpoint; single-region, modest hardware

## When in doubt

Open `docs/requirements/Functional.md`, `docs/requirements/Non-Functional.md`,
and `docs/architecture/architecture.md` before adding features, changing
behavior, or inventing APIs/architecture that conflict with them. Cite the
matching `FR-*` / `NFR-*` ID for any non-trivial change.

## Useful skills

- `$spec` (`.codex/skills/create-spec`) — write a spec to `.codex/spec/` and
  create a feature branch for a new feature, before any code is written.
- `$trace` (`.codex/skills/trace-requirement`) — check a proposed change
  against the requirements docs before building it.
- `$adr` (`.codex/skills/write-adr`) — draft a new ADR in `docs/adr/` for a
  significant decision.
- `add-endpoint` (`.codex/skills/add-endpoint`) — scaffold a new HTTP
  endpoint following the LLD layering above.
- `$test` (`.codex/skills/test-feature`) — write and run tests for a spec via
  the `test-writer` / `test-runner` subagents.
- `$review` (`.codex/skills/code-review`) — parallel quality + security
  review of the current diff via the `quality-reviewer` / `security-reviewer`
  subagents, before commit.
- `$ship` (`.codex/skills/ship-feature`) — commit, push, and open a PR for
  the current feature branch (does not merge).
- `$seed` (`.codex/skills/seed-data`) — seed a demo user and realistic
  links/clicks in the local datastore, for demoing analytics.

Spawn the `spec-guardian` subagent (`.codex/agents/spec-guardian.toml`) for a
read-only second opinion before implementing anything non-trivial, before any
code is written. `quality-reviewer` and `security-reviewer`
(`.codex/agents/`) do the equivalent check after code exists, used by
`$review`. `test-writer` and `test-runner` (`.codex/agents/`) write and
execute tests from a spec, used by `$test`.
