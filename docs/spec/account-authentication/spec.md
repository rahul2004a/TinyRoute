# Spec: Account authentication

## Objective

Provide a secure, understandable account-authentication lifecycle: visitors
can register with password/OTP or Google OAuth; users can sign in, remain
signed in through browser restarts, reset a password, sign out, and delete
their account. This enables authenticated URL creation and management without
making the Next.js application an authentication authority.

The backend remains the source of truth for authentication, rate limiting,
session lifetime, token revocation, CSRF, and error handling. The frontend
renders those outcomes and gives users a narrow, accessible sign-in experience.

This feature satisfies:

| Requirement | Application in this feature |
| --- | --- |
| FR-ACC-01 | Visitors can register with email/password plus OTP verification or Google OAuth. |
| FR-ACC-02 | Password/Google sign-in issues an access JWT and server-side refresh session; logout immediately revokes the current refresh session and denies the current access token. |
| FR-ACC-03 | Refresh sessions persist for up to 30 days of inactivity and can renew an expired access JWT. |
| FR-ACC-04 | A password reset uses a single-use email link and invalidates all of that user's sessions and access JWTs. |
| FR-ACC-05 | Account deletion revokes authentication, anonymizes personal data, and stops every owned short URL from redirecting. |
| FR-ABS-02 | Password login, Google flow, and refresh requests are rate-limited per client. |
| FR-CRE-02 | This feature supplies the authenticated state used to reject and route signed-out creation attempts to sign-in. |
| NFR-SEC-01 | Authentication traffic is HTTPS-only. |
| NFR-SEC-02 | Passwords use Argon2id with a unique salt and are never stored in plaintext. |
| NFR-SEC-03 | JWT signing material, OAuth client secrets, and cookie-related secrets come from the environment and are never committed. |
| NFR-SEC-05 | Request input is validated and never interpolated into queries or raw HTML. |
| NFR-SEC-06 | Access JWT and refresh token use HttpOnly, Secure, SameSite cookies; access JWT lifetime is at most 15 minutes, and Redis contains only a hash of the refresh token with a sliding 30-day idle TTL. |
| NFR-SEC-08 | Authentication failures are safe and do not expose internals. |
| NFR-SEC-09 | Logout deletes the current refresh session and revokes the current access-token `jti` until its expiry. |
| NFR-SEC-10 | JWT signing keys are environment-managed and carry a `kid` to support rotation. |
| NFR-PRV-01 | JWTs carry only the permitted identity and token claims; Google tokens and unnecessary profile data are not retained. |
| NFR-PRV-02 | Client IP is hashed for the rate-limit key and is not retained beyond 24 hours. |
| NFR-PRV-04 | Account deletion removes personal data from the live datastore within 24 hours. |
| NFR-OBS-01 | Structured logs omit passwords, JWTs, refresh tokens, session identifiers, authorization headers, and cookies. |
| NFR-MNT-01, NFR-TST-02 | Authentication behavior is covered by automated checks and contributes to the 70% coverage target. |
| NFR-DEP-02 | Any required relational schema is introduced through a versioned Flyway migration. |

### In scope

- Email/password registration with a pending-registration cookie, OTP email,
  OTP verification, and resend flow. Access and refresh tokens are issued only
  after successful verification.
- Google OAuth registration and sign-in, including state validation and the
  callback exchange; Google registration skips OTP and persists no Google
  tokens.
- Password sign-in for an existing password identity.
- A short-lived access JWT plus a renewable, opaque Redis-backed refresh
  session, `POST /api/auth/refresh`, and a signed-in-state endpoint.
- Logout of the current browser session: remove the current Redis refresh
  session, revoke the access JWT `jti`, and clear browser cookies.
- Password-reset request and confirmation, including single-use token handling
  and invalidation of all user sessions/access tokens after a successful reset.
- Authenticated account deletion, including identity removal, user
  anonymization, session/token invalidation, link tombstoning, and redirect
  cache eviction.
- Auth rate limiting, cookie/CSRF/CORS integration, and the login UI states.

### Out of scope

- Link creation, management, analytics, redirect behavior, API keys,
  blocklists, or any public redirect route. Account deletion invokes the
  existing `LinkService` only to tombstone the authenticated user's links.
- Storing Google access or refresh tokens, profile data beyond the existing
  identity subject, or adding another social provider.

Google callback processing finds the existing Google identity or creates the
`User` and `AuthIdentity(GOOGLE)` required by FR-ACC-01. It never uses a Google
token as durable application data.

## User journeys and acceptance criteria

