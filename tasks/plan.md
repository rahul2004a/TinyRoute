# Landing Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans`
> to implement this plan task by task, with `superpowers:test-driven-development`.
> Implementation is inline. Only the independent final reviewer is delegated.
> Execute continuously after written plan approval; keep this plan and the
> checklist current. Do not substitute delegated implementation.

**Goal:** Give visitors a readable public landing page that explains TinyRoute,
accurately distinguishes available and planned capabilities, and leads to the
existing account and link routes.

**Architecture:** Keep the page and static content as Server Components. Use
small Client Components for mobile disclosure navigation, the existing theme
toggle, and pathname-based placement of the floating toggle. The landing page
makes no API/session request; existing account flows and Spring policy remain
authoritative.

**Tech Stack:** Node.js 24 LTS, Next.js App Router, React, strict TypeScript,
Tailwind CSS, existing Radix-based shadcn configuration, Lucide, next-themes,
pnpm, Vitest/RTL, and Playwright. Add no runtime dependency.

**Spec:** [Approved landing-page specification](../docs/spec/landing-page/spec.md).
Written-spec approval and user edits were committed as `5cae563` on 2026-10-11.
Plan and checklist approved by the user ("done") on 2026-10-11; inline execution authorized.

**Checklist:** [tasks/todo.md](todo.md).

## Global Constraints

- Follow AGENTS.md, functional/non-functional requirements, architecture, ADRs
  0001/0002/0003, and DESIGN.md. Trace requirements as recorded in the spec.
- Continue in `/Users/rahul20/Developer/Projects/TinyRoute` on
  `feature/landing-page`, the checkout the user requested for Cursor. The
  auxiliary managed worktree is detached; do not move the feature out of Cursor.
- Before editing, check branch/status and active tasks. Stop for unrelated
  changes; preserve user changes and never discard, stash, or overwrite them.
- No authentication, dashboard, backend, datastore, API, environment, hosting,
  deployment, redirect-policy, analytics, or management implementation changes.
- No new runtime dependency, fake screenshot, statistics, testimonial, price,
  anonymous shortening form, tracking, or unavailable route.
- H1: "Short links. Your control." Use the approved supporting copy and section
  order. Mark Link management and Analytics visibly as `Planned` with future
  wording. Describe analytics as aggregated and delayed by up to one minute.
- Use exactly `/`, `/register`, `/login`, `/links`, `/#features`, and
  `/#how-it-works` for the specified destinations; use real anchors/Next.js Link.
  The separately required skip link resolves to `#main-content`.
- Reuse Geist, semantic color variables, a 1200px marketing container, dark
  default, and DESIGN.md's L1 CSS motion. Keep content server-rendered.
- Header: 56px, non-sticky; mobile disclosure below 1024px; full navigation from
  1024px. Collapse page columns below 768px and stack actions below 480px.
- Minimum interactive hit area: 44 by 44px, including brand, footer, and skip
  links. Keep the primary hero CTA visible before scrolling.
- Acceptance viewports: 320 by 568, 768 by 1024, 1024 by 768, and 1440 by 900px,
  each in dark and light. Reduced motion suppresses movement/smooth scrolling.
- Mobile navigation is a non-modal disclosure. No focus trap or scroll lock.
  Escape closes it and returns focus to its trigger; section navigation focuses
  the heading; desktop resize closes it and preserves visible focus.
- Keep `data-theme`, `defaultTheme="dark"`, `enableSystem={false}`, and
  `tinyroute-color-theme`. Exactly one header toggle on `/`; existing floating
  placement elsewhere. Persist preference across reload and route changes.
- Never read auth cookies, token storage, or cached session state on the landing
  page. Fixed account links must not imply authentication.
- The approved scope includes no product tests/builds during planning. After
  plan approval, observe meaningful failing behavior before implementing each
  change, then green tests and refactoring. Use systematic debugging for failures.
