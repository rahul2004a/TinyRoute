# Link Creation and Redirection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan inline, task by task, with `superpowers:test-driven-development`. Steps use checkbox (`- [ ]`) syntax. Delegation is authorized for one independent final reviewer, not implementation.

**Goal:** Let signed-in users create and copy HTTPS short links with optional aliases and expiry, and let anyone follow eligible links safely.

**Architecture:** Spring controllers translate HTTP; `LinkService` owns creation transactions and `RedirectService` owns resolution policy. Existing PostgreSQL uniqueness and owner locking protect writes; bounded Redis snapshots accelerate redirects, with PostgreSQL fallback and redirect-only local throttling. Next.js renders session and API results through the existing credentialed HTTP client.

**Tech Stack:** Existing Java 21 / Spring Boot 4.1.1 MVC, Security, JPA, PostgreSQL, Redis, Flyway, Maven, JUnit/Mockito/MockMvc/Testcontainers/JaCoCo; Node.js 24 LTS, Next.js App Router, React, strict TypeScript, Tailwind, Radix-backed shadcn/ui, Lucide, React Hook Form/Zod, TanStack Query, pnpm, Vitest/RTL, Playwright. No new dependency.

**Spec:** [Approved specification](../docs/spec/link-creation-and-redirection/spec.md), approved 2026-10-07. Read it together with this plan, [AGENTS.md](../AGENTS.md), requirements, architecture, ADRs, and [DESIGN.md](../DESIGN.md).

## Global Constraints

- Feature and branch: `link-creation-and-redirection`, `feature/link-creation-and-redirection`; base `ecffb917a489cd18770127ede3b1a96f3a0debfb`. Check branch, status, and active tasks before every resumed execution. Stop for unrelated changes or another feature's incomplete tasks.
- Current gate: approach, written specification and saved plan/checklist approved (plan approval 2026-10-08). Inline implementation authorized.
- Keep the locked stack, layer-first Java packages, repository/store interfaces, and existing authentication contract. Never put ownership, redirect policy, or API proxies in Next.js.
- Exclude all analytics, click increments/events, link-management endpoints/UI, auth implementation, blocklists, safe browsing, API keys, and admin tools. Existing `click_count` remains zero for new links.
- PostgreSQL is authoritative. Do not change applied migrations or remove expired/deleted rows. V4/V5 already support this feature; no migration is planned.
- Destinations: absolute ASCII HTTPS URI, at most 8192 characters, valid host/port, no credentials, whitespace, controls, backslashes, malformed escapes, or ambiguous numeric authorities; preserve the exact original string. No destination fetch/DNS lookup.
- Aliases: 3–64 ASCII `[A-Za-z0-9_-]+`, case-sensitive; reserve the entire words `api`, `actuator`, `error`, `health`, `login`, `logout`, `register`, `links`, `settings`, `analytics`, `account`, `password-reset`, case-insensitively.
- Generated codes: eight SecureRandom base62 characters; at most ten candidates, including skipped reserved candidates; never overwrite or reuse a code.
- Expiry: optional explicit-offset ISO-8601, years 0001–9999, at most millisecond precision, strictly future after owner-lock wait and immediately before insertion. `now >= expiresAt` never redirects.
- Creation: existing verified principal and CSRF; 16 KiB API body limit; 201 only after commit, no-store; no automatic POST retry.
- Redirects: Spring GET/HEAD, configured short hostname only, literal exact code, 302 exact stored Location, no-store for every outcome. Only successful 302 has Location. Cookies do not trigger auth work on the public route.
- Cache: schemaVersion 1, original database-read start, maximum five-second deadline capped by expiry; no unknown/expired cache entries, no freshness reset on hits. Redis TTL rounded down to milliseconds.
- Default limits: creation 100 authorized attempts/account/hour; redirect 600 attempts/HMAC-client/minute. Auth budgets unchanged. Retry guidance rounds up, minimum one second.
- Redis defaults: 100 ms connect/command timeouts; one-second redirect cache/limiter backoff with a bounded recovery probe. Redirect fallback: monotonic clock, at most 10,000 live keys, same limit/window, expired entries removed within two minutes, no eviction of active keys.
- UI: existing DESIGN.md palette/type/theme rules; keyboard/screen-reader feedback, 44×44 px targets, reduced motion, widths 320/768/1024/1440 in both themes. Read `frontend/AGENTS.md` and installed Next.js docs before frontend coding.
- Keep externally activated dev/prod profiles, root development Compose unchanged, production Hostinger/Supabase/Vercel boundaries, and secrets out of Git. No provisioning or deployment.
- Complete acceptance criteria, review, and relevant checks before archiving task files, pushing, and making a PR ready. If required local evidence is missing/failing, retain active tasks and use a draft PR. Never merge, delete branches/worktrees, force-push, or open the PR page in a browser.

## File and responsibility map

Paths below are exact planned files, not new feature packages. Java main paths have prefix `backend/src/main/java/com/tinyroute/`; Java test paths have prefix `backend/src/test/java/com/tinyroute/`. Task file lists spell out the paths for execution.

| Area                   | Files and responsibility                                                                                                                                                                                                                                                  |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Domain / validation    | `model/DestinationUrl.java`, `ShortCode.java`, `CreateLinkCommand.java`, `ValidatedLinkInput.java`, `CreatedLink.java`, `RedirectLinkState.java`, `RedirectLookup.java`, `RedirectOutcome.java`; concrete `service/LinkCreationValidator.java`, `ShortCodeGenerator.java` |
| Configuration          | `config/LinkProperties.java`, existing `RateLimitProperties.java`, three existing application YAML files; positive budgets and validated short origin                                                                                                                     |
| Persistence / creation | Extend existing `repository/LinkRepository.java`, `repository/jpa/JpaLinkRepository.java`, `service/LinkService.java`; native owner lock/conflict-safe insert, immutable redirect projection, existing tombstones                                                         |
| Rate limits            | Extend existing `service/RateLimitService.java`; concrete `service/InMemoryRedirectRateLimiter.java`, `cache/StoreFailureBackoff.java`; reuse existing Redis atomic counter and HMAC resolver                                                                             |
| Creation HTTP          | `controller/LinkController.java`, `dto/CreateLinkRequest.java`, `CreateLinkResponse.java`, `StrictStringDeserializer.java`; safe exceptions and shared error envelope/handler                                                                                             |
| Cache / resolution     | Extend `cache/RedirectCache.java`, `cache/redis/RedisRedirectCache.java`; `service/RedirectService.java` owns outcomes, adapter owns strict serialization/timeouts                                                                                                        |
| Public boundary        | `security/PublicRedirectRequestMatcher.java`; extend existing JWT filter/security configuration; `controller/RedirectController.java`, `RedirectPageRenderer.java` for constant HTML                                                                                      |
| Frontend               | `src/app/(account)/links/page.tsx`, `src/features/links/{link-api,link-create-form,link-create-result,link-form-schema}`; home/login navigation and scoped styles                                                                                                         |
| Runtime evidence       | Test-only Spring verification application/config/controller, shared Playwright SMTP fixture, contract/live browser tests, Node built-in load harness, operational timing filter, feature verification evidence                                                            |
| Lifecycle              | Active `tasks/plan.md`, `tasks/todo.md`; completed archive under `docs/spec/link-creation-and-redirection/`; existing PR template                                                                                                                                         |

## Dependencies and execution rules

Execute inline in order: **1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10**. Tasks 4 and 6 expose working HTTP slices; 8 verifies the cross-stack product; 9 proves the performance criterion. No implementation agents or routine checkpoint approval pauses after plan approval.

