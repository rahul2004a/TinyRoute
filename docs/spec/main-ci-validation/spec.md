# Main CI validation repair

## Evidence and scope

The merged CI feature passed PR backend run `37169601910`, but main push run
`37170246447` failed with three `java/user-controlled-bypass` results in
`AuthController.googleCallback`. PR logs contain a diff-range extension pack;
the main report covers the full backend. Frontend has no push trigger.

Repair this discrepancy under NFR-MNT-01 and NFR-TST-01, preserving FR-ACC-01,
NFR-SEC-06, the existing stack, security thresholds, and deployment boundaries.

## Required behavior

- Both workflows validate pull requests targeting main and pushes to main.
  Frontend push detection uses before/after revisions and concurrency falls
  back to the branch ref, matching the backend workflow.
- CodeQL checks the full backend on both events. Disable diff-informed queries
  explicitly in the pinned Action; reject reports marked `diff-informed` at
  the SARIF gate. Preserve the existing severity/error policy.
- `AuthController` unconditionally calls
  `AuthService.validateGoogleCallback(stateCookie, state, code)`, then
  `finishGoogleAuthorization(state, code)` inside its failure-redirect handler.
  The service rejects missing/blank/mismatched state and missing/blank codes
  before calling Google or issuing a session. Invalid callbacks consume the
  transaction associated with the cookie, as before. Provider validation,
  single-use transactions, fixed redirects, and cookie clearing stay intact.
  Validation and invalid-callback cleanup happen before the existing database
  transaction so rejecting invalid input remains possible during DB outages.
- Use a normal fix branch and pull request; do not merge or change main directly.

## Acceptance criteria

- [x] Frontend supports main pushes with valid detection and concurrency.
- [x] Full CodeQL scope is enforced on PRs and main.
- [x] Service-level invalid callback regressions and existing OIDC integration tests pass.
- [x] CI helper suite, workflow lint, backend verification, and hosted PR checks pass.
- [x] Documentation records root causes, repair, and verification evidence.

## Action reference

The pinned Action defines `CODEQL_ACTION_DIFF_INFORMED_QUERIES` in its
[feature flags](https://github.com/github/codeql-action/blob/2892aa5e19bbd11bc0cff5427e3b750a04d9e3c2/src/feature-flags.ts#L255)
and applies PR ranges through its
[analysis implementation](https://github.com/github/codeql-action/blob/2892aa5e19bbd11bc0cff5427e3b750a04d9e3c2/src/analyze.ts#L208).
