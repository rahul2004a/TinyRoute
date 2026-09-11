# TinyRoute architecture

Portfolio URL shortener for one developer. Anyone can follow a short URL; creating and managing links requires a signed-in account. This note traces the high-level and low-level design back to [Functional.md](../requirements/Functional.md) and [Non-Functional.md](../requirements/Non-Functional.md). Diagrams: [HLD.excalidraw](HLD.excalidraw), [LLD.drawio](LLD.drawio).

## Stack (locked)

| Piece | Role |
| --- | --- |
| Next.js App Router + React | Browser UI on Node.js 24 LTS. HTTP client only; no ownership or redirect policy. |
| TypeScript | Strictly typed frontend source and API contracts. |
| Tailwind CSS + shadcn/ui | Styling and owned component source. shadcn/ui uses Radix UI primitives. |
| Lucide React | The single interface icon family. |
| React Hook Form + Zod | Form state and client-boundary validation. Spring Boot remains authoritative. |
| TanStack Query | Browser-side remote data cache and request lifecycle. |
| Recharts | Client-rendered analytics charts with accessible data-table fallbacks. |
| next-themes | Light, dark, and system theme selection using a `data-theme` attribute. |
| Vitest + React Testing Library | Unit and component tests. |
| Playwright | Browser and end-to-end workflow tests. |
| ESLint + Prettier | Static analysis and formatting. |
| pnpm | Frontend package manager and committed lockfile owner. |
| Java + Spring Boot | Backend runtime and application framework. Source of application policy: validation, JWT access, refresh sessions, ownership, redirects, and rate limits. |
| Spring Web MVC | Servlet-based HTTP controllers, filters, and request handling. |
| Spring Security | Authentication, authorization, CSRF protection, and security filters. |
| Spring Data JPA + Hibernate | Repository implementation and relational persistence. |
| Jakarta Bean Validation | Authoritative request and domain-boundary validation. |
| PostgreSQL | System of record for users and links, including tombstones. |
| Redis | Disposable refresh sessions, JWT revocation, redirect cache (TTL ≤ 5 s), and rate-limit counters. |
| Flyway | Versioned PostgreSQL schema migrations. |
| Spring Security JOSE / Nimbus JWT | JWT signing and verification. |
| Argon2id | Password hashing. |
| Spring Security OAuth2 Client | Google OAuth 2.0 sign-in. |
| Spring Mail | Registration and password-reset email delivery. |
| Spring `@Async` + bounded executor | In-process asynchronous click recording without an unbounded task queue. |
| Spring Boot Actuator | Application and datastore health endpoints. |
| SLF4J + Logback | Application logging. |
| JUnit 5 + Mockito | Backend unit tests and test doubles. |
| Spring Boot Test + MockMvc | Backend integration and HTTP-layer tests. |
| JaCoCo | Backend test coverage reporting. |
| Maven | Backend build and dependency management. |
| Docker | Repeatable application packaging and local deployment. |

The frontend execution model is Server Components for layouts and static
structure, with narrowly scoped Client Components for forms, TanStack Query,
charts, and theme controls. A shared typed HTTP client sends browser requests
directly to the Spring Boot JSON API with credentials and the API-required CSRF
protection. Next.js route handlers and Server Actions must not proxy these
requests or duplicate backend validation, authorization, rate limits,
transactions, redirect handling, or other policy.

TanStack Query owns remote server state only; local interaction state stays in
React or React Hook Form. Zod improves client feedback and validates data at the
UI boundary, but API errors and Spring Boot validation remain authoritative.
Recharts charts must preserve reduced-motion behavior and provide equivalent
tabular data. Exact dependency versions are chosen during frontend bootstrap,
pinned in `package.json`, and locked by `pnpm-lock.yaml`; local development, CI,
and the frontend container use Node.js 24 LTS. See
[ADR 0001](../decisions/0001-frontend-stack.md) for the decision and tradeoffs.

