# Link Creation and Redirection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan inline, task by task, with `superpowers:test-driven-development`. Steps use checkbox (`- [ ]`) syntax. Delegation is authorized for one independent final reviewer, not implementation.

**Goal:** Let signed-in users create and copy HTTPS short links with optional aliases and expiry, and let anyone follow eligible links safely.

**Architecture:** Spring controllers translate HTTP; `LinkService` owns creation transactions and `RedirectService` owns resolution policy. A Redis global counter supplies values; fixed-key/salt AES-FF1 scrambles them into eight-character Base62 codes without truncation; PostgreSQL retains allocation metadata, uniqueness, and owner locking, including recovery after counter loss. Bounded Redis snapshots accelerate redirects, with PostgreSQL fallback and redirect-only local throttling. Next.js renders session and API results through the existing credentialed HTTP client.

**Tech Stack:** Existing Java 21 / Spring Boot 4.1.1 MVC, Security, JPA, PostgreSQL, Redis, Flyway, Maven, JUnit/Mockito/MockMvc/Testcontainers/JaCoCo; Node.js 24 LTS, Next.js App Router, React, strict TypeScript, Tailwind, Radix-backed shadcn/ui, Lucide, React Hook Form/Zod, TanStack Query, pnpm, Vitest/RTL, Playwright. No new dependency.

**Spec:** [Feature specification](../docs/spec/link-creation-and-redirection/spec.md): original approved 2026-10-07; plain-counter revision approved 2026-10-08 ("ok do now"); salted FF1 revision approved 2026-10-08 ("done"). Read it with [AGENTS.md](../AGENTS.md), requirements, [architecture](../docs/architecture/architecture.md), ADRs, and [DESIGN.md](../DESIGN.md).

## Global Constraints

- Feature and branch: `link-creation-and-redirection`, `feature/link-creation-and-redirection`; base `ecffb917a489cd18770127ede3b1a96f3a0debfb`. Check branch, status, and active tasks before every resumed execution. Stop for unrelated changes or another feature's incomplete tasks.
- Current gate: original approach/spec/plan approved (plan approval 2026-10-08); Tasks 1–8, including Task 7A, complete; Task 9 sustained-load execution pending. User-requested Redis-counter spec/plan revision approved 2026-10-08 ("ok do now"). The salted AES-FF1 spec/plan revision was approved 2026-10-08 ("done"). Independent final review is complete; retain Tasks 9–10 until the full load protocol passes.
- Keep the locked stack, layer-first Java packages, repository/store interfaces, and existing authentication contract. Never put ownership, redirect policy, or API proxies in Next.js.
- Exclude all analytics, click increments/events, link-management endpoints/UI, auth implementation, blocklists, safe browsing, API keys, and admin tools. Existing `click_count` remains zero for new links.
- PostgreSQL is authoritative. Do not change applied migrations or remove expired/deleted rows. Add V6 for nullable, indexed, range-checked `generation_value`; retain NULL on aliases/legacy random rows. Commit metadata in the same insert as the generated code.
- Destinations: absolute ASCII HTTPS URI, at most 8192 characters, valid host/port, no credentials, whitespace, controls, backslashes, malformed escapes, or ambiguous numeric authorities; preserve the exact original string. No destination fetch/DNS lookup.
- Aliases: 3–64 ASCII `[A-Za-z0-9_-]+`, case-sensitive; reserve the entire words `api`, `actuator`, `error`, `health`, `login`, `logout`, `register`, `links`, `settings`, `analytics`, `account`, `password-reset`, case-insensitively.
- Generated codes: atomic Redis `code:global` increment, no TTL; values `1..218340105584895`; fixed-key/salt AES-FF1 over eight radix-62 digits; output alphabet `0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz`. No hashing/truncation; distinct values map to distinct codes under fixed configuration. At most ten candidates including reserved skips; never overwrite/reuse a code. No random/local fallback.
- Salted encoding: existing Bouncy Castle 1.86 FF1/AES, concrete encoder, no new dependency. `SHORT_CODE_KEY` is canonical Base64 for exactly 32 random bytes; `SHORT_CODE_SALT` is 8–64 ASCII `[A-Za-z0-9:_-]+`. Production requires both; dev examples are public fixtures. Keep key/salt/alphabet/width/version stable across all creators; no per-link salt or rotation. Never log/return/store key/salt with links.
- Counter recovery: query committed `MAX(generation_value)` only on missing key or confirmed generated-code conflict; atomically initialize/advance-and-increment without lowering the current value. Include every retained state/expiry; never decode aliases/legacy codes into the floor. Normal allocation has no database precheck. Invalid/unavailable/expiring counter or failed recovery → 503; valid capacity exhaustion → allocation 409.
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

