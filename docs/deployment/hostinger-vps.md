# Hostinger VPS deployment and delivery

## Status and scope

This is the accepted deployment contract, revised on 2026-10-03. The repository
has application code, local development Compose, and production Spring profiles.
It has **no production Compose definition, backend Dockerfile, release/backup
scripts, or GitHub Actions workflows yet**. The instructions below specify what
those future components must do; they do not claim the application is deployed.
The configuration boundary and safe example are under [infra/vps](../../infra/vps/README.md).

Keep the application stack and boundaries in
[architecture.md](../architecture/architecture.md) and
[ADR 0003](../decisions/0003-production-infrastructure-and-delivery.md).
The frontend stays on Vercel; Spring Boot, Redis, and Caddy run on
one Hostinger VPS, while Supabase hosts PostgreSQL. Root `compose.yml` remains
local-only and still includes local PostgreSQL/Redis. Production must have
its own configuration, networks, volumes, credentials, and runtime verification.

## Prerequisites and capacity

- Provision one Linux VPS in a selected Hostinger location, using a supported
  LTS OS and Docker Engine/Compose versions recorded in bootstrap instructions.
  Record its address, architecture, disk capacity, and purchased backup options.
- Select VPS capacity against the JVM, Redis, and proxy combined. Bound
  heap, connection pools, container memory/CPU, and log growth, leaving room for
  the OS, backups, and deployment. Verify NFR-PER-01..03 through load tests on
  the chosen VPS/Supabase combination; no SKU or database tier is assumed to
  meet those targets without measurement. Select nearby hosting locations.