The backend dependencies above are the lean MVP baseline. Exact versions are
chosen during backend bootstrap, pinned in `pom.xml`, and resolved by Maven.
Add another dependency or tool only when a functional or non-functional
requirement clearly needs it; document significant additions as architecture
decisions. See [ADR 0002](../decisions/0002-backend-stack.md).

Single-region, modest hardware. HTTPS at the edge (NFR-SEC-01). Health check reports process + datastore (NFR-AVL-02).

## High-level design

Signed-in users hit Next.js, which calls the Spring Boot JSON API with HttpOnly cookies: a short-lived JWT access token and a Redis-backed refresh session (NFR-SEC-06). Visitors hit `GET /{code}` with no account. Ingress terminates TLS and routes both. Cookie-based JWTs still require CSRF on mutations.

Inside the monolith: Auth, URL (create/manage), Redirect, and analytics paths that are **not** on the redirect critical path. Redis is consulted first for redirects; a miss or Redis failure falls through to PostgreSQL. If PostgreSQL cannot determine link state, the service returns an error and **never** guesses a `Location` (NFR-REL-02).

Click totals, 30-day trends, and aggregated referrer/device/OS/browser/country/city analytics are MVP (FR-ANA-01..04). A separate analytics worker, API keys, and blocklists are growth/V1 work.

## Layers (LLD)

Controllers translate HTTP only. Services own policy and transactions. Repositories own persistence. Redis access goes through adapters.

```
web          RedirectController, AuthController, LinkController, AnalyticsController,
             HealthController
security     JwtAuthenticationFilter, CsrfProtection, OwnershipGuard
application  AuthService, LinkService, RedirectService, RateLimitService,
             ClickCountService (async), AnalyticsService
auth         AuthProvider map (PasswordAuthProvider, GoogleAuthProvider),
             OtpSender map (EmailOtpSender), GoogleOAuthClient, PasswordHasher,
             JwtTokenService, PasswordResetMailer
domain       User, AuthIdentity, PendingRegistration, Link, LinkStatus, ShortCode,
             DestinationUrl, RefreshSession, AccessToken, RedirectLookup, GoogleProfile,
             ClickEvent, AnalyticsRange, AnalyticsResponse, PasswordResetToken
persistence  UserRepository, AuthIdentityRepository, PendingRegistrationRepository,
             LinkRepository, ClickEventRepository, PasswordResetTokenRepository  (interfaces)
             Jpa* adapters (parameterized JPA, including JpaClickEventRepository)
cache        RedirectCache, RefreshSessionStore, JwtRevocationStore, RateLimitStore  (interfaces)
             RedisRedirectCache, RedisRefreshSessionStore, RedisJwtRevocationStore,
             RedisRateLimitStore  (implementations)
```

Services depend on interfaces, not on Redis or JPA types. That is the same pattern as a typical LLD class diagram: `uses` from controller to service, `depends on` from service to repository/store interface, `implements` from `Jpa*` / `Redis*` to that interface.

Page 1 of [LLD.drawio](LLD.drawio) is an IntelliJ-style UML class diagram. The HTTP entry points are `AuthController`, `LinkController`, `AnalyticsController`, `RedirectController`, and `HealthController`; there is no duplicate facade. Controllers **use** application services, services **depend on** repository/store interfaces, and `Jpa*` / `Redis*` adapters **implement** those interfaces. `JwtAuthenticationFilter` and `CsrfProtection` intercept only the applicable protected requests.

![TinyRoute UML class diagram](LLD.jpg)

## Component interactions

Who calls whom. Solid = on the request path. Dashed = async and must not block.

### Public redirect (`GET /{code}`)

Visitor → HTTPS ingress → **RedirectController** (no session, no CSRF, no ownership).

