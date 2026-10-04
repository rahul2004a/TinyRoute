# CI verification record

## Local evidence (2026-10-04)

- `python3 -m unittest discover -s .github/scripts -p 'test_*.py'`: 10 tests
  pass. Helpers were tested failing before implementation. Actual temporary
  Git repositories exercise PR merge-base, main push, deleted and renamed
  paths, and invalid revisions. Aggregate gates exercise cancelled, failed,
  skipped and absent jobs. Report tests reject missing coverage and empty
  Trivy inventories; SARIF tests enforce threshold/error/malformed handling.
- `actionlint .github/workflows/frontend-ci.yml .github/workflows/backend-ci.yml`:
  pass using checksum-verified actionlint 1.7.12.
- `bash -n .github/scripts/install-tool.sh backend/mvnw`: pass.
- `pnpm install --frozen-lockfile`: pass with Node 24.21.0/pnpm 12.4.2;
  no dependency or lockfile changes. Sandbox DNS restrictions required an
  approved network rerun.
- Frontend `lint`, `format:check`, `typecheck`: pass.
- Frontend `test --run`: 13 files, 48 tests pass.
- Frontend production build with safe `NEXT_PUBLIC_API_BASE_URL` and telemetry
  disabled: pass, eight routes compiled. The sandboxed first build could not
  fetch Geist/Geist Mono from Google Fonts; approved network rerun passed.
- Initial backend wrapper `spotless:apply clean verify`: 48 Surefire unit tests
  and 61 Failsafe integration/security tests pass without skips. PostgreSQL and
  Redis were disposable Testcontainers on random ports; Flyway applied V1–V5.
  JaCoCo counted 1,340 covered/171 missed application lines (88.68%). The normal
  70% gate passes; a smoke check with threshold 1.00 fails as expected.
- Initial SpotBugs/Find Security Bugs `High`/`Max`: zero findings/errors.
- Gitleaks 8.30.1 `git . --log-opts=--all --redact`: 90 commits scanned,
  no leaks. Final branch scan is rerun before delivery.
- Initial Docker `--platform linux/amd64 --target ci` builds successfully with
  the verified JAR. Initial image scan inventories 136 Ubuntu packages and
  132 Java packages. Both Java and image vulnerability scans correctly fail
  on the old vulnerable dependencies described below.

## Findings and decisions

- An initial `trivy fs target` returned success with zero language files.
  Runtime JAR scans use `rootfs`; a tested inventory guard prevents empty scans
  from passing. Image inventories must include Java and OS packages.
- Independent review confirmed JaCoCo silently skips when execution data is
  missing. CI now requires nonempty execution data and a valid aggregate LINE
  report/counter before proceeding to scanning or JAR upload.
- Trivy detected seven HIGH/CRITICAL findings in existing Bouncy Castle 1.84
  and Jackson 3.1.5. Upgrade `bcprov-jdk18on` to 1.86 and the Jackson 3.1 BOM
  to 3.1.7, maintaining the existing application stack. These changes are
  required for the newly requested dependency gate (NFR-SEC-07).
  Sources: [Bouncy Castle 1.86 release](https://www.bouncycastle.org/resources/new-release-bouncy-castle-java-1-86/)
  and [Jackson 3.1.7 release](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.1.7).
- An exploratory all-priority SpotBugs scan reports 68 existing lower-priority
  diagnostics, including Spring-injected mutable objects, JPA representation
  warnings and security-cookie/header usage. Ruling: use explicitly documented
  `High` (priority 1) blocking policy for this feature, with `Max` analysis
  effort; broad production-code refactoring is outside CI implementation.
  No SpotBugs suppression/baseline files are added. Future lower-priority
  remediation requires individual review; the high-priority gate remains live.
- Reviewer verified all 15 Spring test renames preserve their test bodies.
  The scan/coverage enforcement findings and NFR identifier correction were
  addressed. Hosted execution remains required before readiness.

## Final local validation after dependency patches

- Wrapper `spotless:check clean verify`: pass with Jackson 3.1.7 and Bouncy
  Castle 1.86; 48 unit and 61 integration/security tests pass with zero skips.
  Final aggregate line coverage is 83.77%, above the enforced 70% minimum.
- Trivy dependency gate: pass; 133 Java packages identified, zero blocking
  HIGH/CRITICAL findings. Inventory guard passes.
- Both Linux amd64 Docker targets build successfully after the patches.
  Trivy image gate: pass; 136 Ubuntu and 132 Java packages identified, zero
  blocking findings. Java/OS inventory guard passes.
- Image `/app/application.jar` SHA-256 equals the verified local JAR:
  `9d56d640ab1219986696e99549408fd0eaf7daed0b8622a1269dbf95e203ba23`.
- Independent targeted re-review: no material gaps remain in the scan and
  coverage fixes. Hosted execution is the remaining validation boundary.

## Remaining validation

Exercise both hosted workflows and CodeQL in the feature PR. Complete the acceptance checklist and archive
active plan/todo only after these checks pass.

## Hosted compatibility

- [Frontend validation run](https://github.com/rahul2004a/TinyRoute/actions/runs/37167097227):
  complete, all jobs pass including `Frontend CI`.
- Repository CodeQL default setup is already configured. Keep it enabled;
  the new independently gated backend analysis uses `upload: never` and
  artifact SARIF delivery to coexist without repository setting changes.