| Area                   | Files and responsibility                                                                                                                                                                                                                                                                           |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Domain / validation    | `model/DestinationUrl.java`, `ShortCode.java`, `CreateLinkCommand.java`, `ValidatedLinkInput.java`, `CreatedLink.java`, `RedirectLinkState.java`, `RedirectLookup.java`, `RedirectOutcome.java`; concrete `service/LinkCreationValidator.java`, `ShortCodeGenerator.java`, `ShortCodeEncoder.java` |
| Configuration          | `config/LinkProperties.java`, existing `RateLimitProperties.java`, three existing application YAML files; positive budgets and validated short origin                                                                                                                                              |
| Persistence / creation | Extend existing `repository/LinkRepository.java`, `repository/jpa/JpaLinkRepository.java`, `service/LinkService.java`; native owner lock/conflict-safe insert, immutable redirect projection, existing tombstones                                                                                  |
| Counter allocation     | `model/GeneratedShortCode.java`, concrete `service/ShortCodeGenerator.java`; `cache/ShortCodeCounter.java`, `cache/redis/RedisShortCodeCounter.java`; V6 generation metadata and repository recovery query; no availability precheck                                                               |
| Rate limits            | Extend existing `service/RateLimitService.java`; concrete `service/InMemoryRedirectRateLimiter.java`, `cache/StoreFailureBackoff.java`; reuse existing Redis atomic counter and HMAC resolver                                                                                                      |
| Creation HTTP          | `controller/LinkController.java`, `dto/CreateLinkRequest.java`, `CreateLinkResponse.java`, `StrictStringDeserializer.java`; safe exceptions and shared error envelope/handler                                                                                                                      |
| Cache / resolution     | Extend `cache/RedirectCache.java`, `cache/redis/RedisRedirectCache.java`; `service/RedirectService.java` owns outcomes, adapter owns strict serialization/timeouts                                                                                                                                 |
| Public boundary        | `security/PublicRedirectRequestMatcher.java`; extend existing JWT filter/security configuration; `controller/RedirectController.java`, `RedirectPageRenderer.java` for constant HTML                                                                                                               |
| Frontend               | `src/app/(account)/links/page.tsx`, `src/features/links/{link-api,link-create-form,link-create-result,link-form-schema}`; home/login navigation and scoped styles                                                                                                                                  |
| Runtime evidence       | Test-only Spring verification application/config/controller, shared Playwright SMTP fixture, contract/live browser tests, Node built-in load harness, operational timing filter, feature verification evidence                                                                                     |
| Lifecycle              | Active `tasks/plan.md`, `tasks/todo.md`; completed archive under `docs/spec/link-creation-and-redirection/`; existing PR template                                                                                                                                                                  |

## Dependencies and execution rules

Tasks **1–8, including 7A, are complete under the recorded approvals**; their checked steps below retain the actual implementation history. The fixed-key/salt FF1 generator and browser/live evidence are committed. Task 9 timing support is verified, but the sustained-load run remains pending. Independent final review is complete; Task 10 readiness and archival depend on the missing load evidence. No implementation agents or routine checkpoint approval pauses are required.

Every task follows red → observed expected failure → minimal implementation → green/refactor → focused verification → commit. A compile failure caused by a deliberately absent new type is acceptable initial red evidence; an infrastructure failure is not a behavioral red. Use `systematic-debugging` for unexpected failures. Record command/result, red/green evidence, commits, decisions, and unresolved issues in `tasks/todo.md` before moving on or context compaction. Do not claim passes without executing commands.

Each task gives the full Maven unit and integration commands with explicit test selectors. Focused integration checks invoke `test-compile failsafe:integration-test failsafe:verify` so a small test subset is not mistaken for the full application coverage gate. Task 10 runs full `clean verify` with Surefire, Failsafe, static checks and JaCoCo ≥70%. Do not weaken coverage or static checks.

## Task 1: Domain invariants and validated configuration

**Completed history:** The SecureRandom constructor/encoding below describe the verified original implementation, not the revised contract. Task 7A replaces only generator allocation/wiring and reruns affected checks; retain these completed steps.

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

**Completed history:** Task 7A extends the insertion signature with nullable generation metadata and adds the recovery query. Existing owner locking, conflict-safe insertion, expiry checks, deletion behavior, and completed evidence remain required.

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

- [x] **6.1 Write failing HTTP/security tests.** Exact literal case lookup; query ignored; original query/fragment/percent encoding preserved in Location; separate exact case variants; unknown, disabled, deleted, deletedAt and expired fixtures; 302/no-store and every failure no Location/no details. HEAD mirrors headers/status with empty body and uses same client budget. Wrong hostname → safe 404. Reserved, percent-encoded, matrix, slash/multiple-segment and malformed forms never resolve. Malformed firewall 400 is acceptable; ensure no authentication details leak from intended redirect responses. Supply invalid/revoked cookies and make user/revocation dependencies fail: public redirects still perform no auth calls. Keep API, health, error-dispatch and non-GET/HEAD permissions unchanged.

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

- [x] **6.2 Run red:** unit `./backend/mvnw -f backend/pom.xml -B -ntp test -Dtest=PublicRedirectRequestMatcherTest,JwtAuthenticationFilterTest`; focused `./backend/mvnw -f backend/pom.xml -B -ntp test-compile failsafe:integration-test failsafe:verify -Dit.test=RedirectContractIT,RedirectOutageIT,SecurityPerimeterIT`.
- [x] **6.3 Implement one shared, method/path-scoped matcher and controller.** Admit GET/HEAD single-segment candidates without opening `/api/**`, `/actuator/**` or error dispatch; application rejects invalid/reserved forms. Matcher must permit otherwise valid candidate paths even on the wrong host so controller can return 404. JWT `shouldNotFilter` uses the same matcher. Controller checks original raw path equals `/` plus the validated literal code, checks canonical hostname, then throttles and resolves. Do not trust forwarded Host. Map safe service/limiter failure locally to public HTML rather than API error bodies. Build successful Location directly from stored String, avoiding MVC redirect encoding. Render fixed inline CSS/system-font HTML with design palette and no dynamic request values; suppress HEAD body.

```text
REDIRECT → 302, Location=stored exact destination, empty body
NOT_FOUND → 404; UNAVAILABLE → 403; SERVICE_UNAVAILABLE → 503
rate denied → 429 with ceil Retry-After
every branch → Cache-Control: no-store; unsuccessful branch → no Location
```