1. RedirectController → **RateLimitService**.allowRedirect → **RateLimitStore** → Redis `rl:redirect:{clientHash}`
2. RedirectController → **RedirectService**.resolve
3. RedirectService → **RedirectCache**.get (and put on DB success) → Redis `redirect:{code}`. A cached lookup includes `expiresAt` when set; the service checks it before redirecting, and the cache TTL is `min(5 s, remaining time to expiry)`.
4. On miss / Redis down / malformed cache: RedirectService → **LinkRepository**.findByCode → PostgreSQL
5. After a 3xx only: RedirectService ⋯ **ClickCountService**.recordSuccessfulRedirect ⋯ LinkRepository (increment `click_count`) + ClickEventRepository (store a normalized analytics event)

Redirect path does **not** call AuthService, LinkService, AnalyticsService, RefreshSessionStore, JwtRevocationStore, OwnershipGuard, UserRepository, or Next.js. `ClickCountService` processes the event asynchronously; its failure never delays, changes, or rescues the redirect. It receives request metadata only transiently, normalizes it before persistence, and does not store a raw IP address, raw user agent, or raw referrer URL. ClickCountService does **not** call RedirectCache.

### Register / sign-in / refresh / sign-out

Next.js → HTTPS ingress → **CsrfProtection** (mutations) → **AuthController**.

1. AuthController → **RateLimitService**.allowAuth → RateLimitStore → Redis `rl:auth:{clientHash}` (register/login/verify/oauth/refresh/password-reset)
2. AuthController → **AuthService**. `login` uses `providers.get(type)`; OTP uses `otpSenders.get(EMAIL)`.
3. Password register: **PendingRegistrationRepository** + `OtpSender.send`; set `pending_registration` cookie (no access or refresh tokens yet). Verify body is OTP only.
4. Verify / password login / Google callback: **UserRepository** + **AuthIdentityRepository** → PostgreSQL `users` / `auth_identities`
5. AuthService → **JwtTokenService**.createAccessToken (15 min JWT) and **RefreshSessionStore**.create → Redis `refresh:{tokenHash}` (sliding 30-day TTL). Controller sets `access_token` and `refresh_token` cookies; tokens are never returned in JSON (NFR-SEC-06).
6. **JwtAuthenticationFilter** (logout / me / owner APIs) verifies the access JWT, then **JwtRevocationStore**.isRevoked(`jti`) and **UserRepository**.findById to require a non-deleted user whose current `tokenVersion` equals the JWT claim. Refresh is not consulted on every request.
7. `POST /api/auth/refresh` uses the refresh cookie only: RefreshSessionStore.get/touch, then a new access JWT. Rate-limited with other auth calls.
8. Logout: RefreshSessionStore.delete current refresh session; JwtRevocationStore.revoke current `jti` until access-token expiry (NFR-SEC-09).
9. Google start creates an unpredictable `state`, sets it in a short-lived HttpOnly, Secure, SameSite=Lax `oauth_state` cookie, and sends it to Google in the authorization URL. On callback, AuthService compares the returned state with the cookie in constant time and clears the cookie before **GoogleAuthProvider** → **GoogleOAuthClient**.exchangeCode. A missing, expired, or mismatched state fails the callback; Google tokens remain in memory only and are never persisted.

Register has no prior tokens. Google skips OTP. Auth path does **not** call RedirectService, LinkRepository, RedirectCache, or OwnershipGuard. Account deletion (Could) is the exception that may call LinkService to tombstone owned links.

### Password reset

Next.js → HTTPS ingress → **CsrfProtection** (POSTs) → **AuthController**.

1. AuthController → **RateLimitService**.allowAuth → RateLimitStore → Redis `rl:auth:{clientHash}`.
2. Reset request: AuthService looks up the account through **UserRepository** but always returns the same generic response. For an eligible password identity, it creates a random reset token, stores only its hash through **PasswordResetTokenRepository**, and asks **PasswordResetMailer** to send the reset link.
3. Reset confirmation: AuthService validates and consumes the token, updates the password secret through **AuthIdentityRepository**, increments the user's `tokenVersion` through UserRepository, and calls **RefreshSessionStore**.deleteAllByUserId. Subsequent access JWTs fail the filter's token-version check.

