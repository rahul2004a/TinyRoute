# Idle session recovery checklist

- [x] Inspect requirements, architecture, session flow, and branch status.
- [x] Reproduce stale CSRF and misleading errors with failing tests.
- [x] Implement bounded authentication CSRF recovery and accurate error text.
- [x] Verify session recovery and existing authentication behavior.
- [x] Complete frontend checks and production build.
- [x] Review changes, document verification, and archive task files.

## Follow-up

- [x] Capture the actual proxy failure and compare it with Spring.
- [x] Restore the local API rewrite and verify unmocked API traffic.
- [x] Add a failing regression for unnecessary automatic session retries.
- [x] Make failed session checks complete promptly with explicit recovery.
- [x] Verify, review, document, and archive the completed follow-up.
