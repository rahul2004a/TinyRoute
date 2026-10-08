# TinyRoute

TinyRoute is a portfolio-scale URL shortener built to demonstrate production-minded
web engineering without turning a one-developer project into a platform. Visitors
follow short links without an account. Signed-in users create and manage their own
links, while the backend owns authentication, authorization, redirects, analytics,
rate limits, and persistence.

## Project status

**Work in progress.** The account-authentication feature is the active workstream
and is implemented through its review gates. Frontend and backend CI validate
pull requests. Link creation and management, public redirects, analytics,
production infrastructure, and deployment delivery remain future
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
Caddy HTTPS ingress (Hostinger VPS, planned)
  │  private HTTP; API and public redirect hosts
  ▼
Spring Boot API (Docker on the VPS, planned)
  ├── Controllers: HTTP translation only
  ├── Services: policy, transactions, ownership
  ├── JPA repositories: PostgreSQL system of record
  └── Redis adapters: refresh sessions, JWT revocation, rate limits, redirect cache
       │                         │
       ▼                         ▼
  Supabase PostgreSQL         Redis container
  (remote JDBC over TLS)      (private VPS network + persistent volume)
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

| Area                              | Choice                                                                                          |
| --------------------------------- | ----------------------------------------------------------------------------------------------- |
| Frontend                          | Next.js App Router, React, TypeScript, Tailwind CSS, shadcn/ui on Radix UI                      |
| Frontend data and testing         | TanStack Query, React Hook Form + Zod, Vitest/RTL, Playwright                                   |
| Backend                           | Java 21, Spring Boot, Spring Web MVC, Spring Security, Spring Data JPA/Hibernate                |
| Durable data                      | Supabase managed PostgreSQL in production; PostgreSQL with Flyway migrations                    |
| Ephemeral security and cache data | Redis for refresh sessions, JWT revocations, rate limits, and redirect cache                    |
| Security                          | Argon2id, Spring Security JOSE/Nimbus JWT, OAuth2 Client, CSRF, CORS, secure cookies            |
| Local infrastructure              | Docker Compose: PostgreSQL and Redis only                                                       |
| Production target                 | Vercel frontend; Hostinger VPS for Spring Boot, Redis, and Caddy; Supabase PostgreSQL (planned) |

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
infra/vps/                       planned VPS runtime boundary and environment example
docs/
  requirements/                  product and quality requirements
  architecture/                  authoritative system design and diagrams
  decisions/                     accepted architecture decisions
  deployment/                    VPS operations and future CI/CD instructions
  development/                   CI checks and local verification
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
# Backend unit tests (no running datastores needed)
./backend/mvnw -f backend/pom.xml test
# Unit + integration/security tests, coverage and static analysis (requires Docker)
./backend/mvnw -f backend/pom.xml clean verify
# Check Java files changed since origin/main
./backend/mvnw -f backend/pom.xml spotless:check

# Frontend dependencies and checks
pnpm --dir frontend install --frozen-lockfile
pnpm --dir frontend lint
pnpm --dir frontend format:check
pnpm --dir frontend typecheck
pnpm --dir frontend test --run
pnpm --dir frontend build
```

### Backend Docker image

Build from the repository root:

```sh
docker build -t tinyroute-backend:local backend
```

[The Dockerfile](backend/Dockerfile) builds the executable Spring Boot JAR with
Maven and Java 21, then copies it into a Java 21 JRE image running as UID/GID
`10001`. Both base images are pinned by digest; update those digests deliberately
when applying base-image updates. Maven dependencies use a BuildKit cache.
[The build-context allowlist](backend/.dockerignore) includes only `pom.xml` and
`src/main`, plus only `target/application.jar` for CI's verified-artifact target,
excluding local credentials, certificates, tests, and other build output.
This implements NFR-DEP-05/08 and NFR-SEC-03.

Run `./backend/mvnw -f backend/pom.xml clean verify` before releasing an image.
Integration tests start disposable Testcontainers PostgreSQL and Redis on random
ports; they do not use or change local Compose data. Image packaging skips tests because its build has no
datastore services. For a host with a different CPU architecture, build with
the matching `--platform` value, for example `--platform linux/amd64`.

CI downloads the successfully verified JAR and builds `--target ci`; it scans
that runtime image without publishing it. The default target still builds from
source. See [CI checks and required gates](docs/development/ci.md) for workflows,
security thresholds, artifacts, and GitHub required-check setup.

Supply the profile, endpoints, JWT keys, OAuth credentials, and mail settings at
runtime through a protected environment file, using the configuration table
below. Set `SPRING_PROFILES_ACTIVE=prod` in that file. For example:

```sh
docker run --rm --name tinyroute-backend \
  --env-file /absolute/path/backend.env tinyroute-backend:local
