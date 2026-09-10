---
version: beta
name: TinyRoute
description: "A precise, restrained design system for a developer-focused URL shortener: quiet neutral surfaces, one route-teal accent, compact data presentation, accessible dual themes, and motion used only for feedback and orientation."
designVariance: 5
motionIntensity: 3
visualDensity: 6
interactionTier: L1 Refined Static
colors:
  primary: "#0f766e"
  primaryDark: "#2dd4bf"
  canvas: "#f6f9f8"
  canvasDark: "#070a0a"
  ink: "#0b1311"
  inkDark: "#f2f7f5"
typography:
  marketing-title:
    fontFamily: "Geist Sans"
    fontSize: 72px
    fontWeight: 600
    lineHeight: 1.05
    letterSpacing: -2.4px
  page-title:
    fontFamily: "Geist Sans"
    fontSize: 36px
    fontWeight: 600
    lineHeight: 1.15
    letterSpacing: -0.8px
  section-title:
    fontFamily: "Geist Sans"
    fontSize: 30px
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: -0.48px
  body:
    fontFamily: "Geist Sans"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0
  mono:
    fontFamily: "Geist Mono"
    fontSize: 13px
    fontWeight: 450
    lineHeight: 1.45
    letterSpacing: 0
rounded:
  control: 8px
  panel: 12px
  pill: 9999px
spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px
  section: 80px
---

# TinyRoute design system

> Make every route, state, and action easy to scan, understand, and trust.

## Authority and scope

This document governs TinyRoute presentation. It does not define product
behavior, security policy, ownership, validation, analytics policy, rate
limits, API contracts, or redirect handling. If it conflicts with
[Functional requirements](docs/requirements/Functional.md),
[non-functional requirements](docs/requirements/Non-Functional.md), or
[architecture](docs/architecture/architecture.md), those documents win.

The frontend is a Next.js HTTP client for the Spring Boot API. It renders API
results faithfully and must not recreate backend policy. In particular,
GET /{code} and every public redirect decision remain with
RedirectController and RedirectService.

This is a targeted evolution of the existing TinyRoute direction, not a new
brand. Preserve route teal, Geist typography, compact product surfaces, the
current information architecture, and the distinction between public,
authenticated, and destructive workflows.

The design-taste-frontend guidance applies to the landing page and other
marketing surfaces. It is not a dashboard skill. Product screens, data tables,
forms, and analytics follow this document and production UI engineering rules.

## 1. Visual theme and atmosphere

**Design read:** A developer-tool portfolio product for technical users, with
a calm, precise, trust-first language and an independent restrained-minimal
visual system.

**Style:** Restrained developer tool

**Keywords:** precise, calm, compact, technical, trustworthy, data-forward,
responsive, accessible

**Tone:** Confident and plain-spoken, not cinematic, playful, glossy, or
futuristic.

**Feel:** A well-made routing console where the content is the interface.

**Design dials:**

- DESIGN_VARIANCE: 5. Offset compositions are allowed on marketing pages;
  product workflows stay aligned to a predictable grid.
- MOTION_INTENSITY: 3. Static by default with purposeful hover, focus,
  pressed, and short entrance feedback.
- VISUAL_DENSITY: 6. Compact enough for link and analytics data, with enough
  whitespace to keep forms and destructive actions unambiguous.

**Interaction tier:** L1 Refined Static

**Dependencies:** CSS only for motion. Do not add Motion, GSAP, Lenis, WebGL,
or a custom scrolling library for the MVP design. If a later approved feature
needs a library, verify it against the frontend package manifest first.

Marketing pages may use an asymmetric split hero. The primary visual should be
a real TinyRoute interface or a real product screenshot. Do not assemble a
fake dashboard from decorative rectangles. Product pages prioritize scanning,
task completion, and state clarity over visual spectacle.

Use one theme across an entire page. Light and dark are both supported, but a
section must not invert independently to create artificial variety.

## 2. Color palette and roles

All component colors must reference semantic variables. RGB helpers exist for
transparent treatments. Components must not contain hard-coded colors.

