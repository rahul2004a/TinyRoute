# UI polish verification

## Public error page redesign

The screenshot's source was `RedirectPageRenderer`, the independent Spring Boot
HTML response for public short-link errors. The new layout retains backend
status handling and uses explicit navigation to the first validated frontend
origin. No redirect policy or frontend route changed.

Checks on 2026-10-25:

- `./mvnw -B clean test -Dit.test=RedirectContractIT,RedirectOutageIT
failsafe:integration-test failsafe:verify spotless:check`: passed. All 258
  unit tests and 7 redirect integration tests passed, including the new
  renderer navigation tests, anonymous redirects, generic missing/disabled/
  deleted/expired/case-mismatched pages, no-store, empty HEAD responses,
  no Location header on errors, throttling with Retry-After, and fail-closed
  outages. Integration tests used disposable PostgreSQL and Redis containers.
- Chromium checked the actual compiled renderer output for 403, 404, 429,
  and 503 at 320, 768, 1024, and 1440px in both system themes: all 32
  combinations passed. No horizontal overflow or page errors; exactly one H1;
  return action visible in the 900px test viewport; 44px minimum touch target;
  both links point to the configured frontend. No remote resources requested.
- Keyboard Tab reached the wordmark, then the return action with a visible
  2px outline. Reduced motion disabled entrance animation, including when
  changed dynamically. Clicking Back to TinyRoute reached an intercepted home
  fixture on the configured origin.
- Inspected the captured desktop, tablet, and mobile 404 layouts in both
  themes. The status and message separate cleanly, narrow layouts stack,
  supporting text remains readable, and the mobile action fills its column.
- Independent code review: no findings.
- `./mvnw -B -DskipTests package`: passed; packaged the Spring Boot JAR after
  the successful unit and integration test run.
- Prettier check of changed Markdown and browser evidence: passed.
- `git diff --check`: passed.

Local verification required running outside the sandbox for Chromium, Docker,
and Mockito JVM attachment. A clean build removed stale IDE-generated error
classes from the shared target directory before the successful test run.

Browser fixtures used the reserved test origin `https://app.tinyroute.test`.
The browser results verify presentation and explicit navigation; existing
MockMvc integration tests verify real HTTP status and response contracts.

Evidence: [browser results](evidence/browser-results.json),
[desktop dark](evidence/not-found-1440-dark.png),
[desktop light](evidence/not-found-1440-light.png),
[mobile dark](evidence/not-found-320-dark.png), and
[mobile light](evidence/not-found-320-light.png).

The batch stays on `fix/ui-polish` for the user's next changes. Active task
files remain in `tasks/` until the combined batch is ready for a pull request.

## Session-aware landing header

The error-page redesign was committed as `cc297a4` before this follow-up,
as requested by the user. The landing page now uses a client wrapper around
the existing shared header and reads `useSession`. Confirmed signed-in users
see Your links and Account settings in desktop and mobile navigation;
confirmed guests see the existing guest links. Unknown sessions retain a
neutral Your links entry, and background failures preserve known navigation.
Hero, closing CTA, footer, authentication policy, and redirect policy remain
unchanged. The archived landing specification explicitly points to this
user-requested header amendment.

Checks on 2026-10-25:

- `pnpm test --run`: all 27 files and 139 tests passed. The new component
  tests cover fresh/cached authentication, confirmed guests, pending checks,
  unavailable sessions, background failures, and live sign-out cache updates.
- 25 selected Playwright session, header, account-navigation, theme, and
  keyboard regressions passed. The run covered refresh-restored sessions,
  an unavailable backend, cached signed-in navigation, desktop/mobile layouts
  at 1440/320px in both themes, and reloads. Existing account routes, menu
  disclosure, modified clicks, skip-link focus, breakpoint focus transfers,
  and theme persistence also passed.
- Review found that replacing a focused neutral link could drop focus to the
  document body. Added desktop/mobile deferred-session regressions that failed
  before the fix and passed afterward. The header now hands focus to the
  replacement entry only when the old focused anchor was removed and focus
  fell to the body. A separate regression confirms it does not move focus
  away from another control. Final independent review: no findings.
- `pnpm lint`, `pnpm typecheck`, and `pnpm format:check`: passed.
- `pnpm build`: production webpack build passed after the focus correction.
- `git diff --check`: passed.

Browser command:

```sh
pnpm exec playwright test e2e/landing-page.spec.ts --grep 'public landing content|session handling|cached authentication|signed-in header|refresh-restored session|opens|preserve keyboard focus|desktop resize|focused Menu|modified account|one header toggle' --reporter=line
```

Browser tests intercept session APIs with synthetic accounts; they do not
modify real accounts. The existing cached-session test's stale empty-list
wording was updated to match the current No links yet state.

Reviewed header captures: [desktop dark](evidence/signed-in-header-1440-dark.png),
[desktop light](evidence/signed-in-header-1440-light.png),
[mobile dark](evidence/signed-in-header-320-dark.png), and
[mobile light](evidence/signed-in-header-320-light.png).

## Link creation copy

Applied the user's approved subtitle, labels, helpers, placeholders, and
Shorten link action to the signed-in creation form. The heading, field IDs,
validation, request payloads, local-time conversion, and submission/result
behavior are unchanged. Existing form, browser, and live-test selectors now
use the new accessible names.

Checks on 2026-10-25:

- `pnpm test --run src/features/links/link-create-form.test.tsx`: all 16
  existing form tests passed, including submission, expiry, reset, pending
  locking, validation, and retained inputs after failures.
- 15 existing Playwright checks passed: the shared links page in light/dark
  at 320, 768, 1024, and 1440px; keyboard creation and copying; signed-out and
  unknown sessions; validation, alias conflict, throttling, CSRF, and service
  failures. The tests use intercepted API fixtures and created no real links.