The raw reset token is never persisted. The reset token is single-use and expires. This flow satisfies FR-ACC-04 and NFR-SEC-09 without revealing whether an email has an account.

### Account deletion

Next.js → HTTPS ingress → **JwtAuthenticationFilter** (required) → **CsrfProtection** → **AuthController** → **AuthService**.

1. AuthService coordinates one transaction: it increments `tokenVersion`, calls **RefreshSessionStore**.deleteAllByUserId, removes the user's password and OAuth identities through **AuthIdentityRepository**, and asks **LinkService** to tombstone the owned links.
2. LinkService → LinkRepository finds the owner's codes and tombstones every owned link. After the transaction commits, LinkService evicts each `redirect:{code}` entry so no cached destination remains usable.
3. UserRepository marks the account deleted and anonymizes its personal data within 24 hours while retaining the internal user row required by the tombstoned links' `owner_id` foreign key. The authentication filter rejects the deleted user.

Account deletion therefore stops all owned redirects, revokes current sessions, invalidates access JWTs, and preserves link tombstones without retaining account personal data (FR-ACC-05, NFR-SEC-09, NFR-PRV-04).

### Owner create / list / manage

Next.js → HTTPS ingress → **JwtAuthenticationFilter** (required) → **CsrfProtection** (POST/PATCH/DELETE) → **LinkController**.

1. LinkController → RateLimitService.allowCreate → RateLimitStore → Redis `rl:create:{userId}` (**create only**)
2. LinkController → **LinkService** (all link operations)
3. LinkService → **OwnershipGuard**.requireOwned on status / destination / delete
4. OwnershipGuard → LinkRepository.findByIdAndOwnerId (missing and non-owned both 404)
5. LinkService → LinkRepository.save / findPageByOwnerId → PostgreSQL `links`
6. After successful commit: LinkService → RedirectCache.evict → Redis `DEL redirect:{code}`

Next.js never checks `owner_id`. Link path does **not** call AuthService, RedirectService, ClickCountService, or UserRepository (user id comes from the verified JWT `sub`).

### Owner analytics

Next.js → HTTPS ingress → **JwtAuthenticationFilter** (required) → **AnalyticsController** → **AnalyticsService**.

1. AnalyticsService → **OwnershipGuard**.requireOwned → LinkRepository.findByIdAndOwnerId (missing and non-owned both 404)
2. AnalyticsService validates the owner-supplied **AnalyticsRange**, then → **ClickEventRepository**.snapshot → PostgreSQL grouped totals, daily counts, and aggregated referrer/device/OS/browser/country/city breakdowns

`GET` analytics needs authentication and ownership enforcement but no CSRF check. The frontend only renders the response; it does not make ownership or analytics-policy decisions.

### Health

Monitor → HTTPS ingress → **HealthController** → UserRepository/DataSource ping → PostgreSQL (required for UP). Redis PING is optional; Redis loss must not mark the app down (NFR-AVL-02).

Health does **not** call Auth/Link/Redirect services, rate limits, CSRF, JWTs, or refresh sessions.

### Cross-flow coupling (same beans)

| Writer                          | Shared component             | Reader                                        |
| ------------------------------- | ---------------------------- | --------------------------------------------- |
| LinkService (after commit)      | RedirectCache                | RedirectService (cache-aside)                 |
| AuthService                     | RefreshSessionStore          | AuthService.refresh; delete on logout         |
| AuthService / JwtAuthenticationFilter | JwtRevocationStore     | JwtAuthenticationFilter on later requests     |
| RateLimitService                | RateLimitStore               | the same RateLimitService on the next request |
| ClickCountService (async)       | LinkRepository `click_count` | LinkService.list (informational; may lag)     |
| ClickCountService (async)       | ClickEventRepository          | AnalyticsService (totals, trends, breakdowns) |