~~~css
:root {
  color-scheme: light;

  /* Backgrounds */
  --color-canvas: #f6f9f8;
  --color-canvas-rgb: 246 249 248;
  --color-surface-1: #ffffff;
  --color-surface-1-rgb: 255 255 255;
  --color-surface-2: #edf3f1;
  --color-surface-2-rgb: 237 243 241;
  --color-surface-3: #e3ece9;
  --color-surface-3-rgb: 227 236 233;

  /* Borders and text */
  --color-border: #cfddd8;
  --color-border-strong: #9fb3ad;
  --color-ink: #0b1311;
  --color-ink-rgb: 11 19 17;
  --color-ink-muted: #52615d;
  --color-ink-subtle: #65746f;

  /* Brand and interaction */
  --color-primary: #0f766e;
  --color-primary-rgb: 15 118 110;
  --color-primary-hover: #115e59;
  --color-primary-active: #134e4a;
  --color-on-primary: #ffffff;
  --color-focus: #0f766e;

  /* Semantic feedback */
  --color-success: #047857;
  --color-success-rgb: 4 120 87;
  --color-warning: #a34f08;
  --color-warning-rgb: 163 79 8;
  --color-danger: #b91c1c;
  --color-danger-rgb: 185 28 28;
  --color-info: #0369a1;
  --color-info-rgb: 3 105 161;

  /* Overlay and charts */
  --color-overlay: rgb(11 19 17 / 0.68);
  --color-chart-1: #0f766e;
  --color-chart-2: #2a9188;
  --color-chart-3: #65aaa3;
  --color-chart-neutral: #9fb3ad;
}

@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
    color-scheme: dark;
    --color-canvas: #070a0a;
    --color-canvas-rgb: 7 10 10;
    --color-surface-1: #0d1212;
    --color-surface-1-rgb: 13 18 18;
    --color-surface-2: #131a1a;
    --color-surface-2-rgb: 19 26 26;
    --color-surface-3: #1a2323;
    --color-surface-3-rgb: 26 35 35;
    --color-border: #253331;
    --color-border-strong: #3a4b47;
    --color-ink: #f2f7f5;
    --color-ink-rgb: 242 247 245;
    --color-ink-muted: #a7b4b0;
    --color-ink-subtle: #82908c;
    --color-primary: #2dd4bf;
    --color-primary-rgb: 45 212 191;
    --color-primary-hover: #5eead4;
    --color-primary-active: #14b8a6;
    --color-on-primary: #03201b;
    --color-focus: #5eead4;
    --color-success: #34d399;
    --color-success-rgb: 52 211 153;
    --color-warning: #fbbf24;
    --color-warning-rgb: 251 191 36;
    --color-danger: #f87171;
    --color-danger-rgb: 248 113 113;
    --color-info: #38bdf8;
    --color-info-rgb: 56 189 248;
    --color-overlay: rgb(0 0 0 / 0.72);
    --color-chart-1: #2dd4bf;
    --color-chart-2: #5eead4;
    --color-chart-3: #99f6e4;
    --color-chart-neutral: #526561;
  }
}

:root[data-theme="dark"] {
  color-scheme: dark;
  --color-canvas: #070a0a;
  --color-canvas-rgb: 7 10 10;
  --color-surface-1: #0d1212;
  --color-surface-1-rgb: 13 18 18;
  --color-surface-2: #131a1a;
  --color-surface-2-rgb: 19 26 26;
  --color-surface-3: #1a2323;
  --color-surface-3-rgb: 26 35 35;
  --color-border: #253331;
  --color-border-strong: #3a4b47;
  --color-ink: #f2f7f5;
  --color-ink-rgb: 242 247 245;
  --color-ink-muted: #a7b4b0;
  --color-ink-subtle: #82908c;
  --color-primary: #2dd4bf;
  --color-primary-rgb: 45 212 191;
  --color-primary-hover: #5eead4;
  --color-primary-active: #14b8a6;
  --color-on-primary: #03201b;
  --color-focus: #5eead4;
  --color-success: #34d399;
  --color-success-rgb: 52 211 153;
  --color-warning: #fbbf24;
  --color-warning-rgb: 251 191 36;
  --color-danger: #f87171;
  --color-danger-rgb: 248 113 113;
  --color-info: #38bdf8;
  --color-info-rgb: 56 189 248;
  --color-overlay: rgb(0 0 0 / 0.72);
  --color-chart-1: #2dd4bf;
  --color-chart-2: #5eead4;
  --color-chart-3: #99f6e4;
  --color-chart-neutral: #526561;
}
~~~

