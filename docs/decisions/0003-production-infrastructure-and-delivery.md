# ADR 0003: Production infrastructure and delivery

- Status: Accepted; revised for Hostinger VPS and Supabase PostgreSQL
- Date: 2026-09-14
- Updated: 2026-10-03
- Owners: TinyRoute maintainers
- Implementation status: Planned. The deployment guide and environment example
  exist; runtime infrastructure, Docker packaging, and deployment workflows do not.

## Context

TinyRoute needs a small production topology that one developer can afford and
operate. The hosting plan uses a Hostinger VPS and Supabase managed PostgreSQL.
Preserve the Spring Boot
monolith, PostgreSQL/Redis responsibilities, Vercel frontend, host-only cookies,
and existing APIs. Replace the platform-specific infrastructure and delivery
contract while retaining NFR-SEC-01/03/11/12 and NFR-DEP-01/05..09.

The frontend hosting decision still supersedes only the frontend-container
detail in ADR 0001. This revision changes deployment and operations, not the
application technology choices in ADR 0001/0002 or functional scope.

## Decision

Keep Next.js on Vercel and deploy it through GitHub Actions using the Vercel CLI.
Production releases come from protected `main` after GitHub environment approval.
Trusted frontend previews must use isolated configuration and never production
credentials or a broader production CORS allowlist.

Run the Spring Boot Docker image, Redis, and Caddy on one Hostinger
Linux VPS in one selected location, with PostgreSQL hosted in a nearby Supabase
project. Use an independent production Compose
configuration under `infra/vps/`; root `compose.yml` remains exclusively for
local development and is never used as a production base. Pin infrastructure
images and Docker build inputs. Publish the backend image to private GitHub
Container Registry and release the verified image by digest.

Use the existing DNS provider and preserve three hostnames:

- `app.<zone>` points to Vercel.
- `api.<zone>` points to the VPS for Spring APIs.
- `go.<zone>` points to the same VPS for public `/{code}` redirects.

Vercel terminates app TLS. Caddy terminates API/redirect TLS, renews certificates,
and redirects HTTP to HTTPS. It forwards over a private ingress network to
Spring Boot. Redis uses a separate private data network; only the backend joins
both networks. No backend or Redis ports are published. PostgreSQL is reached
at Supabase's provider-hosted endpoint using JDBC over verified TLS, with
database source-IP restrictions for the actual VPS egress addresses and approved
recovery sources. Outbound access supports Supabase, Google OAuth, SMTP,
certificate renewal, and image pulls.

Use a direct PostgreSQL connection when reachable from the container, or the
session pooler on port `5432` for IPv4 connectivity. Keep connection pools within
the selected database plan's limits. Spring authentication, authorization,
JPA, Flyway, and `ddl-auto: validate` remain authoritative. Supabase is used
only as managed PostgreSQL; disable its unused Data API. The frontend continues
to call Spring APIs (NFR-SEC-12, FR-MGT-02).

Authentication cookies remain host-only to `api.<zone>`. Spring Boot remains
responsible for exact-origin credentialed CORS, CSRF, authentication, ownership,
rate limits, and redirects. Caddy replaces untrusted forwarded headers and
Spring's client-address resolver trusts only Caddy's actual peer address.

## Infrastructure and operations contract

| Concern | VPS responsibility |
| --- | --- |
| Host | Owner provisions the VPS, selects location/capacity, patches Linux and Docker, and documents rebuild steps. |
| Ingress | Caddy exposes TCP 80/443; provider and Docker-aware host firewalls restrict SSH to approved sources. |
| PostgreSQL | Supabase-managed durable data outside the VPS; verified JDBC TLS, restricted source IPs, existing Flyway migrations, verified provider backup capabilities, and independent daily encrypted exports. |
| Redis | Private access, runtime authentication, persistent operational state across routine restarts; never the system of record. Lost security state requires safe invalidation before resuming auth. |
| Secrets | Protected host files supply runtime environment variables; secrets never enter Git, image layers, workflow output, or artifacts. |
| Recovery | Docker restart policies recover exited containers; a bounded watchdog detects an unhealthy running backend without restart loops during datastore outages. |
| Logs | Bounded, rotated backend/Docker and host logs with 14-day retention; Vercel retains frontend logs according to its configured plan. No sensitive request access logs. |
| Backup | Owner verifies the Supabase backup plan and schedules daily encrypted application-database exports to independent storage. Hostinger snapshots cover VPS state, not the remote database. Keep the 24-hour RPO and 4-hour RTO. |
| Configuration | Future production Compose, Caddy configuration, release/backup/watchdog scripts, and bootstrap instructions belong under `infra/vps/`. |

The backend, Redis, and certificate volumes share the VPS failure domain.
Supabase PostgreSQL is outside it: a VPS rebuild must reconnect to the existing
database rather than restore it. Database loss requires a separate Supabase
restore procedure. Stateless means the backend container has no durable
application data; Redis and certificate state still need persistent volumes.
The baseline accepts maintenance interruptions and best-effort 99.5% uptime.
No automatic failover, redundant host, orchestration cluster, or infrastructure
provisioning framework is needed for MVP.