Every task follows red → observed expected failure → minimal implementation → green/refactor → focused verification → commit. A compile failure caused by a deliberately absent new type is acceptable initial red evidence; an infrastructure failure is not a behavioral red. Use `systematic-debugging` for unexpected failures. Record command/result, red/green evidence, commits, decisions, and unresolved issues in `tasks/todo.md` before moving on or context compaction. Do not claim passes without executing commands.

Each task gives the full Maven unit and integration commands with explicit test selectors. Focused integration checks invoke `test-compile failsafe:integration-test failsafe:verify` so a small test subset is not mistaken for the full application coverage gate. Task 10 runs full `clean verify` with Surefire, Failsafe, static checks and JaCoCo ≥70%. Do not weaken coverage or static checks.

## Task 1: Domain invariants and validated configuration

**Requirements:** FR-CRE-03/04/06/07/08; NFR-SEC-05/08; AC-03/04/05.

**Files — create:**

- `backend/src/main/java/com/tinyroute/config/LinkProperties.java`
- `backend/src/main/java/com/tinyroute/config/LinkConfiguration.java`
- `backend/src/main/java/com/tinyroute/model/DestinationUrl.java`
- `backend/src/main/java/com/tinyroute/model/ShortCode.java`
- `backend/src/main/java/com/tinyroute/model/CreateLinkCommand.java`
- `backend/src/main/java/com/tinyroute/model/ValidatedLinkInput.java`
- `backend/src/main/java/com/tinyroute/model/CreatedLink.java`
- `backend/src/main/java/com/tinyroute/model/RedirectLinkState.java`
- `backend/src/main/java/com/tinyroute/service/LinkCreationValidator.java`
- `backend/src/main/java/com/tinyroute/service/ShortCodeGenerator.java`
- `backend/src/main/java/com/tinyroute/exception/LinkValidationException.java`

**Files — modify:** `backend/src/main/resources/application-dev.yml`, `application-prod.yml`; `backend/src/test/java/com/tinyroute/ProfileConfigurationTest.java`.

**Tests — create:** `backend/src/test/java/com/tinyroute/model/DestinationUrlTest.java`, `ShortCodeTest.java`; `backend/src/test/java/com/tinyroute/service/LinkCreationValidatorTest.java`, `ShortCodeGeneratorTest.java`; `backend/src/test/java/com/tinyroute/config/LinkPropertiesTest.java`.

**Interfaces:** Consumes existing `LinkStatus`, `AccessToken`, externally selected profiles. Produces:

```java
record DestinationUrl(String value) {
    static DestinationUrl parse(String value, String shortHost);
    static String canonicalHost(String host);
}
record ShortCode(String value) {
    static ShortCode forAlias(String value);
    static boolean isReserved(String value);
}
record CreateLinkCommand(String destinationUrl, String alias, String expiresAt) {}
record ValidatedLinkInput(DestinationUrl destinationUrl, ShortCode alias, Instant expiresAt) {}
record CreatedLink(UUID id, ShortCode code, DestinationUrl destinationUrl,
                   Instant createdAt, Instant expiresAt) {}
record RedirectLinkState(String code, LinkStatus status, String destinationUrl,
                        Instant deletedAt, Instant expiresAt) {}
// All declared API methods are public; domain values enforce format invariants.
// LinkProperties binds tinyroute.links.short-base-url, exposes these methods:
String getShortBaseUrl(); void setShortBaseUrl(String value);
String shortHost(); String shortUrl(ShortCode code);
// Concrete helpers, no helper interfaces:
ValidatedLinkInput LinkCreationValidator.validate(CreateLinkCommand input, Instant now);
void LinkCreationValidator.requireFutureExpiry(Instant expiresAt, Instant now);
ShortCodeGenerator(SecureRandom random);
String ShortCodeGenerator.nextCandidate();
LinkCreationValidator(LinkProperties properties);
Map<String, String> LinkValidationException.fieldErrors();
```

- [x] **1.1 Write domain/config tests first.** Use `now = 2026-10-07T12:00:00Z`, configured origin `https://go.tinyroute.test`. Include exact preservation, HTTPS scheme case, ports, DNS dot/case, punycode, bracketed IPv6 and equivalent self-IP spelling; reject whitespace/controls/user-info/invalid escapes, bad ports, octal/hex/short/integer IPv4, encoded authority, Unicode and oversized values. Test all alias bounds/reservations and deterministic base62 length. Test omitted/null expiry, offsets, millisecond precision, years, equality/past and owner-wait expiry through `requireFutureExpiry`.

```java
assertThat(DestinationUrl.parse("https://example.com/docs?q=java#setup", "go.tinyroute.test").value())
        .isEqualTo("https://example.com/docs?q=java#setup");
assertThatThrownBy(() -> DestinationUrl.parse("https://GO.TINYROUTE.TEST.:443/a", "go.tinyroute.test"))
        .isInstanceOf(LinkValidationException.class);
assertThat(ShortCode.forAlias("Abc").value()).isEqualTo("Abc");
assertThatThrownBy(() -> ShortCode.forAlias("API")).isInstanceOf(LinkValidationException.class);
```

- [x] **1.2 Run red:** `./backend/mvnw -f backend/pom.xml -B -ntp test -Dtest=DestinationUrlTest,ShortCodeTest,LinkCreationValidatorTest,ShortCodeGeneratorTest,LinkPropertiesTest,ProfileConfigurationTest`. Expected absent-type compilation failure, then behavioral failures as classes appear. Record the actual failure.
- [x] **1.3 Implement the values/config/helpers.** Parse with `URI` and explicit ASCII authority/port rules; canonicalize only validated IP literals without DNS. Keep `DestinationUrl.value()` unchanged. Parse expiry with explicit offset, reject fractional precision over three digits and UTC instants outside the stated years. Generate using `SecureRandom.nextInt(62)` for each of eight characters. `LinkConfiguration` provides `Clock linkClock()` returning `Clock.systemUTC()` and `ShortCodeGenerator shortCodeGenerator()` using a fresh SecureRandom; link services/adapters consume the injectable Clock. Bind dev `${SHORT_LINK_BASE_URL:https://localhost:8443}` and prod `${SHORT_LINK_BASE_URL}`; fail startup for invalid origins.

```text
validated = { destination: DestinationUrl.parse(raw, configuredShortHost),
              alias: null or ShortCode.forAlias(rawAlias),
              expiry: null or parseExplicitOffsetMillis(rawExpiry) }
requireFutureExpiry(validated.expiry, now)
shortUrl = validatedOriginWithoutTerminalSlash + "/" + code.value()
```

- [x] **1.4 Run the same focused command green, refactor, and run Spotless on touched Java.** Expected domain/config cases pass, existing profile assumptions updated without hard-coded active profile.
- [x] **1.5 Record evidence and commit:** stage only the listed files plus `tasks/todo.md`; `git commit -m "feat: define link creation invariants and configuration"`.

## Task 2: Atomic creation and the account-deletion fence

**Requirements:** FR-CRE-01/04/07/08; FR-ACC-05; NFR-CON-01/02, NFR-REL-01; AC-01/04/05/11.

**Files — modify:** `backend/src/main/java/com/tinyroute/repository/LinkRepository.java`, `repository/jpa/JpaLinkRepository.java`, `service/LinkService.java`; existing `backend/src/test/java/com/tinyroute/service/LinkServiceTest.java`, `repository/LinkRepositoryIT.java`, `controller/AccountDeletionIT.java`.

