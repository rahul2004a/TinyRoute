# Landing page checklist

Feature: `landing-page`. Specification:
[spec.md](../docs/spec/landing-page/spec.md). Active plan: [plan.md](plan.md).
Execution: inline; one independent final reviewer. Implementation approved on 2026-10-11 (user: "done"). Keep incomplete work active.

## Approvals and setup

- [x] Read AGENTS.md, requirements, architecture, relevant ADRs, DESIGN.md, and the workflow.
- [x] Check branch, clean current main, worktrees, and absence of other active tasks.
- [x] Establish `feature/landing-page`; move checkout to the user's Cursor folder on request.
- [x] Establish frontend baseline: 16 test files, 77 passing tests.
- [x] Approve approach, including Planned management/analytics labels.
- [x] Save, self-review, and approve the written spec after user edits (`5cae563`).
- [x] Save and self-review the implementation plan and this checklist.
- [x] Obtain written approval of `tasks/plan.md` and `tasks/todo.md` before coding.

## Task 1: Theme placement

- [x] Add meaningful single-toggle/root/account-route tests before changing behavior.
- [x] Observe the expected failing provider test and record its output.
- [x] Add inline/floating presentation and the pathname-aware global wrapper.
- [x] Preserve provider configuration, hydration, persistence, and no-request behavior.
- [x] Pass focused provider tests and typecheck; record evidence and commit.

## Task 2: Header and mobile navigation

- [x] Add shared section/account route data and focused behavior tests first.
- [x] Observe missing behavior before implementing navigation.
- [x] Implement brand, desktop links, header theme toggle, skip link, and disclosure.
- [x] Verify keyboard opening, Tab/Shift+Tab, hidden links, and Escape/focus return.
- [x] Verify link close/section focus, modified clicks, and resize/focus transfer.
- [x] Pass focused header/provider tests, lint, and typecheck; record evidence and commit.

## Task 3: Public page and browser acceptance

- [ ] Write content and browser acceptance tests against the old home page.
- [ ] Observe expected missing landing-page behavior before changing the route.
- [ ] Implement server-rendered hero, features, steps, closing CTA, footer, and scoped CSS.
- [ ] Verify approved copy, all five topics, and adjacent accessible Planned labels.
- [ ] Verify all route/section links and three `/links` session-entry outcomes.
- [ ] Verify public page with API traffic aborted and no session/application request.
- [ ] Verify neutral navigation after cached authentication and backend failure.
- [ ] Verify server-rendered content without JavaScript.
- [ ] Verify theme reload/persistence and single-toggle placement across routes.
- [ ] Verify keyboard navigation, focus destinations, and reduced motion in-browser.
- [ ] Measure all eight viewport/theme combinations: overflow, 44px targets, H1, CTA.
- [ ] Measure expanded mobile-menu geometry and text/background contrast in both themes.
- [ ] Check console/page errors and inspect visual/focus behavior.
- [ ] Pass focused component/browser tests; record evidence/decisions and commit.

## Task 4: Review and verification

- [ ] Pass frontend lint with zero warnings.
- [ ] Pass frontend formatting check.
- [ ] Pass frontend typecheck.
- [ ] Pass the full frontend unit/component suite.
- [ ] Pass the production frontend build and inspect root route output.
- [ ] Pass the full credential-free Playwright suite.
- [ ] Save and inspect eight final landing screenshots and two expanded-menu screenshots.
- [ ] Complete one independent final review of the actual branch and evidence.
- [ ] Verify feedback; resolve blocking findings and record any deferred minor findings.
- [ ] Rerun affected checks and refresh evidence after review fixes.
- [ ] Record commands/results, all acceptance evidence, decisions, and limitations.
- [ ] Confirm documentation and links are current and every required checkbox is complete.
- [ ] Review final diff/staged paths and confirm technical work is ready for delivery.

After every technical and acceptance checkbox is complete, follow the plan's
delivery sequence: archive completed task files, commit evidence/archive, push
and create/update the PR, and return its URL/results/limitations. Record delivery
outcomes in verification.md and Git/PR history. Never merge, delete branches,
force-push, or archive incomplete work.

## Acceptance evidence

Mark each criterion only after its runtime or review evidence is recorded in
`docs/spec/landing-page/verification.md`.

- [ ] AC-01: Public page is readable without auth/backend and makes no application request.
- [ ] AC-02: Account routes and `/links` existing session handling behave correctly.
- [ ] AC-03: All links reach valid routes/sections; no placeholders.
- [ ] AC-04: One two-line H1 and primary hero CTA visible in each initial viewport.
- [ ] AC-05: Five feature topics, accurate Planned labels, no fabricated proof.
- [ ] AC-06: Ordered steps, closing CTA, and complete footer.
- [ ] AC-07: Mobile keyboard/Escape/focus/link/resize behavior.
- [ ] AC-08: Both themes at four widths; readable contrast, no overflow, 44px hit areas.
- [ ] AC-09: Persistent theme and exactly one correctly placed toggle across routes.
- [ ] AC-10: Reduced motion, keyboard operation, landmarks, and heading order.
- [ ] AC-11: No authenticated inference from successful, cached, or unavailable sessions.
- [ ] AC-12: Relevant checks and independent review pass, with evidence recorded.