Color rules:

- Route teal is the only brand accent. Use it for primary actions, links,
  selection, focus, and charts.
- Semantic colors communicate status and feedback only. Pair every semantic
  color with text or an icon.
- Do not use pure black, neon glow, decorative gradients, or a second accent.
- Never derive a destination, ownership decision, or redirect outcome from a
  color shown in the client.
- Verify WCAG AA contrast in both themes. Target AAA for core body copy where
  practical.

## 3. Typography rules

Use Geist through Next.js font tooling:

~~~tsx
import { Geist, Geist_Mono } from "next/font/google";

export const geistSans = Geist({
  subsets: ["latin"],
  variable: "--font-geist-sans",
  display: "swap",
});

export const geistMono = Geist_Mono({
  subsets: ["latin"],
  variable: "--font-geist-mono",
  display: "swap",
});
~~~

Do not use a CSS @import or a runtime link element for production font loading.
The standard template's Google Fonts URL rule is intentionally superseded here
by the current Next.js guidance.

| Role | Family | Fluid size | Weight | Line height | Tracking |
| --- | --- | --- | ---: | ---: | ---: |
| Marketing H1 | Geist Sans | clamp(2.75rem, 6vw, 4.5rem) | 600 | 1.05 | -0.033em |
| Page H1 | Geist Sans | clamp(1.75rem, 3vw, 2.25rem) | 600 | 1.15 | -0.022em |
| Section H2 | Geist Sans | clamp(1.375rem, 2vw, 1.875rem) | 600 | 1.2 | -0.016em |
| H3 | Geist Sans | 1.125rem | 550 | 1.3 | -0.008em |
| Body large | Geist Sans | 1.125rem | 400 | 1.55 | 0 |
| Body | Geist Sans | 1rem | 400 | 1.5 | 0 |
| Supporting | Geist Sans | 0.875rem | 400 | 1.45 | 0 |
| Caption | Geist Sans | 0.75rem | 450 | 1.4 | 0.008em |
| Button | Geist Sans | 0.875rem | 550 | 1.2 | 0 |
| Code and aligned data | Geist Mono | 0.8125rem | 450 | 1.45 | 0 |

Typography rules:

- Use one h1 per page and never skip heading levels.
- Keep marketing headlines to two lines. Keep hero support copy to 20 words
  and four lines or fewer.
- Keep paragraphs to a readable 65ch maximum.
- Use sentence case for headings, labels, and buttons.
- Use mono only for codes, URLs, timestamps, aligned counts, and technical
  identifiers.
- Do not use placeholder text as a field label.
- Prohibited defaults: serif display faces, Inter as the primary family,
  uppercase paragraphs, mixed-family emphasis, and decorative micro-labels
  above every section.

Text decoration:

- Marketing H1: no gradient and no shadow.
- Product H1 and H2: no gradient, no shadow, and no decorative underline.
- Body: no text decoration other than semantic links.
- Links: use an offset underline on hover and preserve a visible focus ring.
- Do not use heading glow, kinetic type, or text scramble for TinyRoute.

## 4. Component styling

### Shared shape and state contract

Controls use an 8px radius. Panels use a 12px radius. Pills are reserved for
compact status badges and selected filters. This is the only allowed mixed
radius rule.

Every interactive component must implement default, hover, active,
focus-visible, disabled, loading, and error states when applicable.

