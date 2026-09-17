# Account-authentication implementation checklist

Complete tasks in order unless their dependencies permit otherwise. Do not expand a task into unrelated link, redirect, infrastructure, V1 or Future work. The project Definition of Done applies in addition to each focused check.

## Task 1: Bootstrap Spring Boot testable baseline

**Description:** Create the smallest locked-stack backend with health and MockMvc test harness; do not implement auth.

**Acceptance criteria:**
- [x] Maven uses only required locked dependencies
- [x] Actuator health starts
- [x] No profile or secret is committed

**Verification:**
- [x] Tests pass: `mvn -f backend/pom.xml test`
- [x] Build succeeds: run the relevant backend/frontend build for this slice.
- [x] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** None.

**Files likely touched:**
- `backend/pom.xml`
- `backend/src/main/java/.../TinyRouteApplication.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/.../HealthControllerTest.java`

**Estimated scope:** M (3–5 files or fewer).

## Task 2: Bootstrap strict Next.js client baseline

**Description:** Create strict Next App Router, Tailwind/shadcn prerequisites, TanStack provider and a typed credentialed HTTP client. No API routes or backend policy.

**Acceptance criteria:**
- [x] Node 24/pnpm/strict TS/lint are configured
- [x] Client maps ApiError and sends credentials
- [x] Provider smoke test needs no application call

**Verification:**
- [x] Tests pass: `pnpm --dir frontend test --run`
- [x] Build succeeds: run the relevant backend/frontend build for this slice.
- [x] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** None.

**Files likely touched:**
- `frontend/package.json`
- `frontend/tsconfig.json`
- `frontend/src/app/layout.tsx`
- `frontend/src/lib/api-client.ts`
- `frontend/src/test/providers.test.tsx`

**Estimated scope:** M (3–5 files or fewer).

## Task 3: Add local services and profiles

**Description:** Add development-only Compose PostgreSQL/Redis plus externalized Spring dev/prod profiles.

**Acceptance criteria:**
- [x] Compose has only pinned DB/cache, health checks and named PostgreSQL volume
- [x] Dev uses localhost and prod uses environment secrets/endpoints
- [x] .env.example has placeholders only

**Verification:**
- [x] Tests pass: `docker compose config && mvn -f backend/pom.xml verify`
- [x] Build succeeds: run the relevant backend/frontend build for this slice.
- [x] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Task 1.

**Files likely touched:**
- `compose.yml`
- `.env.example`
- `backend/src/main/resources/application-dev.yml`
- `backend/src/main/resources/application-prod.yml`

**Estimated scope:** M (3–5 files or fewer).

## Task 4: Persist users and identities

**Description:** Add Flyway/JPA user and local/Google identity boundaries with database-enforced normalized uniqueness.

**Acceptance criteria:**
- [x] Migration has users/identities, foreign keys and unique constraints
- [x] Jpa repositories expose only AuthService needs
- [x] Integration tests reject duplicate email and provider-subject races

**Verification:**
- [x] Tests pass: `mvn -f backend/pom.xml -Dtest='*UserRepositoryTest,*AuthIdentityRepositoryTest' test`
- [x] Build succeeds: run the relevant backend/frontend build for this slice.
- [x] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 1, 3.

**Files likely touched:**
- `backend/src/main/resources/db/migration/V1__create_users_and_auth_identities.sql`
- `backend/src/main/java/.../auth/User.java`
- `backend/src/main/java/.../auth/AuthIdentity.java`
- `backend/src/main/java/.../auth/JpaUserRepository.java`
- `backend/src/test/java/.../auth/UserPersistenceIT.java`

**Estimated scope:** M (3–5 files or fewer).

## Task 5: Add password and JWT primitives

**Description:** Implement Argon2id plus a fixed-algorithm issuer/verifier with contract claims.

