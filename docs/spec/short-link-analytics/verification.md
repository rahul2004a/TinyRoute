# Short-link analytics verification

Date: 2026-10-20. Local verification only; no production deployment or data used.

## Automated checks

- Backend: `./backend/mvnw -f backend/pom.xml -B -ntp clean verify` passed after
  the legacy redirect assertion was updated to await asynchronous recording.
  Final `./backend/mvnw -f backend/pom.xml -B -ntp verify` also passed after the
  map API extension: 250 unit tests, 117 integration tests, no skipped
  tests, Spotless/SpotBugs/Find Security Bugs clear, and 2410/2645 covered lines
  (91.1%; required minimum 70%). Disposable PostgreSQL 17.2 and Redis 7.4.2.
- Frontend: lint, format check, typecheck, and Vitest passed; 26 files and 130 tests.
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
- After the user's date-format feedback, chart ticks use `DD/MM`; range labels,
  tooltips, and daily table dates use `DD/MM/YYYY`. The report's two unit tests,
  TypeScript check, formatting check, and all ten analytics browser checks passed
  again. Updated light/dark screenshots reflect this preference.

## Country map and India boundary view

- Approved map extension implemented in individual-link reports only, with a
  complete ISO-coded country list and Unknown from the same owner/range snapshot.
  The existing aggregate query is reused before top-20 folding. Seven contract
  tests pass, including 25 countries, Unknown conservation, date/link isolation,
  overview omission, and positive activity belonging to two different owners.
- The map has bounded 1–8× zoom, drag/keyboard pan, reset, country/count feedback,
  an intensity legend, and the complete Countries table. Range changes reset its
  view. Ctrl/Cmd browser zoom shortcuts are preserved.
- Final analytics browser suite: 12 passed, covering zoom/drag/reset, touch taps,
  scope and date changes, and both themes at 320/768/1024/1440px. The initial 320px
  aspect-ratio overflow was reproduced, corrected, and rerun successfully.
- User-requested India boundary view uses Natural Earth India POV 5.1.1. Source
  checksum, generation-only package pins, shared-edge simplification, and
  licensing are in the [asset notice](../../../frontend/src/features/analytics/data/NOTICE.md).
  The generator validates polygon coverage before/after simplification and
  groups territorial shapes by code with canonical country labels.
- Northern reference locations were checked against the published Survey of
  India outline in its Lambert conformal conic coordinate system. Gilgit,
  Aksai Chin, Tawang, and Delhi were inside that outline. The source India POV
  includes those locations. The shipped SVG regression verifies northern
  reference locations belong to India and do not overlap neighboring polygons.
- The old default outline failed the new geometry regression; the corrected
  asset passes. Zero-activity AU/FR label regressions also pass after combining
  same-code source features. No geographic package or external map service runs
  in the application. No visitor-level location data was added.
- The independent map review found no Critical/Important issue. Its modifier-key,
  intensity wording, missing-geometry wording, and grouped-territory label
  findings were fixed and checked. The production frontend build passes with the
  final asset, including its TypeScript check.
- [Zoomed India preview](ui/india-boundary-view.png) and the updated map theme/size
  screenshots were visually inspected. The local development backend was
  restarted with the map API; health returned `UP`.
- The user's final placement correction moves the map below all six traffic
  breakdowns. The report's two unit tests and all 12 analytics browser cases
  passed again; refreshed screenshots show this order on desktop and mobile.

## Local runtime follow-up

The user's first real analytics request reached a development backend started
before this feature existed. It had the older dependency classpath and database
schema v6. Restarting that local process with the verified current application
and its existing development environment applied Flyway v7 successfully; health
returned `UP`. The user's subsequent screenshot showed the daily activity chart.
No historical visits were backfilled. Restart the backend after pulling this
feature so its new controllers, dependencies, and migration are loaded.

### Unknown geography and Brave follow-up

The user's later screenshot showed Unknown country/city and Chrome for Brave.
A restricted check of the running development configuration confirmed no City
database was configured and no proxy CIDRs were trusted; the application listens
on loopback for the localhost Next.js proxy. The geography adapter intentionally
returns Unknown without a database and for loopback/private addresses even with
one. Existing tests verify both missing configuration and a synthetic positive
lookup. No fabricated location, external IP lookup, or proxy-trust bypass was
introduced.

The redirect regression reproduced a request with Brave's published macOS
User-Agent and `Sec-CH-UA` brand being persisted as Chrome. The iOS fixture also
reproduced Safari instead of Brave. After the fix, existing advertised browser
brands are reduced to the Brave family before queueing. Tests cover brand order,
quoted GREASE entries, malformed/oversized/missing hints, exact-brand matching,
other browser fallbacks, and omission of raw headers/versions from metadata.

