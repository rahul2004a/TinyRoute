# TinyRoute Codex workflow rules

These instructions apply when maintaining files under `.codex/`. Product,
architecture, security, and scope rules are inherited from
[`AGENTS.md`](../AGENTS.md); do not duplicate or override them here.

- Keep skills explicit-only when they have side effects (creating branches,
  writing specs/tests, seeding data, committing, pushing, or opening PRs).
- Keep review agents read-only and focused on their stated review lane.
- Every workflow that implements a feature must identify the matching
  `FR-*`/`NFR-*` requirement and respect the locked Spring Boot/Next.js
  layering in the root instructions.
- Do not add new tools, services, or dependencies to a workflow unless the
  task requires them.
- Preserve the existing safety gates: requirement tracing before feature work,
  review before shipping, and no direct commits to `main`.