```

Attach the container to the runtime's private network with `--network` when
connecting to Redis or an ingress proxy on that network. Container `localhost`
refers to the container itself; the development `.env` and `dev` profile target
host-local services and are not production container configuration. Mount any
required external CA certificate read-only at the configured path.

The image declares private HTTP port `8080`; `SERVER_PORT` can override it.
Its health check calls `/actuator/health` on that port after a 60-second startup
grace period. Docker health status does not itself restart the container.
Keep public traffic behind the TLS-terminating ingress proxy. JVM options can
be supplied through `JAVA_TOOL_OPTIONS`; Java runs directly as PID 1 so it
receives shutdown signals.

### Runtime configuration

Spring profiles are never hard-coded. Use `SPRING_PROFILES_ACTIVE=dev` for local
infrastructure and `SPRING_PROFILES_ACTIVE=prod` with the production Supabase
endpoint, private Redis, and externally supplied secrets. `application-dev.yml` reads the
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

| Concern               | Development and production environment variables                                                                                                                                                                                                     |
| --------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Profile/process       | `SPRING_PROFILES_ACTIVE`; production may also set `SERVER_PORT` (default `8080` behind the TLS-terminating Caddy proxy)                                                                                                                              |
| PostgreSQL            | Dev: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`; prod: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`                                                                                                            |
| Redis                 | Dev: `REDIS_PORT`; prod: `REDIS_HOST`, `REDIS_PORT`, and Spring's `SPRING_DATA_REDIS_PASSWORD` for the private Redis service                                                                                                                         |
| Browser/security      | `ALLOWED_FRONTEND_ORIGINS`, `RATE_LIMIT_HMAC_SECRET`; production also requires `TRUSTED_PROXY_CIDRS` for the exact ingress ranges that append `X-Forwarded-For`                                                                                      |
| Short-code allocation | `SHORT_LINK_BASE_URL`, `SHORT_CODE_KEY`, `SHORT_CODE_SALT`; production requires a stable secret 32-byte key in canonical Base64 and a fixed 8–64 character ASCII salt (`[A-Za-z0-9:_-]+`)                                                            |
| JWT                   | `TINYROUTE_JWT_ISSUER`, `TINYROUTE_JWT_AUDIENCE`, `TINYROUTE_JWT_ACTIVE_KEY_ID`, `TINYROUTE_JWT_SIGNING_PRIVATE_KEY_BASE64`, `TINYROUTE_JWT_ACTIVE_VERIFICATION_PUBLIC_KEY_BASE64`; optional retired verification keys use `SPRING_APPLICATION_JSON` |
| Mail                  | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `REGISTRATION_MAIL_FROM`, `PASSWORD_RESET_CONFIRMATION_URI`                                                                                      |
| Google OAuth          | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`, `GOOGLE_SUCCESS_URI`, `GOOGLE_FAILURE_URI`                                                                                                                                        |
| Local TLS only        | `DEV_TLS_CERTIFICATE`, `DEV_TLS_PRIVATE_KEY`                                                                                                                                                                                                         |

