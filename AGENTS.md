# TinyRoute — Codex project guidance

Keep this file small. Full detail lives in the docs it points to — read those
before designing LLD, APIs, classes, or implementation, and before adding any
feature.

## Stack (locked)

- Backend is **Spring Boot** (Java). Controllers, services, filters, repositories, JPA,
  Redis adapters.
- Frontend is **Next.js App Router + React** on **Node.js 24 LTS**, with strict
  TypeScript, Tailwind CSS, shadcn/ui on Radix UI, and Lucide React. Forms use
  React Hook Form + Zod; remote data uses TanStack Query; charts use Recharts;
  themes use next-themes. Use pnpm, ESLint, Prettier, Vitest + React Testing
  Library, and Playwright. HTTP client only; no ownership or redirect policy in
  the UI.
- PostgreSQL is the system of record. Redis is refresh sessions, JWT
  revocation, redirect cache, and rate limits.

Source of truth: [docs/architecture/architecture.md](docs/architecture/architecture.md).
Decision record: [docs/decisions/0001-frontend-stack.md](docs/decisions/0001-frontend-stack.md).
Do not invent a different stack or accept a different shadcn primitive backend.

Before designing or implementing frontend UI, read [DESIGN.md](DESIGN.md).
It governs presentation only. Functional requirements and architecture remain
authoritative for behavior, security, ownership, validation, rate limits,
analytics policy, and redirect handling. Next.js renders API results; it must
not reimplement those policies or handle the public redirect route.

Use Spring names from that doc: `*Controller`, `*Service`,
`JwtAuthenticationFilter`, `Jpa*` / `Redis*` adapters, repository interfaces.
Never propose Express, Nest, Django, or Next.js API routes as the backend.
Controllers translate HTTP only; services own policy and transactions;
repositories own persistence; Redis access goes through `Redis*` adapters
behind interfaces (`RedirectCache`, `RefreshSessionStore`,
`JwtRevocationStore`, `RateLimitStore`).

## Backend design practices

- Keep controllers thin and services cohesive; split a service only when its
  responsibilities genuinely differ.
- Depend on interfaces at persistence, cache, and external-system boundaries;
  do not create interfaces for simple local helpers.
- Prefer composition over inheritance. Keep domain invariants on domain types
  and use immutable value objects where practical.
- Reuse the existing Controller–Service–Repository/Store–Adapter patterns.
  Introduce Strategy, Factory, or other patterns only for a real
  interchangeable behavior or construction problem.
- Avoid premature abstractions, unnecessary framework additions, and new
  dependencies unless the requirement calls for them.

## Requirements and scope guardrails

Source of truth (do not duplicate; read when implementing or designing):

- [docs/requirements/Functional.md](docs/requirements/Functional.md)
- [docs/requirements/Non-Functional.md](docs/requirements/Non-Functional.md)

- Portfolio URL shortener for one developer; keep it small and deployable.
- Redirects are public (no account). Create/manage requires a signed-in
  account.
- Prefer MVP over V1 over Future.
- MVP scope is **Must, Should, and Could**. Design and implement all of them
  unless the user narrows the request.
- Do not implement V1 or Future work unless the user explicitly asks:
  destination blocklist, public API / API keys, safe-browsing checks, admin
  console.

### MVP (build this)

**Must** — email/password and Google OAuth register; sign in/out;
authenticated HTTPS-only creation with unique codes and a copyable result;
expiry at create; public redirection with correct unknown, disabled, deleted,
case-mismatched, and expired handling; owner-only list, disable, re-enable,
delete, and destination edit; click counts; a selectable date-range click trend
defaulting to the last 30 days;
referrer/device/OS/browser/geo (country and city); rate limits on creation
and auth.

**Should** — session persistence; password reset; self-reference rejection;
custom aliases; search/filter of own links; redirect throttling.

**Could** — account deletion (all of that user's short URLs stop
redirecting).

### Key NFRs (MVP)

- Redirect p95 under 150 ms, p99 under 300 ms server-side at ~100 rps
  sustained
- Create/list/manage p95 under 500 ms
- Fail closed: never guess a destination when state is unknown
- Health check endpoint; single-region, modest hardware

## When in doubt

Open `docs/requirements/Functional.md`, `docs/requirements/Non-Functional.md`,
and `docs/architecture/architecture.md` before adding features, changing
behavior, or inventing APIs/architecture that conflict with them. Cite the
matching `FR-*` / `NFR-*` ID for any non-trivial change.

