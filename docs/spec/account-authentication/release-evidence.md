# Account authentication release evidence

Task 20 evidence updated on 2026-09-29. Backend integration tests ran against
the local Compose PostgreSQL and Redis services. The contract browser suite used
disposable API responses; a separate HTTPS browser test exercised the real
frontend, backend, database, Redis, and a disposable SMTP listener. That test
created and deleted its own account.

## Checks run

| Check | Result | Scope |
| --- | --- | --- |
| `mvn -q -f backend/pom.xml clean verify` | 108 passed; JAR built | Unit, MockMvc, PostgreSQL, Redis, and Flyway migration checks |
| JaCoCo report from that run | 1,341 / 1,512 lines = 88.7% | Backend application classes; exceeds NFR-TST-02's 70% target |
| `pnpm --dir frontend test --run` | 44 passed | Frontend API, session, registration, and theme behavior |
| `pnpm --dir frontend exec playwright test` | 9 passed | Chromium, HTTPS Next.js, disposable contract-shaped API responses |
| `pnpm --dir frontend exec playwright test --config playwright.live.config.ts` | 1 passed | Real registration, OTP email, access-cookie loss with refresh on reload, expired-access logout recovery, reset email, new-password login, and account deletion |
| Frontend lint, format check, typecheck | Passed | Source quality gates |
| `pnpm --dir frontend exec next build --webpack` | Passed | Production frontend routes, including `/settings` |
| `pnpm --dir frontend audit --audit-level high` | No known vulnerabilities found | Committed frontend dependency lockfile |
| `docker compose config --quiet` | Passed | Development infrastructure configuration |

The default `pnpm --dir frontend build` did not complete in this local
environment. Turbopack could not fetch Google Fonts in the sandbox and then
could not bind a local process port. The Webpack production build completed;
the default build remains a deployment/CI check, not a claimed pass here.

The final performance review found bounded Redis work for refresh/logout and
100-session batches for delete-all. It did not establish production p95 latency;
the authentication flows have no separate latency target in the MVP requirements.

## Contract and NFR traceability

| Requirement | Evidence and limit |
| --- | --- |
| FR-ACC-01, FR-ACC-02, FR-ACC-03 | Spring tests cover registration/OTP, password login, Google OIDC, refresh, and logout. The live browser test covers registration through logout; contract browser tests cover refresh, expiry, and Google handoff. Actual Google provider authorization still requires non-local configuration. |
| FR-ACC-04 | `PasswordResetTest` covers hashed, one-time tokens, expiry, and session invalidation. The live browser test captures the reset email, follows its fragment URL, and signs in with the replacement password. |
| FR-ACC-05 | `AccountDeletionTest` covers owned-link tombstones, cache eviction, auth denial, and CSRF. Bounded cleanup has PostgreSQL/Redis integration coverage. The live browser test deletes its disposable account; stopping an actual public redirect cannot be tested until that separate route exists. |
| FR-ABS-02, NFR-SEC-08 | Login, refresh, and OTP-resend rate-limit tests, including trusted-proxy spoofing and mail-capacity saturation; registration tests compare known/new email, resend, and invalid OTP outcomes. `AuthContractTest` checks generic 401 and CSRF 403 responses. |
| FR-CRE-02 | Spring security tests reject signed-out callers on protected API requests. The link-creation endpoint and its sign-in prompt belong to the separate link feature and are not claimed as tested here. |
| NFR-SEC-01 | The local backend and both Playwright suites use HTTPS. Production ingress and HTTP-to-HTTPS behavior remain deployment checks. |
| NFR-SEC-02 | `Argon2PasswordHasherTest` proves salted Argon2id; registration and reset integration tests check hashed persistence. |
| NFR-SEC-03, NFR-SEC-10 | `JwtTokenServiceTest` validates `kid`, algorithm, key rejection, and the environment-bindable active public key. The live backend started with generated DER keys. Production secret sourcing and key rotation remain deployment review items. |
| NFR-SEC-05 | Registration tests cover malformed/unknown JSON fields; repository and concurrent OAuth/OTP tests cover normalized identity uniqueness and creation races. |
| NFR-SEC-06, NFR-SEC-09 | Cookie, JWT, Redis, refresh, logout, reset, and deletion tests cover token lifetime, hashed sessions, simultaneous rotation, the concurrent grace window and later reuse detection, logout overlapping rotation, independent devices, revocation through JWT clock skew, and protected-request denial. Browser session tests cover simultaneous same-tab and cross-tab refresh coordination. |
| NFR-PRV-01 | JWT claims and Google tests cover minimal identity data; no Google access token persistence was added. |
| NFR-PRV-02 | Rate-limit tests cover HMAC-derived keys and bounded expiry. Direct local traffic does not trust forwarded headers; production ingress CIDRs require human review. |
| NFR-PRV-04 | Account deletion tests cover immediate anonymization and retry scheduling. A production 24-hour completion measurement is not available. |
| NFR-OBS-01 | `AuthObservabilityFilter` records bounded method/path/status/duration labels and metrics. Its tests check that query, authorization, cookie, and password fixtures do not enter logs or meter IDs. |
| NFR-MNT-01, NFR-TST-02 | Local lint, format, tests, and builds pass; backend JaCoCo line coverage is 88.6%. Frontend coverage and every-push CI enforcement are not measured here. |
| NFR-DEP-02 | `mvn clean verify` validated all five Flyway migrations against local PostgreSQL. |
| DESIGN.md runtime matrix | Playwright checks 320, 768, 1024, and 1440 pixels in system, light, and dark themes without horizontal overflow. |

JaCoCo emits its CSV/HTML report during `verify`, following the
[JaCoCo Maven plug-in documentation](https://www.jacoco.org/jacoco/trunk/doc/maven.html).

## Remaining release gates

- The Google browser success test simulates the completed backend handoff;
  Spring tests use a controlled provider client to validate callback behavior.
  Live Google authorization needs the configured provider application.
- The public redirect controller is outside this authentication feature. Its
  eventual tests must verify that an account's tombstoned links do not redirect
  and measure NFR-PER-01 and NFR-PER-03 under the specified load. No redirect
  latency or throughput result is claimed here.
- Production origin, proxy, email, JWT rotation, TLS ingress, and rollback
  configuration still require human/deployment review. The human acceptance
  checkpoints remain unchecked in `tasks/plan.md` and `tasks/todo.md`; the
  active task files are therefore not archived yet.