1. A visitor can register with email/password. The system stores a pending
   registration with a hashed password and hashed OTP, sends the OTP, and sets
   only a `pending_registration` cookie. `POST /api/auth/register/verify`
   accepts the cookie plus the OTP only; on success it creates `User` and
   `AuthIdentity(PASSWORD)` and issues auth cookies. (FR-ACC-01, NFR-SEC-02,
   NFR-SEC-06)
2. A visitor can choose Google registration or sign-in. The backend creates an unpredictable
   `state`, stores it in a short-lived HttpOnly, Secure, SameSite=Lax
   `oauth_state` cookie, compares callback state in constant time, clears that
   cookie before exchanging the code, and keeps Google tokens in memory only.
   The callback resolves an identity or creates the user/Google identity
   without OTP. Missing, expired, or mismatched state fails safely. (FR-ACC-01,
   NFR-SEC-06, NFR-PRV-01)
3. A user can open the sign-in page, submit valid email/password credentials,
   and arrive at the authenticated application state. The API sets only
   HttpOnly cookies; it never returns raw tokens in JSON. (FR-ACC-02,
   NFR-SEC-06)
4. Invalid credentials, an unknown identity, a malformed request, an OAuth
   failure, and a rate-limited request give the user an actionable but safe
   response. No response distinguishes whether an email or Google subject is
   registered. (FR-ABS-02, NFR-SEC-08)
5. A signed-in user who restarts the browser can renew an expired access JWT
   with an active refresh session, up to 30 days after the most recent refresh.
   The refresh endpoint reads only the refresh cookie, touches the matching
   Redis session, and issues a replacement access JWT. (FR-ACC-03,
   NFR-SEC-06)
6. A user who signs out loses access to protected pages and APIs immediately:
   the current refresh session is deleted, the access JWT `jti` is revoked for
   its remaining lifetime, and both browser cookies are cleared. A later
   protected request with that access JWT is rejected. (FR-ACC-02,
   NFR-SEC-09)
7. A password-reset request always returns the same generic outcome. For an
   eligible password identity, the backend stores only a random-token hash and
   sends a reset link. Confirmation consumes the single-use token, replaces the
   Argon2id password secret, increments `tokenVersion`, and deletes all refresh
   sessions for that user. (FR-ACC-04, NFR-SEC-02, NFR-SEC-09)
8. An authenticated user can delete the account. One transaction increments
   `tokenVersion`, deletes all refresh sessions, removes password/OAuth
   identities, and asks `LinkService` to tombstone all owned links. After
   commit, redirect cache entries are evicted; the user is marked deleted and
   anonymized within 24 hours. (FR-ACC-05, NFR-SEC-09, NFR-PRV-04)
9. Registration, OTP verification/resend, password login, Google OAuth,
   refresh, and password-reset requests are counted against
   `rl:auth:{clientHash}`.
   When capped, the API returns `429` with retry guidance and the UI retains
   the email field without exposing credentials. (FR-ABS-02, NFR-PRV-02)
10. Every state-changing auth request, including registration, OTP
    verification, login, refresh, password reset, logout, and account deletion,
   honours Spring Security CSRF protection. Credentialed browser requests are
   accepted only from the exact frontend origin; public `go.<zone>` requests
   do not receive API-host authentication cookies. (NFR-SEC-01, NFR-SEC-06;
   ADR 0003)
11. Authentication UI works with keyboard and screen reader, visibly focuses
   controls, announces errors and session changes, works in light/dark/system
   themes, and remains usable at 320px through 1440px. (DESIGN.md)

## Architecture and contracts

### Backend responsibilities

`AuthController` translates HTTP only and calls `RateLimitService` before each
rate-limited auth operation. `RateLimitService` alone depends on
`RateLimitStore`. `AuthService` owns credential verification, provider
selection, state validation, registration, session issuance, refresh, reset,
logout, and deletion policy. It depends on interfaces rather than JPA or Redis
types: `UserRepository`, `AuthIdentityRepository`,
`PendingRegistrationRepository`, `PasswordResetTokenRepository`,
`RefreshSessionStore`, `JwtRevocationStore`, `PasswordHasher`,
`JwtTokenService`, `GoogleOAuthClient`, the existing `AuthProvider` and
`OtpSender` maps, `PasswordResetMailer`, and `LinkService` for account deletion
only.

`Jpa*` adapters own PostgreSQL access. `RedisRefreshSessionStore`,
`RedisJwtRevocationStore`, and `RedisRateLimitStore` own Redis key operations.
`JwtAuthenticationFilter` validates signature, expiry, `typ=ACCESS`, `jti`
revocation, non-deleted user status, and current `tokenVersion` before a
protected controller runs. Redis or revocation-check failure rejects the
protected request rather than allowing an unverifiable token.