**Acceptance criteria:**
- [ ] Password values are Argon2id only
- [ ] JWT validates issuer/audience/algorithm/kid/expiry/skew/token version
- [ ] Keys and secrets never log

**Verification:**
- [ ] Tests pass: `mvn -f backend/pom.xml -Dtest='*Password*Test,*Jwt*Test' test`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Task 1.

**Files likely touched:**
- `backend/src/main/java/.../auth/PasswordHasher.java`
- `backend/src/main/java/.../auth/JwtService.java`
- `backend/src/main/java/.../config/JwtProperties.java`
- `backend/src/test/java/.../auth/JwtServiceTest.java`

**Estimated scope:** M (3–5 files or fewer).

## Task 6: Add Redis auth stores and limits

**Description:** Implement RefreshSessionStore, JwtRevocationStore and RateLimitStore through Redis adapters.

**Acceptance criteria:**
- [ ] Refresh rotation is atomic and reuse kills chain
- [ ] Revocations last until access expiry
- [ ] Rate keys use trusted-proxy HMAC client hash and action limits

**Verification:**
- [ ] Tests pass: `mvn -f backend/pom.xml -Dtest='*Redis*Test,*RateLimit*Test' test`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 1, 3, 5.

**Files likely touched:**
- `backend/src/main/java/.../auth/RedisRefreshSessionStore.java`
- `backend/src/main/java/.../auth/RedisJwtRevocationStore.java`
- `backend/src/main/java/.../rate/RedisRateLimitStore.java`
- `backend/src/test/java/.../auth/RedisAuthStoresIT.java`

**Estimated scope:** M (3–5 files or fewer).

## Task 7: Wire security perimeter

**Description:** Add JwtAuthenticationFilter, exact CORS, CSRF, host-only cookie handling and JSON error translation.

**Acceptance criteria:**
- [ ] Only configured origins get credentialed CORS
- [ ] CSRF endpoint is no-store and mutations need header
- [ ] Cookie/error invariants follow contracts

**Verification:**
- [ ] Tests pass: `mvn -f backend/pom.xml -Dtest='*Security*Test,*Csrf*Test,*JwtAuthenticationFilterTest' test`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 1, 2, 5, 6.

**Files likely touched:**
- `backend/src/main/java/.../config/SecurityConfig.java`
- `backend/src/main/java/.../auth/JwtAuthenticationFilter.java`
- `backend/src/main/java/.../auth/CsrfController.java`
- `backend/src/main/java/.../auth/AuthCookieService.java`
- `backend/src/test/java/.../auth/SecurityPerimeterIT.java`

**Estimated scope:** M (3–5 files or fewer).

## Task 8: Deliver registration and OTP APIs

**Description:** Implement pending registration, verification, bounded resend and async mail without enumeration.

**Acceptance criteria:**
- [ ] Registration yields generic accepted response
- [ ] OTP activates once and handles invalid/expired/exhausted cases
- [ ] Raw OTP is neither stored nor logged

**Verification:**
- [ ] Tests pass: `mvn -f backend/pom.xml -Dtest='*Registration*Test,*Otp*Test' test`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 4–7.

**Files likely touched:**
- `backend/src/main/resources/db/migration/V2__create_pending_registrations.sql`
- `backend/src/main/java/.../auth/AuthService.java`
- `backend/src/main/java/.../auth/AuthController.java`
- `backend/src/main/java/.../mail/RegistrationMailAdapter.java`
- `backend/src/test/java/.../auth/RegistrationFlowIT.java`

**Estimated scope:** M (3–5 files or fewer).

## Task 9: Deliver registration and OTP UI

**Description:** Build accessible registration/verification/resend UI that delegates policy to API.

**Acceptance criteria:**
- [ ] Accessible email/password and CSRF submission
- [ ] No email-existence disclosure
- [ ] Tests cover fields, generic success, invalid OTP and keyboard use

**Verification:**
- [ ] Tests pass: `pnpm --dir frontend test --run -- registration`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 2, 7, 8.