`./mvnw -B -ntp clean verify` passed with 253 unit tests and 118 integration tests,
zero failures/errors/skips, Spotless and static analysis clear, and 2433/2666
covered lines (91.3%). A read-only independent review found no Critical/Important
issue. The local backend was restarted with this verified build and health
returned `UP`. New clicks can show Brave when it advertises its identity; earlier
Chrome/Unknown records cannot be reconstructed because raw headers and visitor
addresses were never stored. Operational prerequisites and detection limits are
documented in [operations.md](operations.md). The sustained-load acceptance
constraint below remains open.

### Supplied DB-IP City database

The user supplied `ip-DB/dbip-city-lite-2026-10.mmdb`. Its 126,998,165-byte file
matches the October 2026 SHA-1 published by DB-IP:
`df17bd24390108ed1d24dcb7718dc60f6b9abd4f`. The existing Java reader identifies
it as `DBIP-City-Lite`. Direct checks through the application's geography adapter
returned US/Mountain View and US/Minneapolis for two public reference addresses
unrelated to visitors. Loopback still correctly returned Unknown. No new runtime
dependency, external lookup, database schema, or proxy-trust change was needed.

The ignored local `.env` now supplies the absolute path. The local backend was
restarted with that value and existing development settings; health returned
`UP`. The binary is excluded from Git. Source/license/update instructions live
in [ip-DB/README.md](../../../ip-DB/README.md). Both reports now carry the DB-IP
attribution link, with no-referrer navigation. The report's two component tests,
targeted formatting/lint checks, TypeScript check, and all 12 analytics browser
cases passed. Refreshed desktop/mobile screenshots were checked for the footer.
Local clicks and
historical Unknown events remain Unknown; useful new geography requires public
visitor traffic reaching the backend through the approved network path.

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

## PR 21 dependency-scan correction

The first GitHub backend run passed tests, coverage, and static analysis, then
failed the packaged dependency scan. Its retained Trivy report identified five
High findings in Jackson 2 core/databind 2.21.5, brought in through GeoIP and
managed by Spring Boot's separate Jackson 2 BOM. The existing Jackson 3.1.7 pin
does not control these artifacts.

Set `jackson-2-bom.version` to 2.21.7, retaining Jackson 3.1.7. The packaged JAR
contains Jackson 2 core/databind/jsr310 2.21.7 and the BOM's annotations 2.21.
`./mvnw -B -ntp spotless:check clean verify` passed with 253 unit tests and 118
integration tests, no failures/errors/skips, coverage met, and static analysis
clear. Public-reference DB-IP checks through the application adapter still
return country/city; loopback remains Unknown. Independent read-only review
found no Important compatibility/security issue.

Repository-pinned Trivy 0.75.0 scanned the packaged `backend/target` with the
same `rootfs --scanners vuln --pkg-types library --severity HIGH,CRITICAL
--exit-code 1` gate: zero findings. The CI inventory helper also passed. No
ignore rule, scanner threshold, workflow gate, application API, or stored
visitor data changed. GitHub's replacement run must independently pass the
dependency scan and previously blocked CodeQL/image checks. Strict sustained
performance acceptance below is a separate open item.

## Performance evidence

The first protocol attempt ran while other verification was active. Its 200
creation samples had p95 38.49 ms, but the original strict driver rejected the
warm-up before the sustained phase. Retain [failed-warmup.json](failed-warmup.json)
for its anonymous capture samples (2997 recorded, no worker failures/rejections,
maximum observed recording delay 174 ms). This is a failed attempt, not evidence
of NFR-PER-01/03 acceptance.

The sustained second attempt is retained as
[failed-sustained.json](failed-sustained.json). It completed 59,972 of 60,000
offered redirects over 600 seconds, with no HTTP errors/timeouts; server p95
5.97 ms and p99 41.78 ms, create p95 22.34 ms. All 62,972 issued warm-up/measured
redirects were captured, with no failed/rejected/stale observations and maximum
observed recording delay 557 ms. The strict original driver rejected 28 arrivals
that were over 100 ms late at the generator. This remains a failed acceptance
attempt, despite the latency and capture results. Performance acceptance and
task archival remain open; the feature must not be marked ready for review yet.

## Operational limits

Geography requires an operator-provisioned licensed City database for useful
real traffic; omission intentionally yields Unknown. The included database is
synthetic test data. There is no historic-event backfill and existing lifetime
counts are preserved. Capture is informational and can undercount process loss,
queue saturation, and database outages. Local benchmark results do not validate
Hostinger/Supabase network latency, a production City database, or deployment
availability. See [operations.md](operations.md).
