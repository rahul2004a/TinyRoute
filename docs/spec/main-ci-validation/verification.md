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
  unit and integration/security tests pass without failures or skips, including
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

Pending repair PR validation. Main is intentionally unchanged until user merge.
