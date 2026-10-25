# Landing page

> Header amendment, 2026-10-25: [the UI polish specification](../ui-polish/spec.md#session-aware-landing-header)
> supersedes this archived specification's no-session-request and fixed guest
> navigation requirements for the landing header. Other marketing content
> and account flows retain their existing behavior.

- Feature: `landing-page`.
- Classification: full specification workflow for the public marketing page,
  extending the existing home route and shared theme-control presentation.
- Approach approved: 2026-10-11 ("yes"), including `Planned` labels for link
  management and analytics.
- Written specification approved: 2026-10-11 after user review and edits.
  Saved plan/checklist approved on 2026-10-11 ("done"); inline implementation
  authorized. Visual amendments were directly requested during execution.
- Implementation and independent review completed: 2026-10-13. See
  [verification.md](verification.md) for acceptance evidence and limitations.
- Execution preference: inline with `superpowers:executing-plans`, TDD, and one
  independent final reviewer when supported. Reviewer delegation is authorized;
  delegated implementation is not authorized.
- Branch: `feature/landing-page`, based on current `main` / `origin/main` commit
  `975d5d4b52daddbb787bf7a2c24b644bd63d10d6`.
- Workspace: the original TinyRoute checkout, also used by Cursor. At the
  user's request, the feature branch was moved here from the clean managed
  worktree; the auxiliary worktree remains detached.

## Outcome and authority

Visitors understand what TinyRoute does, which capabilities are available, and
how to create an account or sign in to create and share short links. The page
also describes planned management and analytics without claiming those
capabilities are available today.

Follow [AGENTS.md](../../../AGENTS.md),
[Functional.md](../../requirements/Functional.md),
[Non-Functional.md](../../requirements/Non-Functional.md),
[architecture.md](../../architecture/architecture.md),
[ADR 0001](../../decisions/0001-frontend-stack.md),
[ADR 0002](../../decisions/0002-backend-stack.md),
[ADR 0003](../../decisions/0003-production-infrastructure-and-delivery.md), and
[DESIGN.md](../../../DESIGN.md).

The [Codex Feature Development Workflow](https://www.notion.com/p/rahultsx/Codex-Feature-Development-Workflow-3db1dfa233d181c5825feeeae73c7ce5)
was read and rechecked through the connected Notion integration on 2026-10-11.
It now names Hostinger as the backend target. AGENTS.md and ADR 0003 govern deployment:
Hostinger runs Spring Boot and Redis, Supabase supplies PostgreSQL, and Vercel
hosts Next.js. This feature includes no provisioning or deployment.

## Scope

Include the public `/` route, responsive header and mobile navigation, hero,
feature overview, how-it-works section, closing CTA, footer, and placement of the
existing theme control within the landing-page header. Reuse the existing
typography, semantic color variables, dual themes, and reduced-motion rules.

Exclude authentication changes, dashboard changes, link-management or analytics
implementation, backend APIs, schema changes, anonymous shortening, pricing,
testimonials, usage statistics, new tracking, and V1/Future features. No new
runtime dependency was needed for the initial page. The user's 2026-10-13
typewriter-component request explicitly adds Motion for the closing CTA only;
the existing navigation, hero lights, and theme control retain their implementation.

The current `/links` page provides authenticated creation. It does not yet
provide an owner list or management actions. The backend currently exposes link
creation and public redirection, without analytics endpoints or management
actions. Those missing capabilities remain explicitly `Planned` on this page.

## Requirement trace

| Requirements                | Application to this feature                                                                                                |
| --------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| FR-ACC-01/02                | Existing registration and sign-in entry points; no new authentication flow.                                                |
| FR-CRE-01/02/03/05          | Explain signed-in HTTPS shortening and copying; `/links` retains existing authentication handling.                         |
| FR-CRE-07/08                | Describe available custom aliases and expiry at creation.                                                                  |
| FR-RED-01                   | Explain that recipients can follow a working shared link without an account.                                               |
| FR-MGT-01 through FR-MGT-07 | Describe intended owner management with a visible `Planned` label; implement none of it here.                              |
| FR-ANA-01 through FR-ANA-04 | Describe intended analytics with a visible `Planned` label; show no fabricated data.                                       |
| NFR-SEC-04/06/09            | Preserve Spring-owned authorization and cookie/session handling; do not infer authentication in the landing page.          |
| NFR-SEC-08                  | Keep internal failure details out of the public page and preserve existing safe session errors.                            |
| NFR-CON-03; NFR-PRV-02/03   | Describe planned analytics as aggregated, potentially delayed by up to one minute; add no tracking or visitor identifiers. |
| NFR-MNT-01                  | Run relevant frontend checks and record acceptance evidence before PR readiness.                                           |
| DESIGN.md                   | Governs contrast, typography, layout, focus, target sizes, responsive behavior, and reduced motion.                        |

Responsive and accessibility acceptance criteria below come from the feature
brief and DESIGN.md; no additional NFR identifiers are invented. Backend latency,
uptime, and load targets are not marketing claims or new measurements for this
frontend-only feature.

## Page content and navigation

Use these sections in order, with one H1 and logical H2/H3 headings:

1. **Header:** TinyRoute brand linking to `/`, one sun/moon theme toggle, Sign in
   linking to `/login`, and Get started linking to `/register`.
2. **Hero:** H1 "Short links. Your control." Supporting copy: "Create short links
   with custom aliases and expiry. Sign in, shorten, and share from one place."
   Primary Get started links to `/register`; secondary Sign in links to `/login`.
   Keep the H1 within two lines and the primary CTA visible in the first viewport.
3. **Features**, with `id="features"`: an uneven, surface-led composition covering
   the five items below. Use real text and Lucide icons, without screenshot
   mockups, chart samples, nonfunctional controls, or invented short-link domains.
4. **How it works**, with `id="how-it-works"`: an ordered sequence explaining
   "Create an account", "Shorten your link", and "Share it anywhere". Explain
   email/password or Google registration, an HTTPS destination with optional alias
   and expiry, and copying/sharing the result. Recipients need no account to
   follow an active, unexpired link. No form is embedded here.
5. **Closing CTA:** follow the user's supplied smooth typewriter component with
   the previous bordered, rounded surface panel and centered composition:
   "Your next short link starts here." above
   "Short links. Big possibilities." and existing account links. Get started
   links to `/register`; Sign in links to `/login`. Reveal the heading once when
   it enters view, with a decorative cursor and reserved layout space. Keep the
   complete heading readable to assistive technology and without JavaScript.
   Reduced motion shows static text and hides the cursor. Use semantic theme
   colors and responsive type that fits at 320px; no Aceternity promotion or
   inert demo buttons. This visual amendment is directly authorized by the user.
6. **Footer:** linked TinyRoute brand, Features, How it works, Get started,
   Sign in, and Your links linking to `/links`, plus the current copyright year
   and TinyRoute name. Group links under Pages and Account headings. Follow the
   user's footer reference with generous spacing, branding/copyright at the left,
   link columns at the right, and a large decorative aria-hidden TinyRoute wordmark
   below, centered horizontally within the footer, with both text layers sharing
   one origin. On mobile place branding above the two link columns. Keep actual links
   and copyright readable; the decorative wordmark may have subtle contrast. Its upper part has a soft
   fade/2px blur, while the lower part remains clearer, matching the user reference.
   Use -0.02em tracking, 1.3 line height, and 8px clearance around both masked
   layers to prevent crowded letters and clipping of the descender or final glyph.
   Do not add legal, social, pricing, or other absent routes.

Feature overview content:

| Item            | Content and availability                                                                                                                                                                                                    |
| --------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Short links     | Available: turn an HTTPS destination into a short link, then copy and share it. Creation requires an account.                                                                                                               |
| Custom aliases  | Available: request a memorable alias when creating a link, subject to availability and backend validation. Do not imply every requested alias is guaranteed.                                                                |
| Expiry          | Available: choose an expiry date and time at creation; the link stops redirecting when it expires.                                                                                                                          |
| Link management | `Planned`: list, search/filter, edit destinations, disable/re-enable, and delete owned links. Do not present a dashboard preview or actionable management controls.                                                         |
| Analytics       | `Planned`: click totals, selectable daily trends defaulting to 30 days, and aggregated referrer, device, OS, browser, country, and city breakdowns. State the intended delay of up to one minute; do not call it real-time. |

`Planned` must appear as visible text adjacent to each unavailable feature's
heading, including on mobile and for screen readers. Use future-oriented copy
for these items and no release dates or delivery promises. The available items
do not need promotional availability badges.

Every navigation item is a real anchor or Next.js Link. Do not use empty `href`,
`href="#"`, disabled imitation links, or links to nonexistent analytics or
management routes. The footer's Your links entry is a neutral route label, not a
claim that the visitor is authenticated or that a management dashboard exists.

## Presentation and interaction

Use the existing DESIGN.md without replacing it or creating a competing design
system. The modern SaaS revision was authorized on 2026-10-12 (user:
"implementation starts"). The latest user-provided hero reference supersedes the
sculpture and URL/route animation concepts: remove the illustration box and
keep copy/grid still. Group the two-line headline, supporting copy, and actions on the left of
an open hero canvas. Put static perspective grid/route lines and softly
illuminated connection points in the right-side background, clear of the text.
The final user instruction adds independently fading lights with irregular
delays, repeating indefinitely, and explicitly removes playback controls.
No standalone illustration card, URL demo, imitation controls, or product
screenshot appears in the hero. The background is aria-hidden, non-interactive,
server-rendered SVG/CSS using semantic colors. A small Client Component enables
CSS opacity cycles after hydration and varies each light's duration randomly
(8–14 seconds) on animation iteration. It makes no application request and uses
no timer. Reduced motion and JavaScript-disabled rendering stay static. No
raster asset, animation library, canvas, or external image host is needed.

Use five varied feature panels, stacked section headings, a connected steps
sequence, and a spacious closing CTA. Keep the marketing container at 1200px,
Geist Sans for prose, and Geist Mono only for actual codes, URLs, dates, or
aligned numbers. Use semantic colors in both themes, the existing dark default,
8px controls, and 12px panel radii where grouping requires a panel. The reference
allows static light halos and an artwork fade mask in this hero only; do not
add another accent, text shadows, glass, oversized shadows, fabricated proof,
or content that depends on motion. The how-it-works sequence uses connected open steps
without enclosing each in a card. All behavior acceptance criteria remain.

The header follows the user's reference: spacious, non-sticky, and continuous
with the hero canvas, without a bottom divider. It is 64px high below 1024px and
80px above. Keep TinyRoute's icon/wordmark at the left and account actions/theme
at the right. The user explicitly removed Features and How it works from both
header navigation variants; retain those real section links in the footer.
Below 1024px, keep brand, theme toggle, and Menu visible with account links in
its disclosure. Above it, show Sign in and Get started inline. Below 768px, collapse page columns; below 480px, stack hero
and closing actions. Use 16px padding at the smallest widths, then 24px and 32px
as space allows. Ensure long text wraps within its container without hiding
page overflow as a workaround.

Mobile navigation is a **non-modal disclosure**, using a native button with
`aria-expanded` and `aria-controls` and a named navigation region. Hidden links
must be absent from the tab order and accessibility tree. Opening leaves focus
on the trigger; Tab and Shift+Tab follow normal document order without a focus
trap or body scroll lock. Escape from the open panel closes it and restores
focus to the trigger. Activating a link closes the panel and follows the real
destination. Footer section navigation reaches the real focusable heading. Entering desktop layout closes the
disclosure state so it does not unexpectedly reopen when returning to mobile.
If a focused panel link becomes hidden during that resize, move focus to its
visible desktop equivalent. This is not a dialog or product sidebar; no new
primitive library is required.

Provide a skip-to-main link, semantic header/main/footer landmarks, named
navigation regions, visible focus indicators, and at least 44 by 44px hit areas
for every interactive target. Brand and footer links are included in that size
requirement. Anchor destinations must be reachable with keyboard navigation.

Use the existing next-themes provider, `data-theme`, dark default, and
`tinyroute-color-theme` storage key. Mount exactly one theme toggle on `/`, in
the header; retain existing floating placement on other routes. Keep the same
sun/moon behavior, accessible action label, hydration safety, and stored choice.
Changing themes and navigating to account pages must not reset the preference.

The hero copy and grid are static. Only the six decorative light points loop,
as explicitly requested. Retain DESIGN.md's short control feedback elsewhere. Content must be visible in the
server-rendered page. Reduced motion disables movement and
smooth scrolling; essential content must not depend on animation completion.
Do not add scroll listeners, parallax, autoplay typing, or animation libraries.

## Architecture and failure behavior

Retain Next.js App Router, React, strict TypeScript, Tailwind, the existing
Radix-based shadcn configuration, and Lucide. The page and static sections are
Server Components; the navigation disclosure, theme control, and decorative
light timing are small Client Components. Keep landing-specific UI within the existing `features/` convention
and landing styles scoped to that page. The implementation plan will name exact
files and tests after this specification is approved.

The public page makes **no application API or session request** and reads no
auth cookies, browser token storage, or cached session data. Registration,
sign-in, and Your links destinations are fixed for every visitor, including
signed-in users. Do not personalize navigation with an account email, avatar,
logout control, authenticated status, or an inferred session. No backend
availability is required to render or navigate the marketing page.

`/register`, `/login`, and `/links` remain existing routes. After following
Your links, the existing session flow checks `/api/auth/me`, renews a session
when appropriate, and presents the existing sign-in prompt for a confirmed
signed-out visitor. If the session cannot be checked, the existing safe error
and retry state remains responsible for the outcome; unavailable state must not
expose an authenticated create form. The landing page does not add guards,
redirects, refresh logic, API proxies, Server Actions, or route handlers.

Spring Boot continues to own authentication, ownership, validation, rate limits,
analytics policy, and public `/{code}` redirects. No backend, datastore,
migration, infrastructure, or environment-configuration change is required.

## Acceptance criteria

| ID    | Observable result                                                                                                                                                                                                                                                                                    |
| ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| AC-01 | A signed-out visitor opens `/` successfully and receives all main content without an application API or session request. Backend failure does not make the page unreadable.                                                                                                                          |
| AC-02 | Header, hero, and closing Get started links open `/register`; Sign in links open `/login`. Footer Your links opens `/links`, whose existing session handling shows creation only after authentication, prompts a confirmed signed-out visitor, and shows a safe error if session verification fails. |
| AC-03 | Every header/footer link resolves to `/`, `/register`, `/login`, `/links`, `/#features`, or `/#how-it-works` as specified; section links reach existing headings. No placeholder or nonexistent routes appear.                                                                                       |
| AC-04 | The hero clearly states the purpose and has one H1 within two lines. Its primary Get started CTA is fully visible before scrolling at the acceptance viewports below.                                                                                                                                |
| AC-05 | All five feature topics appear. Management and analytics carry visible, accessible `Planned` labels and future-oriented copy; no fake UI, measurements, testimonials, usage totals, or real-time analytics claims appear.                                                                            |
| AC-06 | How it works explains account creation, authenticated shortening, and copying/sharing in order. The closing CTA and complete footer are present.                                                                                                                                                     |
| AC-07 | Below 1024px, keyboard users can open and traverse mobile navigation; hidden links cannot receive focus. Escape closes the panel and returns focus to its trigger. Selecting a link closes it, and desktop/mobile resizing leaves predictable closed state.                                          |
| AC-08 | At 320, 768, 1024, and 1440px in both themes, text and focus states are readable, the document has no horizontal overflow, and every interactive hit area is at least 44 by 44px.                                                                                                                    |
| AC-09 | Theme choice survives reload and navigation to account routes. There is one landing-header toggle, with a meaningful action label; other routes retain their existing toggle placement.                                                                                                              |
| AC-10 | Reduced-motion emulation suppresses movement and smooth scrolling while all content and controls remain usable. Keyboard order, landmarks, and heading hierarchy remain clear.                                                                                                                       |
| AC-11 | The landing page shows no account-specific or authenticated state for successful, unavailable, or previously cached sessions; session availability is evaluated only by the existing account flows after navigation.                                                                                 |
| AC-12 | Relevant lint, formatting, type, unit/component, build, browser, visual, and independent-review checks pass before task archiving and PR readiness. Evidence and limitations are recorded.                                                                                                           |

Use these width/height pairs for reproducible first-viewport checks: 320 by
568px, 768 by 1024px, 1024 by 768px, and 1440 by 900px. Check both themes and
capture representative desktop/mobile evidence. These pairs define AC-04;
content must still reflow at shorter heights without cropping or overlap.

## Verification and delivery

The unchanged frontend baseline passed on 2026-10-11:

- `pnpm --dir frontend test --run`: 16 test files and 77 tests passed.

This establishes the starting point, not verification of the new feature.

After written-spec and plan approvals, use TDD for disclosure behavior, theme
placement, and navigation contracts. Cover keyboard open/traversal/Escape/focus,
link activation and resize, account destinations, and available/planned copy
with meaningful component or browser assertions. Use Playwright for the actual
Server Component page, route transitions, persisted theme after reload, reduced
motion, overflow, hit areas, and first-viewport CTA visibility.

Run the repository's credential-free API fixtures for `/links` entry behavior:
authenticated, confirmed signed-out, and unavailable session responses. Also
abort application API traffic while visiting `/` and assert that it remains
readable without making session requests. Verify that unavailable sessions do
not display an authenticated creation form. Fixtures demonstrate frontend
behavior, not a live backend integration or a new authorization guarantee.

Inspect desktop/mobile screenshots in both themes, computed text/background
contrast against WCAG AA, focus visibility/order, heading names, and console
errors. Check long content and expanded navigation at the smallest width. A
screenshot alone does not prove target sizes or overflow; measure DOM geometry.

Required frontend commands:

```sh
pnpm --dir frontend lint
pnpm --dir frontend format:check
pnpm --dir frontend typecheck
pnpm --dir frontend test --run
pnpm --dir frontend build
pnpm --dir frontend exec playwright test
```

Read the installed Next.js guides before coding, as frontend/AGENTS.md requires.
Backend tests, load tests, and database setup are not required for this
frontend-only change unless implementation reveals an approved dependency on
them. Any such scope change must be raised before expanding the feature.

Following plan approval, keep active progress in `tasks/plan.md` and
`tasks/todo.md`, complete the authorized independent final review, and archive
only fully completed task files under this feature directory. Commit and push
the branch and create/update the PR using the repository template with actual
verification evidence and UI screenshots. Keep incomplete work active and the
PR draft if any required gate remains open. Do not merge, delete branches,
force-push, open the PR page, or deploy.

## Specification self-review

Reviewed on 2026-10-11 for scope, consistency, missing decisions, and ambiguity.
The specification fixes route targets, content availability labels, mobile
navigation semantics, theme placement, session-neutral behavior, and viewport
dimensions. Requirement links resolve locally. No implementation tasks or code
are authorized by approach approval alone. User review and written-spec approval
were followed by saved-plan approval before implementation. The completed
[plan](plan.md) and [checklist](todo.md) are now archived; final evidence is in
[verification.md](verification.md).

Visual revision approved and inline implementation explicitly authorized on
2026-10-12 (user: "implementation starts") after the saved
[redesign proposal](redesign-proposal.md). This supersedes the original text-only
visual direction; behavior, requirements, acceptance criteria, and scope remain
unchanged. The revised presentation was self-reviewed for consistency with
DESIGN.md and the original twelve acceptance criteria.
