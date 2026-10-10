# Link creation and redirection

- Feature: `link-creation-and-redirection`
- Classification: architectural; creation and public resolution extend existing
  link, security, persistence, and cache interfaces.
- Approach approved: 2026-10-07.
- Original written specification approved: 2026-10-07; original implementation
  plan approved: 2026-10-08. Tasks 1–7 were implemented under those approvals.
- Redis-counter revision requested: 2026-10-08, to match HLD.excalidraw.
  This revision and its revised plan were approved 2026-10-08 ("ok do now").
  A subsequent request adds salt and rejects truncated HMAC to avoid generated
  collisions. The AES-FF1 written spec/plan was approved 2026-10-08 ("done");
  Task 7A is implemented and verified. Tasks 1–10 are complete, including the
  passing local sustained-load run on 2026-10-10. Independent feature/evidence
  review and the final readiness audit are complete.
- Execution: inline with `superpowers:executing-plans`, TDD, and one independent
  final reviewer when supported. Delegated implementation is not authorized.
- Branch: `feature/link-creation-and-redirection`, based on `origin/main`
  commit `ecffb917a489cd18770127ede3b1a96f3a0debfb`.
- Completed implementation records: [plan](plan.md) and [checklist](todo.md).

## Authority and outcome

Signed-in users can turn an HTTPS destination into a copyable short URL,
optionally selecting an alias and expiry. Anyone can follow a known active,
unexpired link without signing in.

This specification follows [AGENTS.md](../../../AGENTS.md),
[Functional.md](../../requirements/Functional.md),
[Non-Functional.md](../../requirements/Non-Functional.md),
[architecture.md](../../architecture/architecture.md),
[ADR 0001](../../decisions/0001-frontend-stack.md),
[ADR 0002](../../decisions/0002-backend-stack.md),
[ADR 0003](../../decisions/0003-production-infrastructure-and-delivery.md),
[DESIGN.md](../../../DESIGN.md), and the existing
[authentication contracts](../account-authentication/contracts.md).