- [x] **6.4 Run green plus creation security tests.** Explicitly prove Redis cache/limiter outage → DB redirect for normal traffic, DB unknown state → safe 503, degraded client throttling → 429 without affecting another client. Assert no click_count changes or event writes.
- [x] **6.5 Record evidence and commit:** `git commit -m "feat: serve public redirects with safe state responses"`.

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

- [x] **7.1 Read frontend guidance and installed Next.js docs for routing/client boundaries. Write failing RTL/API tests.** Session pending/signed-out/error; form fields retained on validation/conflict/rate/service failure; explicit server messages; no owner/configured short URL construction; local timezone conversion; one pending POST even on repeated click; no mutation retry after timeout/401/CSRF. A 401 rechecks session through existing renewal, then requires deliberate resubmission or sign-in; CSRF rejection clears cached bootstrap. Success copies server URL once, announces only after clipboard promise succeeds; denied/missing clipboard retains selectable URL/manual guidance. Keyboard label/error associations and live regions.

```typescript
// RTL fixture supplies a successful API response whose shortUrl is authoritative.
await user.click(screen.getByRole("button", { name: "Copy short link" }));
expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
  createdLink.shortUrl,
);
expect(await screen.findByRole("status")).toHaveTextContent("Copied");
// A separately rejected clipboard promise must never show the success message.
```

- [x] **7.2 Run red:** `pnpm --dir frontend test --run src/features/links src/test/api-client.test.ts src/features/auth/login-form.test.tsx`. Expected absent feature imports/UI behavior, with existing auth tests retained.
- [x] **7.3 Implement focused page and typed mutation.** Server page wraps client form; RHF/Zod provides format feedback, Spring remains authoritative for reserved/self-host/ownership/rate/state policy. Optional empty UI inputs are omitted; destination/alias are not trimmed. TanStack mutation sets `retry: false`. Preserve fields after failures. Present selectable full server URL and returned expiry. Native Clipboard API call occurs in click handler; await success before announcement. Reuse established theme/type/layout components, visible focus, 44 px targets and reduced-motion styles. Home and signed-in login view link to `/links`; add no analytics/management navigation.

```typescript
const mutation = useMutation({ mutationFn: createLink, retry: false });
// API request options: POST /api/links, JSON input, X-CSRF-TOKEN,
// responseSchema with required nullable expiresAt; apiRequest adds credentials.
// On ambiguous failure: "Creation may have succeeded. Check your last result
// before trying again." Keep retry as an explicit user action.
```

- [x] **7.4 Run green plus frontend lint/typecheck.** Verify all existing auth tests, no new auth mechanism/token storage, no Next API route or public redirect route. Fix any installed Next API mismatch using its local docs.
- [x] **7.5 Record evidence and commit:** `git commit -m "feat: add accessible short link creation and copying"`.

## Task 7A: Replace random generation with the HLD Redis counter

**Status:** Plain-counter revision approved 2026-10-08 ("ok do now"); salted AES-FF1 revision approved 2026-10-08 ("done"). Complete: focused unit/configuration checks, real datastore/security regressions and V5 migration compatibility passed.

**Requirements:** FR-CRE-01/04/07/08, FR-RED-06, FR-ABS-01; functional assumptions 5/6; NFR-CON-01, NFR-REL-01/02, NFR-PER-02; AC-01/04/05/08/09/10/11.

**Files — create:**

- `backend/src/main/resources/db/migration/V6__add_link_generation_value.sql`
- `backend/src/main/java/com/tinyroute/model/GeneratedShortCode.java`
- `backend/src/main/java/com/tinyroute/service/ShortCodeEncoder.java`
- `backend/src/main/java/com/tinyroute/cache/ShortCodeCounter.java`
- `backend/src/main/java/com/tinyroute/cache/redis/RedisShortCodeCounter.java`
- `backend/src/main/java/com/tinyroute/exception/ShortCodeCounterExhaustedException.java`
- `backend/src/test/java/com/tinyroute/model/GeneratedShortCodeTest.java`
- `backend/src/test/java/com/tinyroute/service/ShortCodeEncoderTest.java`
- `backend/src/test/java/com/tinyroute/cache/RedisShortCodeCounterTest.java`
- `backend/src/test/java/com/tinyroute/cache/RedisShortCodeCounterIT.java`

**Files — modify:** `backend/src/main/java/com/tinyroute/config/LinkProperties.java`, `backend/src/main/resources/application-dev.yml`, `application-prod.yml`, `.env.example`, `README.md`, `backend/src/test/java/com/tinyroute/config/LinkPropertiesTest.java`; `backend/src/main/java/com/tinyroute/model/Link.java`, `repository/LinkRepository.java`, `repository/jpa/JpaLinkRepository.java`, `service/ShortCodeGenerator.java`, `service/LinkService.java`, `config/LinkConfiguration.java`; corresponding existing `service/ShortCodeGeneratorTest.java`, `service/LinkServiceTest.java`, `service/LinkCreationIT.java`, `repository/LinkRepositoryIT.java`, `controller/LinkCreationContractIT.java`, `controller/LinkCreationCommitFailureIT.java`, `controller/AccountDeletionIT.java`, `ProfileConfigurationTest.java`. Update any existing generator stubs/insertion callers found by `rg -n 'nextCandidate|insertIfCodeAvailable|new ShortCodeGenerator' backend/src`; preserve their original behavioral assertions. Update the feature spec/architecture/ledger with actual approval and evidence.

**Interfaces:** Consumes existing owner fence, validation, code uniqueness, configured Redis timeouts, and the creation transaction. Produces the following public contracts, superseding the original generator and insert signatures in Tasks 1/2:

```java
record GeneratedShortCode(ShortCode code, long generationValue) {
    public static final long MAX_VALUE = 218_340_105_584_895L;
    // Constructor validates positive range and exactly eight Base62 characters.
    // Generator/encoder enforce correspondence without putting secrets in the model.
}
interface ShortCodeCounter {
    OptionalLong nextValueIfInitialized(); // empty ONLY for a missing key
    long advanceAndIncrement(long committedFloor); // floor in 0..MAX_VALUE
}
// Store exhaustion is an internal boundary exception, not an HTTP response.
class ShortCodeCounterExhaustedException extends RuntimeException {}
// Concrete coordinator, no second generator interface:
ShortCodeEncoder(byte[] key, byte[] salt); // defensive copies; 32-byte key
ShortCode ShortCodeEncoder.encode(long generationValue);
String LinkProperties.getCodeKey(); void LinkProperties.setCodeKey(String value);
String LinkProperties.getCodeSalt(); void LinkProperties.setCodeSalt(String value);
byte[] LinkProperties.codeKeyBytes(); byte[] LinkProperties.codeSaltBytes();
// Byte accessors validate canonical Base64/range/ASCII with generic safe errors.
ShortCodeGenerator(ShortCodeCounter counter, LinkRepository repository, ShortCodeEncoder encoder);
GeneratedShortCode ShortCodeGenerator.nextCandidate();
GeneratedShortCode ShortCodeGenerator.nextCandidateAfterConflict();
long LinkRepository.findMaxGenerationValue(); // all retained states, NULL excluded
int LinkRepository.insertIfCodeAvailable(UUID id, String code, UUID ownerId,
        String destinationUrl, Instant createdAt, Instant expiresAt,
        Long generationValue); // 1 inserted, 0 exact-code conflict
```

Counter adapter maps missing to empty, capacity to `ShortCodeCounterExhaustedException`, and invalid/unavailable/null/unexpected Redis replies to `ServiceUnavailableException`. Generator maps capacity to the existing `CodeAllocationFailedException`; a failed/invalid committed-floor lookup is safe service-unavailable. An encoding failure for a valid allocation is safe service-unavailable with no insert or fallback. No code/destination/owner/key/salt values are logged.

- [x] **7A.1 Write failing encoding, coordinator, store, and persistence tests.** Verify salted FF1 using independently decrypted digit fixtures, determinism and unique outputs for distinct values under fixed key/salt, concurrent thread safety, range boundaries, invalid values, aliases bypassing the counter, ordinary allocation without a MAX query, missing-key recovery, and confirmed-conflict recovery. Verify migration on V5 data and fresh startup, unchanged alias/legacy code strings/NULL metadata, retained generated metadata after deletion/expiry, and a maximum-valued alias unable to poison the floor. Real Redis tests use concurrent callers and a start barrier for unique allocation and competing initialization; simulate a removed key and an older restored value with committed PostgreSQL rows. Test malformed/noninteger/negative/too-large/wrong-type/TTL values, Redis outage, and numeric exhaustion without wrap. Force ten alias/legacy collisions and assert no eleventh allocation, no extra quota, no overwrite, and no row on unsafe failure.

```java
// Public test fixtures; no production secrets.
byte[] fixtureKey = new byte[32];
for (int i = 0; i < 32; i++) fixtureKey[i] = (byte) i;
byte[] fixtureSalt = "tinyroute-test-v1".getBytes(StandardCharsets.US_ASCII);
var encoder = new ShortCodeEncoder(fixtureKey, fixtureSalt);
assertThat(encoder.encode(1).value()).matches("[A-Za-z0-9]{8}");
assertThat(encoder.encode(1)).isEqualTo(encoder.encode(1));
assertThat(encoder.encode(1)).isNotEqualTo(encoder.encode(2));
assertThatThrownBy(() -> encoder.encode(0)).isInstanceOf(IllegalArgumentException.class);
// Separate FF1 decryption asserts the intended plaintext digits independently.
String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
String code = encoder.encode(62).value();
byte[] ciphertext = new byte[8];
for (int i = 0; i < 8; i++) ciphertext[i] = (byte) alphabet.indexOf(code.charAt(i));
var decryptor = new FPEFF1Engine();
decryptor.init(false, new FPEParameters(new KeyParameter(fixtureKey), 62, fixtureSalt));
byte[] recovered = new byte[8];
decryptor.processBlock(ciphertext, 0, 8, recovered, 0);
assertThat(recovered).containsExactly((byte) 0, (byte) 0, (byte) 0, (byte) 0,
        (byte) 0, (byte) 0, (byte) 1, (byte) 0);
// ShortCodeGeneratorTest uses Mockito boundary mocks, no Redis/JPA dependency.
when(counter.nextValueIfInitialized()).thenReturn(OptionalLong.of(62));
assertThat(generator.nextCandidate().generationValue()).isEqualTo(62L);
verify(repository, never()).findMaxGenerationValue();
// Separate missing-key test:
when(counter.nextValueIfInitialized()).thenReturn(OptionalLong.empty());
when(repository.findMaxGenerationValue()).thenReturn(62L);
when(counter.advanceAndIncrement(62L)).thenReturn(63L);
assertThat(generator.nextCandidate().generationValue()).isEqualTo(63L);
```