The persisted auth model is architecture-defined: `users`, `auth_identities`,
`pending_registrations`, and `password_reset_tokens`. Password identities
contain an Argon2id secret hash; Google identities contain only the provider
subject; pending registrations and reset records retain only token/OTP hashes.
PostgreSQL is authoritative. Redis stores no raw refresh token and is
disposable operational state.

### HTTP boundary

The feature implements the architecture-defined auth boundary. Its stable JSON
DTOs, response/error schema, cookie, CSRF, OAuth, rate-limit, retry, and
service/adapter contracts are defined in [contracts.md](contracts.md). Spring
Security implementation choices must satisfy that contract rather than moving
policy into Next.js.

| Endpoint | Behavior |
| --- | --- |
| `POST /api/auth/register` | Rate-limits the client, validates the registration request, creates a pending registration, sends an OTP, and sets only the pending-registration cookie. |
| `POST /api/auth/register/verify` | Uses the pending-registration cookie plus OTP-only body to create a password user/identity and issue access and refresh cookies. |
| `POST /api/auth/register/resend-otp` | Rate-limits the client and sends a replacement OTP for the pending registration without issuing auth cookies. |
| `POST /api/auth/login` | Rate-limits the client, validates password credentials for an existing identity, then sets the access and refresh cookies. Invalid credentials return a safe authentication failure. |
| `GET /api/auth/google/start` | Starts the Google OAuth flow after setting the short-lived state cookie. |
| `GET /api/auth/google/callback` | Validates and clears state, exchanges the authorization code, resolves an existing Google identity or creates the required user/identity, then sets the access and refresh cookies. |
| `POST /api/auth/refresh` | Rate-limits the client, validates and touches only the refresh session, then replaces the access cookie. |
| `POST /api/auth/logout` | Requires a valid access JWT and CSRF token; deletes the current refresh session, revokes the current `jti` through access-token expiry, and clears both auth cookies. |
| `GET /api/auth/me` | Requires a valid access JWT and returns only the non-sensitive signed-in state needed by the frontend. |
| `POST /api/auth/password-reset` | Rate-limits the client and always returns the same generic outcome; an eligible password identity receives a reset link whose raw token is never persisted. |
| `POST /api/auth/password-reset/confirm` | Rate-limits the client, validates and consumes the reset token, changes the password, increments token version, and invalidates all sessions. |
| `DELETE /api/auth/account` | Requires a valid access JWT and CSRF token; deletes the account's identities and sessions, tombstones owned links, evicts their redirect cache entries, and anonymizes the user record. |

Authentication errors follow the project boundary: `400` validation failure,
`401` invalid or expired authentication, `403` CSRF failure, `429` rate limit
with retry guidance, and a safe `5xx` for unavailable dependencies. Tokens,
cookie values, OAuth codes, passwords, user IDs, and backend internals never
appear in an error payload or log.

### Session rules

- Access cookie: a signed JWT with `sub`, `jti`, `iat`, `exp`, `typ=ACCESS`,
  `tokenVersion`, and a `kid`; expires within 15 minutes.
- Refresh cookie: an opaque random value. Redis stores
  `refresh:{tokenHash} -> {userId, lastAccessAt}` and maintains a sliding
  30-day idle TTL. `refresh-user:{userId}` indexes hashes for future reset or
  account-deletion flows.
- Logout removes only the current refresh-token hash, then records
  `revoked-access:{jti}` until the access JWT expires. It must not revoke other
  device/browser sessions.
- Password reset and account deletion increment `tokenVersion` and remove all
  `refresh:{tokenHash}` records listed in `refresh-user:{userId}`. The filter
  rejects existing access JWTs whose token version no longer matches.
- Cookie attributes are HttpOnly, Secure, and SameSite as specified by the
  architecture. They are host-only to `api.<zone>`; no raw JWT or refresh
  token is readable by Next.js JavaScript.

### Frontend responsibilities

The App Router frontend uses Server Components for page structure and a narrow
Client Component for the form, submit state, and TanStack Query session state.
It calls the Spring API through the shared typed, credentialed HTTP client;
it does not use a Next.js route handler, Server Action, or browser storage for
tokens. React Hook Form and Zod provide early password/email feedback, while
the API response remains authoritative.

The `/login`, registration, and password-reset routes use DESIGN.md's narrow
centered panel: labelled controls, clear separate Google method where relevant,
one route-teal primary action, inline validation/error text, minimum 44px
controls, and a live region for auth outcomes. Registration preserves the
pending email context during OTP retry; reset requests never reveal account
existence. The sign-out control belongs in authenticated navigation/account
space, and account deletion is separated from routine settings with explicit
consequences. No token, provider subject, or internal error is rendered.

## Commands

These commands become executable once the backend and frontend bootstrap
directories exist; this specification does not bootstrap either application.