**Files — create:** `backend/src/main/java/com/tinyroute/exception/AliasUnavailableException.java`, `CodeAllocationFailedException.java`; `backend/src/test/java/com/tinyroute/service/LinkCreationIT.java`.

**Interfaces:** Consumes Task 1 values, existing `AccountDeletionCleanupRepository`, `RedirectCache.evict(String)`. Extend repository while preserving both existing cleanup methods:

```java
Optional<Integer> LinkRepository.lockActiveOwnerTokenVersion(UUID ownerId);
int LinkRepository.insertIfCodeAvailable(UUID id, String code, UUID ownerId,
        String destinationUrl, Instant createdAt, Instant expiresAt); // 1 inserted, 0 collision
Optional<RedirectLinkState> LinkRepository.findRedirectStateByCode(String code);
CreatedLink LinkService.create(AccessToken principal, CreateLinkCommand command);
void LinkService.tombstoneOwnedLinks(UUID userId); // existing signature unchanged
```

- [x] **2.1 Write failing service/repository tests.** Assert one custom insert or bounded generated retries, ten-candidate exhaustion, no fallback for alias conflict, unrelated DB failure → safe service-unavailable, correct response identity/timestamps, click_count zero. Use real PostgreSQL transactions and latches to force two simultaneous same-alias attempts, generated collisions, permanent expired/deleted reservations, independently allocated `Abc`/`abc`, and both owner-lock/deletion orderings. Assert at most one winning row and never an overwritten destination. Add expiry crossing while waiting for the owner lock.

```java
// Repository integration test inside a TransactionTemplate transaction; ownerId is a fresh fixture user.
assertThat(linkRepository.lockActiveOwnerTokenVersion(ownerId)).contains(0);
assertThat(linkRepository.insertIfCodeAvailable(UUID.randomUUID(), "Abc", ownerId,
        "https://example.com/first", now, null)).isEqualTo(1);
assertThat(linkRepository.insertIfCodeAvailable(UUID.randomUUID(), "Abc", ownerId,
        "https://example.com/second", now, null)).isZero();
assertThat(linkRepository.findRedirectStateByCode("Abc").orElseThrow().destinationUrl())
        .isEqualTo("https://example.com/first");
```

- [x] **2.2 Run red:** unit `./backend/mvnw -f backend/pom.xml -B -ntp test -Dtest=LinkServiceTest` and focused integration `./backend/mvnw -f backend/pom.xml -B -ntp test-compile failsafe:integration-test failsafe:verify -Dit.test=LinkRepositoryIT,LinkCreationIT,AccountDeletionIT`. Expected absent creation methods or failure of race/conflict assertions; never accept Docker/startup failure as red.
- [x] **2.3 Implement native parameterized persistence and cohesive `LinkService.create`.** Lock the owner first, compare current persisted version to principal, validate after wait, and hold through commit. Native `ON CONFLICT (code) DO NOTHING` returns zero solely for the existing code constraint and keeps the transaction usable. Build the read projection without a user join. Recheck expiry using injected UTC `Clock` immediately before each insertion. Retain existing deletion cleanup semantics. Register after-commit eviction; catch its failure without changing a committed success, and never log code/owner/destination.

```sql
select token_version from users where id = :ownerId and deleted_at is null for update;
insert into links (id, code, owner_id, destination_url, status, click_count,
                   created_at, updated_at, expires_at)
values (:id, :code, :ownerId, :destinationUrl, 'ACTIVE', 0,
        :createdAt, :createdAt, :expiresAt)
on conflict (code) do nothing;
```

- [x] **2.4 Run focused checks green and repeat the deterministic concurrency scenarios.** Assert a returned create is visible from a separate transaction; deletion wins → authentication rejection, creation wins → later tombstone. No test relies solely on sleep for race ordering; timeouts bound latch/future waits.
- [x] **2.5 Record evidence and commit:** `git commit -m "feat: persist links atomically with owner eligibility fencing"` after staging listed files and ledger.

## Task 3: Creation budgets and bounded degraded redirect throttling

**Requirements:** FR-ABS-01/03; NFR-REL-02, NFR-PRV-02; AC-08/09.

**Files — modify:** `backend/src/main/java/com/tinyroute/service/RateLimitService.java`, `config/RateLimitProperties.java`, `config/LinkConfiguration.java`, `exception/GlobalExceptionHandler.java`; `backend/src/test/java/com/tinyroute/cache/RateLimitServiceTest.java`, `cache/RedisAuthStoresIT.java`, `exception/GlobalExceptionHandlerTest.java`.

**Files — create:** `backend/src/main/java/com/tinyroute/service/InMemoryRedirectRateLimiter.java`, `cache/StoreFailureBackoff.java`; `backend/src/test/java/com/tinyroute/service/InMemoryRedirectRateLimiterTest.java`, `cache/StoreFailureBackoffTest.java`.

**Interfaces:** Reuse `RateLimitStore.increment(String, Duration)`, `RateLimitCounter`, `RateLimitDecision`, `ClientAddressResolver.clientHash(HttpServletRequest)`; auth methods/keys unchanged. Produce:

```java
RateLimitDecision RateLimitService.allowCreation(UUID userId);
RateLimitDecision RateLimitService.allowRedirect(HttpServletRequest request);
InMemoryRedirectRateLimiter(int maximumAttempts, Duration window,
        int maximumKeys, LongSupplier ticker);
RateLimitDecision InMemoryRedirectRateLimiter.allow(String clientHash);
void InMemoryRedirectRateLimiter.purgeExpired(); // scheduled every 60 seconds
StoreFailureBackoff(Duration cooldown, LongSupplier ticker);
boolean StoreFailureBackoff.tryAcquire();
void StoreFailureBackoff.recordSuccess();
void StoreFailureBackoff.recordFailure();
// Concrete backoff accepts Duration and LongSupplier; local limiter accepts
// maximumAttempts, window, maximumKeys and LongSupplier for deterministic tests.
// RateLimitProperties adds positive creationMaximumAttempts=100,
// creationWindow=PT1H, redirectMaximumAttempts=600, redirectWindow=PT1M.
```

- [x] **3.1 Write failing tests.** Account request 100 allowed/101 rejected; different account independent; internal code retry does not call limiter again. Client request 600 allowed/601 rejected, same GET/HEAD key, untrusted XFF ignored and malformed trusted metadata safe failure. Redis first-use TTL stays fixed under subsequent requests. Creation Redis failure → 503; redirect Redis failure → fallback. Fake monotonic time tests boundary reset, one recovery probe under concurrent callers, capacity 10,000 rejecting a new key while existing budgets stay usable, and expired cleanup within two minutes.

```java
var ticks = new java.util.concurrent.atomic.AtomicLong();
var limiter = new InMemoryRedirectRateLimiter(2, Duration.ofMinutes(1), 10_000, ticks::get);
assertThat(limiter.allow("client-a").allowed()).isTrue();
assertThat(limiter.allow("client-a").allowed()).isTrue();
assertThat(limiter.allow("client-a").allowed()).isFalse();
assertThat(limiter.allow("client-b").allowed()).isTrue();
ticks.set(Duration.ofMinutes(1).toNanos());
assertThat(limiter.allow("client-a").allowed()).isTrue();
```