- Keep task files active until all acceptance/review gates pass. Push and create
  the PR when authorized work is complete; never merge, delete branches,
  force-push, open the PR page, or deploy.

## Files and interfaces

| File                                                    | Responsibility                                                                                                             |
| ------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `frontend/src/app/theme-selector.tsx`                   | Existing toggle; add presentation-only `placement?: "floating" \| "inline"`, defaulting to floating.                       |
| `frontend/src/app/global-theme-selector.tsx`            | New client wrapper; `GlobalThemeSelector()` renders the floating selector only when pathname is not `/`.                   |
| `frontend/src/app/providers.tsx`                        | Mount the wrapper; preserve query client and theme-provider configuration.                                                 |
| `frontend/src/test/providers.test.tsx`                  | Existing provider behavior plus single-toggle/root/account-route transitions.                                              |
| `frontend/src/features/landing/landing-navigation.ts`   | Readonly section/account link arrays shared by header and footer.                                                          |
| `frontend/src/features/landing/landing-brand.tsx`       | `LandingBrand()` renders the real `/` brand link using Link2 and TinyRoute.                                                |
| `frontend/src/features/landing/landing-header.tsx`      | `LandingHeader()` renders desktop navigation, inline selector, and mobile disclosure.                                      |
| `frontend/src/features/landing/landing-header.test.tsx` | Disclosure/link/focus/resize behavior, with realistic matchMedia event fixtures.                                           |
| `frontend/src/features/landing/landing-page.tsx`        | Synchronous `LandingPage()` Server Component composing header, main sections, and footer. Keep local static sections here. |
| `frontend/src/features/landing/landing.css`             | Landing-scoped typography, composition, focus, breakpoints, and reduced-motion styling.                                    |
| `frontend/src/features/landing/landing-page.test.tsx`   | Observable content, landmarks, planned labels, and navigation contract assertions.                                         |
| `frontend/src/app/page.tsx`                             | Render `LandingPage` and import scoped landing CSS; remain a Server Component.                                             |
| `frontend/e2e/landing-page.spec.ts`                     | Browser journeys, viewport matrix, geometry, session fixtures, persistence, and motion checks.                             |
| `docs/spec/landing-page/verification.md`                | Actual results, acceptance evidence, independent-review findings, decisions, and limitations. Create during execution.     |
| `docs/spec/landing-page/evidence/`                      | Final reviewed landing screenshots, without account data or secrets.                                                       |

Do not introduce a generic navigation framework or refactor authentication
branding. The two repeated navigation groups justify one small shared data
module. Use relative imports consistently with the existing code and tests.

The shared link contracts are:

```ts
export const LANDING_SECTION_LINKS = [
  { href: "/#features", label: "Features" },
  { href: "/#how-it-works", label: "How it works" },
] as const;

export const LANDING_ACCOUNT_LINKS = [
  { href: "/login", label: "Sign in" },
  { href: "/register", label: "Get started" },
] as const;
```

The footer adds Your links at `/links`. The fragment IDs belong on the actual
section H2 elements, with `tabIndex={-1}` for focus after section navigation.
The main landmark is `id="main-content"`, also programmatically focusable.

## Dependencies and baseline

Task 1 precedes Task 2, which consumes inline theme placement. Task 3 consumes
Tasks 1/2 and integrates the actual route. Task 4 follows the completed page and
all browser acceptance checks. Update `tasks/todo.md` after every completed step
and record red/green evidence, decisions, and commits in the progress ledger.
Include updated active task records in each task commit so progress survives
handoff. Stage explicit task paths; never sweep unrelated work into a commit.

Node 24.13.0, pnpm 12.4.2, locked frontend dependencies, Playwright 1.63.0, and
local HTTPS certificates are available. The unchanged baseline was 16 test files
and 77 passing tests. Existing browser configuration uses Chromium and
`https://localhost:3000`, with fixture API origin `https://localhost:8443`.
Browser tests use contract-shaped disposable responses, not a live account or
database. No backend startup is needed.

