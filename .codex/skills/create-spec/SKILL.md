---
name: create-spec
description: Create a spec file and feature branch for a new TinyRoute feature from a short description. Writes only the spec and branch, not application code. Explicit-only; invoke $spec followed by a feature description.
---

# Create spec ($spec)

You are a senior developer spinning up a new TinyRoute feature. Always follow
the rules in [AGENTS.md](../../../AGENTS.md). This skill only writes a spec
file and creates a branch — it must never write application code.

User input: everything after `$spec`.

## Plan Mode

Perform read-only prerequisite and collision checks, then return the proposed
title, slug, branch, spec outline, and implementation handoff. Do not create a
branch or file until Plan Mode has ended.

## Step 1 — Check the working tree is clean

Run `git status`. If there are uncommitted, unstaged, or untracked files,
stop immediately and tell the user to commit or stash before proceeding. Do
not continue until the working tree is clean.

## Step 2 — Parse the input

From the text after `$spec`, extract:

1. `feature_title` — human-readable title in Title Case.
   Example: "Custom Alias" or "Password Reset".
2. `feature_slug` — git- and file-safe slug: lowercase kebab-case, only
   `a-z`, `0-9`, and `-`, maximum 40 characters.
   Example: `custom-alias`, `password-reset`.
3. `branch_name` — format `feature/<feature_slug>`.
   Example: `feature/custom-alias`.

If any of these can't be inferred from the input, ask the user to clarify
before proceeding.

## Step 3 — Research and prevent duplicate specs

Read AGENTS.md, both requirements documents, the relevant architecture
sections, every existing file under `.codex/spec/`, and only the source
manifests, routes, public signatures, and types relevant to the request. Use
targeted search; do not read all backend or frontend source by default.

If an existing spec substantially covers the request, stop and ask whether to
update it. Never overwrite an existing spec.

## Step 4 — Resolve the slug and ref names

Check the exact local and remote branch refs with `git show-ref --verify` and
check `.codex/spec/<feature_slug>.md`. If either name is taken without being a
duplicate feature, append `-01`, `-02`, etc. to `feature_slug`, and derive both
`branch_name` and the spec filename from that same final slug.

## Step 5 — Fetch and create the feature branch

Run:

```
git fetch origin main
git switch --create <branch_name> --no-track origin/main
```

Do not run `git pull`: its merge/rebase behavior depends on user configuration.
If fetch fails, stop before creating the branch. If branch creation fails,
report the current branch and do not write the spec.

## Step 6 — Write the spec

Save a new file at `.codex/spec/<feature_slug>.md` with exactly this
structure:

```markdown
# Spec: <feature_title>

## Overview

One paragraph describing what this feature does and why it's being built.

## Requirement traceability

The matching `FR-*`/`NFR-*` ID(s) from Functional.md / Non-Functional.md,
each with its Priority (Must/Should/Could) and Release (MVP/V1/Future). If no
existing ID matches, mark it as "proposed" rather than inventing an ID that
looks canonical. If any part of this feature is V1/Future scope, flag it
explicitly as needing the user's sign-off before implementation.

## Depends on

Which existing features, endpoints, or data must already exist for this to
work. If none: state "No dependencies".

## Routes

Every new or changed route: `METHOD /path` — description — access level
(public / signed-in / owner-only) — auth, CSRF, and rate-limit notes (which
`RateLimitService` bucket, if any). If no new routes: state "No new routes".

## Layers touched

Per the LLD layering in architecture.md — list what's new or changed at each
layer that applies: Controller, Service, domain types, repository-or-store
interface, `Jpa*`-or-`Redis*` adapter. Include any new Redis key pattern and
its TTL if one is introduced. If a layer is untouched, omit it rather than
padding the list.

## Database changes

Any new tables, columns, indexes, or constraints, verified against the
`Data` section of architecture.md. If none: state "No database changes".

## Frontend changes

What changes in the Next.js app. Remember Next.js is an HTTP client only —
it must not own ownership or redirect policy. If none: state "No frontend
changes".

## Files to change

Every existing file that will be modified.

## Files to create

Every new file that will be created.

## New dependencies

Any new libraries. If none: state "No new dependencies".

## Rules for implementation

Specific constraints the implementer must follow. Include each rule only when
it applies to the feature:

- Ownership checks via a single `id AND owner_id` query (missing and
  non-owned both 404, per NFR-SEC-04).
- Fail closed: never guess or return a result when link/auth state is
  unknown (NFR-REL-02).
- Destinations must be well-formed `https` only.
- No PII collected or stored beyond what NFR-PRV-01/02/03 allow.

Add any feature-specific rules after these.

## Definition of done

A specific, testable checklist. Each item must be verifiable by running the
app or a test, and call out which items are on a critical path covered by
NFR-TST-01.
```

## Step 7 — Report to the user

Print a short summary in exactly this format:

```
Branch:    <branch_name>
Spec file: .codex/spec/<feature_slug>.md
Title:     <feature_title>
```

Then tell the user to review the spec and run `$trace`. Before implementation,
confirm a read-only `spec-guardian` review exists for the final spec and has no
blocking findings. Do not print the full spec in chat unless explicitly asked.
Remind the user that this skill only wrote the spec and created the branch —
no application code was touched.
