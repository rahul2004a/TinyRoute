# Paginated link history verification

Completed 2026-10-14 on `fix/guest-only-auth-pages`, with the user's explicit
approval to continue after committing prior work at `2a5ad5b`.
Requirements: FR-MGT-01/02, FR-CRE-01/05, NFR-SEC-04/06/08, NFR-MNT-01.

## Behavior and review

Added Spring-owned, owner-scoped cursor listing using the existing owner/time/id
index and a maximum 51-row query. The backend determines status, omits deleted
rows, validates bounded inputs, and returns immutable records without owner
identifiers. Reads do not use the creation quota. No schema migration, backend
framework, dependency, or public redirect change was needed.

Reused the existing frontend API, result, and form files. The immediate copyable
result remains available after creation; the saved list below it reloads from
PostgreSQL on browser refresh, uses 10 rows per page, and returns to the first
page after successful creation. Query keys separate account caches. Loading,
empty, error/retry, desktop table, mobile cards, and copy actions are covered.
Reviewed the final diff and whitespace checks. Existing frontend files were
extended; new backend model/DTO records follow the established layer structure.

## Verification

- Full backend `./mvnw -q verify spotless:check` passed, including the coverage
  gate, integration tests with disposable PostgreSQL/Redis, and static analysis.
  Results: 239 unit tests, 108 integration tests, 90.66% line coverage, and zero
  SpotBugs findings.
- The listing contract covers missing/invalid/revoked credentials, ignored
  foreign owner parameters, forged cursor positions, equal timestamps, page
  boundaries, deleted rows, backend-derived expiry, safe invalid inputs, empty
  results, and no creation-quota consumption.
- Frontend lint, formatting, production build, and TypeScript checks passed.
- All 99 frontend unit/component tests passed; after correcting a test-only
  TypeScript option, the affected 16 creation-form tests also passed again.
- Broad browser regression run: 62 passed, with one error-recovery fixture
  initially failing because development remounting consumed its one failure.
  Changed the fixture to remain unavailable until explicitly recovered. The
  final affected run passed 5/5 cases, including refresh persistence, both page
  directions, copying, new-creation refresh, both themes at 320/1440px, and retry.
- Inspected generated desktop dark and mobile light history screenshots. The
  list fits without horizontal overflow; existing shared header/footer and form
  presentation are retained. Generated test artifacts remain ignored.

An initial restricted Maven run could not attach Mockito's test agent; the
approved unsandboxed run succeeded. A targeted backend verification hit the
project-wide coverage gate, so full verification was used. The first full run
hit one existing 100ms Redis timeout while frontend/browser checks were running;
the full rerun without competing checks passed. No timeouts or quality gates
were weakened.

The real-backend live browser fixture was updated but not executed; backend
contract tests and browser fixtures provide the recorded coverage. Restart the
locally running Spring Boot application to load the new GET endpoint. Browser
refresh then reads existing saved records; no data migration is required.
No load-test latency claim is made for this change.

## Creation key regression

The user reported a duplicate React key after creating a URL. Independent debug
review confirmed that the immediate result and saved-history siblings shared
the created UUID as their key. Added a browser console assertion to the existing
creation/pagination journey and reproduced the failure before the fix. Prefixing
keys with `result:` and `history:` resolves the collision while preserving both
components' reset behavior (FR-CRE-01/05, FR-MGT-01).

Final checks: 16 creation-form tests and all four affected browser journeys
passed in light/dark themes at 320/1440px, with no duplicate-key errors. Lint,
formatting, TypeScript, production build, and whitespace checks passed.