Before coding, read the installed Next.js guides listed below. They were
consulted while planning; their actual installed APIs remain authoritative:

- `frontend/node_modules/next/dist/docs/01-app/01-getting-started/05-server-and-client-components.md`
- `frontend/node_modules/next/dist/docs/01-app/03-api-reference/04-functions/use-pathname.md`
- `frontend/node_modules/next/dist/docs/01-app/03-api-reference/02-components/link.md`
- `frontend/node_modules/next/dist/docs/01-app/02-guides/testing/vitest.md`
- `frontend/node_modules/next/dist/docs/01-app/02-guides/testing/playwright.md`

Use synchronous Server Component tests for static structure and Playwright for
the actual routed page. No rewrite or Cache Components change is needed. Keep
the existing test configuration and certificates; never stop another developer's
server or replace environment files. Confirm a reused server serves this
checkout. If a required port is occupied by unrelated work, resolve test-server
isolation without changing production behavior.

## Task 1: Preserve theme behavior while allowing header placement

**Files:** Modify `frontend/src/app/theme-selector.tsx`,
`frontend/src/app/providers.tsx`, and `frontend/src/test/providers.test.tsx`;
create `frontend/src/app/global-theme-selector.tsx`.

**Consumes:** Existing next-themes provider/hydration behavior and `usePathname`.
**Produces:** `ThemeSelector({ placement?: "floating" | "inline" })` with the
existing no-prop behavior, and `GlobalThemeSelector(): React.JSX.Element | null`.
No new session or query behavior. **Acceptance:** AC-01/09/11.

- [x] Add provider tests with a hoisted mutable pathname mock, defaulting to
      `/login` for existing tests. At `/`, render one inline selector as a child of
      Providers and assert there is exactly one theme-action button in its header.
      Rerender at `/login` without the landing header and assert exactly one floating
      toggle remains. Retain the existing keyboard/persistence/no-request assertions.
      Core behavioral assertion after the test's normal setup:

  ```tsx
  render(
    <Providers>
      <header>
        <ThemeSelector placement="inline" />
      </header>
    </Providers>,
  );
  expect(
    screen.getAllByRole("button", { name: /Switch to .* mode/ }),
  ).toHaveLength(1);
  expect(within(screen.getByRole("banner")).getByRole("button")).toBeTruthy();
  ```

- [x] Run `pnpm --dir frontend test --run src/test/providers.test.tsx`; observe
      the new root test fail because the current provider adds a second toggle.
- [x] Add the presentation prop and pathname wrapper. Keep the current floating
      classes for the default placement; inline placement removes fixed positioning
      and uses DESIGN.md control treatment. Replace only the provider's toggle mount
      with the wrapper; keep storage, query, and hydration semantics unchanged.
- [x] Rerun the focused tests; expect all provider tests to pass. Refactor only
      presentation duplication while keeping behavior green. Run frontend typecheck.
- [x] Record red/green evidence and commit the task files and active records as
      `feat: support landing header theme placement`.

## Task 2: Build accessible desktop and mobile navigation

**Files:** Create `frontend/src/features/landing/landing-navigation.ts`,
`frontend/src/features/landing/landing-brand.tsx`,
`frontend/src/features/landing/landing-header.tsx`, and
`frontend/src/features/landing/landing-header.test.tsx`.

**Consumes:** Task 1's inline selector and the shared link contracts above.
**Produces:** `LandingBrand(): React.JSX.Element`,
`LandingHeader(): React.JSX.Element`, and the readonly navigation arrays.
Use stable accessible names: trigger `Menu`, desktop region `Primary
navigation`, mobile region `Mobile navigation`. **Acceptance:** AC-02/03/07/09/10.