- [x] **7A.2 Run red and record actual expected failures.** Unit: `./backend/mvnw -f backend/pom.xml -B -ntp test -Dtest=GeneratedShortCodeTest,ShortCodeEncoderTest,ShortCodeGeneratorTest,RedisShortCodeCounterTest,LinkServiceTest,LinkPropertiesTest`. Integration: `./backend/mvnw -f backend/pom.xml -B -ntp test-compile failsafe:integration-test failsafe:verify -Dit.test=RedisShortCodeCounterIT,LinkRepositoryIT,LinkCreationIT`. Absent contracts/behavior are expected red; Docker/sandbox failures are not. Run Maven commands sequentially to avoid shared build artifacts.

- [x] **7A.3 Implement encoding, V6, metadata, and recovery query.** Implement ShortCodeEncoder: validate positive counter range, create eight radix-62 digit bytes, initialize a fresh FF1/AES engine per call with fixed key/salt, encrypt the whole digit array and map encrypted digits to Base62 characters. GeneratedShortCode validates format/range without decoding or holding secrets. No custom cipher, hash, or truncation. Map `@Column(name = "generation_value") private Long generationValue` on Link. Extend only the native insertion column/parameter list; aliases pass NULL, generated candidates pass their allocation. Add the MAX query below; do not add a code existence query or decode legacy strings.

```sql
alter table links add column generation_value bigint;
alter table links add constraint links_generation_value_range
  check (generation_value is null or generation_value between 1 and 218340105584895);
create index links_generation_value_idx on links (generation_value desc)
  where generation_value is not null;

select coalesce(max(generation_value), 0) from links
  where generation_value is not null;

insert into links (id, code, owner_id, destination_url, status, click_count,
                   created_at, updated_at, expires_at, generation_value)
values (:id, :code, :ownerId, :destinationUrl, 'ACTIVE', 0,
        :createdAt, :createdAt, :expiresAt, :generationValue)
on conflict (code) do nothing;
```

- [x] **7A.4 Implement the Redis adapter and concrete generator.** Use existing `StringRedisTemplate` and `DefaultRedisScript<Long>`, key `code:global`. A single script supports normal allocation with empty `ARGV[1]`, or recovery with canonical decimal floor `ARGV[1]`. Java validates floor range before passing it; Lua also validates it. Return sentinels `-1` missing, `-2` capacity, `-3` invalid state; positive replies are allocations. A wrong-type GET throws and maps to safe service-unavailable. Never reset malformed state or remove its TTL to disguise an error.

```lua
local maximum = 218340105584895
local function parse(value)
  if #value > 15 then return nil end
  if value ~= '0' and not string.match(value, '^[1-9]%d*$') then return nil end
  local number = tonumber(value)
  if not number or number < 0 or number > maximum then return nil end
  return number
end
local floor = nil
if ARGV[1] ~= '' then
  floor = parse(ARGV[1])
  if not floor then return -3 end
end
local raw = redis.call('GET', KEYS[1])
local current = 0
if raw then
  current = parse(raw)
  if not current or redis.call('PTTL', KEYS[1]) ~= -1 then return -3 end
elseif not floor then
  return -1
end
local advance = floor and floor > current
local value = advance and floor or current
if value >= maximum then return -2 end
if not raw or advance then redis.call('SET', KEYS[1], ARGV[1]) end
return redis.call('INCR', KEYS[1])
```

All numbers in the accepted range are below `2^53`, preserving exact integer arithmetic in Redis Lua. SET uses the canonical decimal argument rather than Lua scientific notation. Redis script atomicity covers validation, initialization/advance, and increment.

```text
nextCandidate:
  counter.nextValueIfInitialized()
  if missing: counter.advanceAndIncrement(repository.findMaxGenerationValue())
  return new GeneratedShortCode(encoder.encode(value), value)
nextCandidateAfterConflict:
  value = counter.advanceAndIncrement(repository.findMaxGenerationValue())
  return new GeneratedShortCode(encoder.encode(value), value)
```

Wire ShortCodeEncoder from validated properties and `ShortCodeGenerator(ShortCodeCounter, LinkRepository, ShortCodeEncoder)` in LinkConfiguration. Remove only this generator's SecureRandom import/bean construction; existing authentication randomness is unchanged. No eager counter reset or application-startup floor query is needed. Add key/salt configuration tests first, then validate startup configuration and wire dev/prod environment values. Errors never echo keys/salt. Production has no defaults; dev/test values are public fixtures. Update README/.env.example with stable configuration, separate-secret, no-rotation and recovery requirements; real secrets stay outside Git.

```java
byte[] digits = new byte[8];
long remainder = generationValue;
for (int i = 7; i >= 0; i--) { digits[i] = (byte) (remainder % 62); remainder /= 62; }
var cipher = new FPEFF1Engine();
cipher.init(true, new FPEParameters(new KeyParameter(key), 62, salt));
byte[] encrypted = new byte[8];
cipher.processBlock(digits, 0, digits.length, encrypted, 0);
StringBuilder encoded = new StringBuilder(8);
for (byte digit : encrypted) encoded.append(ALPHABET.charAt(Byte.toUnsignedInt(digit)));
return new ShortCode(encoded.toString());
```

- [x] **7A.5 Integrate bounded creation retry and run green.** Inside the existing owner-fenced transaction, maintain a flag selecting `nextCandidateAfterConflict()` only for the next candidate following an INSERT conflict. Clear it after obtaining that candidate. Reserved skips use the ordinary next allocation. Each loop iteration consumes one of ten candidates; final conflict returns 409 without allocating again. Alias path obtains no GeneratedShortCode and passes NULL metadata. Recheck future expiry after counter/recovery waits and immediately before insertion. Existing commit/after-commit semantics remain intact.

