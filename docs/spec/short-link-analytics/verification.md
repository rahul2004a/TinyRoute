# Short-link analytics verification

Date: 2026-10-20. Local verification only; no production deployment or data used.

## Automated checks

- Backend: `./backend/mvnw -f backend/pom.xml -B -ntp clean verify` passed after
  the legacy redirect assertion was updated to await asynchronous recording.
  Final `./backend/mvnw -f backend/pom.xml -B -ntp verify` also passed after the
  country display-order fix: 250 unit tests, 116 integration tests, no skipped
  tests, Spotless/SpotBugs/Find Security Bugs clear, and 2402/2638 covered lines
  (91.1%; required minimum 70%). Disposable PostgreSQL 17.2 and Redis 7.4.2.
- Frontend: lint, format check, typecheck, and Vitest passed; 24 files and 125 tests.
  Node 24 and pinned pnpm 12.4.2/Recharts 3.10.1. Production webpack build passed.
- Browser: `TINYROUTE_E2E_API_BASE_URL=https://localhost:3000 pnpm --dir frontend
exec playwright test e2e/short-link-analytics.spec.ts e2e/link-creation.spec.ts
e2e/idle-session-recovery.spec.ts e2e/account-authentication.spec.ts`: 58 passed.
  The existing local frontend used same-origin API routing, so its configured
  origin was passed explicitly to the older auth/link fixtures. The new analytics
  fixture supports both routing modes and never reaches real accounts.
- Analytics browser coverage includes overview/detail and preserved applied
  ranges; all range-based totals/tables; custom validation; no activity; failure,
  Retry and Refresh; generic missing-resource state; session loss; drawer focus;
  light/dark at 320/768/1024/1440px; and no whole-page horizontal overflow.
- Screenshots under [ui/](ui/) were inspected at desktop and mobile widths. Existing
  historical link screenshots generated during regression runs were restored;
  only this feature's evidence is included.

## Correctness, security, and privacy

- Fixed-clock tests verify UTC last-30-day defaults, inclusive single-day/leap-day
  boundaries, and partial/malformed/reversed/future/oversized range rejection.
- Actual PostgreSQL tests verify concurrent atomic event/count writes, rollback,
  deleted-owner insertion gating, daily zero fill, owner scoping, bounded top-20
  dimensions with Other/Unknown/Direct fallback conservation, and country ties
  sorted by displayed English labels.
- HTTP tests cover anonymous 401s and identical missing/non-owned/deleted/malformed
  404s even with invalid dates; duplicate date parameters fail safely.
- Public GET redirects count on database and warm-cache paths. HEAD, health,
  disabled, unknown, and failed resolution paths do not enter capture.
- Coarse header normalization removes credentials, referrer paths/queries,
  unrecognized UA text, and overlong/IP-literal referrers. Tests exercise the
  synthetic local MaxMind reader and explicit Unknown fallback.
- Worker tests verify stale-event rejection, geographic failures becoming Unknown,
  swallowed storage failures, and nonblocking rejected task submission. The
  executor is fixed at four workers with a 512-slot queue and default rejection;
  no caller-runs behavior exists.
- Schema and responses expose no visitor address, visitor hash/identifier, raw UA,
  raw referrer URL, coordinates, or arbitrary metadata. No analytics query cache
  is persisted in browser storage. Errors do not log async arguments or raw input.
- Dedicated frontend tests cover identity-changing session recovery, cancellation
  while refresh is pending, account-scoped cache keys, and removal of the old
  query before publishing the new report. Late recovery checks current cached
  session identity before updating it. Account changes remount link pagination.

## Review

The Superpowers code-review workflow performed a read-only independent review.
The Important finding was session recovery across an account switch. The fix
synchronizes recovered identity, rejects replay under a different account key,
checks cancellation before publishing identity, and refuses to overwrite a newer
cached session. Deferred recovery/cancellation regressions pass. Follow-up review
found no remaining Important/Critical issue in those fixes. Country ordering and
pagination reset findings were also fixed. Existing Google OAuth navigation to
Your links was retained as an explicit scope limit; password sign-in preserves
safe analytics deep links. No OAuth routing policy was added.

## Performance evidence

The first protocol attempt ran while other verification was active. Its 200
creation samples had p95 38.49 ms, but the original strict driver rejected the
warm-up before the sustained phase. Retain [failed-warmup.json](failed-warmup.json)
for its anonymous capture samples (2997 recorded, no worker failures/rejections,
maximum observed recording delay 174 ms). This is a failed attempt, not evidence
of NFR-PER-01/03 acceptance. The final quiet-host protocol is recorded separately
when complete.

## Operational limits

Geography requires an operator-provisioned licensed City database for useful
real traffic; omission intentionally yields Unknown. The included database is
synthetic test data. There is no historic-event backfill and existing lifetime
counts are preserved. Capture is informational and can undercount process loss,
queue saturation, and database outages. Local benchmark results do not validate
Hostinger/Supabase network latency, a production City database, or deployment
availability. See [operations.md](operations.md).