- [x] Write header tests using RTL/user-event, cleanup, and a matchMedia fixture
      that can dispatch a real change from mobile to desktop. Assert exact hrefs and
      that hidden panel links are not accessible. Assert opening leaves focus on the
      trigger, Tab reaches Features, Shift+Tab returns, and Escape closes/restores
      focus. Add section-focus, route-link close, desktop-resize focus transfer,
      and return-to-mobile-closed tests. A central interaction case is:

  ```tsx
  const trigger = screen.getByRole("button", { name: "Menu" });
  trigger.focus();
  await user.keyboard("{Enter}");
  expect(trigger.getAttribute("aria-expanded")).toBe("true");
  await user.tab();
  expect(document.activeElement?.getAttribute("href")).toBe("/#features");
  await user.keyboard("{Escape}");
  expect(trigger.getAttribute("aria-expanded")).toBe("false");
  expect(document.activeElement).toBe(trigger);
  ```

- [x] Run `pnpm --dir frontend test --run src/features/landing/landing-header.test.tsx`.
      Confirm the new behavior is absent before implementing the components.
- [x] Implement the shared data, brand link, and compact client header. Use a
      native button, `aria-expanded`, `aria-controls`, `hidden` on the closed panel,
      and ordinary links. React owns disclosure state; refs own focus targets.
      Use a `(min-width: 1024px)` matchMedia change listener with cleanup, not a raw
      resize/scroll listener. Escape handling must cover focus in the trigger or
      open panel without intercepting unrelated controls elsewhere on the page.
- [x] Use real section URLs. For plain in-page activation, close the panel and
      focus the destination heading without preventing normal fragment navigation.
      Preserve modified-click/new-tab behavior. Account links use Next.js Link;
      disable their prefetch to keep loading deliberate. Desktop resize transfers
      focus from a disappearing panel link to its visible matching desktop link.
- [x] Style the header with Tailwind semantic variables, 56px height, 44px hit
      areas, visible focus, and desktop/mobile visibility. No modal behavior,
      animation library, or additional dependency. Include the skip link to
      `#main-content`; its visible focused position must fit the viewport.
- [x] Rerun the header and provider tests, then lint/typecheck. Expect all to
      pass. Record evidence and commit the four files and active records as
      `feat: add accessible landing navigation`.

## Task 3: Integrate the public page and prove browser acceptance

**Files:** Modify `frontend/src/app/page.tsx`; create
`frontend/src/features/landing/landing-page.tsx`,
`frontend/src/features/landing/landing.css`,
`frontend/src/features/landing/landing-page.test.tsx`, and
`frontend/e2e/landing-page.spec.ts`.

**Consumes:** Tasks 1/2, shared navigation, and approved copy/availability rules.
**Produces:** Synchronous `LandingPage(): React.JSX.Element`, rendered at `/`,
with `main-content`, `features`, and `how-it-works` focus targets, and the final
browser acceptance suite. **Acceptance:** AC-01 through AC-11.

- [ ] Before changing the existing route, add component assertions against
      HomePage for the H1, five feature topics, both adjacent Planned labels,
      ordered three-step explanation, all landmarks, section IDs, closing CTA,
      footer/year, and route destinations. Use semantic queries and scope labels to
      the corresponding feature; do not snapshot CSS classes or the entire page.
      The initial assertion must fail against the current home page:

  ```tsx
  render(
    <Providers>
      <HomePage />
    </Providers>,
  );
  expect(
    screen.getByRole("heading", {
      level: 1,
      name: "Short links. Your control.",
    }),
  ).toBeTruthy();
  expect(
    within(screen.getByRole("contentinfo"))
      .getByRole("link", {
        name: "Your links",
      })
      .getAttribute("href"),
  ).toBe("/links");
  ```

- [ ] Add Playwright acceptance tests before styling/integrating the page.
      First run `pnpm --dir frontend exec playwright test e2e/landing-page.spec.ts`;
      record the missing new page behavior as the expected failure. Scope locators
      to main/header/footer or their named regions where labels repeat.