## CI/CD policy

1. Every push/PR runs checks against disposable local services without
   deployment credentials: Maven verification, frontend lint/format/type/tests,
   build, and relevant browser checks. Fork code never receives secrets.
2. After checks pass on `main`, publish the backend image using scoped
   `GITHUB_TOKEN` package permissions and record its commit and digest.
3. Gate production jobs through the protected GitHub `production` environment.
   Serialize releases. Use an environment-scoped SSH key for a restricted
   deployment account and a separately verified SSH host key.
4. The VPS pulls only the approved digest with a pull-only registry credential.
   Keep runtime application secrets on the VPS. Flyway currently runs at backend
   startup; migration failure prevents a healthy release. Do not invent a
   standalone migration command before a runner exists.
5. Verify backend/datastore health and public TLS before recording the release.
   Keep the prior digest and a documented manual rollback path. Database changes
   must be compatible with that image; restoring an old image cannot undo schema
   changes. Automated failed-release rollback and uninterrupted redirects remain
   V1 work (NFR-DEP-03/04), not features supplied by the VPS.
6. Use a separate Vercel CLI workflow for preview/production builds. Keep
   production API origins and frontend build-time configuration explicit.
   Configure branch protection/environments after checks exist; those repository
   settings are not runtime infrastructure.

Detailed prerequisites, configuration names, delivery sequence, and release
verification are in [the deployment guide](../deployment/hostinger-vps.md).

## Consequences

- Application architecture and functional behavior stay unchanged.
- A single VPS lowers infrastructure complexity but gives the owner patching,
  security, Redis, log, and host recovery duties. Supabase operates PostgreSQL;
  the owner still configures access, backup coverage, and database recovery.
- Vercel and the VPS remain separate deployment targets under one GitHub Actions
  approval policy. Moving the frontend to the VPS needs a separate decision.
- Co-located VPS services compete for resources, and Supabase database calls add
  cross-host latency; load testing must verify NFR-PER-01..03 across both targets.
- Supabase plan/backup capabilities and independent exports must meet the
  existing recovery targets. Free-plan inactivity pausing is suitable only for
  demos with acknowledged availability limits. Always-on production needs a
  plan without inactivity pausing; this record does not purchase/select a tier.
- This revision records the target; operational automation and deployment must
  pass their own implementation and runtime verification before first release.

## Alternatives considered

- **Move Next.js to the VPS:** deferred to retain Vercel hosting and minimize
  changes to the application delivery model.
- **PostgreSQL in a VPS container:** superseded by Supabase to reduce database
  operations and separate durable data from the backend host's failure domain.
- **Managed Redis:** possible later; the lean baseline keeps Redis private and
  close to the backend on the VPS.
- **A provisioning framework or deployment control panel:** deferred; a documented
  host bootstrap and version-controlled runtime configuration suffice for one VPS.
- **Provider backups alone:** the selected Supabase plan's coverage must be
  verified; retain independent encrypted application-database exports. Hostinger
  snapshots contain no Supabase database data.
- **Multiple hosts or orchestration:** deferred until measurements justify the
  operational cost.

## Requirement drivers

- HTTPS, secrets, network isolation, and proxy trust: NFR-SEC-01/03/06/10/11/12,
  FR-ABS-02/03.
- Recovery, logs, backups, and privacy: NFR-AVL-01/02/04, NFR-OBS-01/05,
  NFR-BAK-01..05, NFR-PRV-02/03.
- Repeatable deployment, migrations, immutable images, and persistent-state
  boundaries: NFR-DEP-01..09, preserving their existing release priorities.

## References

- [Hostinger VPS dashboard](https://www.hostinger.com/support/5726606-how-to-use-the-vps-dashboard-in-hostinger/)
- [Hostinger VPS backups and snapshots](https://www.hostinger.com/support/1583232-how-to-back-up-or-restore-a-vps-at-hostinger/)
- [Caddy automatic HTTPS](https://caddyserver.com/docs/automatic-https)
- [Caddy forwarded-header behavior](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy)
- [Docker restart policies](https://docs.docker.com/engine/containers/start-containers-automatically/)
- [Docker firewall behavior](https://docs.docker.com/engine/network/packet-filtering-firewalls/)
- [GitHub Actions container publishing](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images)
- [Vercel deployments from GitHub Actions](https://vercel.com/kb/guide/how-can-i-use-github-actions-with-vercel)
- [Supabase database connections](https://supabase.com/docs/guides/database/connecting-to-postgres)
- [Supabase database network restrictions](https://supabase.com/docs/guides/platform/network-restrictions)
- [Supabase Data API isolation](https://supabase.com/docs/guides/api/securing-your-api)
- [Supabase backup capabilities](https://supabase.com/docs/guides/platform/backups)
- [Supabase production availability checklist](https://supabase.com/docs/guides/deployment/going-into-prod)