Next.js never talks to PostgreSQL, Redis, repositories, OwnershipGuard, or RedirectService.

## Domain model (MVP Must)

**User** — `id`, `emailNormalized`, `tokenVersion`, optional `deletedAt`. No `passwordHash` or profile data lives on User. Password hashes live on `AuthIdentity` (`PASSWORD` only). Argon2id with a unique salt per password (NFR-SEC-02). Increment `tokenVersion` on password reset and account deletion so previously issued JWTs fail verification (NFR-SEC-09). Account deletion anonymizes the user record while preserving its internal id for link tombstones.

**AuthIdentity** — `provider` (`PASSWORD` | `GOOGLE`), `subject` (normalized email or Google `sub`), optional `secretHash`. Unique `(provider, subject)`. No OTP or display name here.

**PendingRegistration** — password signup before OTP succeeds: hashed password, hashed OTP, and email. Found via `pending_registration` cookie token, not via email on `VerifyOtpRequest`.

**Link** — `id`, `code` (ShortCode), `owner`, `destinationUrl`, `status`, timestamps, optional `expiresAt` and `deletedAt`, informational `clickCount` (may lag ≤ 1 min). Expiry is an MVP creation option: once `expiresAt` is past, the link stops redirecting without owner action (FR-CRE-08, FR-RED-07). Destination is **https-only and owner-editable** (FR-CRE-03, FR-MGT-07). Codes are case-sensitive, unique, and never reused (FR-CRE-04, FR-RED-06, FR-MGT-05).

**LinkStatus** — `ACTIVE` | `DISABLED` | `DELETED`. Delete writes a tombstone; the row stays so the code cannot be issued again.

**ShortCode** — generate or parse; uniqueness is the database unique constraint plus bounded retry on conflict. Never overwrite an existing code.

**DestinationUrl** — well-formed `https`; reject destinations that point at TinyRoute’s own host (FR-CRE-06 Should).

**RefreshSession** — opaque random refresh token in an HttpOnly / Secure / SameSite cookie; only the token hash lives in Redis as `refresh:{tokenHash}` → `{userId, lastAccessAt}`, with a sliding 30-day idle TTL (NFR-SEC-06, FR-ACC-03). Used only to mint a replacement access JWT. Raw refresh tokens are never stored.

**AccessToken** — signed JWT, 15-minute expiry, HttpOnly / Secure / SameSite cookie. Claims: `sub` (user UUID), `jti`, `iat`, `exp`, `typ=ACCESS`, `tokenVersion`. No email, password data, or OAuth tokens (NFR-PRV-01). Signing keys come from the environment and include a `kid` for rotation (NFR-SEC-10). On logout, `jti` is written to `revoked-access:{jti}` until `exp`.

**RedirectLookup** — `{status, destination?, expiresAt?}`. RedirectService only redirects a known ACTIVE, unexpired lookup; malformed or incomplete cache entries are misses, never redirects. A cached entry expires no later than the link's `expiresAt`.

**ClickEvent** — successful redirect metadata: link id, timestamp, normalized referrer domain, device, operating system, browser, country, and city. It contains no visitor IP, persistent visitor identifier, raw user agent, or raw referrer path/query (FR-ANA-01..04, NFR-PRV-02/03). An IP may be used transiently for country/city lookup and is then discarded.

**AnalyticsRange / AnalyticsResponse** — the owner-supplied reporting range (`from`, `to`) and its total, daily counts, and aggregated analytics dimensions. The UI requests the last 30 days for the MVP trend (FR-ANA-03). Analytics is informational and may lag by up to one minute (NFR-CON-03).

**User 1 — owns — 0..\* Link.** **User 1 — owns — 1..\* AuthIdentity.**