- [ ] Implement the server page and landing-scoped CSS. Use local static section
      functions for hero, features, how-it-works, closing CTA, and footer; only the
      header/theme boundaries are client components. Use the exact approved H1 and
      support copy. Keep feature availability honest and no fake previews. The
      footer reuses the two link arrays, adds Your links, and renders the year.
- [ ] Use asymmetric typography/spacing and an uneven flat feature composition.
      Keep H1 to two lines, 65-character prose measure, mobile-first stacking, and
      44px targets. Add once-only opacity/transform entrances and a complete local
      reduced-motion fallback. Do not hide overflow to mask a layout defect.
- [ ] Make the browser suite prove the following observable contracts:

  | Scenario             | Required assertions                                                                                                                                                                                                                                            |
  | -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
  | Public/no backend    | Abort `**/api/**`, visit `/`, assert content/links visible and application request list empty, including after scrolling. No account email, logout, or authenticated state.                                                                                    |
  | Registration/sign-in | Click every visible Get started/Sign in placement in isolated visits; assert `/register` or `/login` and the existing form heading.                                                                                                                            |
  | Section/footer links | Check exact hrefs; activate both section links and assert fragment, visible destination, and keyboard focus. Assert no empty, placeholder, or unexpected href.                                                                                                 |
  | Mobile disclosure    | At 320/768px test Enter, Tab/Shift+Tab, Escape/focus, link close, section-focus, and desktop/mobile resize; hidden links cannot receive keyboard focus.                                                                                                        |
  | Theme                | Set light/dark through the real toggle, reload, assert `data-theme` and storage. Navigate to account routes and assert choice and one toggle remain; return to `/` and assert header placement.                                                                |
  | `/links` entry       | In separate contexts, fixture authenticated, confirmed signed-out, and 503 session responses. Follow footer Your links; expect the existing creation form, sign-in prompt, or safe error with no destination input, respectively. No creation request is sent. |
  | Cached session       | Enter `/links` with a successful fixture, return using its Home link, then make API unavailable. On `/`, assert fixed public navigation and no cached identity/session UI or new API request.                                                                  |
  | Server rendering     | Use a JavaScript-disabled context to prove the static content and real account/footer links are present without waiting for hydration.                                                                                                                         |
  | Reduced motion       | Emulate `reduce` before navigation; assert visible content and no movement/smooth scrolling in computed landing styles. Keyboard navigation remains usable.                                                                                                    |

  Reuse existing fixture response shapes, CORS headers, and secure-origin
  configuration. Confirmed sign-out needs `/me` and `/refresh` 401; renewal may
  request CSRF. Unavailable state uses the existing generic 503 envelope.
  Fixture helpers stay private to this browser file; do not change auth policy
  or cache settings to make a test pass.

- [ ] Run the eight viewport/theme cases at the spec's exact dimensions. After
      fonts are ready and entrance motion has completed, measure document width,
      all visible interactive bounding boxes, hero CTA bounds, and H1 line height.
      Require `scrollWidth <= clientWidth`, targets at least 44px in both dimensions,
      CTA entirely inside the initial viewport, and H1 at most two lines. Repeat
      overflow/target checks for expanded mobile navigation. An essential browser
      assertion is:

  ```ts
  const overflows = await page.evaluate(
    () =>
      document.documentElement.scrollWidth >
      document.documentElement.clientWidth,
  );
  expect(overflows).toBe(false);
  const cta = await page
    .getByRole("main")
    .getByRole("link", {
      name: "Get started",
      exact: true,
    })
    .first()
    .boundingBox();
  expect(cta).not.toBeNull();
  expect(cta!.y).toBeGreaterThanOrEqual(0);
  expect(cta!.y + cta!.height).toBeLessThanOrEqual(page.viewportSize()!.height);
  ```

- [ ] Measure text/background contrast for hero/body/supporting copy, navigation,
      Planned labels, and buttons in both themes using computed styles and WCAG
      relative luminance (4.5:1 normal text, 3:1 large text). Resolve transparent
      backgrounds through ancestors. Inspect focus visibility and control contrast;
      do not claim a full accessibility certification from these focused checks.
      Assert no unexpected page or console errors during the landing journeys.
