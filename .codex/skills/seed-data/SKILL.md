---
name: seed-data
description: Seeds a demo user plus realistic short links with backdated click events, for demoing TinyRoute's dashboard and analytics locally. Only runs against a local/dev datastore. Explicit-only, invoke with $seed.
---

# Seed data ($seed)

Populates local Postgres with realistic demo data so the analytics UI (click
counts, 30-day trend, referrer/device/OS/browser/geo per FR-ANA-01..04) has
something worth looking at. Follow [AGENTS.md](../../../AGENTS.md).

## Safety

- Proceed only when all of these are true: the active Spring profile is
  `local`, `dev`, or `test`; the resolved database host is `localhost`,
  `127.0.0.1`, or a Compose service explicitly defined in this repository;
  and the database name is `tinyroute_dev` or `tinyroute_test`.
- Print only the redacted resolved host and database name before insertion.
  Never print datasource credentials or treat an arbitrary container hostname
  as proof that the target is safe.
- If any check is absent, ambiguous, production-like, or staging-like, stop.

## Plan Mode

Perform read-only schema, configuration, and prerequisite checks and report the
exact seed command that would run. Do not connect to or modify a datastore.

## Usage

- `$seed user` — create one demo user.
- `$seed links <owner-email> <count> <months>` — create `<count>` links owned
  by that exact local demo user, with click events backdated across `<months>`
  months.
- `$seed all` — both, using sensible defaults (1 user, 30 links, 3 months).

If arguments are missing or invalid, show this usage block instead of
guessing.

## Step 1 — Read the schema

Read the current JPA entities / migration files under `backend/` for the
actual `users` and `links` table shapes (and any click-event table) — don't
assume the schema in architecture.md's "Data" section hasn't drifted.

Require the JPA entities, versioned migrations, and an existing executable
dev-only seed entrypoint. If any is missing, stop and name the prerequisite.
Do not create production code, temporary repository files, or ad hoc SQL as
part of `$seed`.

## Step 2 — Seed a user

Generate one realistic demo user:
- Email (e.g. `demo.user+<n>@example.com`) — check
  uniqueness against `email_normalized` before inserting.
- Password hashed with the same hasher the app uses (`PasswordHasher` /
  Argon2id or bcrypt, matching NFR-SEC-02) — never plaintext, never a
  different algorithm "just for seeding".
- Print the plaintext password used, once, so it can be used to log in.
- Do not create or store a display name, profile field, or other personal data
  prohibited by NFR-PRV-01.

## Step 3 — Seed links + clicks

For the exact user selected by `<owner-email>` (or the user created by
`$seed all`):
- Generate `<count>` links with varied, realistic https destinations and
  unique codes (never colliding with an existing code, per FR-CRE-04 /
  FR-MGT-05 — codes are never reused).
- Spread click events across the requested number of past months, varying
  referrer, device, OS, browser, and country/city (FR-ANA-04) so the
  30-day trend and breakdown charts have shape instead of a flat line.
- Insert through the same repository pattern the app uses (parameterized
  queries only, per NFR-SEC-05) — do not hand-write raw SQL string
  concatenation.
- Do it in one transaction (or one per link), rolling back everything on
  any failure rather than leaving partial data.

## Step 4 — Confirm

Print: the demo user's email + password, how many links and click events
were inserted, and 3–5 sample records.

## Rules

- This is local dev tooling, not a feature — do not write a spec or open a
  branch for it.
- Never seed data that would violate NFR-PRV-03 (no visitor IP/identifier in
  click analytics) — synthetic geo/device/browser strings only, no real
  IPs.
