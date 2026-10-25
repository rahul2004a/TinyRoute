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
