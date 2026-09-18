# TinyRoute

TinyRoute is a portfolio-scale URL shortener built to demonstrate production-minded
web engineering without turning a one-developer project into a platform. Visitors
follow short links without an account. Signed-in users create and manage their own
links, while the backend owns authentication, authorization, redirects, analytics,
rate limits, and persistence.

## Project status

**Work in progress.** The account-authentication feature is the active workstream.
Its first seven foundation tasks are complete: application bootstrapping, local
datastores, user and identity persistence, password and JWT primitives, Redis
session/revocation/rate-limit stores, and the browser security perimeter.

Registration and OTP verification are currently being implemented. End-to-end
authentication, link creation and management, public redirects, analytics,
Google sign-in, password reset, account deletion, production infrastructure, and
CI/CD are not complete yet. Do not treat this repository as deployed software.

## What TinyRoute is intended to deliver

The MVP includes:

- Email/password and Google sign-up; sign-in, sign-out, persisted sessions,
  password reset, and account deletion.
- Authenticated creation of HTTPS short links, custom aliases, expiry, and
  copyable results.
- Public redirects that fail closed for unknown, disabled, deleted,
  case-mismatched, and expired codes.
- Owner-only link list, search, destination editing, disable/re-enable, and
  permanent deletion.
- Click totals, date-range trends, and aggregated referrer, device, OS,
  browser, country, and city analytics.
- Creation, authentication, and redirect throttling.

The full scope, priority, and acceptance criteria live in
[Functional requirements](docs/requirements/Functional.md). Performance,
security, reliability, privacy, and delivery targets live in
[Non-functional requirements](docs/requirements/Non-Functional.md).

## Architecture at a glance

```text
Browser (Next.js / Vercel)
  │  credentialed HTTPS + CSRF header
  ▼
Spring Boot API (AWS ECS Fargate, planned)
  ├── Controllers: HTTP translation only
  ├── Services: policy, transactions, ownership
  ├── JPA repositories: PostgreSQL system of record
  └── Redis adapters: refresh sessions, JWT revocation, rate limits, redirect cache
       │                         │
       ▼                         ▼
  PostgreSQL                  Redis
```

Next.js is an HTTP client only. It does not proxy ordinary API calls, handle the
public short-code route, store tokens, or recreate backend policy. Spring Boot
owns all security, ownership, validation, redirect, and rate-limit decisions.

The authoritative design is in [Architecture](docs/architecture/architecture.md).
The records behind the locked frontend, backend, and production-delivery choices
are [ADR 0001](docs/decisions/0001-frontend-stack.md),
[ADR 0002](docs/decisions/0002-backend-stack.md), and
[ADR 0003](docs/decisions/0003-production-infrastructure-and-delivery.md).

## Technology stack

| Area                              | Choice                                                                               |
| --------------------------------- | ------------------------------------------------------------------------------------ |
| Frontend                          | Next.js App Router, React, TypeScript, Tailwind CSS, shadcn/ui on Radix UI           |
| Frontend data and testing         | TanStack Query, React Hook Form + Zod, Vitest/RTL, Playwright                        |
| Backend                           | Java 21, Spring Boot, Spring Web MVC, Spring Security, Spring Data JPA/Hibernate     |
| Durable data                      | PostgreSQL with Flyway migrations                                                    |
| Ephemeral security and cache data | Redis for refresh sessions, JWT revocations, rate limits, and redirect cache         |
| Security                          | Argon2id, Spring Security JOSE/Nimbus JWT, OAuth2 Client, CSRF, CORS, secure cookies |
| Local infrastructure              | Docker Compose: PostgreSQL and Redis only                                            |
| Production target                 | Vercel frontend; Spring Boot on ECS Fargate in `ap-south-1` (planned)                |

## Repository guide

```text
backend/                         Spring Boot API and its tests
  src/main/java/com/tinyroute/
    controller/                  HTTP translation
    service/                     business rules and transactions
    repository/                  persistence contracts and Jpa* adapters
    cache/                       cache contracts and Redis* adapters
    security/                    JWT, cookies, filters, and guards
    model/                       JPA entities and value objects
    config/                      Spring configuration
  src/main/resources/
    db/migration/                Flyway schema migrations
    application*.yml             shared, dev, and prod configuration
frontend/                        Next.js application, tests, and UI foundations
docs/
  requirements/                  product and quality requirements
  architecture/                  authoritative system design and diagrams
  decisions/                     accepted architecture decisions
  spec/                          feature specifications and contracts
tasks/                           active plan and checklist for the current feature
compose.yml                      local PostgreSQL and Redis only
```

