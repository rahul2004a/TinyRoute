# Idle session recovery verification

Branch: `fix/idle-session-recovery`, based on clean, fetched `main` at `2d63bb3`.
Requirements: FR-ACC-02/03, NFR-SEC-06/08/09.

## Findings

- Access cookies expire after 15 minutes. Refresh cookies and Redis sessions
  have a 30-day idle lifetime; there is no intended two-hour sign-out.
- `csrf.ts` caches the token in tab memory. Login, registration verification,
  Google authentication, and logout clear the API's shared CSRF cookie. An
  already-open tab can consequently retain a rejected token.
- Refresh previously propagated the rejection. Login could reuse a rejected
  token and mapped every non-rate-limit error, including service/network
  failures, to an email/password error. The login page could display that
  message alongside its session-check error, matching the reported UI.
- This confirms a recoverable failure path, not the original request's cause.
  No failing request status, request ID, or backend log accompanied the report.
  Query retries may already recover a single stale-token refresh rejection;
  persistent service failures can also produce the original two messages.
- The running local backend's `/actuator/health` returned `200`, status `UP`,
  during investigation. This does not establish its state at the earlier error.

## Change and safety

Login, refresh, and logout share one bounded recovery helper. After an explicit
`403 CSRF_INVALID`, it clears the cached token, bootstraps a current token, and
repeats the request once. A failed recovery drops its cached token. It does not
replay network failures, 5xx responses, credential failures, rate limits, or
unrecognized 403 responses. Spring's CSRF filter rejects the request before the
controller executes, making this specific replay safe; see
[Spring Security's CSRF processing](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-components).

Existing refresh coalescing, Web Locks, rotation-conflict polling, and signed-out
handling remain covered. Login displays distinct fixed messages for credential,
CSRF, rate-limit, and service errors. Its submission error takes precedence over
the earlier session-check error. Email is preserved and password is cleared on
submission failure. No backend policy, token lifetime, or cookie change.

## Evidence

- Regression-first run: 8 new failures across the existing session API and login
  component suites, reproducing stale-token recovery gaps and duplicate alerts.
- Full Vitest run: **113 passed across 18 files**.
- Playwright: **3 passed**, covering a two-hour clock advance followed by client
  navigation and stale-CSRF refresh, stale-CSRF login without another manual
  credential submission, and a service outage with one accurate error.
- Browser fixtures fulfill every API request; they do not access or mutate real
  accounts. The clock and API state simulate inactivity and a shared CSRF
  change; they do not prove the original environment's outage or cookie state.
- Browser assertions were corrected to exclude Next.js's route announcer and
  await the background query after cached session content first renders.
- ESLint, Prettier, and TypeScript checks passed.
- Production `next build --webpack` passed, including compilation, TypeScript,
  prerendering, and build traces. The first sandboxed run could not resolve
  Google Fonts; the approved network-enabled rerun fetched the existing fonts
  and passed without configuration changes.
- Final diff and whitespace review passed. Backend files are unchanged, so no
  backend test/build run was needed for this frontend authentication repair.

The project-referenced Superpowers skills were unavailable in the installed
catalog and searched skill directories. Inspection, regression-first work,
verification, and diff review were performed inline.

## Follow-up: captured local routing failure

The user's follow-up confirmed `https://localhost:3000/login`. Unmocked probes
captured the failure that the earlier intercepted browser tests could not see:

| Request | Before reload | Time |
| --- | --- | --- |
| Frontend `/api/auth/me` | Next.js HTML 404 | 24 ms |
| Frontend `/api/auth/csrf` | Next.js HTML 404 | 160 ms |
| Direct Spring `/api/auth/me` | JSON 401 `AUTHENTICATION_FAILED` | 185 ms |

The running `.next/dev/routes-manifest.json` had empty rewrite arrays. The
configured `BACKEND_ORIGIN` and production manifest already contained the
correct Spring proxy. Reloading the dev configuration regenerated the proxy
and restored `/api/auth/csrf` to **JSON 200 in 38 ms**. No backend, cookie,
credential, or destination-policy change was needed. The exact event that
left the earlier dev process with empty rewrites was not established.

The session query also inherited TanStack Query's three retries and default
1/2/4-second backoff, adding seven seconds to a fast, permanent routing error.
See [the query retry documentation](https://tanstack.com/query/latest/docs/framework/react/guides/query-retries).
`useSession` now disables those outer retries. The existing bounded CSRF and
concurrent-rotation recovery remains inside the API lifecycle. Explicit
`refetch` and normal focus/online recovery still work; an API error never
becomes an authenticated or signed-out success.

### Additional verification

- Added regression tests with query-client retries deliberately enabled:
  both 404 and 503 originally called the session API four times. Both now call
  it once and support a successful explicit retry.
- Full Vitest run: **115 passed across 19 files**.
- Updated browser run: **4 passed**. Service-outage rendering took 370 ms;
  a missing API route rendered the error/form in 350 ms instead of waiting
  through the inherited retry backoff. These are single local observations.
- A separate isolated browser used the real frontend rewrite and Spring
  endpoints with no API interception. The login form appeared in **626 ms**,
  with JSON responses for `/me` (401), the lock's `/me` recheck (401), `/csrf`
  (200), and `/refresh` (401). There were no page alerts. This is the expected
  signed-out lifecycle for a fresh browser, not a real user's session.
- One CSRF-bound login with a deliberately nonexistent diagnostic identity
  reached Spring and returned JSON **401 `AUTHENTICATION_FAILED`** in 716 ms.
  No real account credentials were used or changed. CSRF and cookie values
  were not printed. The diagnostic script and screenshot remain outside the
  repository under `/private/tmp`.
- ESLint, Prettier, and TypeScript checks passed after the follow-up changes.
- The production build passed again after the session-query change, including
  TypeScript, static-page generation, and build traces. Final diff review and
  whitespace checks passed; all follow-up task items are complete.

The earlier CSRF fix addressed a separate recovery gap. The captured runtime
proxy failure explains the follow-up's unavailable sign-in, and automatic
query backoff explains its prolonged initial checking state. Mocked API browser
checks must be paired with an unmocked local API probe for this diagnosis.