```text
recoverNext = false
for candidate in 0 .. (alias present ? 0 : 9):
  allocation = alias present ? null :
      (recoverNext ? generator.nextCandidateAfterConflict() : generator.nextCandidate())
  recoverNext = false
  code = alias present ? alias : allocation.code
  if code reserved: continue
  createdAt = clock.instant truncated to milliseconds
  validator.requireFutureExpiry(expiry, clock.instant)
  inserted = repository.insertIfCodeAvailable(id, code, owner, destination,
      createdAt, expiry, allocation present ? allocation.generationValue : null)
  if inserted == 1: register existing after-commit eviction; return CreatedLink
  if alias present: throw AliasUnavailableException
  recoverNext = true
throw CodeAllocationFailedException
```

Run the 7A.2 selectors green, then `./backend/mvnw -f backend/pom.xml -B -ntp test -Dtest=LinkPropertiesTest,ProfileConfigurationTest,LinkCreationValidatorTest,ShortCodeTest,GlobalExceptionHandlerTest` and `./backend/mvnw -f backend/pom.xml -B -ntp test-compile failsafe:integration-test failsafe:verify -Dit.test=LinkCreationContractIT,LinkCreationCommitFailureIT,AccountDeletionIT,AccountDeletionRetryJobIT,RedisRedirectCacheIT`. Prove counter failure returns 503 without a new row, generated success commits metadata before response, rollback emits no 201, owner deletion is still serialized, and expiry reached during allocation cannot be inserted. No analytics write or redirect counter dependency.

- [x] **7A.6 Record actual evidence and commit.** Update spec/architecture from pending to implemented only after green checks, record V6/recovery/compatibility decisions and counts in the ledger, format touched Java and run `git diff --check`. Stage only Task 7A files/docs and updated generator test callers, retaining uncommitted Task 8 work. Commit `git commit -m "feat: allocate generated short codes from Redis counter"`. Resume Task 8; Task 9 must measure this generator, including allocation and commit.

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

- [x] **8.1 Write failing browser/fixture tests.** Contract Playwright tests exercise session/error branches, malformed server errors, conflict/rate/CSRF/session recovery without automatic mutation, clipboard permission/rejection, double submission, themes and keyboard. Live test registers/verifies a disposable account through existing auth, creates/copies the example destination, reads clipboard with granted permission, and performs anonymous GET/HEAD with `maxRedirects: 0`; compare exact Location. Use test-only state fixtures for disabled/deleted/expired and wait at most five seconds plus bounded polling for state-cache convergence. Test cached expiry at its instant, independent alias conflict, account deletion stopping a created URL. Assert test fixture is absent from production packaging and requires a random token even locally.

```typescript
const response = await anonymous.get(shortUrl, { maxRedirects: 0 });
expect(response.status()).toBe(302);
expect(response.headers().location).toBe(
  "https://example.com/docs?q=java#setup",
);
expect(response.headers()["cache-control"]).toBe("no-store");
```

- [x] **8.2 Run red:** focused Playwright `pnpm --dir frontend exec playwright test e2e/link-creation.spec.ts`; fixture integration `./backend/mvnw -f backend/pom.xml -B -ntp test-compile failsafe:integration-test failsafe:verify -Dit.test=LinkVerificationFixtureIT`. Missing live application is initially red for the live command, not evidence of product failure.
- [x] **8.3 Implement test-only disposable application and shared fixtures.** Use the installed Spring Boot Maven `test-run` goal, verified to accept `spring-boot.run.main-class`. Bind application to loopback; test-only higher-priority security chain requires the raw `/__verification/` prefix followed by a nonempty subpath and random `TINYROUTE_VERIFICATION_TOKEN` plus loopback source. It never alters `/api/**` auth/CSRF or shadows the valid single-segment alias `__verification`. State fixtures write only this application's Testcontainers data. Seed 100 distinct codes and three fresh password accounts for Task 9; credentials stay in process memory and are never committed/logged. Configure test-only trusted proxy `127.0.0.2/32`, leaving direct browser localhost traffic untrusted. Browser tests use ordinary auth/API calls. Do not use the owner's Compose database. Require local TLS files; generate only missing test certificates using the documented setup, not production credentials.

```sh
SPRING_PROFILES_ACTIVE=dev ./backend/mvnw -f backend/pom.xml -B -ntp spring-boot:test-run -Dspring-boot.run.main-class=com.tinyroute.LinkVerificationApplication
pnpm --dir frontend exec playwright test --config playwright.live.config.ts
```

The verification token is supplied through the environment, not command arguments or tracked files. Managed Next dev uses existing HTTPS configuration and `NEXT_PUBLIC_API_BASE_URL=https://localhost:8443`. Stop only processes started for this verification; Testcontainers cleans disposable data.

- [x] **8.4 Run both browser suites green and inspect UI evidence.** Full contract suite plus full live config preserve the existing auth journey. Capture creation/results/errors at 320/768/1024/1440 in light/dark, keyboard flow, and reduced motion. Review screenshots for overflow/focus/contrast/target size; save sanitized review images under `docs/spec/link-creation-and-redirection/ui/` and results in verification.md. No account/cookie/token values in evidence.
- [x] **8.5 Record evidence and commit:** `git commit -m "test: verify creation and redirects across the real stack"`.

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

- [x] **9.1 Write timing/harness failures first.** Filter tests include downstream security/throttle/error work in elapsed time, fixed path templates and safe tags, thrown failure status, no secret/input interpolation. Node tests cover nearest-rank p95/p99, failure-inclusive error denominator, dropped/late issuance failing, missing server samples/overflow failing, no redirect-following, and status distribution. Fixture timing access requires the loopback/token guard. CI helper test requires `ci.classify(["tools/link-performance.mjs"]) == (True, False)` so a harness-only change cannot skip its checks.

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