```sh
# Development infrastructure
docker compose up -d

# Backend
mvn test
mvn verify
mvn spring-boot:run

# Frontend
pnpm install --frozen-lockfile
pnpm lint
pnpm test --run
pnpm exec playwright test
pnpm build
pnpm dev
```

## Project structure

```text
backend/
  src/main/java/.../web/AuthController.java        HTTP translation
  src/main/java/.../application/AuthService.java   auth/session policy
  src/main/java/.../security/JwtAuthenticationFilter.java
  src/main/java/.../persistence/Jpa*.java          PostgreSQL adapters
  src/main/java/.../cache/Redis*.java              Redis adapters
  src/main/resources/db/migration/                 Flyway migrations
  src/test/java/.../                               JUnit, Mockito, MockMvc tests
frontend/
  app/(public)/login/page.tsx                       sign-in route
  app/(public)/register/                            registration and OTP routes
  app/(public)/password-reset/                      reset request/confirmation routes
  components/auth/                                  form and account-session UI
  lib/api/                                          shared typed HTTP client
  test/                                             Vitest/RTL tests
  e2e/                                              Playwright journeys
docs/spec/account-authentication/spec.md            this approved specification
```

Exact Java package roots and frontend file names follow the bootstrap layout;
the controller/service/filter/adapter names above are fixed architectural
roles, not a second application structure.

## Code style

Use thin controllers, cohesive services, immutable request/response records,
Jakarta validation at the backend boundary, and explicit result handling.
Never log secret-bearing inputs.

```java
@PostMapping("/api/auth/logout")
ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser user) {
    authService.logout(user);
    return ResponseEntity.noContent().build();
}
```

The controller above translates the authenticated request only. Session
deletion, JWT revocation, and transaction boundaries remain in `AuthService`
and its boundary adapters; the controller maps the service result to cookie
directives. Frontend code uses strict TypeScript, semantic HTML, Tailwind
utility classes backed by the design tokens, Radix-backed shadcn/ui primitives,
and Lucide React only.

## Testing strategy

- JUnit 5 and Mockito unit tests cover pending-registration lifecycle, OTP
  verification/resend, password-provider selection, invalid credentials,
  Google-state failure/success and first-time identity creation, access-token
  claim construction, refresh-session hashing/TTL behavior, logout ordering,
  reset-token consumption, deletion coordination, and fail-closed revocation
  checks.
- Spring Boot Test and MockMvc cover cookie attributes, CSRF enforcement,
  CORS origin rejection, generic reset outcome, `401`/`403`/`429` contracts,
  protected endpoint denial after logout/reset/deletion, and the absence of
  token values in responses/logs.
- Redis-store tests verify that only token hashes are persisted, a logout
  deletes the current session, reset/deletion deletes all sessions, and
  revocation expires with the JWT rather than remaining indefinitely.
  PostgreSQL migration tests verify the required identity, pending-registration,
  and reset-token schema is versioned through Flyway.
- Vitest and React Testing Library cover registration/OTP, sign-in, reset, and
  deletion labels; validation; disabled/loading controls; API error/rate-limit
  rendering; live-region announcements; and clearing client session state after
  logout or account deletion.
- Playwright covers password registration and sign-in, Google registration with
  a controlled provider stub, persisted-session refresh, reset invalidation of
  another session, sign-out, account deletion stopping owned redirects,
  protected-route denial, keyboard flow, and the required responsive/theme
  matrix from DESIGN.md.

## Boundaries

- Always: keep Spring Boot authoritative; validate inputs; use credentialed
  requests with CSRF; hash refresh, OTP, and reset tokens before persistence;
  fail closed on missing Redis/revocation state; redact secrets from logs; run
  relevant tests before each commit.
- Ask first: a database model beyond the architecture-defined auth tables;
  new dependencies; changed cookie/domain/SameSite policy; a new OAuth
  provider; changed CORS policy; CI, infrastructure, or production-secret
  configuration changes.
- Never: return or store raw passwords, JWTs, refresh tokens, OAuth tokens,
  or client IPs; put authentication policy in Next.js; proxy through Next.js
  route handlers; create an unverified password account; retain Google tokens;
  weaken CSRF, rate limits, token revocation, ownership enforcement, or test
  coverage; implement V1/Future API-key, blocklist, safe-browsing, or admin
  features.

## Open questions

1. What API-origin and app-origin values will local development use before the
   production `app.<zone>` and `api.<zone>` hosts are configured?
2. What local-development mail transport and reset-link base URL will be used
   before production email configuration exists?

## Approval gate

Implementation must not begin until this specification is reviewed and
approved. Planning then belongs in `tasks/plan.md`, followed by
`tasks/todo.md`; neither file is created or changed by this specification task.