~~~css
:root {
  --radius-control: 0.5rem;
  --radius-panel: 0.75rem;
  --radius-pill: 9999px;
  --control-height: 2.75rem;
  --focus-ring: 0 0 0 2px var(--color-canvas),
                0 0 0 4px var(--color-focus);
  --ease-standard: cubic-bezier(0.16, 1, 0.3, 1);
}

:where(button, a, input, select, textarea, [tabindex]):focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
}
~~~

### Buttons

~~~css
.button {
  min-block-size: var(--control-height);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding-inline: 1rem;
  border: 1px solid transparent;
  border-radius: var(--radius-control);
  font: 550 0.875rem/1.2 var(--font-geist-sans);
  white-space: nowrap;
  cursor: pointer;
  transition:
    background-color 160ms var(--ease-standard),
    border-color 160ms var(--ease-standard),
    color 160ms var(--ease-standard),
    transform 120ms var(--ease-standard);
}

.button:hover { text-decoration: none; }
.button:active { transform: translateY(1px); }
.button:disabled,
.button[aria-disabled="true"] {
  cursor: not-allowed;
  opacity: 1;
  background: var(--color-surface-2);
  border-color: var(--color-border);
  color: var(--color-ink-subtle);
}
.button[aria-busy="true"] { cursor: progress; }
.button-primary {
  background: var(--color-primary);
  color: var(--color-on-primary);
}
.button-primary:hover { background: var(--color-primary-hover); }
.button-primary:active { background: var(--color-primary-active); }
.button-secondary {
  background: var(--color-surface-1);
  border-color: var(--color-border);
  color: var(--color-ink);
}
.button-secondary:hover {
  background: var(--color-surface-2);
  border-color: var(--color-border-strong);
}
.button-danger {
  background: var(--color-danger);
  color: var(--color-surface-1);
}
.button-danger:hover { background: rgb(var(--color-danger-rgb) / 0.88); }
~~~

Use one primary action per action group. Destructive actions are never the
default and must use explicit labels such as "Delete link".

### Links

~~~css
.text-link {
  color: var(--color-primary);
  text-decoration-line: underline;
  text-decoration-thickness: 1px;
  text-underline-offset: 0.2em;
  transition:
    color 160ms var(--ease-standard),
    text-decoration-thickness 160ms var(--ease-standard);
}
.text-link:hover {
  color: var(--color-primary-hover);
  text-decoration-thickness: 2px;
}
.text-link:active { color: var(--color-primary-active); }
.text-link[aria-disabled="true"] {
  color: var(--color-ink-subtle);
  pointer-events: none;
}
~~~

### Forms

~~~css
.field { display: grid; gap: 0.5rem; }
.field-label {
  color: var(--color-ink);
  font: 550 0.875rem/1.35 var(--font-geist-sans);
}
.field-help {
  color: var(--color-ink-muted);
  font: 400 0.875rem/1.45 var(--font-geist-sans);
}
.input {
  min-block-size: var(--control-height);
  inline-size: 100%;
  padding: 0.625rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
  background: var(--color-surface-1);
  color: var(--color-ink);
  font: 400 1rem/1.5 var(--font-geist-sans);
  transition:
    background-color 160ms var(--ease-standard),
    border-color 160ms var(--ease-standard);
}
.input::placeholder { color: var(--color-ink-subtle); }
.input:hover { border-color: var(--color-border-strong); }
.input:focus { border-color: var(--color-focus); }
.input:disabled {
  background: var(--color-surface-2);
  color: var(--color-ink-subtle);
  cursor: not-allowed;
}
.input[aria-invalid="true"] { border-color: var(--color-danger); }
.field-error {
  color: var(--color-danger);
  font: 450 0.875rem/1.45 var(--font-geist-sans);
}
~~~

Labels sit above inputs. Helper text describes accepted input before failure.
Errors sit below the input and are connected with aria-describedby.
Recoverable errors must not clear the user's input.

