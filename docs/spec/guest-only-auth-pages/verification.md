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

## Links-page layout follow-up

The user requested the shared header/footer on `/links` and directed continuation
in existing files only. Reused `LandingHeader` with account navigation and
exported the existing footer from `landing-page.tsx`; no new files were added
for this follow-up. Scoped landing styles to the header/footer wrappers so the
form retains its original typography. Replaced the floating theme button with
the shared inline toggle, added the skip-link target, and marked Your links as
the current page. Updated DESIGN.md and this existing specification/checklist.

- Lint, formatting, TypeScript, and production build passed.
- Unit/component tests: 93 passed across 18 files.
- Browser auth/landing/link regressions: 57 passed; the remaining test expected
  the old Home text link. Updated it to activate the shared TinyRoute home link.
- Final affected browser run: 9/9 passed, covering that home journey plus all
  four required widths in both themes. Verified one header, footer, H1, and theme
  toggle; account destinations, active navigation, mobile Escape/focus handling,
  skip-to-content, no horizontal overflow, and theme switching.
- Live-test account-settings selectors were scoped to the footer navigation;
  real-backend live tests were not run. Existing visual artifacts were preserved.
- Final diff review and whitespace checks passed. Authentication redirects,
  session restoration, creation, and sign-out remained covered by browser tests.

## Creation form reset

The requested form reset is implemented after successful creation only. The
regression test initially failed with the destination still present, then all
15 creation-form tests passed. Four browser checks confirmed cleared fields,
copyable success, clipboard fallback, and retained values on validation/alias
failures. Lint and TypeScript checks passed. Browser/live fixtures that make a
second creation now deliberately fill the reset fields again.

The user subsequently authorized committing all prior changes and continuing
on this branch. Those completed changes are committed at `2a5ad5b`. The saved-link
listing extension is recorded in the [pagination specification](../paginated-link-history/spec.md)
and its verification record.