- [x] **3.2 Run red:** `./backend/mvnw -f backend/pom.xml -B -ntp test -Dtest=RateLimitServiceTest,InMemoryRedirectRateLimiterTest,StoreFailureBackoffTest,GlobalExceptionHandlerTest`; focused Redis integration `./backend/mvnw -f backend/pom.xml -B -ntp test-compile failsafe:integration-test failsafe:verify -Dit.test=RedisAuthStoresIT`.
- [x] **3.3 Implement namespaces and fallback.** Creation key `rl:create:{UUID}`, redirect key `rl:redirect:{HMAC}`; use existing atomic Redis store. Separate redirect backoff from auth/creation. `LinkConfiguration` wires the fallback with validated redirect properties, 10,000 maximum keys and `System::nanoTime`. Synchronize bounded fallback updates or use an equivalent atomic critical section: remove expired entries, find/create a window only when capacity permits, increment once, deny beyond cap. Do not evict live clients or retain raw addresses. Use scheduled cleanup even during idle periods. Apply ceil retry rounding to JSON/header with no change to auth budgets.

```text
redirect: derive HMAC safely → if Redis attempt allowed, increment
          → success: return Redis decision
          → failure/backoff: local.allow(hash)
creation: increment Redis → decision; store failure → ServiceUnavailableException
retrySeconds = max(1, seconds + (remainingNanosWithinSecond > 0 ? 1 : 0))
```

- [x] **3.4 Run green, including existing auth rate tests.** Confirm recovery/restart may start a fresh best-effort window, documented as single-process degraded behavior, with no creation/auth fallback.
- [x] **3.5 Record evidence and commit:** `git commit -m "feat: enforce link budgets with bounded redirect fallback"`.

## Task 4: Authenticated creation HTTP contract

**Requirements:** FR-CRE-01/02/03/04/07/08; NFR-SEC-06/09, NFR-CON-01; AC-01/02/03/04/08.

**Files — create:** `backend/src/main/java/com/tinyroute/controller/LinkController.java`, `dto/CreateLinkRequest.java`, `dto/CreateLinkResponse.java`, `dto/StrictStringDeserializer.java`; `backend/src/test/java/com/tinyroute/controller/LinkCreationContractIT.java`, `LinkHttpTestSupport.java` (shared real-security fixture).

**Files — modify:** `backend/src/main/java/com/tinyroute/dto/error/ApiErrorResponse.java`, `exception/GlobalExceptionHandler.java`; `backend/src/test/java/com/tinyroute/exception/GlobalExceptionHandlerTest.java`, `security/RequestBodyLimitFilterTest.java`.

**Interfaces:** Consume `LinkService.create(AccessToken, CreateLinkCommand)`, `allowCreation(UUID)`, `LinkProperties.shortUrl(ShortCode)`. Produce transport records and POST endpoint:

```java
record CreateLinkRequest(String destinationUrl, String alias, String expiresAt) {}
record CreateLinkResponse(UUID id, String code, String shortUrl, String destinationUrl,
                          Instant createdAt, Instant expiresAt) {}
ResponseEntity<CreateLinkResponse> LinkController.create(
        AccessToken principal, CreateLinkRequest request);
// @AuthenticationPrincipal / @RequestBody; annotate each request String with the
// DTO-scoped Jackson 3 StrictStringDeserializer. Only string/null are accepted;
// required destination presence and semantic errors are service validation.
ApiErrorResponse ApiErrorResponse.aliasUnavailable(String requestId);
ApiErrorResponse ApiErrorResponse.codeAllocationFailed(String requestId);
```

- [x] **4.1 Write full-security MockMvc integration failures first.** Use existing real JWT key/test datastores and CSRF bootstrap fixtures. Verify missing/invalid/expired/revoked/wrong-version/deleted-owner credentials, valid cookie without CSRF, trusted CORS, malformed JSON, unknown ownerId field, numeric/boolean/array/object fields, explicit null required destination, declared/streamed >16 KiB bodies. Assert no row and no quota consumption for auth/CSRF rejection. Semantic invalid input/conflict consumes one authorized attempt. Verify all status/error contracts, accurate field messages, expiresAt null present, committed next-read, no owner/analytics/creation Location, and no-store.

```java
// csrfCookie/header and accessCookie come from the existing real bootstrap/login fixture.
mockMvc.perform(post("/api/links").cookie(accessCookie, csrfCookie)
        .header("X-CSRF-TOKEN", csrfToken).contentType("application/json")
        .content("{\"destinationUrl\":\"https://example.com/docs?q=java#setup\"}"))
    .andExpect(status().isCreated())
    .andExpect(header().string("Cache-Control", "no-store"))
    .andExpect(header().doesNotExist("Location"))
    .andExpect(jsonPath("$.destinationUrl").value("https://example.com/docs?q=java#setup"))
    .andExpect(jsonPath("$.ownerId").doesNotExist());
```

- [x] **4.2 Run red:** focused `./backend/mvnw -f backend/pom.xml -B -ntp test-compile failsafe:integration-test failsafe:verify -Dit.test=LinkCreationContractIT`; unit `./backend/mvnw -f backend/pom.xml -B -ntp test -Dtest=GlobalExceptionHandlerTest,RequestBodyLimitFilterTest`. Expected authenticated POST currently lacks contract.
- [x] **4.3 Implement thin controller, strict DTO decoding, and safe exception mappings.** Use the existing security default authentication/CSRF, not a new auth mechanism. Quota check occurs once before service validation. Keep String coercion changes DTO-scoped so auth decoding stays unchanged; use installed Jackson 3 APIs. Unknown JSON fields retain the global rejection. Error maps use explicit safe messages/field names and never exception text. Map known `DataAccessException`/`TransactionException` failures, including a failure at proxy commit, to safe 503; do not expose stack/SQL text or catch every unchecked exception indiscriminately. Return 201 after the proxied transactional service returns. Force explicit null expiresAt serialization.

```text
principal → allowCreation(principal.userId()) → create(principal, request-to-command)
         → CreateLinkResponse(created values, configured shortUrl) → 201/no-store
LinkValidationException → 400/VALIDATION_ERROR/allowlisted fieldErrors
AliasUnavailableException → 409/ALIAS_UNAVAILABLE
CodeAllocationFailedException → 409/CODE_ALLOCATION_FAILED
known datastore inability → 503/SERVICE_UNAVAILABLE, no internal message
```

- [x] **4.4 Run green plus existing `AuthContractIT,SecurityPerimeterIT,AccountDeletionIT`.** Confirm security errors preserve existing behavior and creation cannot authorize with stale state after the user-row fence. Force an actual transaction rollback/commit failure in the service boundary test: assert no 201 or cache success assumption is emitted and the HTTP response is safe 503.
- [x] **4.5 Record evidence and commit:** `git commit -m "feat: expose secured link creation API"`.

## Task 5: Expiry-safe redirect cache and authoritative resolution

**Requirements:** FR-RED-01/02/03/04/05/06/07; NFR-REL-01/02, NFR-CON-02; AC-05/06/07/09/11.

**Files — create:** `backend/src/main/java/com/tinyroute/model/RedirectLookup.java`, `RedirectOutcome.java`, `service/RedirectService.java`; `backend/src/test/java/com/tinyroute/service/RedirectServiceTest.java`, `cache/RedisRedirectCacheIT.java`.

**Files — modify:** `backend/src/main/java/com/tinyroute/cache/RedirectCache.java`, `cache/redis/RedisRedirectCache.java`, `backend/src/main/resources/application.yml`; `backend/src/test/java/com/tinyroute/service/AccountDeletionRetryJobTest.java`, `service/AccountDeletionRetryJobIT.java` where extending mocks/contracts requires it.

