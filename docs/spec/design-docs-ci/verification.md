# Documentation-only CI verification

Base: fetched `origin/main` at `f2959e0`, including the merged settings redesign.
Branch: `fix/design-docs-ci`. Requirements: NFR-MNT-01 and NFR-SEC-03.

## Tests and review

- Added documentation and mixed-path regression cases first. The documentation
  regression failed for nine existing classifications, confirming that design,
  agent guidance, requirements/architecture/decision files, application READMEs,
  and the pull-request template incorrectly selected application jobs.
- After excluding root `docs/**` and case-insensitive `.md` files before path
  classification, `python3 -m unittest discover -s .github/scripts -p 'test_*.py'`
  passed all 16 tests. The deliberate invalid-revision test prints Git's
  `missing-ref` error while passing its expected-exception assertion.
- Verified the settings PR's actual changed-file list (`3e35221..8f0f0cb`) now
  selects frontend validation only. Documentation-only inputs select neither;
  frontend/backend code mixed with documentation selects only the respective
  application. CI-script changes select both.
- `git diff --check` passed.
- Reviewed the workflows: their triggers, pre-check/secret scanning steps,
  conditional application jobs, and fail-closed aggregate gates are unchanged.
  No workflow YAML, application code, dependencies, runtime secrets, or deployment
  settings changed, so application builds and browser tests are not required.

## Hosted behavior

Both workflow records and required aggregate checks continue to appear for
documentation-only changes. The pre-checks and secret scan run; frontend
verification and backend verification/CodeQL/Docker jobs skip when unaffected.
This fix modifies `.github/scripts/`, so its own PR correctly selects both
applications. The new selection applies to subsequent PRs after merge.
