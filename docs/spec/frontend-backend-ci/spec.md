# Frontend and backend CI

## Intent and scope

Implement the supplied frontend/backend CI diagrams as credential-free GitHub
Actions validation. GitHub branch rules can require two stable checks:
`Frontend CI` and `Backend CI`. This feature validates changes; production
publishing, deployment, merging, and repository ruleset mutation remain outside
this feature.

Requirements: NFR-TST-01 (critical-path regression tests), NFR-TST-02 (70% line
coverage), NFR-SEC-03 (no committed secrets), NFR-SEC-07 (dependency scanning,
explicitly requested here although otherwise V1), NFR-DEP-02 (Flyway), and
NFR-DEP-05 (backend Docker packaging). Preserve the locked application stack.

## Workflow contract

- Frontend: pull requests targeting `main` (opened, synchronize, reopened).
- Backend: the same pull requests plus pushes to `main`.
- Workflows start without event-level path filters so required checks always
  report. Checkout full history; compare PR base/head using merge-base, and
  pushes using before/after. Deleted and renamed paths count. Missing/zero
  push base conservatively validates both applications. Detection failures fail.
- Application directories trigger their own validation. `.github/**`, shared
  requirements/architecture/decisions, `AGENTS.md`, `DESIGN.md`, `compose.yml`,
  root `.env.example`, and `.gitattributes` trigger both. Ordinary documentation skips costly jobs.
- Read-only token permissions by default; no production/environment secrets,
  no `pull_request_target`, no registry login, and no persistent credentials.
  Pin actions to full release commit SHAs. Superseded runs may be cancelled.
- Tool archives use reviewed SHA-256 digests pinned per tool/platform in the
  repository; downloading a matching remote checksum file cannot authorize
  replaced release assets (NFR-SEC-07).
- Scan the entire current Git history for secrets on every backend-workflow
  event, even when backend validation is unnecessary. Redact findings.
- Stable aggregate gates run with `always()`. Successful pre-check plus an
  unaffected application passes; affected applications require every named job
  to succeed. Failure, cancellation, and unexpected skips fail closed.

## Frontend

Node.js 24 from `frontend/.nvmrc`; Corepack uses `pnpm@12.4.2` from
`frontend/package.json`. Cache pnpm's download store using the lockfile hash,
then frozen install. Run ESLint with zero warnings, Prettier (ADR 0003),
`next typegen` and `tsc --noEmit`, Vitest in one-shot mode, and
`next build --webpack`, with `NEXT_PUBLIC_API_BASE_URL=https://api.tinyroute.test`.
Use existing package scripts. Playwright is outside the supplied CI diagrams.

## Backend

Java 21, a pinned Maven wrapper and dependency cache. Spotless checks changed
Java files relative to `origin/main`; formatting legacy untouched files is not
required. Surefire runs unit `*Test`; rename Spring-context tests to `*IT` and
run them in Failsafe's integration-test/verify phases. A composed imported test
configuration starts singleton disposable PostgreSQL 17.2 and Redis 7.4.2
Testcontainers on random ports, overriding only test connection properties.
Flyway migrates the container database and Hibernate validates it. Docker is
required; unavailable infrastructure fails tests rather than skipping them.

`./mvnw -B -ntp clean verify` packages the Spring Boot JAR, combines unit and
integration coverage, reports JaCoCo, and enforces bundle LINE covered ratio
at least 0.70 across production application code. SpotBugs with the Find
Security Bugs detector runs in verify and fails on high-priority findings
(SpotBugs threshold `High`, effort `Max`). Lower-priority findings remain a
separate remediation concern; adding CI does not refactor application behavior.
CI also requires nonempty JaCoCo
execution data and an application LINE coverage report. Trivy rootfs scans the
verified JAR's runtime dependencies; HIGH/CRITICAL findings block (including
unfixed findings). Scan reports must contain Java packages, plus OS packages for
images; empty scans fail. Reports upload even after failure; the JAR uploads only
after successful verification/scanning.

After backend verification, parallel jobs run CodeQL Java security-extended
analysis and Docker validation. CodeQL uses a manual clean compile, retains
SARIF as an artifact, and explicitly gates SARIF security-severity >= 7.0 or error
findings. Repository default CodeQL setup remains enabled and owns Security-tab
publication; this workflow sets `upload: never` to avoid conflicting with it.
SARIF rule references resolve both driver and extension-component rules.
The CodeQL job alone has `security-events: read` for Action feature discovery,
and explicitly retains PR file coverage metadata. JaCoCo owns test coverage.
Missing/invalid SARIF fails. Docker builds a target of the existing
Dockerfile that copies the downloaded verified JAR, preserves the existing
runtime contract, and Trivy scans HIGH/CRITICAL OS/runtime dependency findings.
No image is pushed. Keep the existing source-building Docker target as default.

Hosted CodeQL flagged the existing auth log filter because its closed allowlist
returned original request strings. Use canonical constant method/path strings
for identical bounded log output, removing untrusted-data flow into logs
(NFR-OBS-01); preserve authentication behavior.

## Acceptance criteria

- [x] Both workflows implement triggers, conservative change detection, and stable gates.
- [x] Frontend checks use Node 24, packageManager pnpm, frozen install, and safe build env.
- [x] Secrets, failed/cancelled jobs, and unexpectedly skipped required jobs block the backend gate.
- [x] Maven separates unit/integration tests, provisions isolated datastores, checks format/static analysis, and enforces 70% coverage.
- [x] CodeQL findings and HIGH/CRITICAL Trivy findings produce failing job results.
- [x] Docker validation consumes the successfully verified JAR artifact.
- [x] Helper tests cover deletion/rename/diff semantics, missing report output, and gate failure/skip conditions; workflow syntax validation passes.
- [x] Relevant local checks pass, hosted-only checks are exercised in the feature PR, and required-check setup is documented.
- [x] Documentation and feature task archive are current before readiness.

## References

- [GitHub Maven CI](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven)
- [CodeQL analyze inputs](https://github.com/github/codeql-action/blob/main/analyze/action.yml)
- [Spring dynamic test properties](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/dynamic-property-sources.html)
- [Testcontainers singleton lifecycle](https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/)