- [x] **9.2 Run red:** `node --test tools/link-performance.test.mjs`; `python3 -m unittest discover -s .github/scripts -p 'test_*.py'`; backend `./backend/mvnw -f backend/pom.xml -B -ntp test -Dtest=LinkRequestTimingFilterTest`, plus fixture integration command from Task 8. Expected absent harness/filter or incorrect measurement/change-detection assertions.
- [x] **9.3 Implement Node built-in HTTPS load driver and test-only timing collector.** Use keep-alive requests without following redirects; for the benchmark bind IPv4 client source to `127.0.0.2` and provide 100 distinct fixture XFF identities, with test-only proxy trust. Read fixture accounts, log in normally with CSRF/cookie handling, then issue at least 200 successful counter-generated creates with alias omitted, spread below 100/account; no mutation retry. Include Redis allocation, salted FF1 encoding, and PostgreSQL commit in server creation timing; former random-generator measurements do not satisfy the revised result. Keep credentials/cookies private in memory. Warm redirects for 30 seconds, reset server samples, then schedule a full 600-second 100 rps open-loop run across at least 100 codes/clients. Do not reset quotas or bypass limits. Record issuance lateness/dropped arrivals and drain bounded in-flight requests. Use a bounded collector sufficient for all offered requests; overflow fails evidence. Calculate server percentiles from complete durationNanos samples, client round trips separately. Count every main-run non-302/timeout as error; abuse 429 tests remain separate.

```sh
node --test tools/link-performance.test.mjs
node tools/link-performance.mjs --api-base https://localhost:8443 --rate 100 --duration 600 --warmup 30 --create-samples 200 --clients 100 --output .local-verification/link-performance.json
```

Use trusted local certificate validation, e.g. `NODE_EXTRA_CA_CERTS` pointing to the local CA; do not globally disable TLS verification. Run creation and redirect measurements in separately reset timing phases. Document sample clock, nearest-rank computation and collection boundaries.

Execution environment option: the same protocol can run inside a native Linux
Docker namespace when the host cannot bind `127.0.0.2`. The test-only Java app
and Node driver share that namespace, with read-only project/classpath mounts,
disposable PostgreSQL/Redis and a private writable report directory. Keep the
exact proxy source, TLS validation, normal limits and thresholds. Record image
IDs, runtime versions and resource limits; this is a local measurement and does
not establish production topology performance.

If native Linux TLS-client initialization prevents accurate scheduling, use the
native host Node driver through an owned localhost-only TCP pass-through proxy.
`--via-local-proxy` changes the client's bind address to host loopback; the proxy
connects to Java from the original exact trusted source `127.0.0.2`. TLS remains
end-to-end, headers unchanged, limits and protocol identical. Verify fixture
access and record the extra hop. This changes the test transport only.

Keep CI integration narrow: classify `tools/link-performance.mjs` and `tools/link-performance.test.mjs` as frontend-verification inputs; add `node --test ../tools/link-performance.test.mjs` to the existing Node 24 verification job. This runs pure harness tests in CI, not the ten-minute load or credentialed live browser suite; record those full runs locally.

- [ ] **9.4 Run focused tests green, then execute the actual protocol.** Record offered/issued/completed, achieved throughput/elapsed time, server p95/p99, client latency separately, statuses/timeouts/error rate, runtime versions, hardware and datastore placement, cache refill behavior and exact command. Passing requires complete offered issuance and samples, creation p95 <500 ms, redirect p95 <150 ms/p99 <300 ms/errors <0.5%. Keep cap defaults enabled. Under-issuance, missing data or failed thresholds keeps this task incomplete; debug and rerun only after a justified fix.
- [ ] **9.5 Update reproducible docs/evidence and commit:** `git commit -m "perf: verify sustained redirect and creation latency"`. Label local results local; Hostinger/nearby Supabase remains an explicit production release verification limitation, not a production claim.

## Task 10: Independent review and completion readiness

**Requirements:** All acceptance criteria; NFR-TST-01/02, NFR-MNT-01; feature lifecycle and PR workflow.

**Files — modify:** `docs/spec/link-creation-and-redirection/spec.md`, `verification.md`, `tasks/plan.md`, `tasks/todo.md`, `README.md` as required by final evidence/review. Any review fix changes only relevant files/tests from Tasks 1–9, including 7A. Keep the spec's plan/checklist pointers accurate after archiving.

**Files — archive only when fully complete:** `tasks/plan.md` → `docs/spec/link-creation-and-redirection/plan.md`; `tasks/todo.md` → `docs/spec/link-creation-and-redirection/todo.md`. Rewrite local plan links for archived location. Use `.github/pull_request_template.md` for PR body.

**Interfaces:** Consumes AC evidence, task commits, full verification results and independent review findings; produces fully completed implementation/task records ready for the post-completion archive and PR sequence below. User owns merge.

- [x] **10.1 Reconcile acceptance matrix and run fresh relevant full checks.** Read `verification-before-completion`; read current branch/status and inspect diff for scope/secrets. Run:

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

Recorded outcome: all listed full checks passed, including 344 backend tests,
90.27% line coverage, 77 frontend tests, 27 browser contracts, two live journeys,
nine Node harness tests and 14 CI helper tests. Independent review fixes and
documentation links are verified. Backend and frontend PR CI passed at `22f241b`,
including CodeQL and dependency/image scans. The acceptance matrix correctly
leaves AC-10 open: the actual load report belongs to Task 9 and remains required
before readiness (10.3) and final task completion (10.4). Checking this step does
not claim performance acceptance or authorize archival.

