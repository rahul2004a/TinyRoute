# Idle session recovery

Fix the existing session lifecycle under FR-ACC-02/03 and NFR-SEC-06/08/09.
An expired 15-minute access cookie must recover through an active 30-day
refresh session. A CSRF token cached in an idle tab can become stale when
another tab signs in or out and changes the shared API cookie. The frontend
currently propagates that rejection, while login labels every failure as an
email/password error.

## Acceptance criteria

- After an explicit `403 CSRF_INVALID`, session refresh and password login
  obtain a current CSRF token and retry once. Logout uses the same recovery.
- Retry only a confirmed CSRF rejection, which Spring rejects before executing
  the mutation. Do not retry credential failures, rate limits, network errors,
  unknown 403 responses, or server failures.
- Preserve refresh coalescing, cross-tab coordination, concurrent-rotation
  handling, and genuine signed-out behavior. A service outage stays an error.
- Login distinguishes credential failures from session/service failures and
  avoids simultaneous session-check and submission error messages.
- Verify stale CSRF recovery after simulated inactivity, bounded rejection,
  error messages, and existing session behavior with regression tests.

The reported screenshot does not establish the actual HTTP failure. Record
confirmed reproductions separately from possible infrastructure causes.
No backend token lifetime, cookie, authorization, or revocation policy changes.

## Follow-up: local API routing and session-check delay

The new report confirms local development at `https://localhost:3000`.
Unmocked requests to `/api/auth/me` and `/api/auth/csrf` return Next.js HTML
404s, while direct Spring `/api/auth/me` correctly returns JSON 401. The
running dev manifest has empty rewrites despite configured `BACKEND_ORIGIN`.
The production manifest has the expected proxy. The session query inherits
three retries, adding 1 + 2 + 4 seconds before exposing the routing failure.

- Restore the running local proxy and verify its unmocked JSON responses.
- Session queries must not automatically replay the full refresh lifecycle
  after an error; show the error promptly and allow explicit retry/focus recovery.
- Preserve the existing bounded CSRF and concurrent-refresh recovery.
- Verify the real proxy separately from browser API fixtures, which can mask
  a missing rewrite.
