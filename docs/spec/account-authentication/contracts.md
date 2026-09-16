# Account-authentication contracts

This document is the stable contract for the account-authentication feature.
It supplements [spec.md](spec.md); when the documents conflict, functional
requirements and architecture take precedence. Spring Boot owns every policy
in this document. Next.js calls the API with credentials and renders its
responses; it does not create, parse, store, or authorize tokens.

## Conventions

- API base: `https://api.<zone>/api/auth`. Production browser calls originate
  only from `https://app.<zone>` with `credentials: "include"`.
- JSON uses UTF-8, camel-case member names, and `application/json`.
- Unknown request fields are rejected. Fields not stated as nullable are
  required. Request bodies are at most 16 KiB.
- Every mutation needs the `X-CSRF-TOKEN` header except the Google callback,
  which is protected by its OAuth transaction. A missing or invalid token is
  `403 CSRF_INVALID`.
- `GET /api/auth/csrf` is unauthenticated and returns a token for the exact
  allowed frontend origin. Its response is `Cache-Control: no-store`; the
  frontend holds the returned value only in memory and supplies it in the
  request header. Authentication success and logout invalidate the previous
  CSRF token, so the frontend fetches a replacement before the next mutation.
- Auth cookies are host-only to `api.<zone>`, `HttpOnly`, `Secure`, and
  `SameSite=Lax`; their `Path` is `/`. Their names and clearing attributes
  are fixed: `__Host-tinyroute_access` and `__Host-tinyroute_refresh`, both
  cleared with the same path and `Max-Age=0`. Development uses HTTPS too; it
  does not weaken production cookie attributes.
- `oauth_state` and `pending_registration` are separate short-lived,
  HttpOnly, Secure, host-only cookies, each scoped to `/api/auth` and cleared
  on successful consumption or expiry. Starting a second flow of the same type
  in the same browser replaces the first; multiple concurrent tabs are not an
  MVP guarantee.
- API responses, logs, analytics, and error bodies never contain password,
  OTP, reset token, OAuth authorization code, access token, refresh token,
  provider subject, or cookie value.

## Shared response types

```ts
type ErrorCode =
  | "VALIDATION_ERROR"
  | "AUTHENTICATION_FAILED"
  | "CSRF_INVALID"
  | "OAUTH_FAILED"
  | "OTP_INVALID"
  | "OTP_EXPIRED"
  | "RESET_TOKEN_INVALID"
  | "RATE_LIMITED"
  | "SESSION_UNAVAILABLE"
  | "SERVICE_UNAVAILABLE";

interface ApiError {
  error: {
    code: ErrorCode;
    message: string;
    fieldErrors?: Record<string, string>;
    retryAfterSeconds?: number;
    requestId: string;
  };
}

interface SessionResponse {
  authenticated: true;
  user: { email: string };
}

interface PendingRegistrationResponse {
  status: "PENDING_VERIFICATION";
}

interface GenericAcceptedResponse {
  status: "ACCEPTED";
}
```

`requestId` is an opaque correlation value. It is not a user, session, or
token identifier. `fieldErrors` is present only for syntactic/format validation
and never reveals account existence. `Retry-After` and `retryAfterSeconds`
must agree when the response is `429`.

## HTTP endpoints

### CSRF bootstrap

`GET /api/auth/csrf` returns `200`:

```json
{ "csrfToken": "opaque-value" }
```

It must issue or expose the server-expected CSRF token through the configured
Spring Security repository and return a token usable in `X-CSRF-TOKEN`. The
endpoint returns no account state and is not cacheable. It exists because the
Next.js origin cannot read host-only API cookies.

### Password registration

`POST /api/auth/register`

```json
{ "email": "user@example.com", "password": "user-entered-secret" }
```

`email` is trimmed, Unicode-NFC normalized, then lower-cased with
`Locale.ROOT`; it must be at most 254 characters and pass server-side email
validation. `password` is 12–128 characters and is never normalized. The
server creates or replaces a pending registration only when no user owns that
normalized email, stores only Argon2id and OTP hashes, sends an OTP, and sets
`pending_registration`. An existing account receives the same `202` response
without a pending-record change or email. It never issues auth cookies.

`POST /api/auth/register/verify`

```json
{ "otp": "123456" }
```