- [x] **10.2 Request one independent final review using `requesting-code-review`.** Delegate review only, supply base/current commit IDs, approved spec/plan, diff and evidence. Reviewer must inspect Review Focus below and report concrete severity/file/line findings, including missing verification. Apply `receiving-code-review`; verify findings, write a regression red test for behavioral fixes, implement minimally, run affected checks green, and record follow-up commits. No new implementation delegation. Repeat only the checks made stale by changes; rerun full affected backend/frontend gates if fixes alter that side. A load-affecting fix requires new full load evidence.
- [ ] **10.3 Complete documentation and readiness audit.** All implementation/AC checks and required evidence must be done, blocking review findings resolved, production limitations explicit. If required local work cannot finish, leave tasks active, describe the exact blocker and create/update a draft PR; do not mark it ready or archive. Record approved status/timestamps in spec without changing its substantive contract.
- [ ] **10.4 Finalize the completed task records.** Apply `finishing-a-development-branch`; inline preference and explicit user shipping instructions replace its generic execution/merge choices. Record final review/check outcomes and commit IDs, complete every implementation/AC/readiness checkbox and this task, and check documentation links. Archive and shipping are subsequent lifecycle actions, not unfinished implementation checkboxes in an archived plan.

## Post-completion archive and PR sequence

After Tasks 1–10, including 7A, and all acceptance checks are complete, perform these already-authorized actions continuously:

1. Move the fully completed plan/checklist to `docs/spec/link-creation-and-redirection/plan.md` and `todo.md`. Adjust their relative links; preserve checked boxes and evidence. Stage only feature files and commit with `git commit -m "docs: archive verified link feature tasks and evidence"`.
2. Run `git diff --check`, then `git push -u origin feature/link-creation-and-redirection`. Check for an existing PR; create/update using `.github/pull_request_template.md` and an exact body file. Include requirements, exact checks, UI evidence, V6 migration/legacy compatibility/counter recovery, cache/limit impact and local-only performance qualification. No merge, force-push, branch deletion, or browser launch.
3. Confirm pushed commit, PR state and clean feature working tree. Return PR URL, verification evidence, unresolved production topology verification and degraded-window behavior. If delivery is blocked after implementation completion, report that concrete limitation; do not mark a nonexistent push/PR successful. PR metadata need not trigger a new implementation or test cycle.

## Acceptance coverage and review focus

| Criterion | Implementation | Required evidence                                                              |
| --------- | -------------- | ------------------------------------------------------------------------------ |
| AC-01     | 2, 4, 6, 7, 7A | committed generated value/next-read + live create/copy/exact redirect (8)      |
| AC-02     | 2, 4, 7        | real JWT/revocation/user/CSRF tests + signed-out/session UI                    |
| AC-03     | 1, 4, 7        | URL matrix, no row on invalid data, server field feedback                      |
| AC-04     | 1, 2, 4, 7A    | concurrent allocation/recovery, alias/legacy conflicts, permanent reservations |
| AC-05     | 1, 2, 5, 6, 7A | post-lock/allocation expiry checks and populated-cache instant boundary        |
| AC-06     | 2, 5, 6        | exact case/encoding/fragment/query Location and anonymous GET/HEAD             |
| AC-07     | 5, 6           | complete state/preference matrix; safe HTML/no Location/no details             |
| AC-08     | 3, 4, 6, 7     | limits/key isolation/first TTL/ceil retry/default-enabled load                 |
| AC-09     | 3, 5, 6, 7A    | cache fallback; counter/recovery failure; safe DB failure and bounded probe    |
| AC-10     | 7A, 9          | ≥200 counter-generated creates; full 100 rps/600-second server-side report     |
| AC-11     | 2, 5, 7A, 8    | both deletion race orders, retained allocation metadata, live deletion         |
| AC-12     | 7, 8           | keyboard/clipboard/errors, eight theme-width screenshots, reduced motion       |

Final reviewer checks: fixed-key/salt FF1 bijection, thread safety, validated secrets/non-disclosure and stable configuration; atomic counter/capacity/recovery behavior and no normal-path MAX/precheck; V6 alias/legacy compatibility and retained metadata; unique namespace/non-reuse; conflict-safe transaction recovery; owner lock order/version/deletion races; expiry after allocation waits and cache instant boundary; original-read deadline under delayed fill; cache serialization/time validation; no destination on inactive/failure; literal host/path matching and public JWT omission; CSRF/CORS/quota order; no mutation retry; fallback/probe memory/time bounds; HMAC privacy; unchanged auth/deletion behavior; no click writes/events/per-code metrics; safe UI/error states; complete local load evidence and honest production limits.

## Planning self-review

Original self-review completed 2026-10-07; original plan approved 2026-10-08 and Tasks 1–7 executed. Redis-counter revision self-review completed 2026-10-08: Task 7A replaces the generator/insert interfaces, adds V6 and an external-store boundary, covers atomic initialization/recovery/alias/legacy failures, and updates acceptance/performance mappings. Completed history remains checked; Task 8 is now complete and Tasks 9/10 remain active until sustained-load acceptance passes. No new dependency, authentication/UI policy, analytics, or production provisioning change is planned. The plain-counter revision was approved 2026-10-08 ("ok do now"). The salted FF1 revision was self-reviewed and approved 2026-10-08 ("done"): FF1 retains a one-to-one mapping and eight-character length, uses the existing dependency, and explicitly requires fixed key/salt configuration. Tasks 7A and 8 are implemented and verified; independent review is complete and its minor harness/documentation findings are resolved. Sustained-load evidence remains pending.