**Interfaces:** Consume exact repository projection, domain destination/code invariants, `StoreFailureBackoff`, injected UTC `Clock`. Preserve eviction and add:

```java
record RedirectLookup(int schemaVersion, String code, LinkStatus status,
        String destinationUrl, Instant expiresAt, Instant loadedAt, Instant validUntil) {
    boolean isUsableFor(String requestedCode, Instant now, String shortHost);
}
record RedirectOutcome(Kind kind, String destinationUrl) {
    enum Kind { REDIRECT, NOT_FOUND, UNAVAILABLE, SERVICE_UNAVAILABLE }
}
Optional<RedirectLookup> RedirectCache.get(String code);
void RedirectCache.put(RedirectLookup lookup);
void RedirectCache.evict(String code); // preserve throwing failures for deletion retries
RedirectOutcome RedirectService.resolve(ShortCode code);
```

- [x] **5.1 Write failing unit/integration tests.** Assert exact read/hit, case mismatch, deletedAt overriding status, expired-disabled precedence, ACTIVE invalid/null/self-host destination → 503, cache corruption/version/type/missing-explicit-null/wrong-code/time-bound violations → DB. Fake clock verifies exact expiry and five-second cutoff; delayed database read/late cache put cannot extend freshness. Redis read/write failure still returns known DB state, unknown is never cached, stale cache plus DB failure returns 503, valid fresh cache can work during DB outage. Inspect real TTL ≤ remaining original deadline and sanitized inactive entries. Verify deletion eviction still retries failures.

```java
var expiry = Instant.parse("2026-10-07T12:00:02Z");
var snapshot = new RedirectLookup(1, "Abc", LinkStatus.ACTIVE,
        "https://example.com/docs?q=java#setup", expiry,
        expiry.minusSeconds(2), expiry);
assertThat(snapshot.isUsableFor("Abc", expiry.minusMillis(1), "go.tinyroute.test")).isTrue();
assertThat(snapshot.isUsableFor("Abc", expiry, "go.tinyroute.test")).isFalse();
assertThat(snapshot.isUsableFor("abc", expiry.minusMillis(1), "go.tinyroute.test")).isFalse();
```

- [x] **5.2 Run red:** `./backend/mvnw -f backend/pom.xml -B -ntp test -Dtest=RedirectServiceTest,AccountDeletionRetryJobTest`; focused `./backend/mvnw -f backend/pom.xml -B -ntp test-compile failsafe:integration-test failsafe:verify -Dit.test=RedisRedirectCacheIT,AccountDeletionRetryJobIT`.
- [x] **5.3 Implement strict cache adapter and policy resolver.** Adapter requires all cache field names/types including nullable expiry, validates schema/status, and drops destination on inactive writes. Get/put use their own one-second backoff; evict remains a real attempted operation so cleanup does not falsely finish. Use 100 ms Redis connect/command defaults and test normal auth regression. Resolve outside a long-lived transaction; the single repository read uses READ COMMITTED and no user join. Capture read-start before lookup, compute deadline `min(readStart+5s, expiresAt)`, validate again after read, then perform best-effort bounded put. Null/malformed authoritative state is safe unavailable, never a guessed destination.

```text
fresh valid cache → evaluate deleted/expiry/disabled/active at current Clock time
otherwise: readStart = now; read exact database state
           unknown → NOT_FOUND, no cache
           deleted marker → DELETED projection with no destination
           compute original-read deadline; validate state and current expiry
           cache only complete nonexpired state with positive remaining TTL
           return known outcome even if put fails
required database failure → SERVICE_UNAVAILABLE
```

- [x] **5.4 Run green.** Include parallel callers during Redis outage to assert bounded attempts, and account-deletion cleanup tests to confirm key namespace and retry semantics remain compatible.
- [x] **5.5 Record evidence and commit:** `git commit -m "feat: resolve links through bounded expiry-safe cache"`.

## Task 6: Public Spring redirect boundary and safe pages

**Requirements:** FR-RED-01–07, FR-ABS-03; NFR-SEC-08, NFR-REL-01/02; AC-05/06/07/08/09.

**Files — create:** `backend/src/main/java/com/tinyroute/security/PublicRedirectRequestMatcher.java`, `controller/RedirectController.java`, `controller/RedirectPageRenderer.java`; `backend/src/test/java/com/tinyroute/security/PublicRedirectRequestMatcherTest.java`, `controller/RedirectContractIT.java`, `controller/RedirectOutageIT.java`.

**Files — modify:** `backend/src/main/java/com/tinyroute/config/SecurityConfig.java`, `security/JwtAuthenticationFilter.java`; `backend/src/test/java/com/tinyroute/security/JwtAuthenticationFilterTest.java`, `SecurityPerimeterIT.java`.

**Interfaces:** Shared matcher implements existing Spring `RequestMatcher.matches(HttpServletRequest)`. Consume `RedirectService.resolve(ShortCode)`, `RateLimitService.allowRedirect(HttpServletRequest)`, `LinkProperties.shortHost()`, domain canonicalization. Produce:

```java
ResponseEntity<String> RedirectController.redirect(String code, HttpServletRequest request);
String RedirectPageRenderer.render(int status); // only fixed 403/404/429/503 pages
// GET /{code}, Spring HEAD equivalent; raw-path/host checks before resolve.
```

- [ ] **6.1 Write failing HTTP/security tests.** Exact literal case lookup; query ignored; original query/fragment/percent encoding preserved in Location; separate exact case variants; unknown, disabled, deleted, deletedAt and expired fixtures; 302/no-store and every failure no Location/no details. HEAD mirrors headers/status with empty body and uses same client budget. Wrong hostname → safe 404. Reserved, percent-encoded, matrix, slash/multiple-segment and malformed forms never resolve. Malformed firewall 400 is acceptable; ensure no authentication details leak from intended redirect responses. Supply invalid/revoked cookies and make user/revocation dependencies fail: public redirects still perform no auth calls. Keep API, health, error-dispatch and non-GET/HEAD permissions unchanged.

```java
mockMvc.perform(get("/Abc").secure(true).with(request -> {
        request.setServerName("localhost"); return request;
    }).queryParam("ignored", "value"))
    .andExpect(status().isFound())
    .andExpect(header().string("Location", "https://example.com/docs?q=java#setup"))
    .andExpect(header().string("Cache-Control", "no-store"));
mockMvc.perform(head("/Abc").secure(true))
    .andExpect(status().isFound()).andExpect(content().string(""));
```

- [ ] **6.2 Run red:** unit `./backend/mvnw -f backend/pom.xml -B -ntp test -Dtest=PublicRedirectRequestMatcherTest,JwtAuthenticationFilterTest`; focused `./backend/mvnw -f backend/pom.xml -B -ntp test-compile failsafe:integration-test failsafe:verify -Dit.test=RedirectContractIT,RedirectOutageIT,SecurityPerimeterIT`.
- [ ] **6.3 Implement one shared, method/path-scoped matcher and controller.** Admit GET/HEAD single-segment candidates without opening `/api/**`, `/actuator/**` or error dispatch; application rejects invalid/reserved forms. Matcher must permit otherwise valid candidate paths even on the wrong host so controller can return 404. JWT `shouldNotFilter` uses the same matcher. Controller checks original raw path equals `/` plus the validated literal code, checks canonical hostname, then throttles and resolves. Do not trust forwarded Host. Map safe service/limiter failure locally to public HTML rather than API error bodies. Build successful Location directly from stored String, avoiding MVC redirect encoding. Render fixed inline CSS/system-font HTML with design palette and no dynamic request values; suppress HEAD body.