MVP extensions beyond the core Must types: `CustomAlias` (FR-CRE-07), `PasswordResetToken` (FR-ACC-04), and account deletion (FR-ACC-05). They may remain visually separated in the class diagram, but are not V1 work. `ClickEvent`, `AnalyticsRange`, and `AnalyticsResponse` are MVP types. Codes are never reused after deletion or expiry.

Out of the class model: destination blocklist, public API keys, admin console, safe-browsing. Do not add OTP or profile data on `AuthIdentity`, per-provider fields on `AuthService`, Google tokens in PostgreSQL, email on `VerifyOtpRequest`, or an `smsSender` field on `AuthService`.

## Data

**PostgreSQL**

- `users(id, email_normalized UNIQUE, token_version, deleted_at NULL, created_at, updated_at)`; account deletion anonymizes personal fields but retains the row for link tombstones
- `auth_identities(id, user_id FK, provider, subject, secret_hash NULL, created_at)` UNIQUE `(provider, subject)`
- `pending_registrations(id, token_hash UNIQUE, email_normalized, password_hash, otp_hash, expires_at, attempts)`
- `password_reset_tokens(id, user_id FK, token_hash UNIQUE, expires_at, created_at)`; raw tokens are never stored and the row is deleted when consumed
- `links(id, code UNIQUE case-sensitive, owner_id FK, destination_url, status, click_count, created_at, updated_at, deleted_at, expires_at?)`
- `click_events(id, link_id FK, clicked_at, referrer_domain, device, operating_system, browser, country, city)`; it stores normalized aggregate dimensions only, never IPs, visitor identifiers, raw user agents, or raw referrer URLs.
- Index `(owner_id, created_at DESC, id DESC)` for cursor pagination.
- Index `(link_id, clicked_at DESC)` for the 30-day trend and analytics snapshots.
- FK `owner_id` RESTRICT: account deletion is Could, not MVP Must.

**Redis**

| Key                          | Value                    | TTL / notes                                                   |
| ---------------------------- | ------------------------ | ------------------------------------------------------------- |
| `redirect:{code}`            | RedirectLookup           | `min(5 s, remaining expiry)`; evict after commit of create/status/destination/delete |
| `refresh:{tokenHash}`        | `{userId, lastAccessAt}` | sliding 30 days; delete on logout; delete-all on reset/delete |
| `refresh-user:{userId}`      | set of token hashes      | supports delete-all on password reset or account deletion; expires with its last session |
| `revoked-access:{jti}`       | marker                   | remaining access-token lifetime; checked by JwtAuthenticationFilter |
| `rl:auth:{clientHash}`       | counter                  | auth cap (FR-ABS-02)                                          |
| `rl:create:{userId}`         | counter                  | create cap (FR-ABS-01)                                        |
| `rl:redirect:{clientHash}`   | counter                  | redirect throttle (FR-ABS-03 Should)                          |

Client IP is hashed for rate-limit keys and not retained beyond 24 hours (NFR-PRV-02). Click analytics stores no visitor IP or identifier; request metadata is reduced to normalized aggregate dimensions before persistence (FR-ANA-01..04, NFR-PRV-03).

**Consistency:** commit to PostgreSQL, then evict the redirect cache. Stale cache cannot outlive the ≤ 5 s TTL or a link's expiry, whichever comes first (NFR-CON-01/02, FR-RED-07). Redis loss blocks new login and refresh and fail-closes revocation checks; it does not change the database truth. Unexpired access JWTs whose `jti` cannot be checked are rejected.

## Flows

**Register / sign-in / create** — Rate-limit auth. Password register stores a pending row, emails an OTP, and sets a pending cookie. Verify OTP (cookie + `{otp}` only) creates `User` + `AuthIdentity(PASSWORD)`, an access JWT, and a refresh session. Login uses the provider map and issues the same two cookies. Google uses `GoogleOAuthClient.exchangeCode` then the same cookies (no OTP, no stored Google tokens). Create requires a valid access JWT + CSRF, `rl:create:{userId}`, https + self-host checks, optional `expiresAt`, and a new code (retry only on unique conflict), then evicts `redirect:{code}`.