The create form covers destination URL, optional custom alias, and optional
expiry. It renders HTTPS validation, self-reference, collision, and rate-limit
responses returned by the API. After success, it shows the full short URL in a
mono result panel with a copy button and an announced "Copied" confirmation
(FR-CRE-01 through FR-CRE-08, FR-ABS-01).

### Panels, cards, and empty states

~~~css
.panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-panel);
  background: var(--color-surface-1);
}
.panel-interactive {
  transition:
    background-color 160ms var(--ease-standard),
    border-color 160ms var(--ease-standard);
}
.panel-interactive:hover {
  background: var(--color-surface-2);
  border-color: var(--color-border-strong);
}
.panel-interactive:focus-within { border-color: var(--color-focus); }
.panel[aria-disabled="true"] {
  background: var(--color-surface-2);
  color: var(--color-ink-subtle);
}
.empty-state {
  display: grid;
  justify-items: start;
  gap: 0.75rem;
  padding: clamp(1.5rem, 4vw, 3rem);
}
~~~

Use panels only when the border or surface communicates hierarchy. Prefer
spacing, headings, and sparse dividers for ordinary grouping. Empty states
explain what is absent and offer one relevant next action.

### Navigation

~~~css
.top-nav {
  min-block-size: 3.5rem;
  border-block-end: 1px solid var(--color-border);
  background: rgb(var(--color-canvas-rgb) / 0.96);
}
.nav-link {
  min-block-size: 2.75rem;
  display: inline-flex;
  align-items: center;
  padding-inline: 0.75rem;
  border-radius: var(--radius-control);
  color: var(--color-ink-muted);
}
.nav-link:hover {
  background: var(--color-surface-2);
  color: var(--color-ink);
}
.nav-link:active { background: var(--color-surface-3); }
.nav-link[aria-current="page"] {
  background: var(--color-surface-2);
  color: var(--color-ink);
  font-weight: 550;
}
.nav-link[aria-disabled="true"] {
  color: var(--color-ink-subtle);
  pointer-events: none;
}
~~~

Keep desktop navigation on one line and no taller than 64px. Product navigation
is limited to Links, Analytics, and Account. Use a visible label with the
mobile menu control.

### Status badges

~~~css
.badge {
  display: inline-flex;
  align-items: center;
  min-block-size: 1.5rem;
  padding-inline: 0.5rem;
  border: 1px solid currentColor;
  border-radius: var(--radius-pill);
  background: transparent;
  font: 550 0.75rem/1 var(--font-geist-sans);
}
.badge-success { color: var(--color-success); }
.badge-warning { color: var(--color-warning); }
.badge-danger { color: var(--color-danger); }
.badge-neutral { color: var(--color-ink-muted); }
~~~

Use text labels such as Active, Disabled, Expired, and Deleted. Decorative
status dots are prohibited.

### Link management

Desktop uses a compact semantic table with code, destination, status, clicks,
created date, and an actions menu. Mobile uses one link per card with the same
critical information and actions. Long destinations truncate visually, but an
accessible affordance exposes the complete value.

Search and filters live above the list. Pagination lives below it. Filter and
pagination state should use URL search parameters so it remains shareable and
restorable (FR-MGT-01, FR-MGT-06).

Disable and re-enable are reversible. Destination editing uses a focused
dialog or side panel. Permanent deletion requires confirmation that names the
short code and explains that it cannot be reused (FR-MGT-03 through
FR-MGT-07). Missing and non-owned resources receive the same presentation
(FR-MGT-02, NFR-SEC-04).

### Analytics

Lead with total clicks, then the selected-range daily trend, followed by
referrer, device, operating system, browser, country, and city breakdowns
(FR-ANA-01 through FR-ANA-04). Default the date range to the last 30 days.

Charts use route teal shades plus neutrals. They require direct labels,
keyboard-reachable tooltips, an accessible text summary, and a tabular
fallback. Do not use color as the only way to distinguish a series.

Label analytics as potentially delayed by up to one minute. Do not describe it
as real-time. Never depict raw IP addresses, raw referrer URLs, raw user
agents, persistent visitor IDs, or individual visitors (NFR-CON-03,
NFR-PRV-02, NFR-PRV-03).

