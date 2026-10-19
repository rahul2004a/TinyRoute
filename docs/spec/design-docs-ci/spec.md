# Documentation-only CI selection

## Problem and requirements

Documentation edits currently trigger expensive application validation when
they touch `DESIGN.md`, `AGENTS.md`, requirements/architecture/decision files,
or Markdown under `frontend/` or `backend/`. Documentation is not application
code or a build input. Scope CI to affected applications while preserving
automated checks and fail-closed gates (NFR-MNT-01), including secret scanning
(NFR-SEC-03).

## Behavior

- Root `docs/**` and Markdown documentation anywhere in the repository do not
  select either application's expensive jobs.
- Remaining `frontend/**` changes select frontend validation only. This includes
  source, tests, dependencies, assets, and configuration.
- Remaining `backend/**` changes select backend validation only.
- Changes affecting both applications select both.
- Shared CI code/configuration under `.github/**`, `compose.yml`, root
  `.env.example`, and `.gitattributes` continue to select both.
- Existing frontend load-harness scripts keep their frontend test coverage.
- Required aggregate gates, pre-checks, secret scanning, missing-revision
  failures, and conservative first-push validation remain unchanged. Workflows
  still report checks; only unnecessary application jobs skip.

## Acceptance

- [x] Documentation-only inputs select neither application, including Markdown
      within application folders and `.github/`.
- [x] Frontend-only and backend-only code/config inputs select their application.
- [x] Mixed documentation/code inputs preserve the affected application(s).
- [x] Shared executable/configuration changes continue to select both.
- [x] CI-helper regressions pass; operational CI documentation is current.
