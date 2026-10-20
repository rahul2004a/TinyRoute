# Short-link analytics

Status: proposed design; awaiting user review before implementation.
Branch: `feature/short-link-analytics`.
Baseline: `f75203f` (`main` and `origin/main`, checked 2026-10-20).

## Outcome and authority

Signed-in owners can inspect successful redirects across their links or for one
selected link, choose a reporting range, and understand traffic sources and
daily activity. Lifetime totals remain distinct from range totals.

Behavior follows [Functional.md](../../requirements/Functional.md),
[Non-Functional.md](../../requirements/Non-Functional.md), and
[architecture.md](../../architecture/architecture.md). Presentation follows
[DESIGN.md](../../../DESIGN.md). Stack and production boundaries remain those
of ADRs 0001–0003. This specification refines the architecture's analytics flow;
it does not replace redirect, authentication, or management policies.

| Requirement               | Feature behavior                                                              |
| ------------------------- | ----------------------------------------------------------------------------- |
| FR-ANA-01/02              | Count successful redirects and display range and per-link lifetime totals.    |
| FR-ANA-03                 | UTC daily trend, selectable range, default last 30 calendar days.             |
| FR-ANA-04                 | Referrer, device, OS, browser, country, and city breakdowns.                  |
| FR-MGT-02; NFR-SEC-04     | Every query is owner-scoped; missing and non-owned links share a generic 404. |
| NFR-CON-03                | Asynchronous capture; disclose up to one minute of recording delay.           |
| NFR-PRV-02/03             | No stored visitor address, identifier, raw user agent, or raw referrer URL.   |
| NFR-PER-01/03; NFR-REL-02 | Analytics cannot block or change a redirect or supply a fallback destination. |
| NFR-SEC-05/08; NFR-DEP-02 | Bound queries, parameterize SQL, return safe errors, and migrate with Flyway. |

Scope includes the overview, individual-link page, read APIs, capture, persistence,
traffic normalization, tests, and operating documentation. Creation and management
behavior, visitor identification, unique visitors, sessions, live tracking,
exports, public analytics, bot exclusion, and V1/Future features are excluded.

## Existing implementation

`links.click_count` already exists and `GET /api/links` returns it, but redirects
currently do not update it. There is no click-event table, analytics service/API,
analytics UI, or geographic lookup. The redirect cache holds a code and validated
state, not a link identifier. Existing link pagination, session renewal, owner
filtering, themes, and the settings drawer can be reused.

No historical redirect events can be reconstructed. Analytics begins with this
release. Existing lifetime counts must be preserved; do not fabricate events or
reset counts. A pre-existing lifetime value can therefore exceed event history.

## Approach and alternatives

Use the architecture's bounded, in-process asynchronous capture and PostgreSQL
events, with query-time aggregation. This is the smallest approach that provides
all six dimensions and arbitrary supported date ranges with atomic count writes.

Synchronous database writes would couple redirect latency and availability to
analytics. Precomputed cubes, Redis event queues, and separate brokers/workers
would add persistence and recovery policies that this MVP does not need. They
are not selected. Informational analytics can undercount capture failures, as
already allowed by the functional assumptions and architecture.

## Successful-redirect capture

A counted click is a completed Spring MVC `GET` response issuing the existing
302 to a validated destination. It is not proof that the visitor loaded that
destination. Repeated successful requests count separately, including automated
traffic. `HEAD`, `OPTIONS`, health/readiness checks, auth redirects, failed
requests, malformed/case-mismatched codes, unknown/disabled/deleted/expired links,
throttled requests, and service errors do not count.

Use a narrowly registered `security/RedirectAnalyticsInterceptor` completion hook. The
existing redirect controller marks only its confirmed redirect branch with the
validated short code. The hook requires that marker, GET, status 302, and no
completion exception, then hands capture to `ClickCountService`. Do not infer a
click from an arbitrary Location header. This HTTP completion seam implements
the architecture's "after a successful 3xx" arrow; resolution policy stays in
`RedirectService`. Record a server timestamp when the redirect is issued, before
queueing, so worker delay never shifts an event into another reporting day.