**Files likely touched:**
- `frontend/src/app/(auth)/register/page.tsx`
- `frontend/src/app/(auth)/verify-email/page.tsx`
- `frontend/src/features/auth/registration-form.tsx`
- `frontend/src/features/auth/auth-api.ts`
- `frontend/src/features/auth/registration-form.test.tsx`

**Estimated scope:** M (3–5 files or fewer).

## Task 10: Deliver password sign-in and me APIs

**Description:** Implement verified local login and current-session retrieval with generic failures.

**Acceptance criteria:**
- [ ] Verified user receives documented session cookies
- [ ] Unknown/wrong/unverified input is indistinguishable
- [ ] me rejects stale, revoked and expired tokens

**Verification:**
- [ ] Tests pass: `mvn -f backend/pom.xml -Dtest='*PasswordLogin*Test,*CurrentSession*Test' test`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 4–8.

**Files likely touched:**
- `backend/src/main/java/.../auth/AuthController.java`
- `backend/src/main/java/.../auth/AuthService.java`
- `backend/src/main/java/.../auth/LoginRequest.java`
- `backend/src/main/java/.../auth/SessionResponse.java`
- `backend/src/test/java/.../auth/PasswordLoginIT.java`

**Estimated scope:** M (3–5 files or fewer).

## Task 11: Deliver login and session-bootstrap UI

**Description:** Build login and client session query based on backend me, not client authorization.

**Acceptance criteria:**
- [ ] Accessible generic login errors
- [ ] Authenticated UI derives from me
- [ ] Tests cover success, rate limit and expiry

**Verification:**
- [ ] Tests pass: `pnpm --dir frontend test --run -- login`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 2, 7, 10.

**Files likely touched:**
- `frontend/src/app/(auth)/login/page.tsx`
- `frontend/src/features/auth/login-form.tsx`
- `frontend/src/features/auth/use-session.ts`
- `frontend/src/features/auth/auth-api.ts`
- `frontend/src/features/auth/login-form.test.tsx`

**Estimated scope:** M (3–5 files or fewer).

## Task 12: Deliver refresh and sign-out APIs

**Description:** Implement CSRF-bound refresh rotation and logout revocation/cookie clearing.

**Acceptance criteria:**
- [ ] Refresh rejects reuse/expiry/stale-version/bad CSRF
- [ ] Logout revokes server state before clearing
- [ ] No token appears in JSON and responses are no-store

**Verification:**
- [ ] Tests pass: `mvn -f backend/pom.xml -Dtest='*Refresh*Test,*Logout*Test' test`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 5–7, 10.

**Files likely touched:**
- `backend/src/main/java/.../auth/AuthController.java`
- `backend/src/main/java/.../auth/AuthService.java`
- `backend/src/main/java/.../auth/RefreshSessionStore.java`
- `backend/src/main/java/.../auth/AuthCookieService.java`
- `backend/src/test/java/.../auth/SessionLifecycleIT.java`

**Estimated scope:** M (3–5 files or fewer).

## Task 13: Deliver browser refresh and sign-out UX

**Description:** Connect session UI to refresh/logout; retain CSRF only in memory.

**Acceptance criteria:**
- [ ] One documented refresh recovery path
- [ ] No browser token storage
- [ ] Network logout failure remains retryable

**Verification:**
- [ ] Tests pass: `pnpm --dir frontend test --run -- session logout`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 2, 7, 11, 12.

**Files likely touched:**
- `frontend/src/features/auth/auth-api.ts`
- `frontend/src/features/auth/use-session.ts`
- `frontend/src/features/auth/logout-button.tsx`
- `frontend/src/lib/csrf.ts`
- `frontend/src/features/auth/session-lifecycle.test.tsx`

**Estimated scope:** M (3–5 files or fewer).

## Task 14: Deliver Google OIDC APIs

