# Link creation and redirection verification

Feature `link-creation-and-redirection`; base `ecffb917a489cd18770127ede3b1a96f3a0debfb`.
Status: feature implementation and browser/live verification are complete; independent final review is complete and sustained load evidence is pending. This is not a completion or production performance claim.

Delivery: [draft PR #15](https://github.com/rahul2004a/TinyRoute/pull/15).
The active plan/checklist are retained until AC-10 and completion readiness pass.

## Counter, encoding and migration

User approved the fixed-key/salt AES-FF1 revision on 2026-10-08 ("done"). The eight-character radix-62 permutation uses the existing Bouncy Castle 1.86 dependency and Redis `code:global`. Normal allocation has no PostgreSQL availability/MAX query; missing-key and confirmed-conflict recovery use committed numeric metadata. Aliases and legacy codes remain NULL and cannot poison the recovery floor. Deleted/expired allocations remain reserved (FR-CRE-04/07; FR-RED-06; NFR-REL-01/02).

Observed red: the new focused tests failed compilation on missing encoder/counter/recovery contracts. Green: 54 encoder/counter/service units; 37 real PostgreSQL/Redis/security integration regressions; 64 configuration/domain/error units plus one isolated-schema V5→V6 migration integration. A final eight-test service run explicitly verifies ten candidates with no eleventh allocation. All selectors passed without failures/errors. Migration verifies unchanged legacy code/destination/tombstone strings, NULL metadata, range constraint and partial index. Concurrent Redis initialization and ordinary allocation each use 100 callers with unique values; encoder concurrency covers 2,000 calls.

## Browser and presentation

Full contract Playwright suite: 27 tests passed in 31.6 seconds, followed by eight theme/viewport contrast checks in 8.9 seconds. Existing authentication fixture had an ambiguous password locator because the visibility button also matched; corrected to an exact label. Keyboard creation/copying, denied clipboard/manual selection, signed-out/unknown/session renewal states, validation/conflict/quota/CSRF/service errors, no automatic mutation retry, and double submission are covered (FR-CRE-02/05; NFR-SEC-09; NFR-CON-01).

24 sanitized screenshots under [ui/](ui/) cover create/result/error in both themes at 320/768/1024/1440 pixels. Each case verifies no horizontal page overflow, reduced motion, 44-pixel creation/copy targets, and at least 4.5:1 measured foreground/background text contrast, excluding disabled controls. Screenshot-only styling hides the Next development portal; product rendering is unchanged. Manual image review covers narrow and wide layouts, retained results, field errors and focus visibility.

Disposable backend fixture/isolation integration: four tests passed. `/__verification/*` exists only on the test classpath and requires a loopback source plus a private token. Normal production application has no fixture endpoint. Datastore property callbacks are resolved before Spring application beans, using Testcontainers PostgreSQL 17.2 and Redis 7.4.2, never the owner's Compose database. The standalone test app explicitly allows its test JWT bean to override the production factory; this setting does not affect the packaged app.

Both live journeys passed on isolated frontend/backend ports 3001/8444, including actual clipboard copying, anonymous GET/HEAD, case mismatch, custom-alias conflict, cached disabled/deleted convergence, exact cached expiry and account-deletion redirects. Initial live link journey correctly failed on a repeated submission with stale CSRF: a private diagnostic confirmed that the existing authenticated request flow rotates its CSRF cookie. The new regression failed first, then link creation was changed to bootstrap CSRF before each manual mutation and clear the cache afterward. No backend authentication policy changes or POST retries. Full live rerun passed: both authentication and link journeys, 2 tests in 1.1 minutes (FR-CRE-02; NFR-SEC-09).

## Operational timing and pending load protocol

Timing uses `System.nanoTime` around the complete application filter chain, including authentication, CSRF, limits, allocation, FF1, PostgreSQL commit and redirect resolution. Fixed route/method/status/duration fields only; no code, destination, owner, cookie, client address or hash. The bounded test-only collector holds 100,000 records and fails evidence on overflow. No click counting/events/analytics are implemented (NFR-PER-01/02/03; NFR-OBS-01).

TDD red: missing timing filter and load module; CI harness classification failed. Green: three timing-filter tests, six Node harness self-tests and 14 CI helper tests. Guarded timing-fixture read/reset and production-isolation tests passed.

Planned measured protocol: 200 successful counter-generated creates across three disposable accounts; 30-second redirect warm-up; 100 requests/second for 600 seconds across 100 codes/clients, with default limits enabled. Built-in HTTPS never follows redirects and validates the local certificate chain. Open-loop arrivals allow at most 100 ms scheduling lateness; any dropped or excessively late arrival fails the run. Complete issuance/completion/server samples are required. All non-302 and transport failures remain in the denominator. Server nearest-rank p95/p99 are separate from client round trips. Achieved throughput must be at least 99.5 rps; strict latency/error thresholds remain <150/<300 ms/<0.5%, creation p95 <500 ms.

Two complete-duration local attempts did not pass the strict issuance gate.
The direct Linux driver issued 59,954/60,000 requests (46 late drops, max 203.26 ms),
with server p95/p99 5.68/18.41 ms and zero request errors. The native macOS driver
through a local TCP pass-through issued 59,995/60,000 requests (five late drops,
max 125.26 ms), with server p95/p99 8.29/69.63 ms and zero request errors. Its 200
creates all succeeded with server p95 23.30 ms. These latency observations do
**not** establish AC-10 because complete issuance is mandatory.

The explicitly [failed raw report](failed-local-load.json.gz),
[runtime/hardware metadata](performance-environment.json), and
[reproducible relay setup/source](performance-setup.md) retain the actual result.
The native driver validated certificates and hostname end-to-end; health and
source-guarded bootstrap returned 200. Wrong-hostname and untrusted-certificate
probes were rejected. Backend trust stayed exactly `127.0.0.2/32`, with default
100 creations/hour/account and 600 redirects/minute/client. The proxy changed no
TLS bytes or HTTP headers. No production configuration changed.

Diagnostics localized Linux-client stalls to native TLS setup. Context, CA,
group and session-cache experiments did not fix sustained issuance and were not
adopted. Independent review identified unnecessary settled-promise retention;
checkpoint `d3124b7` fixes it and supports the byte relay. Eleven harness tests and
14 CI helper tests pass. Both the relay and driver changes were independently
reviewed without findings. A later transport-worker experiment performed worse
and was discarded. The underlying host/runtime scheduling problem is unresolved.
A quiet uninterrupted session or isolated test machine is needed for another
full run; the user has been asked. Tasks 9–10 remain active and the PR remains draft.
Independent follow-up review recomputed the raw report's percentiles, counts and
checksums and checked its privacy and reproducible setup; no findings. This does
not approve AC-10 completion. The owned backend and relay verification containers
were stopped and removed after measurement.

## Full check results

Fresh full Maven `clean verify`: 238 unit + 106 integration tests, no failures/errors/skips; JaCoCo 2,013/2,230 application lines = 90.27%, exceeding the unchanged 70% gate; SpotBugs/FindSecBugs verify passed. Spotless check passed. The first full run exposed seeded fixture rows blocking older auth cleanup and cached test pools exhausting the shared disposable database. Scoped fixture-account teardown and a test-only four-connection/zero-idle pool fixed those issues; production pools and security remain unchanged.

Initial PR frontend CI passed. Backend pre-check stopped on one Gitleaks generic
API-key finding: the explicitly public local FF1 key in `.env.example`, verified
as Base64 of bytes `0x00..0x1f`. The pinned/checksum-verified Gitleaks 8.30.1
reproduced the single finding locally. `.gitleaksignore` records only that exact
historical commit/file/rule/line fingerprint, with its rationale; no rule or path
is disabled. The same full-history scan then passed with no findings. The rerun
at `22f241b` passed all backend and frontend gates, including CodeQL and
dependency/image scans: [backend run](https://github.com/rahul2004a/TinyRoute/actions/runs/37777646191)
and [frontend run](https://github.com/rahul2004a/TinyRoute/actions/runs/37777646123).
The initial skipped jobs are superseded by these successful results.

Frontend lint/format/typecheck, full Vitest (77 tests across 16 files), production webpack build and full contract Playwright (27 tests in 31.7 seconds) passed after the CSRF regression fix. Full live suite passed 2 journeys in 1.1 minutes. Node harness self-tests (9, including final-review filesystem regressions) and CI helper tests (14) passed. Frozen dependencies were restored after sandbox DNS restrictions interrupted checks; no dependency versions changed.

## Independent final review

PR follow-up on 2026-10-09: [backend run at `1cf4e04`](https://github.com/rahul2004a/TinyRoute/actions/runs/37782401827)
failed during `LinkCreationCommitFailureIT` setup with Mockito
`UnfinishedStubbingException` on the final transaction-manager commit method,
before any creation request. The previous CI run passed with identical backend
source. The fixture now uses a real PostgreSQL constraint trigger with
`DEFERRABLE INITIALLY DEFERRED`, scoped to the synthetic test owner and removed
after the test. A calibration transaction proves the insert finishes before
commit fails; the HTTP contract still requires 503, no Location, no stored row,
and no private details (NFR-CON-01; NFR-REL-02). The deferred timing follows the
[PostgreSQL 17 trigger contract](https://www.postgresql.org/docs/17/sql-createtrigger.html).
Without the fault the regression failed 503 versus 201; with it, four handler
unit tests and the commit integration test pass. Full `spotless:check clean verify` passed with 238 unit + 106 integration tests,
zero failures/errors/skips, 90.27% line coverage and zero SpotBugs findings.
Independent follow-up review found no issues. All eight CI checks passed at
`4864ecc`: [backend](https://github.com/rahul2004a/TinyRoute/actions/runs/37894704771)
and [frontend](https://github.com/rahul2004a/TinyRoute/actions/runs/37894704649).
Frontend/product code and dependency versions are unchanged.

One independent reviewer inspected base `ecffb917a489cd18770127ede3b1a96f3a0debfb`
through `26dfcdaa47999a07bbc53507ce45bf3b215bbf9f`, the approved spec/plan,
verification record and representative UI evidence. No critical implementation
defects were found. The reviewer independently confirmed all 344 backend tests
and 90.27% coverage, and checked counter recovery, permanent uniqueness,
owner fencing, exact cache expiry, public authentication isolation, throttling,
privacy and absence of analytics.

The sustained-load gate remains incomplete and keeps the PR in draft. Minor
findings were resolved: report output is prepared before measurement, and current
approval/Task 8 status records are reconciled. The report preflight tests first
failed on the missing exported helper, then all nine Node tests passed, including
real filesystem checks for nested private output, invalid directory targets and
preserving previous evidence. No product behavior changed; earlier full backend
and frontend results remain applicable (NFR-PER-01–03; NFR-TST-01/02).

## Outstanding completion gates and limitations

- Independent final review is complete; its minor report preflight and stale-status findings are fixed and affected checks pass.
- Save a passing complete sustained-load report; existing failed reports do not satisfy AC-10.
- Keep fixed FF1 key/salt configuration; rotation requires a future namespace/recovery design. Short links are public URLs, not authorization tokens.
- Local benchmarks do not establish latency or availability on Hostinger/nearby Supabase/Vercel. Verify the production topology before release.
- Cache changes may converge within the bounded five-second snapshot window; expiry is enforced at its exact instant, including cached results. Redis cache failure falls back to PostgreSQL; unknown authoritative state never produces Location.
- Archive active tasks only when all local acceptance and review gates pass; never merge/delete branches or force-push.
