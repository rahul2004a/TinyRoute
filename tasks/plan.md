# Main CI validation repair implementation plan

**Goal:** Make merge-time frontend and backend CI consistent with PR validation.
**Spec:** `docs/spec/main-ci-validation/spec.md`
**Approach:** Repair workflow event/scope configuration and move existing
Google callback input policy into AuthService. No dependencies or scanner
suppressions. Implement inline within the user's authorized debugging request.

## Task 1: Consistent workflow events and CodeQL scope

- [x] Add a SARIF regression that rejects `incrementalMode=diff-informed`,
  including comma-separated modes, while accepting unrestricted reports.
- [x] Observe failure using the existing Python helper suite.
- [x] Add the frontend main push trigger, push SHA fallbacks, and branch-ref
  concurrency fallback. Set `CODEQL_ACTION_DIFF_INFORMED_QUERIES=false` in
  the backend CodeQL job; reject restricted SARIF in `sarif_findings`.
- [x] Run the whole helper suite and actionlint; update the CI guide.

## Task 2: Unconditional OAuth validation at the service boundary

- [x] Add `AuthServiceGoogleOAuthTest` for null/blank/mismatched state,
  null/blank code, invalid transaction, and provider rejection. Observe RED.
- [x] Move state/code validation and invalid transaction disposal into
  `validateGoogleCallback(String stateCookie, String state, String code)`;
  remove the conditional authentication bypass in AuthController.
- [x] Review fix: keep this validation outside the existing database
  transaction. A Spring-proxy test with an unavailable transaction manager
  must reject invalid input and consume its cookie-bound state without opening
  a DB transaction. Observed RED before the boundary repair.
- [x] Format changed Java and run `./mvnw -B -ntp clean verify` with Docker.
- [ ] Review the diff, push the fix, open a PR, and confirm unrestricted
  hosted CodeQL and both aggregate gates pass.
- [ ] Record evidence and archive completed task files before review readiness.

## Review focus

Missing cookie/state/code must never issue a session. Invalid callbacks must
discard the cookie-bound transaction without consuming an unrelated state.
Datastore failures must remain unavailable errors. A valid callback must still
consume exactly once and return fixed success/failure destinations. Push runs
must compare main before/after SHAs; PR security scans must include unchanged code.

## Review decision

The initial three-argument transactional completion method introduced a new DB
dependency for invalid input. Keep unconditional controller delegation as two
service calls: nontransactional validation followed by existing transactional
completion. This preserves the original cleanup boundary with no new dependency
or service split. Unchanged provider-network error mapping and other auth policy
remain outside this repair.