**Description:** Use backend OAuth2 Client for state/nonce/PKCE and validated OIDC callback.

**Acceptance criteria:**
- [ ] Start binds short-lived state/nonce/PKCE with fixed redirect URI
- [ ] Callback validates signature/issuer/audience/expiry/nonce/email_verified
- [ ] Persist only Google sub and reject collisions without auto-linking

**Verification:**
- [ ] Tests pass: `mvn -f backend/pom.xml -Dtest='*GoogleOidc*Test,*OauthCallback*Test' test`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 4–7, 12.

**Files likely touched:**
- `backend/src/main/java/.../config/OAuth2ClientConfig.java`
- `backend/src/main/java/.../auth/GoogleOidcService.java`
- `backend/src/main/java/.../auth/OAuthCallbackController.java`
- `backend/src/main/java/.../auth/OauthTransactionStore.java`
- `backend/src/test/java/.../auth/GoogleOidcFlowIT.java`

**Estimated scope:** M (3–5 files or fewer).

## Task 15: Deliver Google handoff UI

**Description:** Add Google actions and callback result presentation; browser owns no OAuth policy.

**Acceptance criteria:**
- [ ] Action starts backend authorization
- [ ] Success/failure reach documented destinations
- [ ] Tests need no provider credential

**Verification:**
- [ ] Tests pass: `pnpm --dir frontend test --run -- google oauth`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 11, 13, 14.

**Files likely touched:**
- `frontend/src/features/auth/google-sign-in-button.tsx`
- `frontend/src/app/(auth)/login/page.tsx`
- `frontend/src/app/(auth)/oauth-result/page.tsx`
- `frontend/src/features/auth/oauth-result.test.tsx`

**Estimated scope:** S (3–5 files or fewer).

## Task 16: Deliver password-reset APIs

**Description:** Implement generic request plus atomic one-time completion with hashed 256-bit opaque fragment token.

**Acceptance criteria:**
- [ ] Known/unknown email yield same accepted result
- [ ] Only hash persists and raw token is fragment-only
- [ ] Completion updates password and invalidates all sessions once

**Verification:**
- [ ] Tests pass: `mvn -f backend/pom.xml -Dtest='*PasswordReset*Test' test`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 4–8, 12.

**Files likely touched:**
- `backend/src/main/resources/db/migration/V3__create_password_reset_tokens.sql`
- `backend/src/main/java/.../auth/PasswordResetService.java`
- `backend/src/main/java/.../auth/AuthController.java`
- `backend/src/main/java/.../mail/PasswordResetMailAdapter.java`
- `backend/src/test/java/.../auth/PasswordResetFlowIT.java`

**Estimated scope:** M (3–5 files or fewer).

## Task 17: Deliver password-reset UI

**Description:** Build accessible request/completion views that consume fragment token safely in browser.

**Acceptance criteria:**
- [ ] Request is generic
- [ ] Completion validates confirmation and failure cases
- [ ] Raw token stays out of HTTP query, analytics and logs

**Verification:**
- [ ] Tests pass: `pnpm --dir frontend test --run -- password-reset`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 2, 7, 16.

**Files likely touched:**
- `frontend/src/app/(auth)/password-reset/page.tsx`
- `frontend/src/app/(auth)/password-reset/complete/page.tsx`
- `frontend/src/features/auth/password-reset-form.tsx`
- `frontend/src/features/auth/password-reset.test.tsx`

**Estimated scope:** S (3–5 files or fewer).

## Task 18: Deliver account-deletion integration

**Description:** Delete auth artifacts and call LinkService tombstone contract without direct link persistence access.

**Acceptance criteria:**
- [ ] Delete needs current auth/CSRF and ends sign-in/refresh
- [ ] Calls LinkService.tombstoneOwnedLinks(userId) and links fail closed
- [ ] Retry/cache failures are observable