### Authentication and account

Use a narrow centered panel for email and password plus Google registration or
sign-in. Keep each method clear without implying that one is less secure.
Provide explicit verification, resend, session refresh, password reset,
signed-out, and account-deletion states (FR-ACC-01 through FR-ACC-05).

Never imply that access or refresh tokens are visible to JavaScript or stored
in browser storage. Account deletion is separated from routine settings and
requires an explicit consequence statement.

### Dialogs, menus, and feedback

Use native semantics or well-tested accessible primitives. Opening a dialog
moves focus inside it, focus remains trapped while open, Escape closes when
safe, and closing restores focus to the trigger. Action menus support arrow
keys and Escape.

Loading states use skeletons matching the final layout. Use a small progress
indicator only for short local actions. Success appears near the action origin,
not only in a toast. Generic server errors retain recoverable input and never
expose stack traces, queries, secrets, or internal paths (NFR-SEC-08).

### Public redirect outcomes

The backend owns these responses. If public HTML is rendered:

- Unknown, deleted, expired, and case-mismatched codes use the same generic
  not-found treatment (FR-RED-03, FR-RED-05 through FR-RED-07).
- Disabled codes use a distinct "Link unavailable" treatment without owner or
  destination details (FR-RED-04).
- An uncertain datastore state uses a generic service error with no Location
  header and no fallback destination (NFR-REL-02).

## 5. Layout principles

~~~css
:root {
  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 0.75rem;
  --space-4: 1rem;
  --space-6: 1.5rem;
  --space-8: 2rem;
  --space-12: 3rem;
  --space-16: 4rem;
  --space-20: 5rem;
  --container-marketing: 75rem;
  --container-product: 90rem;
  --container-reading: 65ch;
}
.container {
  inline-size: min(100% - 2rem, var(--container-product));
  margin-inline: auto;
}
.container-marketing {
  inline-size: min(100% - 2rem, var(--container-marketing));
  margin-inline: auto;
}
.container-reading {
  inline-size: min(100%, var(--container-reading));
}
.product-grid {
  display: grid;
  grid-template-columns: 14rem minmax(0, 1fr);
  min-block-size: 100dvh;
}
~~~

- Build mobile first. Enhance layouts at 640px, 768px, 1024px, 1280px, and
  1536px.
- Use CSS Grid for page composition. Do not use percentage-based flex math.
- Use min-block-size: 100dvh, never 100vh, for viewport-height regions.
- Marketing max width is 1200px. Product max width is 1440px. Reading width is
  65 characters.
- Desktop product shell uses a 224px sidebar, a 56px top bar, and a fluid
  content area.
- Default product page padding is 24px at tablet and 32px at desktop. Mobile
  page padding is 16px.
- Section gaps are 48px in product UI and up to 80px on marketing pages.
- Avoid equal three-card feature rows. Product tables and analytics grids may
  be regular when comparison and scanning require alignment.
- Hero content must fit the initial viewport with its primary CTA visible.

## 6. Depth and elevation

| Level | Treatment | Use |
| --- | --- | --- |
| Flat | No shadow; canvas or spacing | Default page regions and data groups |
| Bordered | 1px color-border | Inputs, panels, tables, cards |
| Raised | Tinted subtle shadow | Menus, popovers, sticky controls |
| Overlay | Tinted stronger shadow plus scrim | Dialogs only |

~~~css
:root {
  --shadow-raised:
    0 1px 2px rgb(var(--color-ink-rgb) / 0.06),
    0 8px 24px rgb(var(--color-primary-rgb) / 0.05);
  --shadow-overlay:
    0 2px 4px rgb(var(--color-ink-rgb) / 0.08),
    0 20px 56px rgb(var(--color-primary-rgb) / 0.09);
}
.popover { box-shadow: var(--shadow-raised); }
.dialog { box-shadow: var(--shadow-overlay); }
~~~

