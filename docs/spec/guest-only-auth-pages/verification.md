# Guest-only auth pages verification

Verified 2026-10-14 on `fix/guest-only-auth-pages`, based on `1339cbb`.
Requirements: FR-ACC-01/02/03, NFR-SEC-06, NFR-MNT-01.

- Regression tests initially failed for login navigation, existing authenticated
  sessions on both pages, and registration's missing pending-session gate.
- Final `pnpm lint`, `pnpm format:check`, `pnpm typecheck`, and `pnpm build` passed.
- Final `pnpm exec vitest run`: 92 tests passed across 18 files.
- Browser auth, landing-navigation, and link-creation regression run: 49 passed;
  the sole failure was the previous expectation that registration renders before
  checking a session with JavaScript disabled. Updated it to require reaching
  `/register`, displaying the session-check status, and hiding credentials.
- Final targeted browser run: 7/7 passed, covering password login/reload/sign-out,
  both auth routes with existing and refresh-restored sessions, history
  replacement, OTP success to settings, and the no-JavaScript navigation case.
- Existing visual-artifact generation cases were excluded; this change does not
  alter those layouts. Tracked screenshots were not regenerated.
- Updated the real-backend live test's routing/sign-out expectations. That test
  was not executed: it requires a disposable backend and SMTP sink setup.
- Reviewed the final diff and `git diff --check`. Sign-out uses global theme
  tokens because account settings is outside the auth-shell token scope.

The frontend uses the existing `useSession` API result and refresh handling;
it does not read tokens or change backend authorization, cookies, or public
short-URL redirects. Successful OTP registration retains its existing settings
navigation without a competing links redirect. Registration waits for browser
session verification; JavaScript-disabled visits remain in the checking state.

The referenced Superpowers workflows were absent from the installed session
catalog and local skill directories. Planning, regression-first implementation,
verification, and final review were performed inline using the project workflow.