The active verification key is associated with `TINYROUTE_JWT_ACTIVE_KEY_ID`.
During rotation, keep retired public keys until every JWT signed by them has
expired by adding the `tinyroute.jwt.verification-public-keys` map through
`SPRING_APPLICATION_JSON`, for example
`{"tinyroute":{"jwt":{"verification-public-keys":{"retired-key-id":"<base64-x509-der>"}}}}`.
The signing private key and all verification public keys are base64-encoded DER,
not PEM text.

Generated codes use Redis `code:global` and salted AES-FF1 over eight Base62
digits (FR-CRE-04/07). Distinct counter values produce distinct generated codes
under fixed key/salt configuration. Keep this key separate from JWT and rate-limit
secrets, and keep the key, salt, alphabet, width, and encoder version unchanged
across creators and deployments. The committed development values are public
local-only fixtures; production has no defaults. This feature provides no key
rotation or per-link salt protocol. Public short links remain discoverable URLs.

PostgreSQL stores the final code and committed numeric allocation. Normal
allocation performs no database availability or high-water query. Missing Redis
state or a confirmed generated-code conflict recovers from the maximum committed
allocation, including deleted and expired rows. Aliases and legacy random codes
have NULL allocation metadata and cannot raise that floor. Retain all code
reservations. A malformed, expiring, or unavailable counter fails creation safely;
there is no random or process-local fallback. Redirects do not use this allocator
and still fall back to PostgreSQL on cache failure (NFR-REL-02).

### Local HTTPS for registration

From the repository root, create and trust a local certificate, then start both
applications over HTTPS:

For Google sign-in, create a Google OAuth client of type **Web application** and
add `https://localhost:8443/api/auth/google/callback` to its authorized redirect
URIs. Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in the backend's exported
environment. The redirect URI in Google Cloud must exactly match the backend's
`GOOGLE_REDIRECT_URI`, including scheme, port, and path.

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

### Disposable link verification and load protocol

The link feature's live gate requires the test-only verification application,
not an ordinary development database. It launches PostgreSQL/Redis Testcontainers,
seeds three disposable accounts and 100 links, and exposes loopback/token-guarded
fixtures only on the test classpath. It is never packaged in the backend image.
Use existing local TLS certificates and externally activate `dev`. Supply a fresh
random `TINYROUTE_VERIFICATION_TOKEN` through the environment; keep it private and
use the same token in the browser/load terminals. Do not put it in command arguments
or tracked files. No production JWT keys are needed by this test entry point.

```sh
./backend/mvnw -f backend/pom.xml -B -ntp test-compile
SPRING_PROFILES_ACTIVE=dev ./backend/mvnw -f backend/pom.xml -B -ntp spring-boot:test-run -Dspring-boot.run.main-class=com.tinyroute.LinkVerificationApplication
pnpm --dir frontend exec playwright test --config playwright.live.config.ts
```

Defaults are frontend/backend/SMTP ports 3000/8443/1025. If occupied, use an
isolated frontend checkout/copy with its own build output, and set
`TINYROUTE_E2E_FRONTEND_PORT=3001`, `TINYROUTE_VERIFICATION_PORT=8444`,
`TINYROUTE_E2E_SMTP_PORT=1026`, backend `MAIL_PORT=1026`, and
`NEXT_PUBLIC_API_BASE_URL=https://localhost:8444`. Match backend
`ALLOWED_FRONTEND_ORIGINS=https://localhost:3001` and
`PASSWORD_RESET_CONFIRMATION_URI=https://localhost:3001/password-reset/confirm`.
Keep certificate references valid in that checkout. Never stop another developer's
server or point state fixtures at their datastores. The backend test pool is bounded
to four connections with zero minimum idle; production pool settings are unchanged.