The Java package layout is layer-first across the application. Do not create
per-feature Java package trees: controllers call services, services use repository
and cache interfaces, JPA adapters own PostgreSQL access, and Redis adapters own
Redis access.

## Local development

### Prerequisites

- JDK 21
- Node.js 24 LTS and pnpm 12
- Docker Engine with Docker Compose

### Start local infrastructure

```sh
cp .env.example .env
docker compose up -d
docker compose ps
```

Compose intentionally starts only PostgreSQL and Redis. PostgreSQL data is kept
in the named `postgres-data` volume; Redis development data is disposable. The
Spring Boot application runs separately through Maven or an IDE.

### Install and verify

```sh
# Backend tests (require the local PostgreSQL and Redis services)
mvn -f backend/pom.xml test
mvn -f backend/pom.xml verify

# Frontend dependencies and checks
pnpm --dir frontend install --frozen-lockfile
pnpm --dir frontend lint
pnpm --dir frontend typecheck
pnpm --dir frontend test --run
pnpm --dir frontend build
```

### Runtime configuration

Spring profiles are never hard-coded. Use `SPRING_PROFILES_ACTIVE=dev` for local
infrastructure and `SPRING_PROFILES_ACTIVE=prod` only with private production
endpoints and externally supplied secrets. `application-dev.yml` reads the
Compose ports on localhost; `application-prod.yml` expects every endpoint and
secret from the environment.

The committed `.env.example` and `frontend/.env.example` contain placeholders
only. Never commit JWT signing keys, database credentials, OAuth credentials,
email credentials, or a populated `.env`/`.env.local` file.

The project does not yet publish a stable full-browser runtime command: the
in-progress authentication feature is still supplying its local JWT and email
configuration. The verification commands above are the supported way to check
the current foundation.

## Security model

- Passwords use Argon2id; raw passwords are never persisted.
- Access JWTs are RS256-signed, short-lived (15 minutes), key-ID-aware, and
  validated for issuer, audience, token type, expiry, and token version.
- Access and refresh tokens are sent only in host-only, `HttpOnly`, `Secure`,
  `SameSite=Lax` cookies. Refresh tokens are stored in Redis only as hashes and
  use a 30-day sliding idle lifetime.
- The API allows credentialed CORS only from configured frontend origins.
  Mutations require CSRF protection; the CSRF bootstrap response is not cached.
- Redis-backed authentication rate limits use HMAC-derived client-address keys
  and accept forwarded addresses only from configured trusted proxies.
- PostgreSQL is the system of record. Redis is disposable operational state, so
  authorization must fail closed if required security state is unavailable.

The exact API and cookie rules for the active authentication feature are in
[its contracts](docs/spec/account-authentication/contracts.md).

## Documentation map

| Read this                                                                         | When you need to know                                                    |
| --------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| [Functional requirements](docs/requirements/Functional.md)                        | Product behavior, MVP scope, and requirement IDs                         |
| [Non-functional requirements](docs/requirements/Non-Functional.md)                | Performance, security, reliability, privacy, and deployability targets   |
| [Architecture](docs/architecture/architecture.md)                                 | System boundaries, configuration, deployment topology, and design rules  |
| [Account-authentication specification](docs/spec/account-authentication/spec.md)  | Current feature intent, acceptance criteria, and implementation approach |
| [Account-authentication contracts](docs/spec/account-authentication/contracts.md) | HTTP, cookie, JWT, CSRF, OAuth, and session semantics                    |
| [Active checklist](tasks/todo.md)                                                 | What is complete and what remains in the current feature                 |
| [Design guide](DESIGN.md)                                                         | Frontend presentation rules only                                         |

## Contribution rules

Start feature work from a clean, current `main` branch and use
`feature/<feature-name>`. Keep one concern per branch; do not implement or
commit directly on `main`. Before designing or implementing, read the relevant
requirements and architecture documents, and cite the matching `FR-*` or
`NFR-*` identifier for non-trivial work.

Keep controllers thin, put policy and transactions in services, and use
repository/cache interfaces at persistence boundaries. Add a dependency only
when a stated requirement needs it. See [AGENTS.md](AGENTS.md) for the complete
project workflow and guardrails.
