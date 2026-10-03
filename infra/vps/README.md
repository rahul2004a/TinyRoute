# Hostinger VPS infrastructure boundary

The accepted target is one Linux VPS running Caddy, the Spring Boot image,
and private Redis; PostgreSQL is managed by Supabase. Next.js remains on Vercel. See
[ADR 0003](../../docs/decisions/0003-production-infrastructure-and-delivery.md)
and [the deployment guide](../../docs/deployment/hostinger-vps.md).

This directory currently contains documentation and a safe backend environment
example only. It does **not** contain a deployable stack, release command, or
provisioning automation. Implementation must add and verify:

- An independent production Compose definition with pinned images, resource
  bounds, health checks, private ingress/Redis networks, and Redis/certificate
  volumes. Production includes no VPS PostgreSQL container or database volume.
- Caddy configuration for the API and redirect hosts, certificate persistence,
  HTTP-to-HTTPS routing, safe forwarded headers, and private operational routes.
- A restricted release command that accepts an approved backend image digest,
  checks Flyway/startup health, records releases, and supports manual rollback.
- A bounded health watchdog, independent encrypted Supabase database export job,
  log rotation, and documented VPS rebuild/database restore procedures.
- GitHub Actions checks, registry publishing, protected SSH backend deployment,
  and Vercel CLI frontend delivery, with pinned action versions.

Root `compose.yml` remains the development datastore stack and must not be
included, extended, or used as the production base. Production services must
not receive local defaults or use development volumes.

Copy the example's values into a protected file outside the checkout, such as
`/etc/tinyroute/backend.env`, with root ownership and mode `0600`, then replace
every placeholder. Supply that file to the backend container's runtime
environment; Spring Boot does not automatically load `.env` files.
Mount the Supabase CA read-only for the JDBC URL's `sslrootcert` and restrict
database access to the VPS's actual egress addresses and approved recovery
sources. Redis and Caddy need their own narrowly scoped configuration; never
pass database credentials or all backend secrets to every container. Configure
Supabase plan/backups and disable its unused Data API before production use.

Do not commit populated environment files, registry tokens, SSH keys, private
certificates, backups, or runtime state. No provider account API token is
required for the selected SSH delivery model.