- Create a Supabase PostgreSQL project and record its region, PostgreSQL version,
  connection limits, and backup/restore capabilities. A production plan must
  support the availability target without inactivity pausing. Free-tier demos
  have an explicit availability limitation and need scheduled independent
  backups; no paid tier is purchased or provisioned by these documents. See
  [Supabase's production checklist](https://supabase.com/docs/guides/deployment/going-into-prod).
- Retain a custom domain with `app`, `api`, and `go` subdomains under the same
  registered domain. Use the existing DNS provider; no particular DNS host is
  required. Configure Vercel's app domain and a private Amazon S3 backup bucket.
- Prepare operator SSH keys, a restricted deployment account, verified SSH host
  keys, private registry access, production SMTP, Google OAuth registration,
  JWT keys, and exact environment values before first release.
- The owner patches the VPS OS/runtime, checks disk space and backup completion,
  and responds to failures. Supabase operates PostgreSQL; the owner configures
  access, verifies backup coverage, and owns the database restore procedure.
  There is no VPS failover (NFR-AVL-01, NFR-SEC-11/12, NFR-DEP-06/09).

## DNS, TLS, and routing

| Host | Destination | Responsibility |
| --- | --- | --- |
| `app.<zone>` | Vercel-provided DNS target | Next.js UI; Vercel-managed TLS |
| `api.<zone>` | VPS address | Caddy TLS; forward API requests to Spring Boot |
| `go.<zone>` | VPS address | Caddy TLS; forward public redirects to Spring Boot |

Publish A records for the VPS hosts. Publish AAAA records only if IPv6 routing
and firewall rules are also configured and tested. Caddy must persist its
certificate/account data, obtain/renew certificates, and redirect HTTP to HTTPS.
Allow TCP 80/443 for certificate validation and public traffic. Do not expose
the backend HTTP port. Caddy's [automatic HTTPS requirements](https://caddyserver.com/docs/automatic-https)
describe DNS and reachability prerequisites (NFR-SEC-01).

The proxy preserves paths, queries, and code case and does not cache redirect
decisions or duplicate destination/ownership/rate-limit policy. Keep API routes
off `go.<zone>` and restrict operational endpoints. At most expose the minimal
`/actuator/health` status on the API host for monitoring; keep component details,
probe paths, metrics, and all other `/actuator` endpoints private.

Use credentialed browser requests only from `https://app.<zone>`, with
`NEXT_PUBLIC_API_BASE_URL=https://api.<zone>` configured at frontend build time.
Cookies stay host-only, Secure, HttpOnly, and SameSite=Lax; do not add a parent
domain attribute. Mutations still require CSRF. The default `*.vercel.app`
preview domain is cross-site to the production API: it must not be treated as
an authenticated production environment. Use isolated, same-site preview hosts
and backend configuration when full authentication previews are implemented
(NFR-SEC-06, FR-ACC-01..04).

## Private networking and firewall

The future production stack must have an ingress network for Caddy/backend
and a separate private data network for backend/Redis. Only the
backend joins both. Redis and Spring Boot have **no published host ports**.
Caddy publishes TCP 80/443, and its admin interface remains private. Preserve
backend outbound access to Supabase PostgreSQL, Google HTTPS, and SMTP,
and proxy access to certificate authorities. Private data networking must not
accidentally remove the backend's required internet egress.

Apply provider and host firewall rules, including IPv6. Restrict SSH to approved
operator and deployment sources, disable password/root SSH login after key
access is verified, and retain a recovery-access procedure. A fixed-egress CI
runner or an explicitly managed short-lived source allowlist is needed for
restricted CI SSH; ordinary hosted-runner addresses must not be assumed fixed.
Do not place an untrusted PR runner on the production VPS.

Docker-published ports can bypass ordinary host firewall assumptions; verify
rules against [Docker's firewall behavior](https://docs.docker.com/engine/network/packet-filtering-firewalls/)
and test from outside the host. Do not publish data ports and rely on a host
firewall to hide them (NFR-SEC-12).

Caddy must discard client-supplied `X-Forwarded-For` and set it from the actual
connection; do not configure arbitrary internet clients as trusted proxies.
See [Caddy's forwarded-header rules](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy).
Configure `TRUSTED_PROXY_CIDRS` with Caddy's pinned, observed peer address using
a narrow `/32` or `/128`, and verify it from inside the backend network.
Do not broadly trust private networks. Keep the servlet's actual peer address
for `ClientAddressResolver`; do not enable framework-wide forwarded-header
rewriting that changes it before this resolver runs (FR-ABS-02/03, NFR-PRV-02).

## Supabase PostgreSQL connection and access

Use the existing JDBC driver, Spring Data JPA, and Flyway. Spring remains the
source of authentication, ownership, and all application policy; the browser
calls only the Spring API. Disable the unused Supabase Data API before creating
application tables and verify TinyRoute objects are not granted to its browser
roles. The existing migrations/schema and `ddl-auto: validate` stay unchanged.
Do not adopt the quickstart's schema auto-update or a second migration system.
See [Supabase Data API controls](https://supabase.com/docs/guides/api/securing-your-api)
(FR-MGT-02, NFR-SEC-12, NFR-DEP-02).

Take the actual JDBC host, port, database, and role from the project's Connect
panel. Prefer a direct connection when the backend container can reach it;
otherwise use the session pooler on `5432`. Direct connectivity normally needs
IPv6 or the project's IPv4 option; test inside the container, not just on the
VPS host. Keep the transaction pooler off the JPA/Flyway datasource. Configure
a bounded Hikari pool within the project's connection budget, accounting for
operator/export connections and overlapping releases. See
[Supabase's connection modes](https://supabase.com/docs/guides/database/connecting-to-postgres)
and [pooling limits](https://supabase.com/docs/guides/database/connecting-to-postgres/pooling-and-limits).

Keep credentials in `DATABASE_USERNAME`/`DATABASE_PASSWORD`, rather than inside
the URL. Use `sslmode=verify-full` and `sslrootcert` in `DATABASE_URL`; obtain the
matching trusted CA from the project's database settings and mount it read-only
at `/run/tinyroute/supabase-ca.crt`. Hostname and certificate verification must
pass for the chosen direct/session endpoint; do not downgrade TLS to make a
connection work. The example's invalid hostname must be replaced before use.
See [Supabase TLS verification](https://supabase.com/docs/guides/platform/ssl-enforcement)
(NFR-SEC-01/03).

The database is outside the VPS Docker network and uses a provider-hosted
network endpoint. Configure Supabase database source-IP restrictions for the
backend's actual VPS egress addresses and narrowly approved recovery/export
sources, including IPv6 where applicable. Verify allowed and rejected sources
against the selected connection route. PostgreSQL/pooler restrictions do not
protect Supabase HTTP APIs, which is why the unused Data API is disabled.
Use a role scoped to TinyRoute objects and necessary startup Flyway operations;
avoid granting general platform administration. See
[Supabase network restrictions](https://supabase.com/docs/guides/platform/network-restrictions)
(NFR-SEC-11/12).

## Runtime environment and secrets

Start from [infra/vps/.env.example](../../infra/vps/.env.example). Populate a
protected file outside the checkout, such as `/etc/tinyroute/backend.env`, owned
by root with mode `0600`, and inject it only into the backend at container start.
Spring Boot does not automatically load `.env`. The production example is a
container env-file, not a shell script. Mount the trusted database CA separately.
Keep Redis authentication, proxy configuration, and registry credentials separately
scoped to the services that need them. Never print expanded production Compose
configuration or secret-bearing environment values into CI logs.

| Setting | Production value/contract |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod`, selected externally |
| `SERVER_PORT` | `8080` on the private ingress network |
| `DATABASE_URL` | Copied Supabase direct/session JDBC endpoint for database `postgres`, with `sslmode=verify-full` and the mounted `sslrootcert` |
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Actual Supabase database role/session username and protected database password; not Supabase API keys |
| `REDIS_HOST`, `REDIS_PORT` | `redis`, `6379` on the private data network |
| `SPRING_DATA_REDIS_PASSWORD` | Standard Spring Redis property; match private Redis server authentication |
| `ALLOWED_FRONTEND_ORIGINS` | Exact `https://app.<zone>` origin; no wildcard |
| `TRUSTED_PROXY_CIDRS` | Actual Caddy ingress peer address; narrow CIDR |
| `RATE_LIMIT_HMAC_SECRET` | At least 32 cryptographically random bytes, kept stable across normal releases |
| JWT variables | Existing issuer/audience/key-ID and base64 DER private/public key names from the README |
| Mail variables | Authenticated production SMTP with required STARTTLS; production sender and reset-confirmation URI |
| Google variables | Production client/secret; callback exactly `https://api.<zone>/api/auth/google/callback`; exact app success/failure URIs |
| Actuator variables in example | Enable private liveness/readiness probes; readiness includes PostgreSQL and Redis; public details remain disabled |

Use only generated production credentials, never `.env` development defaults.
Protect Docker access because it grants extensive host privileges; a deployment
account must not receive unrestricted Docker-group membership or arbitrary
sudo. Use a narrowly scoped, root-owned release entry point and fixed service
configuration. Preserve JWT key rotation using existing `kid` and retired-public
key support. Restrict SSH key forwarding and rotate deployment/registry tokens
(NFR-SEC-03/10/11).

## Persistence, backups, and restore

Supabase stores durable PostgreSQL data outside the VPS. There is no production
PostgreSQL container or database volume on the VPS. Redis has its own
persistent volume for sessions, revocation, and other operational
state across routine restarts; configure persistence and capacity deliberately.
Avoid eviction of security state under cache pressure. Redis is not a backup or
the source of truth. Never delete volumes during a release or use development
volume names (NFR-DEP-08).

Verify the selected Supabase plan's backups and recovery options. Paid plans
provide daily backups; free projects need owner-scheduled exports. Provider
backup access, retention, and restore behavior must be checked for the actual
project. See [Supabase database backups](https://supabase.com/docs/guides/platform/backups).

Run an automated daily job on the VPS that creates a consistent TinyRoute
application-database export, encrypts it, and uploads it over HTTPS to a private
Amazon S3 bucket outside both the VPS and the Supabase project. Use an approved
direct or session connection with verified TLS. Capture application schema/data
and Flyway history plus separately maintained role/grant/rebuild information;
do not overwrite Supabase-managed schemas or roles during restore. Keep backup
credentials separate from backend/release credentials and PR CI.

The [Supabase-to-S3 backup contract](supabase-s3-backup.md) specifies the daily
timer, failure/retry behavior, encrypted upload, private bucket/IAM settings,
and retrieval/restore gates. Keep decryption-key custody outside the VPS and
bucket. Check completion, freshness, integrity, disk use, and retrieval. The
baseline retains daily exports for at least 7 days through S3 lifecycle rules;
NFR-BAK-05's acceptance priority remains V1. Ensure expired copies and restored
personal data follow NFR-PRV-04. Select and provision the actual bucket, region,
backup identity, and encryption recipient before installing the job.

Hostinger backups and snapshots are supplemental VPS-recovery aids. They contain
no Supabase PostgreSQL data. Verify the purchased schedule and retention;
database recovery must use Supabase restore capabilities or independent exports.
See [Hostinger's backup guidance](https://www.hostinger.com/support/1583232-how-to-back-up-or-restore-a-vps-at-hostinger/)
(NFR-BAK-01..03).

The written recovery runbook must restore service within 4 hours of the owner
starting and distinguish these failure paths. Before rolling back PostgreSQL,
close public traffic and stop backend/async writers so no requests use mixed
database and Redis state:

1. **VPS loss:** rebuild from recorded OS/Docker/bootstrap configuration, restore
   restricted access, secrets, and the trusted CA, and reconnect to the existing
   Supabase database. Verify/update database source restrictions for any changed
   egress addresses. Do not restore an intact database just because the VPS failed.
2. **Database loss:** use the selected Supabase recovery procedure or restore
   the compatible application export into a clean Supabase project. Recreate
   scoped roles/grants and disabled Data API configuration, verify data/Flyway
   history, and update the endpoint/CA/credentials/source restrictions before
   applying any new migrations. Invalidate surviving redirect caches and refresh
   sessions, and complete the security reset below even if Redis survived:
   restored user `tokenVersion` values can undo later credential invalidations.
   Reconcile known post-backup deletions and security changes before reopening;
   keep restored personal data subject to deletion cleanup under NFR-PRV-04.
3. **Security reset after database rollback or Redis-state loss:** start Redis
   with fresh cache/session/revocation state; do not restore stale refresh
   sessions or revocation snapshots. Invalidate old access credentials using the
   existing signing-key controls with a new key ID and without retained old
   verification keys. Expect users to sign in again. Keep affected auth and
   redirect routes closed until restored database state and security changes
   are reconciled; fail closed when their correctness is uncertain
   (NFR-REL-02, NFR-SEC-09).
4. Pull the matching backend digest, run compatible startup migrations against
   Supabase, start Caddy, and verify health, origins, auth, and redirect correctness
   before updating DNS/reopening traffic.
5. Record restore time and backup age. A documented clean-environment restore
   drill remains V1 acceptance (NFR-BAK-04); no tested RTO is claimed here.

## Health recovery and logs

Use Docker restart policies for exited containers and host reboot recovery.
A Docker health check marks a running container unhealthy; it does not itself
restart that container. A separate host watchdog must check private liveness,
use a startup grace period and bounded consecutive-failure threshold, and
serialize with releases before restarting the backend. Check readiness and
datastore reachability separately: do not restart the backend repeatedly when
PostgreSQL or Redis is down. Docker owns process restart behavior; avoid a
second competing process supervisor. See [Docker restart policies](https://docs.docker.com/engine/containers/start-containers-automatically/)
(NFR-AVL-02/04).

Bound Docker/host log disk use and rotate with 14-day retention. Make production
logs available through restricted operator access and document Vercel's actual
build/runtime retention for its plan. Preserve existing structured request logs;
do not enable proxy access logs that retain raw IPs, cookies, authorization,
tokens, query strings, or sensitive paths. Use sanitized method/status/duration
metadata when ingress request logging is needed. Check all operational and
failure logs against NFR-OBS-01/05 and NFR-PRV-02/03. External uptime alerts,
error tracking, and broader metrics remain V1 scope.

## CI/CD implementation contract

No workflows currently exist. Future workflows use these jobs and inputs:

| Stage | Trigger and permissions | Required behavior |
| --- | --- | --- |
| Checks | Push/PR; read-only source access; no deployment secrets | Disposable PostgreSQL/Redis; Maven `verify`; frontend lint, format check, typecheck, tests, build, and applicable browser checks |
| Backend image | Verified `main`; `contents: read`, `packages: write` | Build pinned, non-root Docker image; publish to private GHCR using `GITHUB_TOKEN`; record commit and image digest |
| Backend release | Verified `main`; approved `production` environment | Host-verified restricted SSH; digest pull, Supabase Flyway/startup and private Redis health gates, prior-release record, release result |
| Frontend preview | Trusted branch; isolated preview environment | Vercel CLI preview with preview-only credentials/configuration; no production API authentication assumptions |
| Frontend release | Verified `main`; approved `production` environment | Vercel CLI build/deploy with production API origin; record deployment URL/ID for rollback |

The existing local checks are documented in [Readme.md](../../Readme.md).
CI must provide disposable local PostgreSQL/Redis test configuration and local
certificates for HTTPS browser tests; PRs never connect to the production Supabase
database. Pin third-party actions by commit and
tool/image versions; use frozen frontend lockfiles. Never execute untrusted PR
code in a privileged `pull_request_target` job or give fork jobs secrets.
Configure required checks after the workflows exist (NFR-MNT-01, NFR-DEP-09).

| Credential/input | Storage and scope |
| --- | --- |
| `GITHUB_TOKEN` | Ephemeral image-publishing job token with scoped package write permission |
| `VPS_HOST`, `VPS_SSH_PORT`, `VPS_DEPLOY_USER` | GitHub environment variables; non-secret connection settings |
| `VPS_DEPLOY_SSH_KEY` | GitHub `production` environment secret; dedicated restricted deployment key |
| `VPS_SSH_KNOWN_HOSTS` | Protected environment value verified through a trusted channel; enforce `StrictHostKeyChecking=yes` |
| GHCR pull credential | Protected VPS registry configuration; minimum `read:packages` permission and package access; never a registry write token |
| `VERCEL_TOKEN` | Appropriate isolated GitHub preview/production environment secret |
| `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID` | GitHub environment variables for the correct Vercel project |
| Backend application secrets | Protected VPS runtime files; never CI artifacts or build inputs |

Publishing images uses [GitHub's container registry workflow](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images).
The VPS pull credential is separate; see [registry authentication](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry).
Do not learn the SSH host key by blindly trusting `ssh-keyscan` during a release.
No provider API token or cloud identity role is needed for SSH deployment.

The backend release entry point must perform this sequence:

1. Acquire a release lock shared with the watchdog; verify the allowed GHCR
   repository/digest, configuration, datastore health, and current backup.
2. Pull the already verified image by digest and record the current healthy
   digest/configuration version. Do not build or install application dependencies
   on the VPS or use `latest` as a release identifier.
3. Replace only the backend container, preserving datastore/certificate volumes.
   Flyway currently runs against Supabase at Spring startup, from the restricted
   VPS source; a failed migration/startup means failure, not a completed release.
   No standalone migration command exists.
4. Wait for private readiness (including Supabase PostgreSQL and Redis), verify public HTTPS
   and a minimal API smoke check, then record the digest/commit and result. Once
   implemented, redirect smoke checks must exercise FR-RED-01..07.
5. On failure, report it and use the documented previous-digest rollback after
   checking schema compatibility. Release the lock even on failure; never erase
   data to make a container healthy.

Use GitHub production concurrency to queue releases rather than cancel one in
progress; backend and frontend production jobs must share the chosen serialized
release policy. Also use the VPS lock so manual releases cannot race CI. The
eventual single release command must be documented and verified with these
steps before NFR-DEP-01 is claimed complete.

For Vercel, configure production `NEXT_PUBLIC_API_BASE_URL` before the build and
follow its [GitHub Actions CLI sequence](https://vercel.com/kb/guide/how-can-i-use-github-actions-with-vercel)
(`vercel pull`, `vercel build`, `vercel deploy --prebuilt`, with the appropriate
production options). Retain Node.js 24 LTS and the existing frontend checks.
Keep GitHub Actions as the release authority; avoid a second automatic
production deployment from Vercel's Git integration. Coordinate API/frontend
changes through backward-compatible contracts rather than an assumed atomic
deployment across both platforms.

## Rollback and first-release gates

MVP requires a recorded prior backend digest and a manual rollback procedure.
Rollback changes only the backend image and compatible configuration. Flyway
schema changes are not undone by an image rollback; use backward-compatible
migrations, or a planned forward fix/restore with understood data-loss risk.
Frontend rollback restores the previous healthy Vercel deployment. Automated
backend rollback within 15 minutes and no redirect downtime remain V1 work
(NFR-DEP-03/04). A single backend replacement may interrupt traffic; the VPS
does not supply rolling deployment or automatic failover.

Before first production release, record evidence for:

- Actual VPS capacity/location and nearby Supabase region/plan, pinned runtime
  inputs, private Redis networks/volumes,
  external port checks including IPv6, and a restricted verified SSH path.
- Correct DNS/certificates, HTTP-to-HTTPS behavior, exact app/API origins,
  host-only cookies, CSRF, proxy-spoofing rejection, Google callback, and SMTP.
- Backend image digest/commit, startup migrations, datastore readiness,
  exited-container recovery, and bounded unhealthy-container recovery.
- Verified database TLS/CA and source restrictions, appropriate connection mode
  and pool bounds, and disabled Supabase Data API with no browser database access.
- Supabase backup coverage, automated daily encrypted S3 upload and retrieval, separate
  VPS/database restore procedures, protected
  secret custody, sanitized logs, disk bounds, and retention settings.
- Passing CI checks, production approval/concurrency, immutable delivery,
  release evidence, and a compatible manual rollback path.
- Full MVP application acceptance and measured NFR-PER-01..03 on the selected
  VPS. Documentation and local profile tests alone do not establish these gates.
