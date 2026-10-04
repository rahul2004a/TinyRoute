# CI checks and required gates

The [frontend workflow](../../.github/workflows/frontend-ci.yml) validates pull
requests targeting `main`. The [backend workflow](../../.github/workflows/backend-ci.yml)
validates those pull requests and pushes to `main`. CI never deploys, pushes an
image, logs into a registry, or reads production secrets.

## Required checks

After these workflows have run on GitHub, configure the `main` branch ruleset
or protection rule to require **Frontend CI** and **Backend CI**, with GitHub
Actions as their source. Require normal PR review and any other existing checks.
This feature supplies the checks; it does not change repository admin settings.
Without that rule, failing workflow checks alone do not enforce merge blocking.
Avoid requiring conditional internal jobs, which intentionally skip for
unaffected applications. If enabling a merge queue later, first add
`merge_group` support and test its diff semantics; these workflows currently
support ordinary pull requests and backend main pushes.

Both aggregate gates run with `always()`. Detection must succeed. If an
application changed, all its required jobs must succeed; failed, cancelled,
missing, or unexpectedly skipped jobs fail its gate. An unaffected application
passes after the pre-check. The backend pre-check scans secrets even for
frontend-only or documentation-only changes. Superseded runs are cancelled;
their cancelled checks do not count as successful validation of a current PR.

## Change detection

Full checkout history supports PR merge-base comparisons and main before/after
push comparisons. `--no-renames` checks both sides of moves; deletions count.
A first push with no prior SHA runs both applications conservatively. Missing
or invalid revisions fail detection. There are no workflow event path filters.

`frontend/**` affects frontend; `backend/**` affects backend. `.github/**`,
requirements, architecture, decisions, root `AGENTS.md`, `DESIGN.md`,
`compose.yml`, and root `.env.example` affect both. Ordinary README, deployment
guide, and feature-spec changes skip the expensive application checks.

## Frontend verification

Use Node.js 24 from `.nvmrc`, Corepack and the exact packageManager pnpm version,
a lockfile-keyed pnpm download-store cache, and `pnpm install --frozen-lockfile`.
Then run existing scripts: `lint`, `format:check`, `typecheck`, `test --run`,
and `build`. ESLint permits zero warnings. Type checking generates Next.js
route types before `tsc --noEmit`. The production build uses webpack and
`NEXT_PUBLIC_API_BASE_URL=https://api.tinyroute.test`. Font fetching needs
outbound Internet access. Playwright remains a separately runnable browser
suite; it is outside these supplied CI diagrams.

## Backend verification

Java 21 and the checksum-pinned Maven 3.9.16 wrapper reproduce the backend
build. Maven dependencies are cached using the POM and wrapper configuration.
Run Spotless on Java changed since `origin/main`; use `spotless:apply` locally
to format those files. The ratchet avoids unrelated changes to legacy sources.

`./backend/mvnw -f backend/pom.xml -B -ntp clean verify` runs:

1. Surefire unit `*Test` tests, without running PostgreSQL or Redis.
2. Spring Boot JAR packaging.
3. Failsafe `*IT` tests, including HTTP authorization/security and persistence
   tests. The imported test configuration starts one disposable PostgreSQL
   17.2-alpine and Redis 7.4.2-alpine per JVM on random ports. Flyway applies
   all migrations; Hibernate validates the schema. Ryuk removes the containers
   at test JVM exit. Docker must be available; tests are never skipped for a
   missing Docker daemon. Root development Compose remains for running the app.
4. Combined JaCoCo report and BUNDLE LINE covered ratio check >= 0.70 on all
   production classes. Critical-path tests remain required independently of
   aggregate coverage. CI requires nonempty execution data and a valid report
   counter, closing JaCoCo's successful skip on missing data. Surefire and
   Failsafe both fail if no tests are found.
5. SpotBugs, including Find Security Bugs, with effort `Max` and threshold
   `High` (priority 1). These findings fail verify. A separate exploratory
   all-priority scan found pre-existing lower-priority diagnostics; this CI
   feature does not suppress them or refactor application behavior to adopt
   a broader static-analysis policy.

Trivy rootfs then scans the packaged JAR's library dependencies (the filesystem
scanner disables binary JAR analysis). Reports must contain Java package
inventory; image reports must also contain OS packages. HIGH and CRITICAL
vulnerabilities block, including unfixed findings. The JAR artifact uploads
only after verification and dependency scanning succeed. Surefire, Failsafe,
JaCoCo, SpotBugs and Trivy reports upload even on failure when produced.

After verification, two jobs run in parallel:

- CodeQL initializes Java security-extended queries, observes a clean Maven
  compile, analyzes and uploads SARIF. An explicit local SARIF gate blocks
  security-severity >= 7.0 and error-level results. Missing, malformed or
  unrecognized reports/results fail closed. SARIF upload alone is not treated
  as proof that findings passed. GitHub CodeQL advanced setup must be enabled
  for this public repository; disable a conflicting default setup if present.
- Docker downloads the current run's verified JAR, copies it to
  `backend/target/application.jar`, and builds the existing Dockerfile's `ci`
  target. It shares the digest-pinned non-root runtime and health check with
  the default source-building target. Trivy scans OS and Java dependencies;
  HIGH/CRITICAL findings block. No image is published.

## Security and maintenance

Actions are pinned to full release commits. Gitleaks 8.30.1 and Trivy 0.75.0
come from official release archives checked against release SHA-256 checksums.
`install-tool.sh` also supports actionlint 1.7.12 for local workflow validation.
Gitleaks scans all checked-out Git history and redacts the JSON report.
Download/scan failures fail the job. Reports expire after seven days.

Use read-only contents permissions and `persist-credentials: false`. Only
CodeQL receives `security-events: write`; GitHub restricts fork PR tokens.
There is no `pull_request_target`, deployment environment, or production secret
reference. Review action, scanner, image and dependency updates in normal PRs.
Fix vulnerabilities or document narrowly scoped, reviewed exceptions before
adding any suppression; this feature has no blanket scanner ignore rules.

## Local CI helper checks

```sh
python3 -m unittest discover -s .github/scripts -p 'test_*.py'
bash .github/scripts/install-tool.sh actionlint /tmp/tinyroute-ci-tools
/tmp/tinyroute-ci-tools/actionlint .github/workflows/frontend-ci.yml .github/workflows/backend-ci.yml
```

The helper tests exercise actual temporary Git repositories and synthetic
SARIF, alongside aggregate job-result combinations. The implementation and
verification record live under [the feature spec](../spec/frontend-backend-ci/spec.md).