Do not add shadows to every card. Do not use pure-black shadows, outer glow,
glassmorphism, or blur over a large scrolling surface. Hierarchy should come
first from spacing, borders, type, and surface color.

## 7. Animation and interaction

**Motion philosophy:** Motion confirms an action or helps users understand a
state change. It never competes with route data.

**Tier:** L1 Refined Static

**Dependencies:** CSS only

~~~css
@keyframes content-enter {
  from {
    opacity: 0;
    transform: translateY(0.5rem);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.page-enter {
  animation: content-enter 280ms var(--ease-standard) both;
}
.skeleton {
  background:
    linear-gradient(
      90deg,
      var(--color-surface-2) 0%,
      var(--color-surface-3) 50%,
      var(--color-surface-2) 100%
    );
  background-size: 200% 100%;
  animation: skeleton-shift 1.2s ease-in-out infinite;
}
@keyframes skeleton-shift {
  from { background-position: 200% 0; }
  to { background-position: -200% 0; }
}
@media (prefers-reduced-motion: reduce) {
  html { scroll-behavior: auto; }
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
~~~

- Animate only transform and opacity for movement.
- Use entrance motion once per page region. Do not replay it during routine
  navigation or filter updates.
- Press feedback may move a button by 1px. Cards do not float or tilt.
- Copy success updates the button label or adjacent live region without
  sparkle, confetti, or unrelated decoration.
- Theme changes are immediate. Do not animate the whole page between themes.
- Never attach raw scroll listeners, replace the cursor, hijack scrolling, add
  parallax, or animate metrics from zero.

## 8. Do and do not

### Do

- Use route teal consistently as the only brand accent.
- Emphasize real link data and clear task progression.
- Use semantic HTML before adding ARIA.
- Keep primary actions visible, short, and on one line.
- Implement loading, empty, success, validation, rate-limit, safe-error, and
  disabled states.
- Preserve user input and context after recoverable failures.
- Pair state colors with explicit labels and icons.
- Test keyboard operation, screen-reader structure, both themes, and all target
  viewports.
- Use a single icon family when the frontend selects one. Prefer an existing
  dependency; otherwise evaluate Phosphor, HugeIcons, Radix, or Tabler.

### Do not

- Do not add purple gradients, neon glow, decorative blobs, glass panels, or
  giant shadows.
- Do not round every surface or use pill-shaped primary buttons.
- Do not use a centered generic hero, three equal feature cards, fake product
  screenshots, fake precision, or invented customer proof.
- Do not use emoji or hand-drawn SVG paths as interface icons.
- Do not use decorative dots, numbered section eyebrows, scroll cues, version
  labels, or ornamental metadata strips.
- Do not hide a label in a placeholder or rely on hover for critical content.
- Do not use color, motion, an icon, or a toast as the sole state signal.
- Do not expose owner identifiers, tokens, raw analytics inputs, internal
  errors, or security implementation details.
- Do not implement ownership, redirect, rate-limit, validation, or analytics
  policy in Next.js.
- Do not add V1 or Future features through design work: destination blocklists,
  safe-browsing checks, public APIs, API keys, or an admin console.

## 9. Responsive behavior

| Viewport | Width | Key behavior |
| --- | ---: | --- |
| Small mobile | 320-479px | One column, 16px page padding, stacked actions, card-form link list |
| Large mobile | 480-767px | One column, paired actions when labels fit, compact filters |
| Tablet | 768-1023px | Reduced grids, drawer navigation, chart scroll only when necessary |
| Desktop | 1024-1439px | 224px sidebar, compact table, two-column analytics where useful |
| Wide desktop | 1440px and above | Centered 1440px shell, controlled line lengths |

~~~css
@media (max-width: 1023px) {
  .product-grid { grid-template-columns: minmax(0, 1fr); }
  .desktop-sidebar { display: none; }
}
@media (max-width: 767px) {
  .container,
  .container-marketing {
    inline-size: min(100% - 2rem, var(--container-product));
  }
  .multi-column { grid-template-columns: minmax(0, 1fr); }
  .link-table { display: none; }
  .link-card-list { display: grid; }
  .dialog {
    inline-size: calc(100% - 2rem);
    max-block-size: calc(100dvh - 2rem);
  }
}
@media (min-width: 768px) {
  .link-card-list { display: none; }
}
@media (pointer: coarse) {
  :where(button, a, input, select, textarea, [role="button"]) {
    min-block-size: 2.75rem;
  }
}
~~~

Touch targets are at least 44 by 44px. No primary workflow may require
horizontal page scrolling. A chart may scroll inside a labeled region only
when reflow would destroy its meaning.

Explicit collapse rules:

- The sidebar becomes a labeled drawer trigger below 1024px.
- Multi-column forms become a single column below 768px.
- Link tables become semantic cards below 768px with all critical actions
  preserved.
- Filter controls wrap into a two-row region, then stack below 480px.
- Action groups stack only when labels would wrap.
- Dialogs become near-full-width and remain inside the dynamic viewport.
- Long URLs truncate in the visual row, while copy and reveal controls retain
  the complete accessible value.

## Accessibility and verification contract

TinyRoute targets WCAG 2.1 AA at minimum.

- Use native buttons, links, inputs, tables, dialogs, and headings.
- All workflows must work with keyboard alone, including menus, dialogs, date
  controls, chart tooltips, copy actions, and confirmations.
- Icon-only controls require accessible names and visible tooltips.
- Announce form errors, copy success, asynchronous completion, and session
  changes through appropriate live regions.
- Focus must be visible in both themes and must not be hidden by sticky UI.
- Reading and DOM order must remain logical when CSS changes the layout.
- Never disable zoom or trap touch gestures.

Before a frontend surface is complete, verify:

- 320px, 768px, 1024px, and 1440px layouts
- light, dark, and system theme behavior
- keyboard order, focus movement, and Escape behavior
- screen-reader names, roles, values, errors, and state changes
- loading, empty, success, validation, rate-limit, offline, and safe-error
  states
- button, form, text, border, chart, and focus contrast
- no console errors, no accessibility warnings, and no horizontal overflow
- plausible Core Web Vitals: LCP below 2.5s, INP below 200ms, CLS below 0.1

## Agent implementation guide

Before implementing any frontend surface:

1. Read the matching FR and NFR requirements plus the relevant architecture
   flow.
2. Confirm the page type: marketing, authenticated product, authentication, or
   public outcome.
3. Use the dials, tokens, radius rule, and interaction tier in this document.
4. Keep Server Components as the default. Isolate interactivity in focused
   Client Components.
5. Separate data fetching and mutation coordination from presentational
   components.
6. Prefer local state, then lifted state, context, URL state, or server state
   according to the actual sharing boundary. Do not introduce a global store
   without demonstrated need.
7. Verify every third-party dependency before importing it. Continue the
   project's existing styling and component approach once those are chosen.
8. Use next/font, 100dvh, CSS Grid, semantic tokens, and the documented
   breakpoints.
9. Audit against every item in the accessibility and verification contract.

## Known gaps

This contract does not choose a component library, icon package, charting
library, styling framework, application route structure, or backend response
shape. Those decisions require an approved feature specification and must
follow the locked architecture. Until the frontend is bootstrapped, use native
semantic HTML and CSS as the baseline rather than introducing dependencies in
this document.

The manual theme-control persistence mechanism should be chosen during
frontend bootstrap. Until then, system preference with a light fallback is
sufficient.

## Provenance

The visual direction began from a Linear-inspired restrained developer-tool
analysis and was rewritten for TinyRoute. It uses no third-party trademarks,
assets, copy, or proprietary fonts. The upstream MIT notice remains at
[awesome-design-md MIT license](docs/licenses/awesome-design-md-MIT.txt).

This revision incorporates the current project web-design specification
structure, the current design-taste-frontend anti-template guidance where it
applies, and the current production frontend UI engineering accessibility and
responsive requirements.