- [ ] Rerun focused component/browser checks and fix failures through
      `superpowers:systematic-debugging`; add a failing regression test before each
      behavior correction. Record passing viewport/contrast measurements and
      decisions. Commit the listed implementation/test files and active records as
      `feat: build public TinyRoute landing page`.

## Task 4: Verify and independently review the completed feature

**Files:** Create `docs/spec/landing-page/verification.md` and final screenshots
under `docs/spec/landing-page/evidence/`; update this plan/checklist and spec
status. On completion move active files to `docs/spec/landing-page/plan.md` and
`docs/spec/landing-page/todo.md`. No unrelated README/status cleanup.

**Consumes:** Implemented route, passing acceptance suite, approved spec/plan,
and actual Git/check evidence. **Produces:** reviewed feature and complete
evidence/task records ready for archiving and PR delivery. **Acceptance:** AC-12
and all preceding ACs.

- [ ] Run the full frontend checks below, inspect outputs, and record exact
      commands, pass counts, build results, and the commit tested in verification.md.
      Run type generation and build sequentially because both write `.next`.
      Independent read-only lint/format checks may be batched. Browser execution
      must not race a build writing the same Next output.

  ```sh
  pnpm --dir frontend lint
  pnpm --dir frontend format:check
  pnpm --dir frontend typecheck
  pnpm --dir frontend test --run
  pnpm --dir frontend build
  pnpm --dir frontend exec playwright test
  ```

- [ ] Inspect the built route output: `/` must retain static content, without
      requiring backend/session access. Inspect final screenshots for all eight
      matrix cases and expanded mobile menus in both themes. Save final images as
      `landing-320-dark.png`, `landing-320-light.png`, `landing-768-dark.png`,
      `landing-768-light.png`, `landing-1024-dark.png`, `landing-1024-light.png`,
      `landing-1440-dark.png`, `landing-1440-light.png`, `navigation-320-dark.png`,
      and `navigation-320-light.png` in the evidence directory. Capture only after
      acceptance checks pass; review every image and fix visible problems.
- [ ] Use `superpowers:requesting-code-review` to dispatch one fresh independent
      reviewer with read-only scope. Supply base commit `975d5d4`, current head,
      spec/plan/checklist, requirement constraints, acceptance evidence, and
      verification.md. Ask for prioritized findings with file references. Resolve
      actual reviewer/model/tool support at dispatch time; do not delegate coding.
- [ ] Apply `superpowers:receiving-code-review` before acting on findings.
      Verify feedback, fix blocking issues inline, and rerun affected checks.
      Record any justified deferred minor finding and its impact. Use
      `superpowers:verification-before-completion` for the final claim; if fixes
      change behavior/layout, repeat the affected browser evidence and checks.
- [ ] Mark every acceptance criterion verified in the evidence record and every
      completed checkbox in todo.md. Link screenshots and record the fixture-based
      integration limitation. This change does not prove live backend integration,
      latency, production availability, or the planned capabilities.
- [ ] Check documentation links, formatting, final diff, whitespace, and intended
      staged paths. Confirm every task and acceptance checkbox is complete. Record
      that implementation, review, and evidence are finished and ready for the
      delivery sequence below.

## Delivery after all task and acceptance checkboxes are complete

These sequential delivery actions follow completed technical work. Record their
outcome in verification.md and Git/PR history; do not add unfinished delivery
checkboxes that would require archiving partially completed task files.

1. Use `superpowers:finishing-a-development-branch` within AGENTS.md. Move the
   completed plan/checklist to `docs/spec/landing-page/plan.md` and `todo.md`,
   preserving completed boxes. Change their relative spec links to `spec.md`
   and update specification/verification links and completion status. Verify
   links, formatting, staged paths, and whitespace again. Commit the completed
   evidence/archive as `docs: archive verified landing page tasks and evidence`.
