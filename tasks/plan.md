# UI polish implementation plan

Scope: [the UI polish specification](../docs/spec/ui-polish/spec.md).
Branch: `fix/ui-polish`; keep subsequent user-supplied fixes on this branch.

1. Update `RedirectPageRenderer` in place with responsive branded error
   markup, semantic palette, generic guidance, keyboard states, reduced
   motion, and navigation to the first validated frontend origin.
2. Verify the navigation and supported states with focused renderer tests.
   Run existing redirect contracts to protect status codes, no-store, HEAD,
   rate limiting, outage handling, and non-disclosure.
3. Render the actual Java output in Chromium at 320, 768, 1024, and 1440px in
   both themes; inspect screenshots, focus, motion preferences, and overflow.
4. Record verification and review the diff. Keep the batch active for the
   user's next changes; archive plan/checklist before making the combined PR
   ready, once all requested changes have been completed.
5. Add a session-aware landing-header wrapper using the existing `useSession`
   query. Replace guest navigation for signed-in users on desktop and mobile;
   retain a neutral links entry while the session is unknown. Preserve the
   presentational shared header and explicit account-page navigation.
6. Verify header state transitions with component and browser tests, run
   frontend checks/build, and record evidence. The error-page redesign was
   committed separately as `cc297a4` before this follow-up began.
7. Apply the user-approved plain-language creation-form copy and update
   affected accessible-name test selectors. Verify existing form/browser
   regressions and frontend checks, then commit this copy-only follow-up.

### Task 8: Analytics not-found presentation

Implement [the analytics 404 amendment](../docs/spec/ui-polish/spec.md#analytics-link-not-found)
on the existing `fix/ui-polish` branch. The user explicitly requested parallel
agent work. Root coordinates docs/browser validation; one delegated agent
owns `frontend/src/features/analytics/analytics-page.tsx`, `analytics.css`,
and the existing `analytics-page.test.tsx` if meaningful regression coverage
needs extending. Do not edit shared settings CSS or other files without
coordinating with root. Do not create branches, commit, push, or delegate again.

- Read root/frontend AGENTS, DESIGN.md, the linked amendment, current
  analytics layout/state/styles, and relevant FR-MGT-02/NFR-SEC-04/08 guidance.
- Replace only the 404 presentation with a vertically composed desktop/mobile
  status/message layout and one primary Back to analytics action. Keep Link
  not found as the H1 and preserve the existing range-aware href. Use existing
  semantic colors, fonts, and Lucide. Keep the account shell and other states.
- Run focused meaningful existing tests and inspect the diff. Root runs browser
  checks, responsive/theme QA, production build, independent review, and commit.
- Report changed files, exact checks/results, and concerns. Implement inline
  within the assigned files; leave the working tree ready for root review.

### Task 9: Consistent signed-out workspace screens

Implement [the signed-out workspace amendment](../docs/spec/ui-polish/spec.md#signed-out-workspace-screens-and-navigation-naming)
on `fix/ui-polish`, preserving completed work and existing behavior.

One delegated implementer owns a shared presentational component and CSS under
`frontend/src/features/auth/account-access-state.*`, the signed-out branches in
`delete-account-panel.tsx`, `analytics-page.tsx`, and `link-create-form.tsx`,
and `frontend/src/app/(account)/links/page.tsx` only if a scoped main class is
needed. Root independently owns sidebar naming, docs, and browser checks.
Do not edit other files or tests without coordinating with root. Do not create
branches, commit, push, or spawn more agents.

- Read root/frontend AGENTS, DESIGN.md, linked spec, current three signed-out
  states, relevant auth/session contracts, and local Next CSS/client docs.
- Build the shared single-panel sign-in presentation, centered in settings and
  analytics; keep the links footer after the initial viewport for guests.
  Keep responsive scrolling and avoid layout changes for signed-in users.
- Use the exact headings/descriptions/hrefs specified in the amendment. This
  component renders props only; existing callers retain policy/session checks.
- Run existing focused form/account/analytics tests and scoped format/lint.
- Write report to `.superpowers/sdd/plan/task-9-report.md` with changed files,
  exact checks, self-review, and concerns. Root handles independent review,
  full checks/build, browser QA, documentation, and commit.

### Task 10: Development indicator configuration

Disable the Next.js development indicator through its supported config option,
validate configuration/types/formatting, and commit separately.
