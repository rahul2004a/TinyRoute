# Account authentication release evidence

Task 20 evidence recorded on 2026-09-26. These checks ran on the local `dev`
profile with PostgreSQL and Redis; browser tests used a disposable HTTP API
stub. No real account was deleted by the browser tests.

## Checks run

| Check | Result | Scope |
| --- | --- | --- |
| `mvn -f backend/pom.xml -Dtest=AuthObservabilityFilterTest,AuthContractTest test` | 3 passed | Structured auth outcomes, secret-safe labels, CSRF and invalid-login contracts |
| `mvn -f backend/pom.xml verify` | 81 passed; JAR built | Backend unit and Spring integration tests against local PostgreSQL and Redis |
| JaCoCo report from `mvn clean verify` | 1,269 / 1,438 lines = 88.2% | Backend application classes; exceeds NFR-TST-02's 70% target |
| `pnpm --dir frontend test --run` | 36 passed | Frontend API and component behavior |
| `pnpm --dir frontend exec playwright test` | 9 passed | Chromium, HTTPS Next.js, disposable API responses |
| Frontend lint, format check, typecheck | Passed | Source quality gates |
| `pnpm --dir frontend exec next build --webpack` | Passed | Production frontend routes, including `/settings` |
| Local HTTPS smoke check | CSRF bootstrap `200`; login without CSRF `403` | Running development backend; response bodies and tokens were not printed |

The default `pnpm --dir frontend build` did not complete here. Turbopack first
could not fetch Google Fonts inside the sandbox, then failed while binding a
local process port even with network access. The supported Webpack build
completed. This is a local build-environment finding, not a claim that the
default build is healthy elsewhere.

## Contract and NFR traceability

| Requirement | Evidence and limit |
| --- | --- |
| FR-ACC-01, FR-ACC-02, FR-ACC-03 | Registration/OTP, password login, Google OIDC, refresh and logout Spring tests; browser journeys cover registration, Google success/failure handoff, persisted refresh, session expiry, sign-out and protected-screen denial. The browser API is stubbed; provider validation is tested by Spring. |
| FR-ACC-04 | `PasswordResetTest` covers hashed one-time tokens, expiry and session invalidation; browser tests cover fragment removal, invalid-token display, reset success and denial of an older browser session given the documented API response. |
| FR-ACC-05 | `AccountDeletionTest` proves owned-link tombstones, cache eviction, auth denial and CSRF. The browser test proves keyboard cancellation, confirmation, retry and client cleanup after 204. |
| FR-ABS-02, NFR-SEC-08 | Login and refresh rate-limit tests; `AuthContractTest` checks generic 401 and CSRF 403 bodies without the submitted password. |
| NFR-SEC-01 | The local backend and Playwright frontend use HTTPS. Production ingress and HTTP-to-HTTPS redirect behavior were not exercised. |
| NFR-SEC-02 | `Argon2PasswordHasherTest` proves salted Argon2id; registration and reset integration tests check hashed persistence. |
| NFR-SEC-03, NFR-SEC-10 | `JwtTokenServiceTest` checks `kid`, signature algorithm and key rejection; profile configuration tests cover external values. Production secret sourcing and key rotation remain deployment review items. |
| NFR-SEC-05 | Registration tests cover malformed and unknown JSON fields; repository tests exercise normalized identity uniqueness. Input validation and repository usage were reviewed; no dynamic query or raw HTML rendering was added here. |
| NFR-SEC-06 | `SecurityCookieTest`, `JwtTokenServiceTest`, `RedisAuthStoresTest` and refresh integration tests cover cookie flags, access expiry, hashed sessions and rotation. |
| NFR-SEC-09 | `RefreshLogoutTest`, `PasswordResetTest` and `AccountDeletionTest` cover revocation and protected-request denial. |
| NFR-PRV-01 | JWT claims and Google OIDC tests cover minimal identity and provider validation. No Google access token persistence is introduced by this task. |
| NFR-PRV-02 | Rate-limit and Redis tests cover HMAC-derived keys and bounded expiry; production client-address forwarding still needs its proxy configuration review. |
| NFR-PRV-04 | Account deletion integration tests prove immediate live-datastore anonymization and retry scheduling. A production 24-hour completion measurement is not available. |
| NFR-OBS-01 | `AuthObservabilityFilter` records method, bounded path, status and duration as JSON and records bounded route/status metrics. Its tests prove query, authorization header, cookie and password fixtures do not enter its logs or meter IDs. The filter runs before Spring Security so 403 CSRF outcomes are counted. |
| NFR-MNT-01, NFR-TST-02 | Local lint, format, tests and builds pass; JaCoCo backend line coverage is 88.2%. Frontend line coverage and every-push CI enforcement are not measured by this report. |
| NFR-DEP-02 | `mvn clean verify` applies and validates the versioned Flyway schema against local PostgreSQL. |
| DESIGN.md runtime matrix | Playwright checks the login screen at 320, 768, 1024 and 1440 pixels in light and dark browser modes with no horizontal overflow. |

JaCoCo is attached to Maven tests and emits its CSV/HTML report during
`verify`, following the [JaCoCo Maven plug-in documentation](https://www.jacoco.org/jacoco/trunk/doc/maven.html).

## Review findings and remaining release gates

- Security: the new telemetry has fixed route names and never reads a request
  body, query string, authorization header or cookie. Backend contract tests
  still prove the session cookie, CSRF, revocation and generic-error behavior.
- Runtime: Playwright verifies the frontend against contract-shaped responses;
  Spring tests verify the real HTTP/security/database/Redis stack. The two are
  not a single live-browser/backend test. The Google browser success test
  simulates the completed backend handoff, while Spring tests use a controlled
  provider client to validate the actual callback. The local HTTPS smoke check
  covered the CSRF success and rejection paths.
- Performance: no redirect throughput or p95/p99 result is claimed. This
  repository has no public redirect controller yet, so NFR-PER-01 and
  NFR-PER-03 and the specified browser check that deletion stops a live
  redirect cannot be measured. A dedicated load script and deployed runtime
  are still needed before those release claims.
- Coverage gate: the Task 20 contract/integration/Playwright criterion remains
  unchecked because the specification also calls for a browser check that
  account deletion stops a live public redirect. The public redirect route is
  outside this feature and is not implemented; API-stubbed browser tests cannot
  prove that behavior. A single live-browser/backend journey is also pending.
- Plan dependencies: Tasks 12, 13 and 14 and earlier checkpoints remain
  unchecked in `tasks/plan.md`. This evidence does not accept those tasks on
  their behalf. Task 20 stays open until its full integration and release
  review can run against the completed system.