2. Push `feature/landing-page` and create/update its PR targeting `main`, using
   `.github/pull_request_template.md`: problem/result, spec and archived documents,
   applicable FR/NFR IDs, actual check results, screenshots, and operational
   impact/limitations. Use a body file for multiline CLI text; do not open a
   browser. If a required gate remains incomplete, keep task files active and
   the PR draft instead; never archive incomplete work or claim readiness.
3. Return the PR URL, verification summary, and material limitations. Keep the
   branch for iteration; no merge, branch deletion, force-push, or deployment.

## Acceptance coverage

| Spec criterion | Tasks and evidence                                                                 |
| -------------- | ---------------------------------------------------------------------------------- |
| AC-01          | Tasks 1/3: no-request provider, aborted-API and server-rendered page tests.        |
| AC-02          | Tasks 2/3: exact account links, route journeys, three `/links` session fixtures.   |
| AC-03          | Tasks 2/3: shared href contracts, section/footer destinations and focus.           |
| AC-04          | Task 3: H1/content assertions, measured first viewport at all eight cases.         |
| AC-05          | Task 3: scoped Planned labels, feature copy, screenshots and review.               |
| AC-06          | Task 3: ordered steps, closing CTA, complete footer/year.                          |
| AC-07          | Tasks 2/3: keyboard, Escape/focus, link close, resize and hidden-link checks.      |
| AC-08          | Task 3: geometry, contrast, viewport/theme matrix and expanded-menu evidence.      |
| AC-09          | Tasks 1/2/3: placement, hydration, persistence/reload and account navigation.      |
| AC-10          | Tasks 2/3: keyboard/landmarks, computed reduced-motion styles and content.         |
| AC-11          | Tasks 1/3: no session request, fixed navigation, cached/unavailable-session tests. |
| AC-12          | Task 4: full checks, screenshot inspection, independent review, evidence and PR.   |

## Review focus

- No root API/session work or inferred authentication, including cached state.
- Existing `/links` protection and unknown-session error behavior stay intact.
- No duplicated theme toggle or preference reset across route changes.
- Real focus behavior when the disclosure closes, navigates, or resizes.
- Native links and modified-click behavior remain usable.
- Availability labels and claims match implemented product capabilities.
- Actual geometry/contrast/first-viewport results, not screenshots alone.
- Server/client boundaries, scoped CSS, reduced motion, and no new dependency.
- Complete acceptance evidence and honest fixture/runtime limitations.

## Progress ledger

Record each task's state, red/green commands/results, commits, and material
decisions here during execution. Do not replace this ledger with chat history.

| Date       | State             | Evidence or decision                                                          |
| ---------- | ----------------- | ----------------------------------------------------------------------------- |
| 2026-10-11 | Baseline          | Clean current main `975d5d4`; 16 test files / 77 tests passed before changes. |
| 2026-10-11 | Approach approved | Asymmetric text-led page, neutral sessions, Planned management/analytics.     |
| 2026-10-11 | Spec approved     | User edits and approval recorded in `5cae563`; Cursor checkout retained.      |
| 2026-10-11 | Plan saved        | Tasks 1-4 are not started; waiting for written plan/checklist approval.       |

Plan self-review: all 12 criteria map to tasks; shared names/types/IDs match;
exact paths and verification commands are specified; scope remains the public
landing page. Inline implementation is authorized; reviewer dispatch follows final verification.

| 2026-10-11 | Plan approved | User replied "done"; continuous inline execution authorized. |
| 2026-10-11 | Task 1 complete | Provider test RED: expected 1 toggle, got 2. GREEN: 3/3 provider tests; typecheck passed. Theme placement only; provider configuration unchanged. |

| 2026-10-12 | Task 2 complete | RED: four tests missing Menu control. GREEN: 7/7 focused tests; lint and typecheck passed. Resize fixture now reflects visible desktop CSS; real-browser focus checks follow. |