```text
REDIRECT → 302, Location=stored exact destination, empty body
NOT_FOUND → 404; UNAVAILABLE → 403; SERVICE_UNAVAILABLE → 503
rate denied → 429 with ceil Retry-After
every branch → Cache-Control: no-store; unsuccessful branch → no Location
```

- [ ] **6.4 Run green plus creation security tests.** Explicitly prove Redis cache/limiter outage → DB redirect for normal traffic, DB unknown state → safe 503, degraded client throttling → 429 without affecting another client. Assert no click_count changes or event writes.
- [ ] **6.5 Record evidence and commit:** `git commit -m "feat: serve public redirects with safe state responses"`.

## Task 7: Accessible create-and-copy frontend

**Requirements:** FR-CRE-01/02/03/05/07/08; DESIGN.md; AC-01/02/03/04/08/12.

**Files — create:** `frontend/src/app/(account)/links/page.tsx`; `frontend/src/features/links/link-api.ts`, `link-form-schema.ts`, `link-create-form.tsx`, `link-create-result.tsx`, `link-api.test.ts`, `link-create-form.test.tsx`, `link-create-result.test.tsx`.

**Files — modify:** `frontend/src/lib/api-client.ts`, `frontend/src/test/api-client.test.ts`, `frontend/src/app/page.tsx`, `frontend/src/features/auth/login-form.tsx`, `login-form.test.tsx`, `frontend/src/app/globals.css` for scoped styles only.

**Interfaces:** Consume existing `apiRequest<T>`, `ApiClientError`, `getCsrfToken/clearCsrfToken`, `useSession`, `sessionQueryKey` and `getCurrentSession`. Produce:

```typescript
export type CreateLinkInput = {
  destinationUrl: string;
  alias?: string;
  expiresAt?: string;
};
export type CreatedLink = {
  id: string;
  code: string;
  shortUrl: string;
  destinationUrl: string;
  createdAt: string;
  expiresAt: string | null;
};
export function createLink(input: CreateLinkInput): Promise<CreatedLink>;
export function localExpiryToInstant(value: string): string | undefined;
export function LinkCreateForm(): React.ReactElement;
export function LinkCreateResult(props: {
  link: CreatedLink;
}): React.ReactElement;
// createLink: existing CSRF bootstrap + credentialed POST, no retry.
// Shared API error schema adds ALIAS_UNAVAILABLE and CODE_ALLOCATION_FAILED.
```

- [ ] **7.1 Read frontend guidance and installed Next.js docs for routing/client boundaries. Write failing RTL/API tests.** Session pending/signed-out/error; form fields retained on validation/conflict/rate/service failure; explicit server messages; no owner/configured short URL construction; local timezone conversion; one pending POST even on repeated click; no mutation retry after timeout/401/CSRF. A 401 rechecks session through existing renewal, then requires deliberate resubmission or sign-in; CSRF rejection clears cached bootstrap. Success copies server URL once, announces only after clipboard promise succeeds; denied/missing clipboard retains selectable URL/manual guidance. Keyboard label/error associations and live regions.

```typescript
// RTL fixture supplies a successful API response whose shortUrl is authoritative.
await user.click(screen.getByRole("button", { name: "Copy short link" }));
expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
  createdLink.shortUrl,
);
expect(await screen.findByRole("status")).toHaveTextContent("Copied");
// A separately rejected clipboard promise must never show the success message.
```

- [ ] **7.2 Run red:** `pnpm --dir frontend test --run src/features/links src/test/api-client.test.ts src/features/auth/login-form.test.tsx`. Expected absent feature imports/UI behavior, with existing auth tests retained.
- [ ] **7.3 Implement focused page and typed mutation.** Server page wraps client form; RHF/Zod provides format feedback, Spring remains authoritative for reserved/self-host/ownership/rate/state policy. Optional empty UI inputs are omitted; destination/alias are not trimmed. TanStack mutation sets `retry: false`. Preserve fields after failures. Present selectable full server URL and returned expiry. Native Clipboard API call occurs in click handler; await success before announcement. Reuse established theme/type/layout components, visible focus, 44 px targets and reduced-motion styles. Home and signed-in login view link to `/links`; add no analytics/management navigation.

```typescript
const mutation = useMutation({ mutationFn: createLink, retry: false });
// API request options: POST /api/links, JSON input, X-CSRF-TOKEN,
// responseSchema with required nullable expiresAt; apiRequest adds credentials.
// On ambiguous failure: "Creation may have succeeded. Check your last result
// before trying again." Keep retry as an explicit user action.
```

- [ ] **7.4 Run green plus frontend lint/typecheck.** Verify all existing auth tests, no new auth mechanism/token storage, no Next API route or public redirect route. Fix any installed Next API mismatch using its local docs.
- [ ] **7.5 Record evidence and commit:** `git commit -m "feat: add accessible short link creation and copying"`.

## Task 8: Browser contracts and disposable live verification

**Requirements:** All functional criteria; NFR-TST-01, NFR-SEC-08, NFR-CON-01/02; AC-01–09/11/12.

**Files — create:** `frontend/e2e/link-creation.spec.ts`, `frontend/e2e-live/link-creation.live.ts`, `frontend/e2e-live/fixtures/smtp-sink.ts`; `backend/src/test/java/com/tinyroute/LinkVerificationApplication.java`, `config/LinkVerificationConfiguration.java`, `controller/LinkVerificationController.java`, `controller/LinkVerificationFixtureIT.java`; `docs/spec/link-creation-and-redirection/verification.md`.

**Files — modify:** `frontend/e2e-live/account-authentication.live.ts` to reuse its existing SMTP implementation without behavior changes; `README.md` for disposable setup; `.gitignore` for `.local-verification/` runtime evidence, never secrets/screenshots with live identity.

**Interfaces:** Test-only application uses the existing `TestInfrastructureConfiguration` and `TestJwtTokenConfiguration`, launches the real Spring app with disposable PG/Redis; no test controller/config in the packaged application. Shared `SmtpSink` retains `start(port?: number)`, `nextMessage(timeoutMs?: number): Promise<string>`, `stop(): Promise<void>`. Test-only fixture HTTP contracts:

```typescript
// GET /__verification/bootstrap, loopback + X-Verification-Token required
type VerificationBootstrap = {
  apiBaseUrl: string;
  shortBaseUrl: string;
  accounts: Array<{ email: string; password: string }>;
  codes: string[];
};
// POST /__verification/links/state
type FixtureStateChange = {
  code: string;
  status: "ACTIVE" | "DISABLED" | "DELETED";
  expiresAt: string | null;
};
```

- [ ] **8.1 Write failing browser/fixture tests.** Contract Playwright tests exercise session/error branches, malformed server errors, conflict/rate/CSRF/session recovery without automatic mutation, clipboard permission/rejection, double submission, themes and keyboard. Live test registers/verifies a disposable account through existing auth, creates/copies the example destination, reads clipboard with granted permission, and performs anonymous GET/HEAD with `maxRedirects: 0`; compare exact Location. Use test-only state fixtures for disabled/deleted/expired and wait at most five seconds plus bounded polling for state-cache convergence. Test cached expiry at its instant, independent alias conflict, account deletion stopping a created URL. Assert test fixture is absent from production packaging and requires a random token even locally.