The [Codex Feature Development Workflow](https://www.notion.com/p/rahultsx/Codex-Feature-Development-Workflow-3db1dfa233d181c5825feeeae73c7ce5)
was read through the connected Notion integration on 2026-10-07. Its reference
to an AWS backend is outdated. Current project guidance and ADR 0003 remain
authoritative: Hostinger runs Spring Boot and private Redis, Supabase supplies
PostgreSQL, and Vercel hosts Next.js. This feature authorizes no production
provisioning or deployment.

## Scope

Include authenticated creation, authoritative destination validation,
self-reference rejection, generated codes, optional custom aliases, optional
expiry, copy feedback, public redirects and safe outcome pages, per-account
creation limits, and per-client redirect throttling. Honor existing disabled,
deleted, and expired link state and existing account-deletion tombstones.

Exclude analytics, click increments, click/event capture, visitor classification,
geolocation, link-management APIs or UI, new authentication behavior, destination
blocklists, safe-browsing checks, API keys, public programmatic APIs, and admin
tools. Do not add list, edit, disable, enable, or delete-link endpoints. Disabled
and deleted fixtures can be established directly in disposable test databases.

Reuse existing authentication, CSRF bootstrap, session renewal, account deletion,
and HTTP client behavior. Navigation to the new creation page is the only
required change to existing authentication UI.

## Acceptance criteria

| ID    | Observable behavior                                                                                                                                                                                                                                                                                                                                                                      | Requirement trace                                        |
| ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| AC-01 | A signed-in user creates `https://example.com/docs?q=java#setup`, receives a unique complete short URL, copies it with one action, and the next request redirects to that exact destination.                                                                                                                                                                                             | FR-CRE-01/04/05; NFR-CON-01, NFR-REL-01                  |
| AC-02 | Missing, invalid, expired, revoked, wrong-version, or deleted-user access credentials never authorize creation. The UI prompts sign-in after an authentication rejection. Missing/invalid CSRF rejects the mutation.                                                                                                                                                                     | FR-CRE-02; NFR-SEC-06/09; architecture security contract |
| AC-03 | Malformed, non-HTTPS, and self-host destinations produce actionable field errors and no link row.                                                                                                                                                                                                                                                                                        | FR-CRE-03/06; NFR-SEC-05/08                              |
| AC-04 | Available valid aliases succeed. Invalid/reserved aliases fail validation; taken aliases conflict. Redis-counter allocation is atomic; fixed-key/salt FF1 maps distinct values to distinct codes without truncation; concurrent inserts, missing-key recovery, stale Redis restores, and alias/legacy collisions never overwrite another row. Deleted and expired codes remain reserved. | FR-CRE-04/07; FR-RED-06; functional assumptions 5/6      |
| AC-05 | Expiry is optional. At `now >= expiresAt`, a link never redirects, including from a previously populated cache.                                                                                                                                                                                                                                                                          | FR-CRE-08, FR-RED-07                                     |
| AC-06 | Anonymous GET resolves an exact, case-sensitive code to the stored destination, including its path, query, fragment, and encoding.                                                                                                                                                                                                                                                       | FR-RED-01/02/06; NFR-REL-01                              |
| AC-07 | Unknown, unallocated case variants, deleted, and expired codes return generic not-found HTML. Disabled, unexpired links return generic unavailable HTML. Neither response exposes a destination or owner or has `Location`.                                                                                                                                                              | FR-RED-03 through FR-RED-07; NFR-SEC-08                  |
| AC-08 | Creation over the hourly account cap and redirects over the client cap return 429 with accurate retry guidance. One client's traffic does not consume another client's redirect budget.                                                                                                                                                                                                  | FR-ABS-01/03; NFR-PRV-02                                 |
| AC-09 | Redis cache failure or invalid cache data falls back to PostgreSQL. Redis limiter failure activates bounded local redirect throttling. Indeterminate link state returns safe 503 without `Location`. Creation fails safely if the code counter or required recovery query cannot complete; it has no random/local generation fallback.                                                   | NFR-REL-01/02; FR-CRE-04                                 |
| AC-10 | Measured server-side creation p95 is below 500 ms. A complete run sustains 100 redirects/second for 600 seconds with p95 below 150 ms, p99 below 300 ms, and errors below 0.5%.                                                                                                                                                                                                          | NFR-PER-01/02/03, NFR-TST-03                             |
| AC-11 | Creation and account deletion serialize correctly: deletion either tombstones a committed creation or prevents creation for the deleted owner. Existing deletion cleanup and the five-second cache bound remain effective.                                                                                                                                                               | Existing FR-ACC-05; NFR-SEC-09, NFR-CON-02               |
| AC-12 | The create/copy flow works with keyboard and screen reader, both themes, reduced motion, and widths 320, 768, 1024, and 1440 px. Loading, validation, conflict, rate-limit, session, clipboard, and service failures are understandable.                                                                                                                                                 | FR-CRE-02/05; DESIGN.md                                  |

AC-01's immediate-resolution guarantee assumes the request precedes any chosen
expiry. Expiry always wins when reached. An allocated `Abc` never falls back to
`abc`. Independently allocated case variants are distinct codes; a case variant
returns 404 when its exact spelling has not been allocated. This follows the
existing case-sensitive PostgreSQL namespace.

## Architecture and responsibilities

Retain Java/Spring Boot, Spring MVC/Security, Jakarta Bean Validation,
JPA/Hibernate, PostgreSQL, Redis, Flyway, Maven, and the layer-first packages.
Use the existing frontend stack, HTTP client, React Hook Form/Zod, and TanStack
Query. No Next.js route handler or Server Action proxies application APIs or
implements public redirects. No new dependency is needed for the approach.

| Component                                    | Responsibility and dependencies                                                                                                                                                                                                                                              |
| -------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `LinkController`                             | Translate authenticated POST DTOs, take identity from the verified principal, call the creation rate limit and `LinkService`, and map the committed result to JSON.                                                                                                          |
| `LinkService`                                | Own creation rules and transactions; retain existing account-deletion tombstoning. Depend on repository/cache contracts and local domain values, not Redis/JPA types or `AuthService`.                                                                                       |
| `RedirectController`                         | Translate public GET/HEAD, invoke redirect throttling and `RedirectService`, and map outcomes to a temporary redirect or static safe HTML.                                                                                                                                   |
| `RedirectService`                            | Own exact-code resolution, cache validation, freshness/expiry checks, PostgreSQL fallback, and state outcomes. No user/session/authentication or analytics calls.                                                                                                            |
| `LinkRepository` and `Jpa*` implementation   | Persist without overwrite; serialize eligible-owner insertion with account deletion; fetch a minimal redirect projection by exact code. Keep existing tombstone/cleanup operations.                                                                                          |
| `ShortCodeGenerator`                         | Obtain allocation values through `ShortCodeCounter`, recover from the committed repository high-water mark only on a missing key or confirmed collision, and return `GeneratedShortCode`. Concrete coordinator using a concrete local ShortCodeEncoder, no helper interface. |
| `ShortCodeCounter` / `RedisShortCodeCounter` | Atomic validated increment and initialize/advance-and-increment of nonexpiring `code:global`; Redis commands and serialization stay in the adapter. No database or link-policy dependency.                                                                                   |
| `RedirectCache` / `RedisRedirectCache`       | Extend the existing eviction contract with lookup and bounded writes. Redis access and serialization stay in the adapter.                                                                                                                                                    |
| `RateLimitService` / `RateLimitStore`        | Retain existing auth budgets; add separate creation and redirect namespaces and the redirect-only bounded fallback. Redis commands stay behind the store contract.                                                                                                           |
| Security configuration and filters           | Protect creation with existing JWT/revocation/user-state and CSRF checks. Admit only the public redirect methods/path and omit authentication work on that route.                                                                                                            |
| Next.js `/links`                             | Render session/API outcomes and the focused creation form, typed mutation, and clipboard feedback.                                                                                                                                                                           |

Domain values `DestinationUrl`, `ShortCode`, `GeneratedShortCode`, and
`RedirectLookup` hold relevant invariants. The generator remains concrete and
depends on repository/store interfaces at the PostgreSQL/Redis boundaries;
inject those interfaces for deterministic tests. Keep HTTP
responses out of services. Add no feature-specific Java packages or empty folders.

## Creation HTTP contract

`POST /api/links` accepts UTF-8 `application/json` with credentials and
`X-CSRF-TOKEN` from the existing CSRF endpoint. The principal supplies the owner
UUID and current token version; the body cannot choose an owner.

```json
{
  "destinationUrl": "https://example.com/docs?q=java#setup",
  "alias": "java-docs",
  "expiresAt": "2027-01-01T00:00:00Z"
}
```

`destinationUrl` is required. `alias` and `expiresAt` may be omitted or null.
An empty alias string is invalid; the frontend omits its unfilled optional
field. These values must be JSON strings when present and non-null; do not
coerce numbers, booleans, arrays, or objects into strings. Unknown fields are
rejected. The existing 16 KiB API body limit applies
to declared-length and streamed requests (413 on overflow).

After PostgreSQL commits, return 201 with `Cache-Control: no-store`:

```json
{
  "id": "8c18f020-5d8d-4e56-9406-f4f19cfdf8d7",
  "code": "java-docs",
  "shortUrl": "https://go.tinyroute.example/java-docs",
  "destinationUrl": "https://example.com/docs?q=java#setup",
  "createdAt": "2026-10-07T12:00:00.000Z",
  "expiresAt": "2027-01-01T00:00:00.000Z"
}
```

The host above illustrates configuration and is not an allocated production
domain. `expiresAt` is always present in the response, with null for no expiry.
Return no owner identifier or analytics fields. The short URL comes exclusively
from validated server configuration, never request Host/forwarded-host headers
or frontend string construction. There is no management-resource `Location`
header because no GET-by-ID API is introduced.

Use the existing `ApiErrorResponse` envelope, extending the shared frontend
error-code parser for the two new conflict codes:

| HTTP | Error code               | Meaning                                                                                                                                       |
| ---- | ------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------- |
| 400  | `VALIDATION_ERROR`       | Invalid destination, alias, expiry, JSON, or unknown fields; safe `fieldErrors` identify relevant fields.                                     |
| 401  | `AUTHENTICATION_FAILED`  | Credentials cannot authorize creation, including an owner that became ineligible before insertion.                                            |
| 403  | `CSRF_INVALID`           | Required CSRF protection failed; do not consume creation quota or write a link.                                                               |
| 409  | `ALIAS_UNAVAILABLE`      | Exact alias already exists in any link state. Message: "This alias is already in use."                                                        |
| 409  | `CODE_ALLOCATION_FAILED` | Generated-code allocation exhausted its bounded attempts or numeric capacity. Message: "We couldn't allocate a short code. Please try again." |
| 413  | `VALIDATION_ERROR`       | Request body exceeds the existing API limit.                                                                                                  |
| 429  | `RATE_LIMITED`           | Hourly creation budget exhausted; include retry guidance.                                                                                     |
| 503  | `SESSION_UNAVAILABLE`    | Existing authentication/revocation verification could not complete.                                                                           |
| 503  | `SERVICE_UNAVAILABLE`    | Creation limiter, code counter, or PostgreSQL cannot complete the operation, including required counter recovery.                             |

Responses reveal no owner details, SQL, stack traces, exception messages, or
tokens. If authentication and CSRF are both absent, Spring's established filter
ordering may reject with 403 first; neither failure authorizes or creates a link.

## Validation and allocation decisions

### Destination

Accept an absolute ASCII HTTPS URI of at most 8192 characters with a valid DNS
hostname, IPv4 address, or bracketed IPv6 address. Internationalized hostnames
use ASCII/punycode and non-ASCII path/query/fragment characters use percent
encoding. Scheme comparison is case-insensitive. A specified port must be
1–65535; omitted ports are valid.

Reject missing hosts, malformed escapes/authorities, user-info credentials,
literal whitespace/control characters, and backslashes. Do not trim or rewrite
the submitted destination. Preserve its original path, query, fragment, case,
and percent encoding in persistence and the successful redirect header.

Compare parsed hostname to the configured short-link hostname after lowercasing
and removing a trailing DNS dot. Canonicalize IP literals without DNS lookup;
reject alternative numeric IPv4 spellings or other ambiguous authorities that
a browser could interpret as a different hostname. Reject an exact self-host
match regardless of port, path, scheme spelling, or host case. Do not compare
hostname suffixes or search for the host inside arbitrary text. An encoded/ambiguous authority that
cannot pass the host parser is invalid. Never fetch or DNS-resolve destinations
as part of validation. Private-address destinations are not a new blocklist.

Examples of safe field messages: "Enter a valid HTTPS URL.", "URLs containing
credentials are not supported.", "Destination must not use TinyRoute's
short-link host.", and "Destination must be 8192 characters or fewer."
No rejected input is interpolated into an error page or log.

### Alias and generated code

- Aliases are 3–64 ASCII characters, matching `[A-Za-z0-9_-]+`.
- Preserve alias case. Whitespace, slashes, dots, percent escapes, and Unicode
  are invalid; do not silently trim, lowercase, or transliterate.
- Reserve these entire words, case-insensitively: `api`, `actuator`, `error`,
  `health`, `login`, `logout`, `register`, `links`, `settings`, `analytics`,
  `account`, and `password-reset`. Return a validation error for a reserved word.
- Allocate a positive Redis counter value `1..218340105584895` (`62^8 - 1`).
  Convert it into eight radix-62 digit bytes, including leading zero digits,
  encrypt those digits with AES-FF1 under fixed key/salt configuration, and map
  the output using `0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz`.
  Publish exactly eight characters, never the unencoded counter. No hashing or
  truncation. FF1 is a bijection for fixed key/salt: distinct allocations do not
  collide through encoding. Reject out-of-range allocations; never wrap.
  Generated candidates still pass format/reserved-word checks; alias, legacy,
  and recovery conflicts remain bounded.
- Allow at most ten generated candidates per request. Retry only a confirmed
  code uniqueness conflict; other datastore failures return safe 503. A custom
  alias conflict returns 409 immediately and is never replaced by a generated code.
  Skip a reserved generated candidate within that same ten-candidate budget;
  exhaustion also returns `CODE_ALLOCATION_FAILED`.
- PostgreSQL's existing unique constraint is the final arbiter. An availability
  precheck cannot authorize an overwrite. Use conflict-safe insertion or isolated
  insertion transactions so a conflict cannot leave later retries in an aborted
  transaction. Never use an upsert that updates an existing row.

### Redis counter and recovery

Use the existing private Redis instance, key `code:global`, with no TTL. On the
normal path, one atomic Lua operation validates the initialized nonnegative
integer and nonexpiring key, then increments. Encode that value and attempt the
database insert; do not query code availability or the committed allocation
high-water mark first. A custom alias makes no code-counter call.

Only if the key is missing, or after a confirmed generated-code insert conflict,
read `COALESCE(MAX(generation_value), 0)` through `LinkRepository`, over all
retained rows regardless of state or expiry. Atomically initialize a missing key
or advance an existing one to `max(current, committedFloor)`, then increment in
the same Lua operation. Concurrent recovery never lowers a counter; retain the
database unique constraint to cover allocations still in flight during recovery.
Each conflict consumes one of the ten candidates; do not allocate an eleventh
candidate after the final failure. Reserved candidates also consume the budget.

The nullable numeric allocation is committed with the link, not before or after
its transaction. Existing random-generated rows and aliases remain unchanged
with NULL metadata, so they cannot incorrectly raise or exhaust the recovery
floor. In particular, never decode all existing code strings as counter values.
Collisions with those rows still use bounded conflict-safe insertion. Expired
rows and permanent deletion tombstones retain their numeric metadata, preventing
reuse after a missing or stale restored counter (FR-CRE-04; assumptions 5/6).

Redis command/connection failures, malformed/noninteger/negative/out-of-range
stored values, wrong-type values, an unexpected key TTL, or a failed recovery
query return safe 503 without inserting a row. A valid counter at its maximum
returns `CODE_ALLOCATION_FAILED` without incrementing or wrapping. No random or
process-local fallback is introduced. Redis uses the existing bounded timeouts;
the counter is not called during public redirect resolution. Rollback and skipped
candidates may leave gaps; no published code is ever overwritten or reused.

Salted FF1 encoding hides the counter sequence and retains eight characters.
A numeric salt/alphabet shuffle alone remains predictable; truncated HMAC
permits generated collisions. Public codes still have a finite namespace and
are not secrets or authorization. This revision aligns allocation with
[HLD.excalidraw](../../architecture/HLD.excalidraw); it does not remove the required
PostgreSQL insert or unique constraint. The original random implementation
already avoided a separate existence SELECT.

### Salted encoder and key lifecycle

Concrete `service/ShortCodeEncoder` uses existing Bouncy Castle 1.86
`FPEFF1Engine`, AES, radix 62, eight digit bytes, and
`FPEParameters(KeyParameter(key), 62, saltBytes)`. Construct an engine per call;
never share mutable cipher state, truncate a hash, or implement a custom cipher.
Use no inverse AES option. A failure to encode a valid allocation returns safe 503 with no insert or fallback.
No new dependency. See
[the FF1 implementation API](https://downloads.bouncycastle.org/java/docs/bcprov-jdk18on-javadoc/org/bouncycastle/crypto/fpe/FPEFF1Engine.html),
[NIST SP 800-38G](https://csrc.nist.gov/pubs/sp/800/38/g/upd1/final), and
[the current revision draft](https://csrc.nist.gov/pubs/sp/800/38/g/r1/2pd).
The `62^8` domain exceeds the draft's one-million minimum; do not claim
certification or describe the draft as a final standard.

Add `tinyroute.links.code-key` from `SHORT_CODE_KEY`, canonical Base64 for exactly
32 random bytes, and `tinyroute.links.code-salt` from `SHORT_CODE_SALT`, 8–64
ASCII `[A-Za-z0-9:_-]+` characters. Production requires both environment values
with no defaults. Dev defaults and `.env.example` may contain explicitly public
local-only fixtures. Reject missing/malformed configuration at startup with
safe errors that never echo it. Keep the key secret and separate from JWT/rate-
limit keys (NFR-SEC-03). The salt is a fixed FF1 tweak; the secret key hides the
sequence.

Keep key, salt, alphabet, width, and encoder version fixed across deployments
and creators. Per-link changes break the shared one-to-one mapping. Rotation
is outside this feature and needs a separate version/namespace and recovery
design. Store only the final code and nullable internal `generation_value`;
no digest or per-link key/salt column. Public redirects look up the stored code,
without decoding or invoking the encoder. PostgreSQL still protects aliases,
legacy codes, and counter-recovery races.

### Expiry

Accept ISO-8601 date/time with an explicit `Z` or numeric UTC offset and at most
millisecond precision, within years 0001–9999. Normalize the instant to UTC;
store and return the same millisecond-precise instant. There is no product-level
maximum horizon. Reject date-only, offset-free, unparseable, excessive-precision,
past, or equal-to-now values with an actionable `expiresAt` field error.

Validate that expiry is strictly future after any owner-lock wait and immediately
before insertion. It may naturally be reached while a request completes; the
resolver still applies the exact `now >= expiresAt` boundary with no grace period.
Use injectable UTC clocks for deterministic boundary tests. The UI labels the user's local timezone,
converts its local date/time to an explicit instant, and displays the returned
expiry. Browser checks improve feedback; Spring validation remains authoritative.

## Persistence and write consistency

Extend the existing `links` entity/table and repository contracts. V4 already
provides UUID identity, `code varchar(64) collate "C" UNIQUE`, owner FK,
destination, status, timestamps, `deleted_at`, and `expires_at`; V5 supports
bounded account-deletion cleanup. Add
`V6__add_link_generation_value.sql`: nullable `generation_value BIGINT`, a check
restricting non-NULL values to `1..218340105584895`, and a partial index over
non-NULL values for the recovery MAX query. Map the nullable value on the existing
JPA entity and include it in the creation insert. Existing random rows and aliases
retain NULL; there is no code rewrite or new table. Never edit V1–V5.

New rows are ACTIVE, have no `deleted_at`, and retain the existing zero default
for `click_count`. This feature never increments that column or writes click
events. Preserve expired rows and deletion tombstones permanently so uniqueness
also reserves those codes. Do not introduce background expiry deletion.

The repository's creation operation must lock the owning `users` row in a mode
that conflicts with account deletion and token-version updates, check its
non-deleted state/current token version, and hold the lock through insertion and
commit. Perform that fence through the link persistence boundary; `LinkService`
does not call `AuthService` or add a `UserRepository` dependency. A JPA-backed
adapter may use a parameterized native query for the atomic persistence contract.

The existing account-deletion transaction already locks that row before
tombstoning links. Therefore a committed creation is included in later deletion,
or a creation following deletion is rejected. Acquire the owner lock before the
code insertion to preserve lock ordering. Do not change the account lifecycle or
authentication contract to implement this consistency fence.

Commit before returning success. After commit, attempt to evict `redirect:{code}`
through `RedirectCache`. Cache eviction failure does not turn a committed create
into a failure. Unknown codes are never cached, and non-reused codes cannot have
a legitimate previous entry, so a new link resolves from PostgreSQL immediately
even when cache eviction fails. No blind automatic mutation retries are added;
after an ambiguous network failure the UI explains that creation may have
succeeded and asks for a deliberate user action. General idempotency keys are
outside scope.

## Public redirect HTTP contract

Spring Boot handles `GET /{code}` and its normal `HEAD` equivalent. The production
ingress sends the go host directly to Spring; Next.js has no corresponding
route. The development HTTPS backend can serve both API and redirect paths.

Resolve codes only when the request hostname matches the configured short-link
hostname under the same hostname/IP canonicalization. An otherwise valid code
path on another host returns safe 404. This prevents the API hostname from
becoming an alternative short-link host that bypasses self-reference rejection;
the development API/short-link hostname may intentionally be the same.

Only a single literal ASCII code segment in the supported format can resolve.
Do not redirect through case folding, trailing-slash normalization, percent-
decoded alternate spellings, matrix parameters, or multi-segment paths.
Malformed/reserved codes do not resolve; existing security/firewall rejections
of malformed requests may use 400 instead of the generic application 404.
Do not broaden public access to `/api/**`, `/actuator/**`, error dispatches, or
non-GET/HEAD methods when making the redirect route public. Existing auth and
health permissions remain unchanged.

The redirect route ignores auth cookies and does not consult user/session or
revocation stores, even if an access cookie is manually supplied. It needs no
CSRF or ownership check. Incoming query parameters are ignored and never merged
into the stored destination. No analytics work follows a successful redirect.

| Known state                                                                                             | HTTP and content                                                       |
| ------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| ACTIVE, complete destination, before optional expiry                                                    | 302 with `Location` equal to the exact stored destination.             |
| Unknown exact code                                                                                      | 404 HTML: "Link not found".                                            |
| DELETED, or `deleted_at` is set                                                                         | 404 HTML: "Link not found".                                            |
| Expired at or before the current instant                                                                | 404 HTML: "Link not found".                                            |
| DISABLED and unexpired                                                                                  | 403 HTML: "Link unavailable"; signing in does not change this outcome. |
| Unrecognized/inconsistent/incomplete authoritative data, or PostgreSQL unavailable on a required lookup | 503 HTML: "Service temporarily unavailable".                           |
| Client redirect budget exhausted                                                                        | 429 HTML: "Too many requests" with `Retry-After`.                      |

Deleted and expired outcomes precede the disabled outcome. An ACTIVE record must
have a nonempty, valid stored destination; otherwise fail closed with 503.
Redirect validation uses the destination invariant, not browser URL rebuilding.
Only successful 302 has `Location`. Every outcome uses `Cache-Control: no-store`
so a browser or intermediary cannot keep redirecting after expiry or state
changes. HEAD has matching status/headers and no response body, and counts
against the same throttle. Successful redirects must use the stored Location
directly without MVC URL encoding/reconstruction changing it.

Outcome pages are small static Spring-rendered HTML with no new template-engine
dependency, JavaScript, third-party resources, destination, or owner data. Use
the design palette, accessible headings/body text, safe generic messages, and
existing security headers. Do not interpolate request codes or raw errors.

## Redirect cache contract

`redirect:{code}` contains a `RedirectLookup` with `schemaVersion` 1: exact code,
known status, optional destination, explicit nullable `expiresAt`, database-read
start instant `loadedAt`, and `validUntil`.
It contains no owner or analytics fields. ACTIVE requires a valid destination;
non-active entries expose no destination. Missing required fields, unknown
versions/statuses, wrong codes, inconsistent times, invalid destinations, or
invalid types are cache misses and never rescue a redirect.

On a miss, perform a short READ COMMITTED case-sensitive database lookup without
a user join; do not reuse an older transaction snapshot. Include
`deleted_at` in the database projection and map a set deletion marker to the
deleted outcome before caching. Record the database-read start instant as
`loadedAt`. Set `validUntil` no later than that instant plus five seconds,
further capped by `expiresAt`. Reject cache entries with future `loadedAt`,
`validUntil <= loadedAt`, or a deadline exceeding either permitted bound. At use,
independently check both freshness and link expiry. Round Redis TTL down to whole
milliseconds, skip a write if the remaining TTL is nonpositive, and never reset
freshness when reading a cache hit.

This deadline covers a read that began before a state change but fills the
cache afterwards: a delayed put cannot extend stale state beyond five seconds.
Do not cache unknown or already-expired results. A cache write/read failure
cannot override a known valid database result. Use the existing key namespace
and eviction method so account-deletion cleanup remains compatible.

An unexpired, structurally valid cache snapshot is usable if PostgreSQL is
temporarily unreachable. When the cache is absent, invalid, or stale, PostgreSQL
must determine state or the response is safe 503. The service never redirects
from a stale entry or a guessed destination. TTL gives existing state changes
their at-most-five-second consistency bound (NFR-CON-02); expiry has the stricter
instant boundary (FR-RED-07).

Redis operations must have bounded connection/command timeouts; use 100 ms
defaults for the co-located development/production topology. After a redirect
cache operation fails, skip further redirect-cache attempts for one second,
then allow a bounded recovery attempt. This local backoff holds no destination
data, leaves auth behavior unchanged, and requires no circuit-breaker dependency.
Document timeout tuning and verify normal auth behavior with these defaults.

## Rate limits and degraded behavior

The following are initial defaults proposed for written-spec approval. Expose
validated positive configuration properties; changing them does not move policy
into the frontend.

| Action          | Budget and key                                                   | Window                                     |
| --------------- | ---------------------------------------------------------------- | ------------------------------------------ |
| Creation        | 100 authorized attempts per account; `rl:create:{userId}`        | One hour from the first counted request.   |
| Public GET/HEAD | 600 attempts per HMAC-derived client; `rl:redirect:{clientHash}` | One minute from the first counted request. |

Use atomic Redis increment plus first-use TTL. Further attempts never extend
the window. Creation authentication and CSRF complete before its quota is used;
validation failures and alias conflicts after the quota check consume an
attempt. Count each external request once; internal generated-code retries do
not consume additional quota. Existing auth action budgets/keys do not change.
Redirect quota includes accepted-format requests with unknown or inactive codes.
Malformed paths cannot resolve and need not allocate limiter state.

429 returns integer `Retry-After` rounded up from the remaining window, at least
one second. The creation JSON `retryAfterSeconds` must match. Do not append a
destination or send Location on a throttled redirect.

Derive client hashes with the existing trusted-proxy resolver and HMAC secret.
Untrusted forwarded headers cannot choose the key. Never retain raw IPs or
client hashes in logs; Redis hashes expire with the one-minute window, well
within NFR-PRV-02's 24-hour ceiling. Invalid proxy/client-address metadata fails
safely with 503 without a destination.

When the redirect Redis limiter fails, use an in-process fallback with the same
per-client limit/window. It receives hashes only, supports at most 10,000 live
keys, uses a monotonic clock, and removes expired entries. At capacity, reject
an unknown key with 429 and retry guidance until capacity becomes available;
never evict an active counter to admit new traffic. Fallback entries must be
removed within two minutes of creation. Back off primary-limiter recovery
attempts for one second after a failure.

The fallback is best-effort protection for the accepted single-backend runtime:
process restart resets it, Redis-to-local transitions may grant a fresh window,
and multiple backend instances would require a revised outage strategy. This
does not change the authoritative link-state checks. Creation/authentication
have no local fallback and still fail closed when their Redis checks fail.

## Configuration and frontend behavior

Add `tinyroute.links.short-base-url`, supplied by `SHORT_LINK_BASE_URL`:
development defaults to `https://localhost:8443`; production requires an
environment value such as `https://go.<zone>`. Validate an exact HTTPS origin
with no user-info, path beyond an optional terminal slash, query, or fragment;
normalize only that terminal slash for short-URL composition. Require a valid
hostname and legal port. Derive self-host validation from this property.

Add creation/redirect limits under existing `tinyroute.rate-limit` configuration
with safe defaults, and document the Redis timeout settings. Keep shared/dev/prod
profile responsibilities and external profile activation. Add only safe local
examples; no production secrets, production Compose, deployment workflow, or
frontend container is created by this feature.

Implement a focused `/links` page, matching the existing fixed Google success
URI. Add a discoverable link from the homepage and the existing signed-in login
view. Keep navigation limited to useful existing destinations; add no empty
management or analytics screens.

Use server components for the layout and a small client boundary for session,
form, mutation, and clipboard interaction. Reuse semantic tokens, Geist fonts,
themes, Radix-based owned components when applicable, and Lucide icons. Preserve
the current design system rather than rewriting DESIGN.md.

The form has labeled destination, optional custom alias, and optional local
expiry inputs, helper text, inline errors, and one primary creation action.
Only session/API responses establish signed-in state. While session inspection
is pending show a status; if signed out show a clear sign-in link; a session
service failure shows a retryable error rather than claiming signed-out state.

Use the shared credentialed HTTP client and existing in-memory CSRF helper for
the typed TanStack Query mutation. Do not automatically retry POST. A 401
rechecks session through existing renewal behavior and offers sign-in or a
deliberate resubmission; it does not silently resubmit the original mutation.
Each manual creation fetches a fresh CSRF bootstrap and clears its cached token
afterwards, accommodating cookie rotation by the existing authentication flow.
No creation POST is automatically retried.

A CSRF failure clears the cached CSRF token and offers resubmission after a new
bootstrap. Quota/service/validation/conflict errors preserve form values and
explain the next action. Prevent accidental duplicate submits while pending.

Success displays the server-returned complete short URL and expiry, with a
44 px minimum copy button. Copy the exact response value using the Clipboard
API on the user's click and announce success only after it succeeds. On clipboard
denial retain a selectable complete URL and announce manual-copy guidance.
Do not construct a short URL from client settings. Long values wrap or can be
revealed without horizontal page overflow. Respect reduced motion and keyboard
focus; no toast, color, or icon is the only feedback.

## Verification and evidence

Use TDD for behavior: observe the expected failure, implement, observe success,
then refactor. Use existing JUnit/Mockito, MockMvc, real PostgreSQL/Redis
integration fixtures, Vitest/RTL, and Playwright. Keep tests in layer-aligned
Java packages. Stubbed browser contracts supplement a live Spring journey;
they do not prove database, authentication, or redirect integration.

Required evidence covers all acceptance criteria, including:

- Exact destination/Location preservation, creation read-after-commit, anonymous
  GET/HEAD, short-host routing, and absence of auth-store calls on public redirects.
- Missing/invalid/revoked/wrong-version/deleted-user JWT, missing/invalid CSRF,
  disallowed browser origin, owner injection, and the API body limit.
- Destination parsing/self-host variants; alias syntax/reserved/taken states;
  forced generated collisions and retry exhaustion; real concurrent alias
  inserts; deleted/expired code reservation; owner deletion/creation ordering.
- Fixed-key/salt FF1 encoding, independent decryption fixtures, unique sampled
  outputs, thread safety, range/capacity and key/salt validation/non-disclosure;
  concurrent atomic Redis allocation and
  missing-key initialization, recovery from deleted/expired generated values,
  stale restored counters, alias/legacy collisions without floor poisoning,
  safe malformed/wrong-type/TTL/outage behavior, and no recovery SELECT on normal
  allocation. Verify V6 against both fresh and existing schemas; no row/code
  rewrite. Counter conflicts must not consume extra quota or bypass expiry checks.
- Expiry just before/equal/after the boundary; capped cache TTL; stale fill
  racing a committed state change; cache reads that do not renew freshness;
  future/invalid cache deadlines and malformed payloads; Redis reads/writes
  failing; PostgreSQL failure when
  a lookup is required; generic error pages with no Location/owner/destination.
- Independent quotas, precise Retry-After, trusted/untrusted forwarded headers,
  Redis-down fallback bounds, capacity handling, expiry cleanup, and recovery.
- No click increments, events, or per-link metrics after redirects, including
  HEAD and error outcomes; regression coverage of existing account deletion.
- Live sign-in, create, copy, and follow without following external destinations
  in automated load runs; UI state and responsive/accessibility checks in AC-12.

Relevant implementation checks follow the current README and CI:

```bash
./backend/mvnw -f backend/pom.xml -B -ntp clean verify
./backend/mvnw -f backend/pom.xml -B -ntp spotless:check
pnpm --dir frontend lint
pnpm --dir frontend format:check
pnpm --dir frontend typecheck
pnpm --dir frontend test --run
pnpm --dir frontend build
pnpm --dir frontend exec playwright test
pnpm --dir frontend exec playwright test --config playwright.live.config.ts
```

Backend integration tests use disposable Testcontainers PostgreSQL/Redis with
Docker, never the owner's Compose database. Browser/live checks use documented
HTTPS setup and disposable accounts/data. At least 70% application line coverage
and complete relevant critical-path coverage remain required (NFR-TST-01/02).
No product build/test rerun is needed for this specification-only change.

### Performance protocol

Commit a reproducible load harness and record its commands and results in feature
verification evidence. Measure Spring server elapsed time across the full
application filter chain, including security, throttling, cache, persistence,
and response construction. Use route-template labels and operational status/time
measurements only: no raw code, destination, owner, IP, cookie, or analytics event
is recorded (NFR-OBS-01). Document clock/precision and percentile calculation.
Client round-trip latency can be reported separately but cannot substitute for
the server-side thresholds.

Run at least 200 successful authenticated counter-generated creation samples across enough fresh
accounts to stay under the unchanged per-account limit. Include normal security
and CSRF checks, Redis allocation, salted FF1 encoding, PostgreSQL persistence,
and no request retry.
Report p95 below 500 ms. Measurements from the former random generator cannot
establish the revised creation performance result.

For redirects, warm up for 30 seconds separately, then issue and complete the
600-second, 100 requests/second run against at least 100 fixture codes, without
following Location. Distribute requests across 100 client identities at about
one request/second each so the default throttle remains enabled and real normal
visitor behavior is represented. Use a test-only trusted proxy fixture to
provide those addresses; do not trust arbitrary forwarded headers or change
production trust configuration for the benchmark. Exercise normal cache expiry
and refills throughout the run.

Record offered, issued, and completed request counts, elapsed time, achieved
throughput, server p95/p99, status distribution, timeouts, errors, fixture count,
runtime/configuration, and hardware/datastore placement. Under-issuing load or
silently excluding failures cannot pass AC-10. The main run expects 302; every
non-302/timeout counts toward its error ratio. Expected 429 behavior belongs in
separate abuse tests. Retain correctness/outage evidence alongside performance
results.

Local disposable-datastore measurements prove the local result only. Production
latency with Hostinger and nearby Supabase remains a release verification item
until measured on that topology. Do not infer production compliance from local
results or provision production as part of this feature. If the required local
run/checks cannot be completed or fail, keep the work incomplete, its task files
active, and any PR draft.

## Workflow state and review focus

Baseline before specification: backend unit command
`./backend/mvnw -f backend/pom.xml -B -ntp -o test` passed 63 tests; frontend
`pnpm --dir frontend test --run` passed 48 tests in 13 files. These ran at the
base commit in the feature worktree. The backend needed execution outside the
sandbox because Mockito JVM attachment failed inside it. No application source
was changed to work around that environment restriction.

The existing feature branch is now checked out in the shared project workspace;
the initial temporary worktree is detached at the same base commit. The saved
specification was approved on 2026-10-07 and original plan on 2026-10-08.
Tasks 1–10, including Task 7A, are complete and verified. The
user requested Redis-counter alignment on 2026-10-08. This revised specification,
architecture, and the archived [plan](plan.md)/[checklist](todo.md) record the approved replacement
as Task 7A. The revised spec and plan were approved 2026-10-08 ("ok do now");
plain-counter implementation was authorized. The subsequent salt/uniqueness
request was approved as the AES-FF1 revision on 2026-10-08 ("done");
Independent feature review is complete; its minor report-output and stale-status
findings are resolved. The full local sustained-load protocol passed on
2026-10-10: 60,000/60,000 redirects, zero errors/drops/late arrivals, server
p95/p99 8.48/40.59 ms and creation p95 35.78 ms. Earlier failed reports remain
rejected; a lid-closed sleep interruption was identified and the passing session
had no overlapping sleep events. The local pass-through preserves exact backend
trust and TLS verification. Final evidence/readiness review is complete; its
minor status reconciliation is resolved. The completed task files are archived
beside this specification with checked boxes and corrected local links.

Final independent review must examine atomic counter allocation, recovery and
capacity, alias/legacy compatibility, namespace/collision safety, transaction
and deletion races, exact Location preservation, cache freshness/expiry,
public-route security isolation, degraded throttling bounds, privacy, frontend
error contracts, and actual performance evidence. Fix blocking findings before
claiming completion. Archive task files only when all acceptance criteria and
checks for this feature are complete; retain recorded production limitations.
Then commit, push, and create/update the PR with the repository template. Do not
merge, delete branches/worktrees, force-push, or open the PR page in a browser.

Specification self-review completed on 2026-10-07: all kickoff success criteria
have explicit acceptance criteria and verification; the stack, layer boundaries,
and exclusions are preserved; format/reserved-word/rate/expiry decisions are
explicit. Review clarified reserved generated-candidate exhaustion, short-host
routing and host canonicalization, expiry checks after owner-lock waits, deletion
markers in redirect projections, and original-read cache freshness. Local source
links and JSON examples were validated before committing.

Redis-counter revision self-review completed 2026-10-08: allocation/recovery
boundaries, V6 metadata, legacy/alias compatibility, concurrent initialization,
safe failure/capacity behavior, bounded candidate/quota handling, and revised
performance evidence are explicit and mapped to Task 7A. Stack, authentication,
UI, redirect behavior, analytics exclusion, and prior implementation evidence
remain intact. The plain-counter revision was approved 2026-10-08 ("ok do now"). The subsequent
salt/uniqueness revision was approved 2026-10-08 ("done"): existing FF1
implementation, no new dependency or truncation, explicit fixed-key/salt limits.
Task 7A implementation and focused regression verification are complete.
