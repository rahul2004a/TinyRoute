# Short-link analytics implementation plan

> For agentic workers: use `superpowers:executing-plans` inline. Track each step below and in `tasks/todo.md`.

**Goal:** Give signed-in owners private range-filtered totals, trends, and traffic breakdowns.
**Architecture:** Complete successful GET redirects into an isolated bounded async worker. Atomically persist normalized events and increment lifetime counts; serve owner-scoped repeatable-read aggregates. Render the API through typed TanStack Query reads and accessible Recharts views.
**Tech stack:** Java 21/Spring MVC/JPA/PostgreSQL/Flyway; Next.js App Router/React/strict TypeScript/Node 24/pnpm.
**Spec:** [Approved specification](../docs/spec/short-link-analytics/spec.md), approved by the user on 2026-10-20: “ok start”.

## Global constraints

- Retain layer-first backend packages and all existing authentication/redirect behavior.
- Never persist visitor addresses, identifiers, raw user agents, or full referrers.
- UTC calendar dates; default 30 days; maximum 366 days; deleted links excluded.
- Every report query uses the verified owner; detail ownership precedes range validation.
- Fixed four-worker/512-task async executor; no caller-runs; 45-second age cap and five-second database timeout.
- Existing semantic palette, Geist fonts, 44px targets, mobile drawer, accessible tables, L1 motion.
- No management features, broker, polling, exports, production deployment, merge, or branch deletion.

## Task 1 — Date rules and atomic persistence

**Files:** `model/AnalyticsRange.java`, `model/ClickEvent.java`, `model/ClickMetadata.java`, `repository/ClickEventRepository.java`, `repository/jpa/JpaClickEventRepository.java`, `db/migration/V7__create_click_events.sql`; tests `model/AnalyticsRangeTest.java`, `repository/ClickEventRepositoryIT.java` (paths relative to backend Java/resources roots).
**Interfaces:** `AnalyticsRange.parse(String from, String to, Clock clock)`; `ClickEventRepository.record(String code, Instant clickedAt, ClickMetadata metadata)`; `totals(UUID ownerId, UUID linkId, AnalyticsRange range)`, `daily(...)`, `breakdown(..., Dimension dimension)`.

- [ ] Write fixed-clock tests for inclusive UTC boundaries, leap days, partial/future/reversed/overlong ranges; run and confirm the missing range implementation fails.
- [ ] Implement strict date parsing and safe field errors. Example: for 2026-10-20, omitted range resolves to 2026-09-21 through 2026-10-20; the exclusive end is 2026-10-21T00:00:00Z.
- [ ] Write real PostgreSQL tests that record two events and assert both `count(*) = 2` and `click_count = 2`, then roll back a third recording and assert neither count changes.
- [ ] Add schema and a parameterized insert/update CTE; event insert returns link ID and counter increments from that row. Preserve all earlier migrations and lifetime values. Scope aggregates through non-deleted owned links.
- [ ] Run `./backend/mvnw -f backend/pom.xml -Dtest=AnalyticsRangeTest test` and targeted Failsafe repository tests; confirm green before proceeding.

## Task 2 — Completed redirect capture and anonymous dimensions

**Files:** `service/ClickMetadataNormalizer.java`, `service/ClickCountService.java`, `service/ClickRecordingWorker.java`, `security/RedirectAnalyticsInterceptor.java`, `config/AnalyticsConfiguration.java`, `client/GeoLocationLookup.java`, `client/MaxMindGeoLocationLookup.java`; modify `RedirectController.java`, `AsyncConfiguration.java`, `ClientAddressResolver.java`, `application.yml`, `pom.xml`; test corresponding service/security/client packages.
**Interfaces:** `normalize(String referer, String userAgent)` returns `ClickMetadata`; `GeoLocationLookup.lookup(InetAddress)` returns country/city only; `recordSuccessfulRedirect(String code, Instant issuedAt, ClickMetadata metadata, InetAddress address)` submits to the worker.

- [ ] Write literal UA/referrer fixtures: Edge wins over Chrome; iOS wins over macOS; referrer strips credentials/path/query; IP literals and oversized input become Unknown.
- [ ] Run the unit tests red, then implement coarse allowlist normalization and a local optional MaxMind reader, pinned to 5.2.0. Synthetic MMDB verifies the adapter; no network lookup or IP cache.
- [ ] Add successful-redirect request marker and MVC completion hook restricted to GET/302/no exception. Add separate async worker with stale-event rejection and transactional five-second recording. Catch failures and expose aggregate-only capture counters.
- [ ] Verify cache hits count, all failed responses/HEAD/health/auth redirects do not, saturation rejects promptly, stale events do not write, and database/geography failure preserves redirects. Run targeted tests then existing redirect contracts.

