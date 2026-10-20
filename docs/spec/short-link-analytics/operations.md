# Running short-link analytics

Open `/analytics` after signing in, or use View analytics beside a saved link.
Overview and detail use UTC calendar dates, default to today and the preceding
29 days, and accept at most 366 days per request. Lifetime totals are independent
of the chosen range. Deleted links are excluded; disabled and expired links
retain their history. Data starts when capture is deployed; there is no historical
backfill. Existing lifetime values remain intact.

Spring exposes authenticated, no-store reads:

```text
GET /api/analytics?from=2026-10-01&to=2026-10-20
GET /api/links/{id}/analytics?from=2026-10-01&to=2026-10-20
```

Omit both dates for the default. Use both with strict YYYY-MM-DD syntax.
Invalid/duplicate/future/reversed/oversized dates return validation errors.
Missing, malformed, deleted, and non-owned link IDs return the same 404 before
range validation. Aggregate buckets include Unknown, Direct / unknown, and
Other where appropriate. The top 20 known values are shown per dimension.

The UI provides daily values in an accessible table as well as the chart.
Snapshots show their UTC timestamp and the recording-delay notice. Refresh
reloads the report; there is no live tracking or browser persistence of reports.
Password sign-in preserves a safe analytics return path. Google OAuth retains
its existing configured return to Your links; changing OAuth transaction routing
is outside this analytics feature.

## Local geography

Obtain a City `.mmdb` through an appropriate
[MaxMind database download](https://dev.maxmind.com/geoip/docs/databases/) account.
Keep the production file and any download credentials outside Git. Set:

```sh
ANALYTICS_GEO_DATABASE_PATH=/absolute/path/to/GeoLite2-City.mmdb
```

Then restart Spring Boot. The reader is opened once and closed at shutdown.
With no path, country/city are Unknown. A configured unreadable or corrupt file
fails startup with a generic message. Unknown/local/private addresses and
unavailable locations remain Unknown without losing the click. The application
makes no geolocation HTTP requests, performs no reverse DNS, and stores no
visitor address or per-address lookup cache.

The operator owns database licensing and update procedures. Replace the file
through an atomic rename and restart the application to load the new reader;
do not overwrite an actively mapped file in place. Do not place download
credentials in backend runtime configuration. The tiny City-Test fixture in
`backend/src/test/resources/geo/` is synthetic and is not useful for real traffic.

## Future VPS integration

Production runtime files remain under `infra/vps/`; the root Compose file stays
development-only. Supply a read-only host-file mount for the City database and
set the environment path to its container location. Ensure the non-root backend
user can read it. Geography data is replaceable reference data, not durable
application state. Do not add a database download process, public container
port, geolocation service, or provider credential to the backend image.

## Capture behavior and metrics

Only a completed GET/302 from the actual redirect controller is captured.
HEAD, health checks, authentication redirects, throttling, and failed link
resolution are excluded. The executor has four workers and 512 queue slots;
full queues reject work without blocking redirect threads. Observations older
than 45 seconds are dropped and each persistence transaction has a five-second
timeout. Captures are informational, not billing-grade; process failure,
saturation, or database failure can undercount. A failed capture never changes
the redirect and is not retried after an ambiguous commit.

Aggregate metrics use no owner, link, visitor, referrer, or address labels:
`tinyroute.analytics.recorded`, `.failed`, `.rejected`, `.stale`, and
`tinyroute.analytics.recording.delay`. Existing Actuator exposure/security is
unchanged. Queued tasks and exceptions must never be logged with raw arguments.

V7 adds only the event table/index and a nonnegative lifetime-count constraint.
Application rollback can retain V7; the prior application does not reference
the table. Do not roll back Flyway history, reset lifetime values, or remove
existing link tombstones. Database backups already include the application
schema, including click events.

## Reproducing performance evidence

Run the backend verification and prepare trusted local TLS as documented in
[the existing protocol](../link-creation-and-redirection/performance-setup.md).
Then run from the repository root:

```sh
python3 docs/spec/short-link-analytics/run-performance.py
```

The wrapper uses the unchanged existing 200-creation/30-second-warm-up/100 rps
for 600 seconds protocol in disposable Docker containers. It enables the
synthetic geography reader and polls a guarded test-only endpoint for anonymous
capture counts/delay. It expects 63,000 captured redirects including warm-up,
matching lifetime totals, no capture failures/rejections/stale observations, and
recording delay below one minute. It retains failed attempts and cleans up only
its own named runtime/relay containers. Ryuk cleans their disposable datastores.
Keep the laptop awake/on AC with its lid open and avoid parallel builds or tests.
No production services, secrets, or account data are used. Private setup stays
under ignored `.local-verification/`; review numeric results before committing
sanitized evidence. Local performance is not proof of the production topology.