- `pnpm lint`, `pnpm typecheck`, and `pnpm format:check`: passed.
- `pnpm build`: production webpack build passed.
- Independent review caught old selectors in the live verification suite;
  those were updated and re-reviewed with no remaining findings. That live
  backend gate was not run for this text-only change.
- `git diff --check`: passed.

Browser command, using the existing local development server's API origin:

```sh
TINYROUTE_E2E_API_BASE_URL=https://localhost:3000 pnpm exec playwright test e2e/link-creation.spec.ts --grep 'links page shares|keyboard creation|signed-out and unknown|creation .* retains' --reporter=line
```

## Analytics link not found

The user explicitly requested parallel-agent implementation. A delegated
implementer updated only the analytics page's 404 markup, scoped styles, and
focused component regressions while root handled docs and browser QA. The
error now has a decorative monospace 404 beside a concise message and primary
recovery action, centered within the existing account workspace. Below 768px
the composition stacks; below 480px the action fills the available width.

The original range-aware recovery href, backend-driven generic response,
session handling, sidebar/drawer, and all other analytics states remain intact
(FR-MGT-02, NFR-SEC-04, NFR-SEC-08). No data provider footer text was changed;
the user's subsequent attribution question was answered separately.

Checks on 2026-10-25:

- Delegated focused tests: 3 files, 8 tests passed, including missing-report
  recovery with/without a date range, no disclosure of sensitive API messages,
  and account-renewal scope isolation.
- `pnpm test --run`: 27 files, all 141 tests passed.
- `pnpm lint`, `pnpm typecheck`, `pnpm format:check`: passed.
- `pnpm build`: production webpack build passed.
- Nine Playwright cases passed: light/dark 404 layouts at 320, 768, 1024,
  and 1440px plus the existing validation, empty activity, retry, generic 404,
  and expired-session regression. Checks cover one H1, hidden decorative
  status, non-disclosure, no report tables, 44px recovery targets, no page
  overflow, skip-link/keyboard focus, mobile drawer dismissal/focus return,
  preserved reporting dates, and successful recovery. At 320×480 the action
  stays reachable through natural scrolling.
- Inspected representative desktop, tablet, and mobile captures across both
  themes. The account shell stays in place, content has an intentional visual
  center, and the message/action remain readable.
- Independent task review: spec compliance passed and code quality passed,
  with no findings.
- `git diff --check`: passed.

Browser command:

```sh
pnpm exec playwright test e2e/short-link-analytics.spec.ts --grep 'analytics missing-link|validation, no activity' --reporter=line
```

Tests use intercepted API fixtures and synthetic accounts, with no real account
changes. Evidence: [desktop dark](evidence/analytics-not-found-1440-dark.png),
[desktop light](evidence/analytics-not-found-1440-light.png),
[mobile dark](evidence/analytics-not-found-320-dark.png), and
[mobile light](evidence/analytics-not-found-320-light.png).

## Signed-out workspace screens and navigation naming

Replaced three separate signed-out layouts with a shared presentational
`AccountAccessState` panel in settings, analytics, and links. It provides one
heading, a decorative lock icon, concise guidance, and one primary Sign in
action. Its own classes scope the card styling and main centering to this state.
The guest links main now fills the first viewport beneath the existing header,
with the marketing footer following it. Signed-in layouts, pending/errors,
account deletion success, and analytics 404 keep their prior behavior.

Settings used a conditional label that called the same `/links` destination
Dashboard on settings and Your links on analytics. The shared sidebar content
now consistently uses Your links on desktop and in mobile drawers. Updated the
design guidance to match the user's correction.

Checks on 2026-10-25:

- Delegated focused tests: 3 files, 23 tests passed.
- `pnpm test --run`: all 27 files and 141 tests passed.
- `pnpm lint`, `pnpm typecheck`, `pnpm format:check`: passed.
- `pnpm build`: production webpack build passed.
- `pnpm exec playwright test e2e/signed-out-workspaces.spec.ts --reporter=line`:
  25 cases passed. All three routes were checked at 320, 768, 1024, and 1440px
  in both themes, including one H1, no private controls or non-auth API calls,
  correct sign-in targets, sidebar naming, drawer focus, skip-link keyboard
  access, visible focus, 44px actions, no horizontal overflow, guest links
  footer placement, and action reachability at 320×480. The individual-link
  analytics login target retains its pathname and selected date range.
- An additional 20 selected browser cases passed: eight recaptured links guest
  cases, eight signed-in links shell regressions, password login/reload/logout,
  account deletion, refresh restoration/expiry, and analytics state recovery.
- Inspected desktop/mobile captures for all three screens across both themes.
  Corrected screenshot capture scroll position so links previews include the
  site header; this was a test-capture adjustment, with no product change.
- Independent task review: spec compliance PASS, code quality PASS, no findings.
- `git diff --check`: passed.

Browser regression command:

```sh
TINYROUTE_E2E_API_BASE_URL=https://localhost:3000 pnpm exec playwright test e2e/signed-out-workspaces.spec.ts e2e/account-authentication.spec.ts e2e/link-creation.spec.ts e2e/short-link-analytics.spec.ts --grep 'signed-out links|password sign-in persists|account deletion requires|persisted refresh session|links page shares|validation, no activity' --reporter=line
```

All browser APIs use synthetic fixtures. No real account was modified.
Evidence: [settings](evidence/signed-out-settings-1440-dark.png),
[analytics](evidence/signed-out-analytics-1440-dark.png),
[links](evidence/signed-out-links-1440-dark.png), and
[mobile links](evidence/signed-out-links-320-light.png).