**Verification:**
- [ ] Tests pass: `mvn -f backend/pom.xml -Dtest='*AccountDeletion*Test' test`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 4–7, 12; blocked until LinkService.tombstoneOwnedLinks(userId) plus cache eviction exist.

**Files likely touched:**
- `backend/src/main/java/.../auth/AuthService.java`
- `backend/src/main/java/.../auth/AuthController.java`
- `backend/src/main/java/.../links/LinkService.java`
- `backend/src/main/java/.../auth/AccountDeletionRetryJob.java`
- `backend/src/test/java/.../auth/AccountDeletionIT.java`

**Estimated scope:** M (3–5 files or fewer).

## Task 19: Deliver account-deletion UI

**Description:** Add explicit confirmation and cleanup only after server-confirmed deletion.

**Acceptance criteria:**
- [ ] Destructive action is accessible and CSRF-protected
- [ ] Confirmed response clears client state
- [ ] Failure is retryable and never falsely shows deletion

**Verification:**
- [ ] Tests pass: `pnpm --dir frontend test --run -- account-delete`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 13, 18.

**Files likely touched:**
- `frontend/src/app/(account)/settings/page.tsx`
- `frontend/src/features/auth/delete-account-dialog.tsx`
- `frontend/src/features/auth/auth-api.ts`
- `frontend/src/features/auth/delete-account-dialog.test.tsx`

**Estimated scope:** S (3–5 files or fewer).

## Task 20: Complete observability and release evidence

**Description:** Add safe telemetry/cross-layer tests and execute security, performance, runtime and contract review.

**Acceptance criteria:**
- [ ] Telemetry records safe auth outcomes and no secrets
- [ ] Contract/integration/Playwright cover documented behavior
- [ ] Evidence covers cited NFRs and review findings

**Verification:**
- [ ] Tests pass: `mvn -f backend/pom.xml verify && pnpm --dir frontend test --run && pnpm --dir frontend exec playwright test`
- [ ] Build succeeds: run the relevant backend/frontend build for this slice.
- [ ] Manual check: exercise the acceptance path and its primary rejection path.

**Dependencies:** Tasks 8–19, with Task 18 unblocked.

**Files likely touched:**
- `backend/src/main/java/.../auth/AuthObservability.java`
- `backend/src/test/java/.../auth/AuthContractIT.java`
- `frontend/e2e/account-authentication.spec.ts`
- `docs/spec/account-authentication/spec.md`
- `docs/spec/account-authentication/contracts.md`

**Estimated scope:** M (3–5 files or fewer).

## Checkpoint: security foundation

- [ ] Tasks 1–7 pass focused checks; Compose config, Maven verification, frontend lint/tests/build pass.
- [ ] Browser verification proves CSRF issue plus cross-origin/missing-CSRF rejection.
- [ ] Human confirms origins, HTTPS, trusted proxies and signing-key source.

## Checkpoint: local account lifecycle

- [ ] Tasks 8–13 pass; a user can register, verify, sign in, reload, refresh and sign out.
- [ ] Generic-error and secret-redaction checks are evidenced.
- [ ] Human reviews the local-account journey.

## Checkpoint: federated and recovery flows

- [ ] Tasks 14–17 pass; OIDC state/nonce/PKCE/claim/collision tests and reset replay/expiry/session-invalidation tests are green.
- [ ] Human confirms Google OAuth and transactional-email non-local configuration.

## Checkpoint: ready for implementation review

- [ ] Tasks 1–20 are checked only after every acceptance/focused verification item passes.
- [ ] `git diff --check` is clean and no secret, token, reset link or local-volume data is tracked.
- [ ] FR-ACC-01…05, FR-ABS-02, FR-CRE-02 and applicable NFR traceability is complete.
- [ ] The Definition of Done is complete: unit/integration/runtime checks, quality/security/performance review, observability, rollback and human acceptance.
- [ ] After approval, archive this completed file and `tasks/plan.md` under `docs/spec/account-authentication/`.
