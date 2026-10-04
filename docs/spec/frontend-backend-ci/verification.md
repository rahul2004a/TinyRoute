# CI verification record

Verified on 2026-10-04; implementation revision `c7a410d`.

## Hosted evidence

- [Frontend validation](https://github.com/rahul2004a/TinyRoute/actions/runs/37167953588):
  all jobs pass, including `Frontend CI`. Frozen install, lint, formatting,
  types, 48 tests, and the production webpack build succeed on Node 24.
- [Backend validation](https://github.com/rahul2004a/TinyRoute/actions/runs/37167953619):
  all jobs pass, including `Backend CI`, secret scanning, Maven verification,
  CodeQL, and Docker validation.
- Downloaded backend reports confirm 49 unit tests and 61 integration/security
  tests, with zero failures, errors, or skips. Disposable PostgreSQL/Redis
  Testcontainers and Flyway migrations ran successfully. JaCoCo reports
  1,344 covered / 171 missed application lines: **88.71%**, exceeding 70%.
- Dependency scan inventories 133 Java packages and zero blocking
  HIGH/CRITICAL findings. The runtime image scan and Java/OS inventory guard
  pass; Docker consumes the verified JAR artifact.
- Downloaded CodeQL Java SARIF contains zero findings. The independent SARIF
  gate passes. Existing repository default CodeQL checks also pass.
- All PR checks pass on this implementation revision. Final archive changes
  receive another complete run before the PR is marked ready.

## Local evidence

- `python3 -m unittest discover -s .github/scripts -p 'test_*.py'`: 11 tests
  pass. Actual temporary Git repositories exercise merge-base, main pushes,
  deletion, rename, and invalid revisions. Gates reject cancelled, failed,
  skipped, and absent jobs. Reports reject missing coverage, empty inventories,
  and malformed SARIF; score/error policy and extension rule resolution are
  exercised. Regression cases were observed failing before their fixes.
- `actionlint .github/workflows/frontend-ci.yml .github/workflows/backend-ci.yml`
  and `bash -n .github/scripts/install-tool.sh backend/mvnw`: pass.
- Frontend frozen install, lint, formatting, types, 48 tests, and build: pass
  with Node 24.21.0 / pnpm 12.4.2 and the safe API URL. Registry and Google Fonts
  access required an approved network rerun after sandbox DNS restrictions.
- Latest wrapper `spotless:apply clean verify`: pass; 49 unit and 61 integration
  tests, zero skips, 88.71% line coverage, no blocking SpotBugs/Find Security Bugs
  findings. The normal coverage gate passes; threshold 1.00 fails as expected.
- Gitleaks 8.30.1 scanned all 95 commits before the archive commit, with no leaks.
- Both Linux amd64 Docker targets build successfully after dependency patches;
  Trivy dependency/image scans and inventory guards pass. The image scan
  inventories 136 Ubuntu and 132 Java packages. The staged image JAR checksum
  matched its verified local JAR in that build.
- Independent review and targeted re-review found no remaining material gaps.
  `git diff origin/main --check` passes; no generated build output is committed.

## Findings and decisions

- Initial `trivy fs target` scanned zero language files. Use `rootfs` for the
  runtime JAR and require a nonempty Java inventory; image reports require
  Java and OS inventories. Tested guards prevent empty success reports.
- JaCoCo can skip when execution data is missing. CI explicitly requires
  nonempty execution data and a valid aggregate LINE report before scanning
  or uploading the JAR.
- Trivy correctly blocked seven existing HIGH/CRITICAL findings in Bouncy
  Castle 1.84 and Jackson 3.1.5. Patch them to 1.86 and 3.1.7 respectively,
  preserving the locked application stack (NFR-SEC-07).
  Sources: [Bouncy Castle 1.86](https://www.bouncycastle.org/resources/new-release-bouncy-castle-java-1-86/)
  and [Jackson 3.1.7](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.1.7).
- An exploratory all-priority SpotBugs scan found 68 preexisting lower-priority
  diagnostics. This feature explicitly blocks priority 1 (`High`) at `Max`
  effort; no suppression or baseline files are added. Lower-priority findings
  require individual review outside CI implementation.
- Repository CodeQL default setup is configured. Keep it enabled; the new
  analysis uses `upload: never`, retains SARIF, and independently gates it.
  Default setup continues to own Security-tab publication.
- Hosted CodeQL stores rules in `tool.extensions`. A failing regression test
  exposed the original parser assumption. Resolve component/index references,
  validate IDs, and retain security-score and default error-level enforcement.
- The resolved report correctly blocked `java/log-injection` in the auth log
  filter. Its closed switches already bounded outputs but returned request
  strings. Canonical constant methods/routes preserve those outputs while
  removing the untrusted-data flow. A regression test covers line breaks and
  sensitive unknown paths; the subsequent hosted analysis has zero findings.

## Delivery

Completed acceptance criteria and archived plan/checklist accompany
[PR #13](https://github.com/rahul2004a/TinyRoute/pull/13). Configure `main` to
require `Frontend CI` and `Backend CI` using [the CI guide](../../development/ci.md).
Repository rules, deployment, merging, and branch deletion are unchanged.

## Security review follow-up

- Reviewed [the GitHub Advanced Security comment](https://github.com/rahul2004a/TinyRoute/pull/13#discussion_r4175685496):
  matching archive/checksum downloads from one mutable release do not prevent
  a replaced binary. Pin all six tool/platform archive digests in the repository
  using official GitHub release-asset metadata and remove remote checksum reads.
- A regression test supplied a replacement executable and matching remote
  checksum for all six combinations. It failed against the old installer, which
  installed/executed the replacement, and passes with pinned digests, rejecting
  it before installation/execution. The helper suite now has 12 passing tests.
- Give only CodeQL `security-events: read` to resolve feature API access and CLI
  fallback annotations. Retain PR file metadata explicitly with
  `CODEQL_ACTION_FILE_COVERAGE_ON_PRS=true`; the JaCoCo gate remains unchanged.
  Verified against the pinned Action's [feature API implementation](https://github.com/github/codeql-action/blob/2892aa5e19bbd11bc0cff5427e3b750a04d9e3c2/src/feature-flags.ts)
  and [PR metadata option](https://github.com/github/codeql-action/blob/2892aa5e19bbd11bc0cff5427e3b750a04d9e3c2/src/init.ts).
- Actionlint, installer shell syntax, and whitespace checks pass. The review-fix
  commit receives hosted validation and annotation inspection before readiness;
  those final results are recorded in PR #13.
