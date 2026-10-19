# Idle session recovery implementation plan (completed)

1. Inspect session refresh, CSRF caching, cookie lifetimes, and error rendering.
2. Add failing regressions for stale CSRF during refresh/login/logout and
   misleading login errors after an idle session-check failure.
3. Add a single bounded CSRF recovery helper; reuse existing authentication
   coordination. Correct login messages without exposing server internals.
4. Run unit/component, browser, lint, formatting, type, and build checks.
5. Review the diff, record evidence and limits, and archive this plan/checklist.

## Follow-up: real local routing failure and slow checks

6. Capture unmocked proxy and direct Spring responses; inspect runtime rewrites.
7. Restore API forwarding in the running local frontend and verify real JSON
   responses, including a CSRF-bound invalid login against Spring.
8. Remove automatic retries of the session lifecycle; verify one failed check
   completes promptly and explicit retry still works.
9. Re-run affected checks and real-browser/network verification, then archive.