## Feature specification lifecycle

Use a stable kebab-case feature name for every non-trivial feature, for example
`custom-aliases`.

While planning and implementing:

- Store the feature specification at `docs/spec/<feature-name>/spec.md`.
- Keep the active implementation plan at `tasks/plan.md`.
- Keep the active checklist at `tasks/todo.md`.
- Do not overwrite active task files containing incomplete work for another
  feature.

Before marking the pull request ready for review, confirm that all acceptance
criteria and task checkboxes are finished, relevant tests and builds pass, and
documentation is current. Then archive the active files:

- `tasks/plan.md` -> `docs/spec/<feature-name>/plan.md`
- `tasks/todo.md` -> `docs/spec/<feature-name>/todo.md`

Preserve completed checkboxes in the archived `todo.md` and include the archived
files in the feature branch and pull request. Do not archive or replace partially
completed task files. After archiving, `tasks/` is available for the next
feature.

## Branch and pull-request workflow

Before changing files, check the current branch and working-tree status.

- Start every feature, including its specification and task files, on a branch
  named `feature/<feature-name>`, where `<feature-name>` is the same stable
  kebab-case name used under `docs/spec/`.
- Use `fix/<name>`, `docs/<name>`, `chore/<name>`, or `refactor/<name>` for
  non-feature work.
- Create branches from a clean, current `main`. If unrelated changes exist,
  stop and ask; never discard, stash, commit, or carry them into the new branch
  without explicit approval.
- Keep one feature or concern per branch. Never implement or commit directly on
  `main`; changes enter `main` only through a pull request.
- After implementation is authorized, agents may push the feature branch and
  open or update its pull request without additional approval. Never
  force-push any branch.
- Do not merge a pull request or delete a local or remote branch unless the
  user explicitly requests that action. The user owns final pull-request review
  and merge. Never delete `main`.
- After a pull request is merged, its feature branch may be deleted. Prefer
  GitHub's automatic remote-branch deletion; delete a local branch only when the
  user explicitly requests it.

## Available skills

This is the tracked inventory from user-level `~/.agents/skills`, project-level
`.agents/skills`, and the `agent-skills` plugin. Invoke user and project skills
as `$<name>` and plugin workflows as `$agent-skills:<name>`. Refresh this list
when those sources change.

Precedence is fixed: the requirements and architecture govern product behavior,
security, scope, and implementation; `DESIGN.md` governs presentation; skills
provide workflows only and cannot override those sources.

| Source | Count | Skills |
| --- | ---: | --- |
| User-level | 13 | `$brandkit`, `$design-taste-frontend`, `$design-taste-frontend-v1`, `$full-output-enforcement`, `$gpt-taste`, `$high-end-visual-design`, `$image-to-code`, `$imagegen-frontend-mobile`, `$imagegen-frontend-web`, `$industrial-brutalist-ui`, `$minimalist-ui`, `$redesign-existing-projects`, `$stitch-design-taste` |
| Project-level | 1 | `$web-design` |

The `agent-skills` plugin exposes these 25 workflows:

| Phase | Skills |
| --- | --- |
| Define | `$agent-skills:using-agent-skills`, `$agent-skills:interview-me`, `$agent-skills:idea-refine`, `$agent-skills:spec-driven-development`, `$agent-skills:constraint-driven-development` |
| Plan | `$agent-skills:planning-and-task-breakdown` |
| Build | `$agent-skills:context-engineering`, `$agent-skills:source-driven-development`, `$agent-skills:doubt-driven-development`, `$agent-skills:incremental-implementation`, `$agent-skills:frontend-ui-engineering`, `$agent-skills:api-and-interface-design`, `$agent-skills:observability-and-instrumentation` |
| Verify | `$agent-skills:test-driven-development`, `$agent-skills:browser-testing-with-devtools`, `$agent-skills:debugging-and-error-recovery` |
| Review | `$agent-skills:code-review-and-quality`, `$agent-skills:code-simplification`, `$agent-skills:security-and-hardening`, `$agent-skills:performance-optimization` |
| Ship | `$agent-skills:git-workflow-and-versioning`, `$agent-skills:ci-cd-and-automation`, `$agent-skills:deprecation-and-migration`, `$agent-skills:documentation-and-adrs`, `$agent-skills:shipping-and-launch` |