The driver binds source `127.0.0.2`, trusted only by this disposable application;
ordinary browser `127.0.0.1` traffic stays untrusted. On macOS, add a temporary
loopback alias with `sudo /sbin/ifconfig lo0 alias 127.0.0.2` and remove it afterwards
with `sudo /sbin/ifconfig lo0 -alias 127.0.0.2`. On systems where 127/8 binding works
without an alias, no administrator setup is needed. The driver validates local TLS:
point `NODE_EXTRA_CA_CERTS` at the mkcert root CA, without disabling TLS validation.

```sh
node --test tools/link-performance.test.mjs
node tools/link-performance.mjs --api-base https://localhost:8444 --rate 100 --duration 600 --warmup 30 --create-samples 200 --clients 100 --output .local-verification/link-performance.json
```

The driver creates missing output directories and a private report file before sending
traffic, rejecting unusable output paths immediately. Existing evidence is preserved
until a replacement report is ready.

Run against freshly restarted disposable data if creation quotas were consumed by
a prior measurement. The driver obtains CSRF for each creation, omits aliases,
never retries POST, and never follows a redirect. It retains all statuses and transport
failures, checks complete issuance/server timing samples, and fails on dropped arrivals
or scheduling lateness above 100 ms. Server percentiles wrap the full Spring chain;
client round trips are separate. Operational logs/metrics contain only fixed route,
method, status and duration, with no link or owner identifiers. See the
[feature verification record](docs/spec/link-creation-and-redirection/verification.md)
for current results and outstanding local/production gates (NFR-PER-01–03).

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

For production, use `SPRING_PROFILES_ACTIVE=prod`, a Supabase PostgreSQL JDBC
endpoint with verified TLS and source-IP restrictions, a private VPS Redis
endpoint, a cryptographically random rate-limit HMAC secret, SMTP authentication
with STARTTLS enabled, exact `https://app.<zone>` / `https://api.<zone>` origins,
and environment-managed JWT key material. Caddy on the Hostinger VPS terminates
public TLS; the private backend container listens on `SERVER_PORT` and must not
be publicly reachable. Supabase stores PostgreSQL data outside the VPS; Redis
uses its own persistent VPS volume, independent of backend releases. Use a
direct database connection when reachable or the session pooler on port `5432`.
Mount the Supabase CA certificate for `sslmode=verify-full`; copy the actual
host/role from the project's Connect panel. Keep Spring authentication, JPA,
Flyway, and `ddl-auto: validate`; disable the unused Supabase Data API.
Production configuration starts from [the VPS environment example](infra/vps/.env.example),
not the development `.env`.

The [Hostinger VPS deployment guide](docs/deployment/hostinger-vps.md) defines
DNS, TLS, firewalls, Supabase connections/backup plans, automated daily encrypted
database exports to Amazon S3, runtime secrets, logs, recovery,
and the future GitHub Actions release sequence. Images will be published to
private GitHub Container Registry and deployed by digest over host-verified
SSH, with GitHub environment approval. Vercel CLI deployment is retained.
The backend Dockerfile is available for image packaging. The guide and examples
describe the accepted target; production Compose, release scripts, and workflows
remain unimplemented.
The [Supabase-to-S3 backup contract](docs/deployment/supabase-s3-backup.md)
defines the daily scheduler, restricted backup identity, retention, and restore
checks. Its runtime installation remains deployment work.

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
| [Hostinger VPS deployment](docs/deployment/hostinger-vps.md)                      | Production prerequisites, operations, CI/CD, and release checks          |
| [Account-authentication specification](docs/spec/account-authentication/spec.md)  | Current feature intent, acceptance criteria, and implementation approach |
| [Account-authentication contracts](docs/spec/account-authentication/contracts.md) | HTTP, cookie, JWT, CSRF, OAuth, and session semantics                    |
| [Authentication checklist](docs/spec/account-authentication/todo.md)              | Completed tasks and acceptance checks for authentication                 |
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
