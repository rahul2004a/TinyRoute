# Main CI repair verification

## Root cause

- [Main backend run](https://github.com/rahul2004a/TinyRoute/actions/runs/37170246447)
  passed precheck, Maven verification, and Docker scanning but failed the CodeQL
  gate on three `java/user-controlled-bypass` results at the Google callback's
  early-return input check. The aggregate gate correctly failed.
- [Passing PR backend run](https://github.com/rahul2004a/TinyRoute/actions/runs/37169601910)
  generated a PR diff-range extension and ran queries with
  `--extension-packs=codeql-action/pr-diff-range`. The unchanged OAuth callback
  did not appear in its report. Main used unrestricted analysis.
- Frontend workflow had only a pull_request event, so merging never started it.

## Local evidence

- CI helper regression initially failed for both `diff-informed` and
  `overlay,diff-informed` reports. After the guard, all 13 helper tests pass.
- Both workflows pass actionlint and `git diff --check`.
- OAuth unit tests initially failed on 11 of 13 cases before service-level
  validation. The transaction-proxy regression failed with
  `CannotCreateTransactionException` before moving validation outside the
  database transaction. It passes after that boundary repair.
- Final `./mvnw -B -ntp spotless:check clean verify`: BUILD SUCCESS;
  63 unit and 61 integration/security tests pass without failures or skips, including
  all existing Google OIDC integration tests. JaCoCo coverage is 89.26%; the
  explicit coverage guard and blocking SpotBugs/Find Security Bugs checks pass.
- Formatting followed by immediately compiling once produced inconsistent
  local class files; fresh clean runs after formatting settled passed. No
  product configuration was changed to bypass this local artifact issue.
- Independent read-only review identified the transaction-before-validation
  regression. The Spring-proxy RED/GREEN test verifies its fix. No other
  material scoped findings; unchanged provider-network error mapping and
  unrelated auth policy remain outside this repair.

## Hosted evidence

Implementation commit `d8cc0f9`, [repair PR #14](https://github.com/rahul2004a/TinyRoute/pull/14):

- [Frontend validation](https://github.com/rahul2004a/TinyRoute/actions/runs/37175247656):
  all jobs and Frontend CI pass, including frozen install, lint, format, types,
  unit/component tests, and production build.
- [Backend validation](https://github.com/rahul2004a/TinyRoute/actions/runs/37175247620):
  all jobs and Backend CI pass, including full CodeQL, secret scanning,
  Maven verification, dependency scanning, and Docker validation.
- Downloaded hosted backend reports independently confirm 63 unit tests,
  61 integration/security tests, zero failures/errors/skips, and 89.26% coverage.
- Downloaded CodeQL SARIF contains zero results and no incrementalMode property.
  Hosted logs explicitly show `CODEQL_ACTION_DIFF_INFORMED_QUERIES=false`,
  no precomputed diff ranges, and no blocking findings. This proves the repair
  was checked with the full scan that previously failed on main.
- Replaying original merge before/after revisions through change detection
  returns `frontend=true` and `backend=true`.

The final documentation/archive commit receives another complete PR validation
before marking the PR ready. Main is intentionally unchanged until user merge;
the new frontend main push trigger is syntax-checked and uses the tested push
comparison, but cannot receive a main event before that merge.
