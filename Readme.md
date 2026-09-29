# TinyRoute

TinyRoute is a portfolio-scale URL shortener built to demonstrate production-minded
web engineering without turning a one-developer project into a platform. Visitors
follow short links without an account. Signed-in users create and manage their own
links, while the backend owns authentication, authorization, redirects, analytics,
rate limits, and persistence.

## Project status

**Work in progress.** The account-authentication feature is the active workstream
and is implemented through its review gates. Link creation and management,
public redirects, analytics, production infrastructure, and CI/CD remain future
work. Do not treat this repository as deployed software.

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
- `mkcert` for browser-trusted local HTTPS

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
only. Docker Compose reads root `.env` automatically; Spring Boot does not. For
local development, replace the security/OAuth/mail placeholders and export the
file before starting Maven:

```sh
set -a
. ./.env
set +a
SPRING_PROFILES_ACTIVE=dev mvn -f backend/pom.xml spring-boot:run
```

Never commit JWT signing keys, database credentials, OAuth credentials, email
credentials, or a populated `.env`/`.env.local` file.

The backend configuration names are explicit:

| Concern | Development and production environment variables |
| --- | --- |
| Profile/process | `SPRING_PROFILES_ACTIVE`; production may also set `SERVER_PORT` (default `8080` behind the TLS-terminating ALB) |
| PostgreSQL | Dev: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`; prod: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` |
| Redis | Dev: `REDIS_PORT`; prod: `REDIS_HOST`, `REDIS_PORT` |
| Browser/security | `ALLOWED_FRONTEND_ORIGINS`, `RATE_LIMIT_HMAC_SECRET`; production also requires `TRUSTED_PROXY_CIDRS` for the exact ingress ranges that append `X-Forwarded-For` |
| JWT | `TINYROUTE_JWT_ISSUER`, `TINYROUTE_JWT_AUDIENCE`, `TINYROUTE_JWT_ACTIVE_KEY_ID`, `TINYROUTE_JWT_SIGNING_PRIVATE_KEY_BASE64`, `TINYROUTE_JWT_ACTIVE_VERIFICATION_PUBLIC_KEY_BASE64`; optional retired verification keys use `SPRING_APPLICATION_JSON` |
| Mail | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `REGISTRATION_MAIL_FROM`, `PASSWORD_RESET_CONFIRMATION_URI` |
| Google OAuth | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`, `GOOGLE_SUCCESS_URI`, `GOOGLE_FAILURE_URI` |
| Local TLS only | `DEV_TLS_CERTIFICATE`, `DEV_TLS_PRIVATE_KEY` |

The active verification key is associated with `TINYROUTE_JWT_ACTIVE_KEY_ID`.
During rotation, keep retired public keys until every JWT signed by them has
expired by adding the `tinyroute.jwt.verification-public-keys` map through
`SPRING_APPLICATION_JSON`, for example
`{"tinyroute":{"jwt":{"verification-public-keys":{"retired-key-id":"<base64-x509-der>"}}}}`.
The signing private key and all verification public keys are base64-encoded DER,
not PEM text.

### Local HTTPS for registration

From the repository root, create and trust a local certificate, then start both
applications over HTTPS:

```sh
TRUST_STORES=system mkcert -install
mkdir -p .local-certs
mkcert -cert-file .local-certs/localhost.pem -key-file .local-certs/localhost-key.pem localhost 127.0.0.1 ::1
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out .local-certs/jwt-private.pem
openssl pkcs8 -topk8 -nocrypt -in .local-certs/jwt-private.pem -outform DER -out .local-certs/jwt-private.der
openssl pkey -in .local-certs/jwt-private.pem -pubout -outform DER -out .local-certs/jwt-public.der
export TINYROUTE_JWT_SIGNING_PRIVATE_KEY_BASE64="$(openssl base64 -A -in .local-certs/jwt-private.der)"
export TINYROUTE_JWT_ACTIVE_VERIFICATION_PUBLIC_KEY_BASE64="$(openssl base64 -A -in .local-certs/jwt-public.der)"
SPRING_PROFILES_ACTIVE=dev mvn -f backend/pom.xml spring-boot:run
```

Run the frontend in a second terminal from the repository root:

```sh
pnpm --dir frontend exec next dev --experimental-https --experimental-https-key ../.local-certs/localhost-key.pem --experimental-https-cert ../.local-certs/localhost.pem
```

With local HTTPS configured, run the deterministic browser journeys with
Playwright's managed Chromium. These tests use disposable contract-shaped API
responses and do not delete a real account:

```sh
pnpm --dir frontend exec playwright install chromium
pnpm --dir frontend exec playwright test
```

After the real backend is running on `https://localhost:8443`, run the separate
live browser/backend contract gate. It starts its own disposable SMTP listener
on port `1025`, creates a unique test account, captures the OTP and reset email,
then verifies registration, session reload, logout, password reset, sign-in, and
account deletion across Next.js, Spring Security, PostgreSQL, and Redis. Keep
port `1025` free and configure the backend mail settings shown above:

```sh
pnpm --dir frontend exec playwright test --config playwright.live.config.ts
```

Open `https://localhost:3000/register`. The frontend calls
`https://localhost:8443/api/auth/csrf`. Maven runs Spring with `backend/` as
its working directory, so the development profile reads `../.local-certs/`.
An IDE with another working directory can set `DEV_TLS_CERTIFICATE` and
`DEV_TLS_PRIVATE_KEY` to absolute `file:` URLs. Keep `.local-certs/` private.
The backend also needs its normal local database, Redis, JWT signing, and mail
configuration from the table above. Compose intentionally does not run an SMTP
server; use a separately managed local listener or configured development SMTP
account. If `frontend/.env.local` already exists, set only
`NEXT_PUBLIC_API_BASE_URL=https://localhost:8443` in that file and restart Next.

For a fresh frontend checkout, copy `frontend/.env.example` to
`frontend/.env.local` before starting Next. Existing `.env.local` settings
should be preserved.

For production, use `SPRING_PROFILES_ACTIVE=prod`, private PostgreSQL and Redis
endpoints, a cryptographically random rate-limit HMAC secret, SMTP authentication
with STARTTLS enabled, exact `https://app.<zone>` / `https://api.<zone>` origins,
and environment-managed JWT key material. The AWS load balancer terminates
public TLS; the private ECS target listens on `SERVER_PORT` and must not be
publicly reachable.

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
  Direct local development leaves `TRUSTED_PROXY_CIDRS` unset; production must
  provide the ingress proxy CIDRs that append `X-Forwarded-For`.
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
| [Authentication checklist](docs/spec/account-authentication/todo.md)             | Completed tasks and acceptance checks for authentication                |
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