`ClickCountService` submits to a dedicated `@Async("analyticsExecutor")` worker;
use a separate proxied worker bean so asynchronous/transactional annotations are
effective. Start with four fixed workers and a queue of 512 tasks. Reject new
tasks when full; never use caller-runs or an unbounded queue. Discard observations
older than 45 seconds before recording and bound database work to five seconds.
Catch submission, enrichment, and persistence failures outside the response
contract. An analytics failure must never change the already chosen redirect.

Queue tasks contain only bounded metadata and the transient client address
needed for local geographic lookup. They must not retain a servlet request,
session, cookie, authorization data, or visitor identifier. Raw metadata must
not appear in object string representations, exceptions, or logs. Drop queued
observations during shutdown rather than retaining them on disk. Capture is
best-effort during process failure, saturation, or datastore outages; do not
add retries with ambiguous duplicate-count semantics.

Normalize headers before queueing where practical. Resolve the client address
using the existing trusted-proxy logic, exposing that same resolved address for
transient geography without weakening rate-limit behavior. Ignore arbitrary
forwarded geographic headers. Never persist or hash that address for analytics,
or cache lookup results by address. Release the address with its task.

## Metadata and geographic lookup

Keep only these normalized dimensions:

| Dimension        | Stored value / fallback                                                                                                                                                                                                                                 |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Referrer         | Lowercase ASCII hostname from a valid HTTP(S) Referer; strip user info, port, path, query, and fragment. Missing header is `Direct / unknown`; malformed, IP-literal, localhost, single-label, or overlong host is `Unknown`. Do not resolve hostnames. |
| Device           | Coarse allowlist: Desktop, Mobile, Tablet, Bot, Unknown.                                                                                                                                                                                                |
| Operating system | Family only: Windows, macOS, Linux, Android, iOS, ChromeOS, Other, Unknown.                                                                                                                                                                             |
| Browser          | Family only: Edge, Opera, Samsung Internet, Chrome, Firefox, Safari, Other, Unknown.                                                                                                                                                                    |
| Country          | ISO country code and English display name from the local database, otherwise Unknown.                                                                                                                                                                   |
| City             | English city plus country code; no coordinates, postal code, network, or precision fields. Missing city is Unknown.                                                                                                                                     |

Use a small concrete normalizer with explicit precedence (tablet before mobile;
Edge/Opera/Samsung before Chrome; Chrome before Safari; Android before Linux;
iOS before macOS). Only recognized user-agent tokens produce a family; wholly
unrecognized input is Unknown. Ignore versions, model names, and arbitrary
unparsed text. Bound raw Referer to 2048 characters and user agent to 1024;
overlong input becomes Unknown instead of being persisted or parsed further.
No user-agent parser dependency or fingerprinting is needed for these coarse
families. Metadata absence never drops an otherwise valid click.