```typescript
const response = await anonymous.get(shortUrl, { maxRedirects: 0 });
expect(response.status()).toBe(302);
expect(response.headers().location).toBe(
  "https://example.com/docs?q=java#setup",
);
expect(response.headers()["cache-control"]).toBe("no-store");
```

- [ ] **8.2 Run red:** focused Playwright `pnpm --dir frontend exec playwright test e2e/link-creation.spec.ts`; fixture integration `./backend/mvnw -f backend/pom.xml -B -ntp test-compile failsafe:integration-test failsafe:verify -Dit.test=LinkVerificationFixtureIT`. Missing live application is initially red for the live command, not evidence of product failure.
- [ ] **8.3 Implement test-only disposable application and shared fixtures.** Use the installed Spring Boot Maven `test-run` goal, verified to accept `spring-boot.run.main-class`. Bind application to loopback; test-only higher-priority security chain requires the raw `/__verification/` prefix followed by a nonempty subpath and random `TINYROUTE_VERIFICATION_TOKEN` plus loopback source. It never alters `/api/**` auth/CSRF or shadows the valid single-segment alias `__verification`. State fixtures write only this application's Testcontainers data. Seed 100 distinct codes and three fresh password accounts for Task 9; credentials stay in process memory and are never committed/logged. Configure test-only trusted proxy `127.0.0.2/32`, leaving direct browser localhost traffic untrusted. Browser tests use ordinary auth/API calls. Do not use the owner's Compose database. Require local TLS files; generate only missing test certificates using the documented setup, not production credentials.

```sh
SPRING_PROFILES_ACTIVE=dev ./backend/mvnw -f backend/pom.xml -B -ntp spring-boot:test-run -Dspring-boot.run.main-class=com.tinyroute.LinkVerificationApplication
pnpm --dir frontend exec playwright test --config playwright.live.config.ts
```

The verification token is supplied through the environment, not command arguments or tracked files. Managed Next dev uses existing HTTPS configuration and `NEXT_PUBLIC_API_BASE_URL=https://localhost:8443`. Stop only processes started for this verification; Testcontainers cleans disposable data.

- [ ] **8.4 Run both browser suites green and inspect UI evidence.** Full contract suite plus full live config preserve the existing auth journey. Capture creation/results/errors at 320/768/1024/1440 in light/dark, keyboard flow, and reduced motion. Review screenshots for overflow/focus/contrast/target size; save sanitized review images under `docs/spec/link-creation-and-redirection/ui/` and results in verification.md. No account/cookie/token values in evidence.
- [ ] **8.5 Record evidence and commit:** `git commit -m "test: verify creation and redirects across the real stack"`.

## Task 9: Server timing and reproducible sustained-load evidence

**Requirements:** NFR-PER-01/02/03, NFR-TST-03, NFR-OBS-01; AC-10.

**Files — create:** `backend/src/main/java/com/tinyroute/security/LinkRequestTimingFilter.java`, `backend/src/test/java/com/tinyroute/security/LinkRequestTimingFilterTest.java`, `backend/src/test/java/com/tinyroute/config/VerificationTimingCollector.java`; `tools/link-performance.mjs`, `tools/link-performance.test.mjs`.

**Files — modify:** test-only `LinkVerificationConfiguration.java`, `LinkVerificationController.java`, `LinkVerificationFixtureIT.java`; feature `verification.md`, `README.md`, `.env.example` for safe configuration placeholders only; `.github/workflows/frontend-ci.yml`, `.github/scripts/ci.py`, `.github/scripts/test_ci.py` to run harness self-tests when its files change.

**Interfaces:** Highest-precedence operational filter wraps the full application chain and uses `System.nanoTime`; fixed route labels `create` / `redirect`, fixed method, numeric status and durationNanos only. Existing MeterRegistry timer `tinyroute.links.duration` and structured operational logs contain no raw URL/code/IP/HMAC/owner/cookie. This is request timing, not link analytics. Test-only Logback collector receives these fixed-field records, holds bounded samples for the benchmark, and exposes authenticated fixture endpoints:

```typescript
// POST /__verification/timings/reset, GET /__verification/timings
type TimingSample = {
  route: "create" | "redirect";
  status: number;
  durationNanos: number;
};
type TimingReadout = { samples: TimingSample[]; overflowed: boolean };
// tools/link-performance.mjs exports pure harness helpers for node:test:
// percentile(values: number[], fraction: number): number
// evaluateRun(input: { offered: number; issued: number; completed: number;
//   elapsedSeconds: number; non302: number; timeouts: number;
//   serverDurationsMs: number[] }): { passed: boolean; reasons: string[] }
// CLI accepts --api-base, --rate, --duration, --warmup, --create-samples,
// --clients, --output; token comes only from environment.
```

- [ ] **9.1 Write timing/harness failures first.** Filter tests include downstream security/throttle/error work in elapsed time, fixed path templates and safe tags, thrown failure status, no secret/input interpolation. Node tests cover nearest-rank p95/p99, failure-inclusive error denominator, dropped/late issuance failing, missing server samples/overflow failing, no redirect-following, and status distribution. Fixture timing access requires the loopback/token guard. CI helper test requires `ci.classify(["tools/link-performance.mjs"]) == (True, False)` so a harness-only change cannot skip its checks.

```javascript
assert.equal(percentile([1, 2, 3, 4, 5], 0.95), 5);
assert.equal(
  evaluateRun({
    offered: 60000,
    issued: 59000,
    completed: 59000,
    elapsedSeconds: 600,
    non302: 0,
    timeouts: 0,
    serverDurationsMs: Array(59000).fill(1),
  }).passed,
  false,
);
```

- [ ] **9.2 Run red:** `node --test tools/link-performance.test.mjs`; `python3 -m unittest discover -s .github/scripts -p 'test_*.py'`; backend `./backend/mvnw -f backend/pom.xml -B -ntp test -Dtest=LinkRequestTimingFilterTest`, plus fixture integration command from Task 8. Expected absent harness/filter or incorrect measurement/change-detection assertions.
- [ ] **9.3 Implement Node built-in HTTPS load driver and test-only timing collector.** Use keep-alive requests without following redirects; for the benchmark bind IPv4 client source to `127.0.0.2` and provide 100 distinct fixture XFF identities, with test-only proxy trust. Read fixture accounts, log in normally with CSRF/cookie handling, then issue at least 200 successful creates spread below 100/account; no mutation retry. Keep credentials/cookies private in memory. Warm redirects for 30 seconds, reset server samples, then schedule a full 600-second 100 rps open-loop run across at least 100 codes/clients. Do not reset quotas or bypass limits. Record issuance lateness/dropped arrivals and drain bounded in-flight requests. Use a bounded collector sufficient for all offered requests; overflow fails evidence. Calculate server percentiles from complete durationNanos samples, client round trips separately. Count every main-run non-302/timeout as error; abuse 429 tests remain separate.

```sh
node --test tools/link-performance.test.mjs
node tools/link-performance.mjs --api-base https://localhost:8443 --rate 100 --duration 600 --warmup 30 --create-samples 200 --clients 100 --output .local-verification/link-performance.json
```

Use trusted local certificate validation, e.g. `NODE_EXTRA_CA_CERTS` pointing to the local CA; do not globally disable TLS verification. Run creation and redirect measurements in separately reset timing phases. Document sample clock, nearest-rank computation and collection boundaries.

Keep CI integration narrow: classify `tools/link-performance.mjs` and `tools/link-performance.test.mjs` as frontend-verification inputs; add `node --test ../tools/link-performance.test.mjs` to the existing Node 24 verification job. This runs pure harness tests in CI, not the ten-minute load or credentialed live browser suite; record those full runs locally.

