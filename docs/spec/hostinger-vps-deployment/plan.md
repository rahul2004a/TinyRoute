# Hostinger VPS Deployment Documentation Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task. Review the completed documentation using superpowers:requesting-code-review.

**Goal:** Align all deployment guidance and configuration examples with Hostinger VPS compute/private Redis and Supabase PostgreSQL.

**Architecture:** Preserve the Spring Boot monolith, its PostgreSQL/Redis boundaries,
and the Vercel frontend. Replace the production platform and operational assumptions
with VPS compute/private Redis, Supabase PostgreSQL, Caddy ingress, and GitHub Actions delivery.

**Tech Stack:** Existing Java/Spring Boot and Next.js stack; planned Docker,
Caddy, Supabase managed PostgreSQL, private GitHub Container Registry, SSH, and GitHub Actions.

**Spec:** `docs/spec/hostinger-vps-deployment/spec.md`

## Global Constraints

- No application behavior, functional scope, frontend design, or stack changes.
- Root `compose.yml` stays development-only; future VPS infrastructure is independent.
- No live provisioning, workflows, deployment, or repository-setting changes.
- Preserve requirement IDs, priorities, performance targets, and V1 boundaries.

## Review Focus

- Host-only cookies and exact production origins across Vercel and VPS hosts.
- Docker health checks versus actual recovery, including shared datastore outages.
- Data durability, off-VPS backup retention, Redis security-state loss, and restore.
- Release credentials, trusted proxy addresses, migrations, and rollback safety.
- Clear separation of documentation updates from unimplemented delivery automation.

### Task 1: Align deployment sources of truth

**Files:** `AGENTS.md`, `Readme.md`, `docs/architecture/architecture.md`,
`docs/decisions/0003-production-infrastructure-and-delivery.md`,
`docs/requirements/Non-Functional.md`.

- [x] Replace obsolete hosting and infrastructure contracts and retain application policy.
- [x] Add the VPS topology and link the deployment guide.
- [x] Verify no old platform requirements remain and NFR IDs/priorities are unchanged.

### Task 2: Document operations and delivery configuration

**Files:** Create `docs/deployment/hostinger-vps.md`, `infra/vps/README.md`,
`infra/vps/.env.example`; annotate `.env.example` and `application-prod.yml`.

- [x] Document prerequisites, networking, runtime secrets, backup/restore, and logs.
- [x] Define future CI/CD jobs, credentials, migrations, health gates, and rollback.
- [x] Provide placeholder configuration matching current Spring environment names.
- [x] Verify examples against the profile configuration test and local Compose checks.

### Task 3: Update diagrams and complete review

**Files:** `docs/architecture/HLD.excalidraw`; specification and active task files.

- [x] Update deployment labels and ingress routing, preserving application modules.
- [x] Validate diagram structure and visually inspect the resulting topology.
- [x] Review the complete diff, check links and references, and resolve findings.
- [x] Record verification, complete acceptance checkboxes, and archive task files.

### Task 4: Revise the production database target to Supabase

**Files:** The deployment sources from Tasks 1-3, including their diagrams and
configuration examples; specification and verification record.

- [x] Replace production PostgreSQL-on-VPS assumptions with Supabase managed PostgreSQL; preserve local Compose and private VPS Redis.
- [x] Document JDBC direct/session connections, verified TLS, source restrictions, provider plan/backup requirements, and Supabase Data API isolation.
- [x] Update both high-level diagrams and the production environment example, leaving Spring behavior and class diagrams unchanged.
- [x] Check relevant links, NFR priorities, environment names, and diagram geometry; run existing profile/Compose checks and independent review.
- [x] Record results, complete revised acceptance criteria, and archive the extended plan/checklist.

### Task 5: Specify automated daily Supabase-to-S3 backups

**Files:** Deployment sources from Tasks 1-4; create
`docs/deployment/supabase-s3-backup.md`; update HLD/architecture diagram and
specification/verification records.

- [x] Select private Amazon S3 as the independent backup destination without changing application hosting.
- [x] Specify a separate daily VPS scheduler, consistent application/Flyway export, verified TLS, client-side encryption, and isolated backup credentials.
- [x] Define S3 access, checksum/completion gates, retention, failure/freshness handling, and restore acceptance.
- [x] Update sources of truth and diagrams; verify links, scope, NFR assignments, and review.
- [x] Record evidence and archive completed documentation tasks. Runtime script implementation and installation remain separately identified work.