The pending-registration cookie selects the record; the body contains no email
or registration identifier. OTPs are six random digits, expire after 10
minutes, allow five failed attempts, and are atomically consumed on success.
On success, the server creates exactly one `User` and
`AuthIdentity(PASSWORD)`, clears the pending cookie, issues auth cookies, and
returns `201` with `SessionResponse`. Expired or exhausted records are removed.

`POST /api/auth/register/resend-otp` has no body and returns `202` with
`PendingRegistrationResponse`. It invalidates the preceding OTP before sending
a new one. A pending registration permits at most three resends per hour.

### Password sign-in and session inspection

`POST /api/auth/login`

```json
{ "email": "user@example.com", "password": "user-entered-secret" }
```

On valid credentials for a non-deleted password identity, the server issues
the access and refresh cookies and returns `200` with `SessionResponse`.
Unknown email, incorrect password, deleted account, or an unavailable password
identity all return `401 AUTHENTICATION_FAILED` with the same public message.

`GET /api/auth/me` requires a valid access cookie and returns `200` with
`SessionResponse`. It returns `401 AUTHENTICATION_FAILED` for missing, expired,
revoked, wrong-type, wrong-version, or deleted-user access tokens.

### Google OAuth

`GET /api/auth/google/start` is a browser navigation, not an XHR. It creates a
single-use, 10-minute authorization transaction and responds `302` to Google.
The transaction binds Google provider, fixed callback URI, random `state`, OIDC
`nonce`, and PKCE `S256` verifier/challenge. It accepts no `returnUrl`,
provider, or redirect URI input from the browser.

`GET /api/auth/google/callback?code={code}&state={state}` is invoked only by
Google. It validates and consumes the transaction before exchanging the code;
provider errors and all failure paths clear the OAuth transaction. The backend
validates the Google ID token signature, issuer, audience, expiry, nonce, and
`email_verified` before using it. `sub` is the only durable Google identity
key.

If `(GOOGLE, sub)` exists, the callback signs in that user. If it does not
exist and the normalized verified Google email is unused, it creates the user
and Google identity. If that email belongs to another account, it does not
automatically link accounts: it returns a fixed failure redirect. Account
linking is not part of this feature.

Success redirects with `303` to the configured fixed frontend URL
`https://app.<zone>/links`; failure redirects with `303` to the fixed URL
`https://app.<zone>/login?error=oauth_failed`. Neither destination is supplied
by the request. No provider token is retained after the callback completes.

### Refresh and logout

`POST /api/auth/refresh` has no body. It requires a valid refresh cookie and
CSRF header. The server verifies the stored token hash, current user state, and
token version, then rotates the refresh token atomically: the presented hash is
removed and a replacement hash/session is created before returning a new access
and refresh cookie. A refresh session has a 30-day sliding idle TTL; concurrent
use of a consumed refresh token is rejected as `401 AUTHENTICATION_FAILED` and
invalidates that session family.

`POST /api/auth/logout` has no body and requires a valid access cookie and
CSRF header. It deletes only the refresh session whose user and current access
`jti` match the authenticated access token, revokes that `jti` through its `exp`, clears
both auth cookies, and returns `204`. The client may clear its local session
view after a `204` or `401`, but it must not treat a network failure as a
successful logout.

### Password reset

`POST /api/auth/password-reset`

```json
{ "email": "user@example.com" }
```

It always returns `202` with `GenericAcceptedResponse`, whether or not an
eligible password identity exists. The public response path is timing-uniform.
For an eligible identity, it replaces any older reset token with a cryptographic
random token of at least 256 bits, stores only its hash, and expires it after
30 minutes.

The mail link is fixed to
`https://app.<zone>/password-reset/confirm#token={opaque-token}`. The fragment
is never sent in an HTTP request. The confirmation page uses
`Referrer-Policy: no-referrer`, loads no third-party content, obtains a CSRF
token, then submits the token only in the JSON body below.

`POST /api/auth/password-reset/confirm`

```json
{ "token": "opaque-token", "newPassword": "user-entered-secret" }
```

It requires CSRF, consumes the token atomically, applies the password rules,
increments `tokenVersion`, deletes all refresh sessions, and returns `204`.
Used, expired, malformed, and unknown tokens all return
`400 RESET_TOKEN_INVALID` with the same public message. It issues no session.

### Account deletion

`DELETE /api/auth/account` has no body and requires a valid access cookie and
CSRF header. It returns `204` after the relational transaction succeeds:

1. Mark the user deleted and increment `tokenVersion`.
2. Remove password and Google identities, reset records, and pending records.
3. Tombstone every owned link through `LinkService`.
4. Queue all owned redirect-cache keys for post-commit eviction.

The account row remains as an internal tombstone to preserve link foreign keys;
its email is replaced with a non-reversible, unique tombstone value. The
anonymization/delete job is idempotent, completes within 24 hours, and alerts
on retry exhaustion. Redis session deletion and cache eviction retry after the
database commit; until eviction succeeds, redirect cache TTL remains bounded to
five seconds. The endpoint requires an active session plus CSRF; fresh
reauthentication is intentionally not an MVP requirement.

## Authentication, rate-limit, and retry semantics

The access JWT has fixed claims `sub`, `jti`, `iat`, `exp`, `typ=ACCESS`,
`tokenVersion`, `iss`, `aud`, and `kid`. The backend accepts one configured
signature algorithm only, validates all required claims with a 60-second clock
skew, and retains a retired signing key only through the last possible access
token expiry. A missing/unknown `kid`, unacceptable algorithm, or unavailable
revocation store fails closed with `401` or `503 SESSION_UNAVAILABLE`; neither
authorizes a protected request.

`RateLimitService` derives `clientHash` only after the trusted ingress proxy has
supplied the configured client address. It uses an HMAC with a rotatable secret,
normalizes IPv6 representations, and retains the hash no longer than 24 hours.
Each action has an independent counter; one auth operation cannot consume
another's budget.

| Action | Limit |
| --- | --- |
| register, password login, Google start, password-reset request | 5 per 15 minutes per client |
| OTP verify | 5 per pending registration and 10 per 15 minutes per client |
| OTP resend | 3 per pending registration per hour |
| refresh | 30 per 15 minutes per session family |
| reset confirmation | 5 per reset token and 10 per 15 minutes per client |

Mutations do not support blind automatic retries. The frontend can retry only
after a known `429` window or a fresh user action. OTP verification, reset
confirmation, account deletion, and registration rely on atomic consumption or
database uniqueness to prevent duplicate effects; their retry outcome after an
unknown network failure is intentionally a new state lookup or user action,
not a repeated request.

## Service and adapter boundary

`AuthController` validates HTTP DTOs, invokes `RateLimitService` where stated,
and maps service results to this contract. `AuthService` owns transactions and
returns domain results, never HTTP objects or cookie strings. It exposes the
following application-level operations:

```java
PendingRegistration startRegistration(RegisterCommand command);
AuthenticatedSession verifyRegistration(PendingRegistrationToken token, Otp otp);
void resendRegistrationOtp(PendingRegistrationToken token);
AuthenticatedSession login(PasswordLoginCommand command);
OAuthRedirect startGoogleAuthorization();
OAuthCallbackResult finishGoogleAuthorization(OAuthCallbackCommand command);
AuthenticatedSession refresh(RefreshToken refreshToken);
void logout(AuthenticatedUser user, RefreshToken refreshToken);
void requestPasswordReset(EmailAddress email);
void confirmPasswordReset(ResetPasswordCommand command);
void deleteAccount(AuthenticatedUser user);
```

`RefreshSessionStore` accepts only token hashes and provides atomic rotate,
delete-current, and delete-all-by-user operations. `JwtRevocationStore` records
`jti` only through its expiry. `RateLimitStore` performs one atomic increment
and TTL decision per named action key. `UserRepository` and
`AuthIdentityRepository` enforce unique normalized email and unique
`(provider, subject)` constraints; the service translates uniqueness races to
safe contract outcomes. `LinkService` remains the only link-tombstoning owner.

## Compatibility and tests

Fields are additive only. Error codes, success status codes, cookie names,
paths, and redirect destinations are public compatibility commitments. Tests
must cover every endpoint’s success/error schema, CSRF renewal after login and
logout, OAuth state/nonce/PKCE failure, provider-email collision, atomic
OTP/reset consumption, refresh-token reuse, proxy-address handling, and
deletion retry/redirect-cache behavior.

## Sources

- [Spring Security CSRF for JavaScript applications](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Security OAuth 2.0 login configuration](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html)
- [Google OpenID Connect ID-token validation](https://developers.google.com/identity/openid-connect/openid-connect)
- [OAuth 2.0 Security Best Current Practice](https://datatracker.ietf.org/doc/html/rfc9700)
- [JWT Best Current Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [OWASP Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html)