- [ ] **9.4 Run focused tests green, then execute the actual protocol.** Record offered/issued/completed, achieved throughput/elapsed time, server p95/p99, client latency separately, statuses/timeouts/error rate, runtime versions, hardware and datastore placement, cache refill behavior and exact command. Passing requires complete offered issuance and samples, creation p95 <500 ms, redirect p95 <150 ms/p99 <300 ms/errors <0.5%. Keep cap defaults enabled. Under-issuance, missing data or failed thresholds keeps this task incomplete; debug and rerun only after a justified fix.
- [ ] **9.5 Update reproducible docs/evidence and commit:** `git commit -m "perf: verify sustained redirect and creation latency"`. Label local results local; Hostinger/nearby Supabase remains an explicit production release verification limitation, not a production claim.

## Task 10: Independent review and completion readiness

**Requirements:** All acceptance criteria; NFR-TST-01/02, NFR-MNT-01; feature lifecycle and PR workflow.

**Files — modify:** `docs/spec/link-creation-and-redirection/spec.md`, `verification.md`, `tasks/plan.md`, `tasks/todo.md`, `README.md` as required by final evidence/review. Any review fix changes only the relevant files/tests from Tasks 1–9. Keep the spec's plan/checklist pointers accurate after archiving.

**Files — archive only when fully complete:** `tasks/plan.md` → `docs/spec/link-creation-and-redirection/plan.md`; `tasks/todo.md` → `docs/spec/link-creation-and-redirection/todo.md`. Rewrite local plan links for archived location. Use `.github/pull_request_template.md` for PR body.

**Interfaces:** Consumes AC evidence, task commits, full verification results and independent review findings; produces fully completed implementation/task records ready for the post-completion archive and PR sequence below. User owns merge.

- [ ] **10.1 Reconcile acceptance matrix and run fresh relevant full checks.** Read `verification-before-completion`; read current branch/status and inspect diff for scope/secrets. Run:

```sh
./backend/mvnw -f backend/pom.xml -B -ntp clean verify
./backend/mvnw -f backend/pom.xml -B -ntp spotless:check
pnpm --dir frontend lint
pnpm --dir frontend format:check
pnpm --dir frontend typecheck
pnpm --dir frontend test --run
pnpm --dir frontend build
pnpm --dir frontend exec playwright test
pnpm --dir frontend exec playwright test --config playwright.live.config.ts
node --test tools/link-performance.test.mjs
python3 -m unittest discover -s .github/scripts -p 'test_*.py'
git diff --check
```

Expected: all pass, full backend line coverage ≥70%, complete critical-path cases, all browser journeys pass. Record exact counts/durations/coverage and attach runtime/load/UI evidence. Dependency/CI checks remain enabled; do not rerun ten-minute load without a relevant change or unresolved performance concern. Backend Mockito attachment/Docker or network restrictions may require the ordinary permission escalation, never a source workaround.

- [ ] **10.2 Request one independent final review using `requesting-code-review`.** Delegate review only, supply base/current commit IDs, approved spec/plan, diff and evidence. Reviewer must inspect Review Focus below and report concrete severity/file/line findings, including missing verification. Apply `receiving-code-review`; verify findings, write a regression red test for behavioral fixes, implement minimally, run affected checks green, and record follow-up commits. No new implementation delegation. Repeat only the checks made stale by changes; rerun full affected backend/frontend gates if fixes alter that side. A load-affecting fix requires new full load evidence.
- [ ] **10.3 Complete documentation and readiness audit.** All implementation/AC checks and required evidence must be done, blocking review findings resolved, production limitations explicit. If required local work cannot finish, leave tasks active, describe the exact blocker and create/update a draft PR; do not mark it ready or archive. Record approved status/timestamps in spec without changing its substantive contract.
- [ ] **10.4 Finalize the completed task records.** Apply `finishing-a-development-branch`; inline preference and explicit user shipping instructions replace its generic execution/merge choices. Record final review/check outcomes and commit IDs, complete every implementation/AC/readiness checkbox and this task, and check documentation links. Archive and shipping are subsequent lifecycle actions, not unfinished implementation checkboxes in an archived plan.

## Post-completion archive and PR sequence

After all ten tasks and acceptance checks are complete, perform these already-authorized actions continuously:

1. Move the fully completed plan/checklist to `docs/spec/link-creation-and-redirection/plan.md` and `todo.md`. Adjust their relative links; preserve checked boxes and evidence. Stage only feature files and commit with `git commit -m "docs: archive verified link feature tasks and evidence"`.
2. Run `git diff --check`, then `git push -u origin feature/link-creation-and-redirection`. Check for an existing PR; create/update using `.github/pull_request_template.md` and an exact body file. Include requirements, exact checks, UI evidence, no migration, cache/limit impact and local-only performance qualification. No merge, force-push, branch deletion, or browser launch.
3. Confirm pushed commit, PR state and clean feature working tree. Return PR URL, verification evidence, unresolved production topology verification and degraded-window behavior. If delivery is blocked after implementation completion, report that concrete limitation; do not mark a nonexistent push/PR successful. PR metadata need not trigger a new implementation or test cycle.

## Acceptance coverage and review focus

| Criterion | Implementation | Required evidence                                                        |
| --------- | -------------- | ------------------------------------------------------------------------ |
| AC-01     | 2, 4, 6, 7     | committed next-read + live create/copy/exact redirect (8)                |
| AC-02     | 2, 4, 7        | real JWT/revocation/user/CSRF tests + signed-out/session UI              |
| AC-03     | 1, 4, 7        | URL matrix, no row on invalid data, server field feedback                |
| AC-04     | 1, 2, 4        | real concurrent conflicts and permanent reservations                     |
| AC-05     | 1, 2, 5, 6     | post-lock expiry test and populated-cache instant boundary               |
| AC-06     | 2, 5, 6        | exact case/encoding/fragment/query Location and anonymous GET/HEAD       |
| AC-07     | 5, 6           | complete state/preference matrix; safe HTML/no Location/no details       |
| AC-08     | 3, 4, 6, 7     | limits/key isolation/first TTL/ceil retry/default-enabled load           |
| AC-09     | 3, 5, 6        | corrupt/missing Redis and required DB failure; bounded fallback/probe    |
| AC-10     | 9              | ≥200 creates; full 100 rps/600-second server-side report                 |
| AC-11     | 2, 5, 8        | both deletion race orders, existing retry cleanup, live account deletion |
| AC-12     | 7, 8           | keyboard/clipboard/errors, eight theme-width screenshots, reduced motion |

Final reviewer checks: unique namespace/non-reuse; conflict-safe transaction recovery; owner lock order/version/deletion races; expiry after waits and cache instant boundary; original-read deadline under delayed fill; cache serialization/time validation; no destination on inactive/failure; literal host/path matching and public JWT omission; CSRF/CORS/quota order; no mutation retry; fallback/probe memory/time bounds; HMAC privacy; unchanged auth/deletion behavior; no click writes/events/per-code metrics; safe UI/error states; complete local load evidence and honest production limits.

## Planning self-review

Self-review completed 2026-10-07: all twelve approved acceptance criteria map to implementation and runtime evidence; domain/repository/cache/controller/frontend/test-fixture names and signatures match across tasks; no new dependency/migration/production runtime change is planned. Concrete red/green assertions and executable verification commands are present for each behavioral task. Approved inline execution is retained. The plan has not been executed; implementation remains gated on user approval.
