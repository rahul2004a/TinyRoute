---
name: seed-data
description: Seeds a demo user plus realistic short links with backdated click events, for demoing TinyRoute's dashboard and analytics locally. Only runs against a local/dev datastore. Explicit-only, invoke with $seed.
---

# Seed data ($seed)

Populates local Postgres with realistic demo data so the analytics UI (click
counts, 30-day trend, referrer/device/OS/browser/geo per FR-ANA-01..04) has
something worth looking at. Follow [AGENTS.md](../../../AGENTS.md).

## Safety

- Refuse to run against anything that isn't clearly local/dev: check the
  active DB connection (`.env`/`application-*.yml` datasource URL) for
  `localhost`/`127.0.0.1`/a container hostname. If it looks like a
  production/staging URL, stop and say so.
- Never touch a database named in a way that suggests production.

## Usage

- `$seed user` — create one demo user.
- `$seed links <count> <months>` — create `<count>` links owned by a chosen
  user, with click events backdated across `<months>` months.
- `$seed all` — both, using sensible defaults (1 user, 30 links, 3 months).

If arguments are missing or invalid, show this usage block instead of
guessing.

## Step 1 — Read the schema

Read the current JPA entities / migration files under `backend/` for the
actual `users` and `links` table shapes (and any click-event table) — don't
assume the schema in architecture.md's "Data" section hasn't drifted.

## Step 2 — Seed a user

Generate one realistic demo user:
- Display name and email (e.g. `demo.user+<n>@example.com`) — check
  uniqueness against `email_normalized` before inserting.
- Password hashed with the same hasher the app uses (`PasswordHasher` /
  Argon2id or bcrypt, matching NFR-SEC-02) — never plaintext, never a
  different algorithm "just for seeding".
- Print the plaintext password used, once, so it can be used to log in.

## Step 3 — Seed links + clicks

For the target user:
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