## Task 3 — Authenticated owner-scoped report APIs

**Files:** `controller/AnalyticsController.java`, `service/AnalyticsService.java`, `security/OwnershipGuard.java`, `dto/AnalyticsResponse.java`, `exception/LinkNotFoundException.java`; modify repository owner lookup and generic exception handler; test `controller/AnalyticsContractIT.java`.
**Interfaces:** `overview(AccessToken, MultiValueMap<String,String>)`, `detail(AccessToken, String id, MultiValueMap<String,String>)` return range, timestamp, delay, range/lifetime totals, daily rows, six breakdowns, and detail link metadata.

- [ ] Write HTTP tests for two owners, no activity, default dates, invalid/duplicate ranges, generic 404 parity (including invalid ranges), and anonymous 401.
- [ ] Confirm APIs fail red before implementing. In one read-only repeatable-read service transaction, verify ownership, parse dates, query counts/days/dimensions, fill missing days, and fold known categories after the top 20 into Other.
- [ ] Set no-store for success/failure; preserve missing metadata buckets and deterministic sorting. Run targeted HTTP tests and relevant ownership/repository tests.

## Task 4 — Typed frontend and complete report states

**Files:** `frontend/src/features/analytics/{analytics-api.ts,analytics-page.tsx,analytics-range-form.tsx,analytics-report.tsx,analytics.css}`, `frontend/src/app/(account)/analytics/{layout.tsx,page.tsx,[id]/page.tsx}`; tests beside API/form/report components.
**Interfaces:** `getAnalytics({id?,from?,to?}, signal?)` validates the complete API response; `AnalyticsPage({id?, rangeQuery})` gates queries on the authenticated session; `AnalyticsReport({report})` renders returned values only.

- [ ] Read installed Next.js page/Server-Client guides. Write request tests for credentials, range serialization, abort, safe API failures, and one bounded 401 session recovery; confirm red.
- [ ] Install pinned Recharts and implement typed schemas/reads. Key queries by account, link, and range with staleTime 0/retry false and AbortSignal.
- [ ] Write component tests for loading/error/retry/zero/detail, six tables, table-chart equivalence, invalid ranges, and lifetime invariance; confirm red then implement.
- [ ] Implement UTC presets using the last server-resolved end date, React Hook Form/Zod custom inputs, URL applied state, loading on range changes, generic detail 404, and explicit snapshot/delay/Refresh controls. Session loss cancels and removes account analytics queries.
- [ ] Run `pnpm --dir frontend test --run src/features/analytics` and typecheck.

## Task 5 — Navigation and responsive integration

**Files:** `features/auth/settings-sidebar.tsx`, `features/links/link-create-result.tsx`, `(account)/links/page.tsx`, landing navigation/availability text; analytics layout/styles; DESIGN.md.

- [ ] Add browser/component assertions that Analytics opens from saved links without requesting the short URL and applied ranges survive detail/back navigation.
- [ ] Reuse the settings sidebar's existing drawer with active route labels; add real Analytics entries and saved-link lifetime/action labels without altering create/manage behavior.
- [ ] Add paginated link selection to overview using existing GET list and lifetime values; distinguish no links from no selected-range activity.
- [ ] Verify dark/light at 320/768/1024/1440px, keyboard focus, direct labels, accessible data tables, and no page overflow. Preserve links page shell.

## Task 6 — Full verification, evidence, and delivery

**Files:** `frontend/e2e/short-link-analytics.spec.ts`, synthetic geography fixture/tests, `docs/spec/short-link-analytics/verification.md`, dependency ADR, README/architecture/infra documentation; active task files.

- [ ] Run targeted analytics Playwright and affected account/links browser regressions using existing HTTPS fixture conventions.
- [ ] Run Maven clean verify (Docker required), frontend lint/format/type/unit/build, and git diff --check. Record actual results; fix failures and rerun affected checks.
- [ ] Run the existing sustained redirect protocol with analytics, measure capture freshness and counter consistency, and record machine/environment limits and screenshots.
- [ ] Update docs for APIs, beginning-of-capture history, local geo setup/read-only production mount/license updates, privacy, and availability copy. Mark spec acceptance criteria only with evidence.
- [ ] Self-review security/query consistency/late account responses; apply verification/review skills. Archive completed plan/todo, commit, push feature branch, and create PR. Do not merge or open the PR page.