Use an offline GeoLite2 City/GeoIP City `.mmdb` through a `GeoLocationLookup`
interface and `MaxMindGeoLocationLookup` adapter in `client/`. The proposed
dependency is `com.maxmind.geoip2:geoip2:5.2.0`, justified by FR-ANA-04 and
documented in a new ADR during implementation. Reuse one reader and call
`tryCity`; no HTTP lookup or reverse DNS. MaxMind documents local file readers
and thread-safe reuse in its [Java API](https://maxmind.github.io/GeoIP2-java/#database-usage).

`ANALYTICS_GEO_DATABASE_PATH` optionally points at a read-only operator-supplied
file. With no path, development still works and country/city show Unknown.
A configured unreadable/invalid file fails startup with a safe configuration
error; a missing address or lookup failure at runtime yields Unknown. The owner
obtains and updates the licensed database outside the backend. Do not download
it or require license credentials during requests, builds, or tests; do not
commit a production database. Document a read-only mount under `infra/vps/`
and license/update responsibilities without implementing the production stack.
Verify a synthetic fixture lookup in tests and record database provisioning as
a prerequisite for useful deployed geographic results. Geography is approximate.

## Persistence and consistency

Add `V7__create_click_events.sql`, retaining earlier migrations unchanged:

- `click_events`: UUID primary key; `link_id` UUID FK to retained links;
  `clicked_at` timestamptz; bounded, normalized referrer/device/OS/browser/country/
  city columns. Use null or fixed categories for unavailable data, never raw input.
- Index `(link_id, clicked_at DESC)` for link/range reads. Retain the existing
  owner index on links and verify query plans for overview joins.
- No visitor address, visitor hash, cookie/session/device identifier, raw user
  agent, full referrer, latitude, longitude, or arbitrary metadata column.

Persist an event and increment `links.click_count` in one worker transaction.
Use parameterized native SQL at the JPA repository boundary; a data-modifying
CTE can insert the event by immutable code and increment the returned link's
counter in one database round trip. Gate new inserts on a non-deleted owner.
The code is never reused, so asynchronous lookup by code is unambiguous and
requires no new identifier field or schema version in the redirect cache.
Do not re-resolve a destination or recheck active/expiry state in the worker:
a valid redirect may finish just before a status or expiry transition.

Event insertion and increment must both commit or both roll back. Concurrent
increments use SQL arithmetic, never entity read/modify/save. No retry after an
uncertain commit. Add nonnegative count constraints without changing existing
counts. Individual link tombstones stay retained as before. Account deletion
blocks subsequent inserts for that deleted owner; anonymous historic events
need no visitor-data cleanup, and the deleted account cannot query them.

Analytics snapshots use read-only repeatable-read transactions so totals,
trend, breakdowns, and lifetime count agree on one committed snapshot. The
overview includes the owner's non-deleted links, including disabled and expired
links. Deleted links are excluded from both overview and detail. Totals can
therefore decrease after link deletion. No management action is added here.

## Date range and API contract

All reporting uses UTC calendar dates and states this in the UI and response.
With neither parameter, the backend uses UTC today minus 29 days through today,
inclusive. With parameters, require both `from` and `to` as strict `YYYY-MM-DD`
dates, `from <= to`, `to <= UTC today`, and at most 366 inclusive days. Reject
empty, duplicate, malformed, partial, reversed, future, and oversized ranges
with 400 `VALIDATION_ERROR` and clear field messages. A single day is valid.
Query timestamps using `[from at 00:00Z, day after to at 00:00Z)`.

The maximum is a proposed query/resource bound for this MVP; ranges may be
chosen anywhere in available history. It does not restrict event retention.

| Endpoint                                        | Result                                                                   |
| ----------------------------------------------- | ------------------------------------------------------------------------ |
| `GET /api/analytics?from=...&to=...`            | Aggregate across the authenticated owner's eligible links.               |
| `GET /api/links/{id}/analytics?from=...&to=...` | One owned link, verified by `id AND owner_id` and non-deleted state.     |
| Existing `GET /api/links?size=10&cursor=...`    | Paginated link selection and each link's existing lifetime `clickCount`. |

Never accept an owner identifier from the caller. Reuse cookie authentication,
session renewal, exact-origin CORS, and security filters. Reads require no CSRF
token and consume no creation quota. All analytics responses and errors use
`Cache-Control: no-store`; no shared HTTP/Next.js response cache.

The detail service first checks ownership, then validates the range, then reads
events. A nonexistent, malformed, non-owned, or deleted identifier returns the
same safe 404 `NOT_FOUND` message, including with invalid ranges. Do not bind
dates to a controller type that rejects them before this ownership check.
Unauthenticated requests return the existing generic 401. Persistence failures
return safe 503; they must not appear as zero activity.

Both successful response shapes contain:

```text
range: { from: YYYY-MM-DD, to: YYYY-MM-DD, timeZone: "UTC" }
generatedAt: ISO-8601 timestamp
recordingDelaySeconds: 60
totalClicks: nonnegative integer (selected range)
lifetimeClicks: nonnegative integer (entire eligible scope)
dailyClicks: [{ date: YYYY-MM-DD, count: nonnegative integer }]
breakdowns: { referrers, devices, operatingSystems, browsers, countries, cities }
```

The detail response additionally contains `link: { id, code, shortUrl,
destinationUrl, status }`, using the existing server-side URL/status rules.
The overview does not embed all links; use existing cursor pagination for its
link selection list. A link's detail lifetime value and list `clickCount` have
the same meaning, regardless of date filter.

Return one daily row for every selected date, ascending and zero-filled. Each
breakdown entry is `{ kind, label, count }`; `kind` is `VALUE`, `UNKNOWN`,
`DIRECT_OR_UNKNOWN`, or `OTHER`. Sort known values by descending count and
ascending label for ties. Keep the top 20 known values per dimension; combine
remaining known values in Other. Keep Unknown and Direct / unknown separate
from Other. Omit zero-count buckets. Each dimension must sum to `totalClicks`,
including fallbacks and Other. City labels include their country code so equal
city names in different countries are not merged. No cross-dimension visitor
records or drilldowns are exposed.

## Frontend presentation and state

Routes: `/analytics` for the overview and `/analytics/[id]` for detail. Use App
Router Server Components for page structure and narrow Client Components for
queries, filters, and charts. Browser requests use the existing typed API client;
Next.js owns no aggregation, ownership, public redirect, or reporting policy.

Follow the existing settings workspace's 224px sidebar and accessible mobile
drawer, with Links, Analytics, and Account navigation. Reuse/extract the actual
drawer behavior and semantic palette rather than building another navigation
system. Keep the existing `/links` header/footer and form layout intact; add an
Analytics navigation entry and a View analytics action beside each saved link.
Scope changes to shared navigation to these real destinations and active states.

Order content: page heading and context; date filter; selected-range total and
clearly labeled lifetime total; daily trend; six labeled breakdowns. The
overview also offers the existing paginated owned-link list with lifetime
counts and detail links. Do not fetch all history to populate a dropdown or
compute overview totals from the current page.

Reuse Geist Sans/Mono, semantic CSS variables, restrained borders, 12px panels,
44px controls, and L1 CSS entrances. Detail shows the selected short URL and
destination as text plus an ordinary safe external link; viewing analytics
must never request the short URL and inflate its clicks. No animated metrics,
decorative charts, or map are needed.

Provide presets Last 7 days, Last 30 days, Last 90 days, and custom labeled date
inputs with Apply. Store applied `from`/`to` in URL parameters and preserve them
when entering/leaving detail. Form edits do not change results until Apply.
Backend validation stays authoritative; mirror date syntax for early feedback
with React Hook Form/Zod. Default dates and applied labels come from the API's
resolved UTC range; the browser does not substitute its local timezone.

Use one analytics response for every metric, daily chart, and breakdown on a
page. Recharts is the only charting dependency. Disable chart animations and
provide direct labels, a readable textual summary, reachable tooltips, and
an explicit Show data table disclosure containing the identical daily values.
Breakdowns use semantic count tables with optional decorative bars, so no
information depends on color or hover. Tables have captions and header scope.

On desktop use two columns for related breakdowns; on mobile stack them and
filters, with a responsive chart. Test both themes at 320, 768, 1024, and 1440px.
Only a labeled table viewport may scroll horizontally; the page must not.

| State                    | Required behavior                                                                                                                                                                |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Session loading          | Do not request or reveal analytics until session resolution.                                                                                                                     |
| Signed out               | Use the existing sign-in flow and preserve the safe analytics return path.                                                                                                       |
| Loading / changing range | Label loading accessibly; clear or hide old metrics until the applied range resolves. Never present previous-range values under the new range label.                             |
| Success                  | Show the response range, all metrics, data tables, and the reporting timestamp.                                                                                                  |
| Zero activity            | Show a range total of 0, an empty trend message, and no invented breakdown values; retain the actual lifetime total. The daily table contains one row per day with a count of 0. |
| No links                 | Distinguish from an owned link with no activity and provide navigation to Your links.                                                                                            |
| Invalid range            | Inline field errors, retained input, and an announced summary; no error-to-zero fallback.                                                                                        |
| Missing / non-owned      | Identical generic Link not found page without destination/owner details.                                                                                                         |
| API failure              | Safe error with Retry, retained range, and no stale successful-data presentation.                                                                                                |

Always show `Clicks may take up to one minute to appear.` and an `As of` UTC
timestamp on successful snapshots, with a Refresh action. This is a snapshot,
not live tracking: no polling, streaming, visitor IDs, or optimistic counts.
Use `staleTime: 0`, normal refetch-on-mount/focus, and one bounded session refresh
following the existing GET client pattern. Do not layer automatic query retries
over session recovery; offer explicit Retry instead.

Query keys include authenticated user ID, scope/link ID, and applied range;
use AbortSignal for superseded requests. Hide data immediately when session
access is lost, cancel/remove analytics queries on sign-out/account change,
and verify that another account cannot see cached data or a late response.
Do not persist analytics queries in browser storage.

## Verification and acceptance

These criteria remain unchecked until implementation and runtime verification:

- [ ] AC1 — Two owners and several links prove overview isolation, selected-range
      totals, per-link lifetime totals, and consistent sums across all dimensions.
- [ ] AC2 — Detail isolates one owned link; missing/non-owned/deleted/malformed
      IDs have identical 404s, even with invalid ranges. Signed-out APIs return 401.
- [ ] AC3 — Fixed-clock tests prove last-30-day defaults, inclusive UTC bounds,
      exact end-date exclusion, zero-filled days, leap day, and all invalid ranges.
- [ ] AC4 — Normal, missing, malformed, overlong, and adversarial metadata produce
      the six allowed dimensions, deterministic top-20/Other buckets, and no raw input.
- [ ] AC5 — GET redirects on database and warm-cache paths count once; HEAD,
      health checks, every failed outcome, throttling, and auth redirects do not.
- [ ] AC6 — Concurrent capture keeps event/count writes atomic; failed writes,
      saturated executor, stale queue entries, and geography failure preserve 302s.
      Accepted events appear within 60 seconds under the sustained baseline.
- [ ] AC7 — Changing the range updates every ranged value in overview/detail;
      lifetime values retain their meaning. Invalid ranges retain inputs and errors.
- [ ] AC8 — Loading, no links, no activity, 404, API failure, Retry, Refresh, session
      expiry, account switching, and cancellation have distinct correct behavior.
- [ ] AC9 — Keyboard/mobile/desktop and both themes pass; chart/table values match,
      filters are labeled, focus works, and no whole-page horizontal overflow occurs.
- [ ] AC10 — Schema, requests, captured tasks/logs, responses, and browser storage
      contain no persisted identifying visitor data. Synthetic geography succeeds
      and the no-database fallback is explicit.
- [ ] AC11 — Maven verify and frontend lint/format/type/unit/build checks pass;
      analytics browser tests and affected auth/link regressions pass. Record actual
      results and limits, coverage, screenshots, and the redirect performance run.
- [ ] AC12 — Requirements/architecture/README and analytics availability copy are
      current; operational geography instructions and dependency ADR are complete.
      Complete and archive active tasks before marking a PR ready.

## Review choices

The proposed defaults are UTC calendar dates, a 366-day maximum per query,
excluding deleted links from reports, top-20 dimensions plus Other, and optional
operator-provisioned local geography. These are explicit reviewable choices;
none has been implemented or presented as an already approved behavior.