**Public redirect** — Validate code + optional redirect throttle → cache-aside → on miss, case-sensitive DB lookup. Redirect only when state is **known ACTIVE, unexpired, and has a destination**; cache TTL is capped by remaining expiry. DISABLED → unavailable (no owner details). UNKNOWN/DELETED/EXPIRED → not-found. Uncertain datastore → safe 5xx, no `Location`. After a successful 3xx, `ClickCountService` runs asynchronously to increment the total and record a normalized analytics event; a capture failure may undercount but never changes the redirect.

**Owner manage** — Valid access JWT + CSRF. List is always `WHERE owner_id = :userId`. Disable, re-enable, delete, and edit destination load `id AND owner_id` in one query (missing and non-owned both 404; NFR-SEC-04). Disable/enable are idempotent. Delete tombstones the row. Destination edit re-validates https. After commit, evict the cache.

**Owner analytics** — Valid access JWT. `AnalyticsService` verifies `id AND owner_id` through `OwnershipGuard`, validates `AnalyticsRange`, then returns the total, daily counts, and aggregated referrer/device/OS/browser/country/city dimensions. The UI requests the last 30 days for the MVP trend. Missing and non-owned links both return 404 (FR-MGT-02, NFR-SEC-04).

**Refresh / logout / reset** — Refresh uses the refresh cookie to mint a new access JWT and slide the Redis TTL. Logout deletes the current refresh session and revokes the current access `jti`. Password reset and account deletion increment `tokenVersion` and delete all refresh sessions for the user. On every protected request, JwtAuthenticationFilter verifies the JWT signature, `jti` revocation state, and the current user's non-deleted state and `tokenVersion` before authorizing it.

## HTTP boundary (MVP)

Public: `GET /{code}`, `GET /actuator/health`.

Auth: `POST /api/auth/register`, `POST /api/auth/register/verify`, `POST /api/auth/register/resend-otp`, `POST /api/auth/login`, `POST /api/auth/refresh`, `GET /api/auth/{provider}/start`, `GET /api/auth/{provider}/callback`, `POST /api/auth/logout`, `GET /api/auth/me`, `POST /api/auth/password-reset`, `POST /api/auth/password-reset/confirm`, `DELETE /api/auth/account`.

Owner: `POST /api/links`, `GET /api/links`, `GET /api/links/{id}/analytics?from={ISO-8601}&to={ISO-8601}`, `PATCH /api/links/{id}/status`, `PATCH /api/links/{id}/destination`, `DELETE /api/links/{id}`.

Errors stay generic (NFR-SEC-08): 401, 403 CSRF, 400 validation, 404 missing/not owned, 409 after bounded code retries, 429 with retry guidance, safe 5xx.

## Quality targets that shape the design

Redirect p95 &lt; 150 ms / p99 &lt; 300 ms at ~100 rps (NFR-PER-01/03). Create/list/manage p95 &lt; 500 ms (NFR-PER-02). State changes visible within 5 s including cache (NFR-CON-02). Daily backups, 24 h RPO, 4 h RTO (NFR-BAK-01..03). Structured logs without secrets (NFR-OBS-01). CI, versioned migrations, repeatable deploy (NFR-MNT-01, NFR-DEP-01/02).

## Growth (documentation only)

Keep the app stateless except shared Redis refresh sessions, JWT revocation, and rate limits; add instances behind the same ingress; tune pools and indexes from load tests before adding Redis HA or a read replica. Move asynchronous analytics capture to a separate worker only when the MVP in-process path no longer meets redirect targets. No multi-region or active/active for this project (NFR-SCL-03).
