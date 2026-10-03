# Hostinger VPS deployment documentation

## Intent and scope

Replace the former production hosting plan with a Hostinger VPS for Spring Boot
and Redis, Supabase managed PostgreSQL, and Next.js on Vercel. Preserve the
existing application stack, layers, APIs, security, and product requirements.
This is a documentation/configuration update, not a production release or an
implementation of the future infrastructure and delivery workflows.

## Deployment decisions

- Use one Linux VPS in one selected Hostinger location. Caddy terminates HTTPS
  for the API and public redirect hosts and forwards to the Spring Boot image.
- Use Supabase only as managed PostgreSQL through the existing JDBC/JPA/Flyway
  stack. Connect using verified TLS and restrict database sources; keep Spring
  authentication/ownership policy and disable the unused Supabase Data API.
- Keep Redis separate from the stateless backend on a private VPS network with
  a persistent volume. Record Supabase backup/plan requirements and independent
  daily database exports; local PostgreSQL remains a development container.
- Keep root `compose.yml` for development datastores only. A future independent
  production definition belongs under `infra/vps/`; it must not extend or use
  development configuration or credentials.
- Use private GitHub Container Registry images addressed by digest and protected
  GitHub Actions releases over host-verified SSH. Keep Vercel CLI delivery.
- Replace managed-platform assumptions with explicit VPS responsibilities for
  patching, secrets, firewalls, health recovery, log retention, and restore.

## Requirement traceability

NFR-SEC-01/03/06/09/10/11/12, NFR-AVL-01/02/04, NFR-BAK-01..05,
NFR-OBS-01/05, NFR-MNT-01/02, NFR-DEP-01..09, and NFR-PRV-02/03/04.
Existing performance targets, release priorities, FR-ACC-01..05, FR-RED-01..07,
and FR-ABS-01..03 remain unchanged.

## Acceptance criteria

- [x] AGENTS.md, README, architecture, ADR, and NFRs agree on the VPS target.
- [x] No active documentation or configuration requires the former cloud's
  resources, regions, credentials, provisioning framework, or deployment commands.
- [x] The architecture diagrams show Vercel, Hostinger ingress/private Redis,
  external Supabase PostgreSQL, and independent backups without changing application relationships.
- [x] CI/CD instructions specify tests, immutable images, release approval,
  SSH host verification, migrations, health gates, and rollback limitations.
- [x] Infrastructure assumptions cover private networks, secrets, HTTPS,
  bounded recovery, logs, daily backups, 24-hour RPO, and 4-hour RTO.
- [x] Environment examples match the existing Spring configuration and contain
  safe placeholders; development setup and application behavior are preserved.
- [x] Documentation links, diagram structure, configuration checks, and review
  pass. Unimplemented runtime capabilities are clearly identified.

## Verification boundary

Validate documentation and examples, inspect the diagram, and run the existing
profile configuration test. No production resources, DNS, secrets, GitHub
settings, or live deployments are changed. Full product builds are unnecessary
for this documentation-only concern with configuration comments/examples.

## Supabase revision acceptance criteria

- [x] Production sources consistently place PostgreSQL in Supabase, Redis on the VPS, and Next.js on Vercel.
- [x] Connections use the existing datasource environment names, direct or session mode, verified TLS, a mounted CA, and source-IP restrictions.
- [x] Backup/restore, free-plan limitations, latency, and VPS-versus-database failure paths are documented without changing NFR priorities.
- [x] Spring authentication, ownership policy, Flyway migrations, and development Compose remain unchanged; no browser database access is introduced.
- [x] Updated diagrams, examples, checks, independent review, and archived task records match the final topology.
