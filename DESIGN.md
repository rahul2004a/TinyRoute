---
version: beta
name: TinyRoute
description: "A restrained developer-tool interface with quiet neutral surfaces, one route-teal accent, compact data presentation, accessible dual themes, and purposeful motion."
designVariance: 5
motionIntensity: 3
visualDensity: 6
interactionTier: L1 Refined Static
colors:
  primary: "#0f766e"
  primary-dark: "#2dd4bf"
  canvas: "#f6f9f8"
  canvas-dark: "#070a0a"
  ink: "#0b1311"
  ink-dark: "#f2f7f5"
typography:
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

## Authority

This file governs presentation only. Product policy comes from [functional
requirements](docs/requirements/Functional.md), [non-functional
requirements](docs/requirements/Non-Functional.md), [architecture](docs/architecture/architecture.md),
and the [frontend stack ADR](docs/decisions/0001-frontend-stack.md). Next.js renders
Spring Boot API results and does not handle GET /{code}. Skills provide reusable
workflows; this file records only TinyRoute-specific presentation decisions.

## 1. Visual theme and atmosphere

**Design read:** A portfolio-grade developer tool for technical users, with a
calm, precise, trust-first visual language.

| Dial | Value | Effect |
| --- | ---: | --- |
| Design variance | 5 | Offset marketing composition; predictable product grids |
| Motion intensity | 3 | Static by default; short feedback and entrance motion |
| Visual density | 6 | Compact link and analytics data without crowding forms |

Use restrained neutral surfaces, crisp typography, sparse borders, and route
teal as the only accent. Marketing may use an asymmetric split hero with a real
TinyRoute UI or screenshot. Product screens prioritize scanning and task
completion. Use next-themes with `data-theme` for light, dark, and system
preference without switching theme families between sections.

## 2. Color palette and roles

~~~css
:root {
  color-scheme: light;
  --canvas: #f6f9f8;
  --surface-1: #ffffff;
  --surface-2: #edf3f1;
  --surface-3: #e3ece9;
  --border: #cfddd8;
  --border-strong: #9fb3ad;
  --ink: #0b1311;
  --ink-muted: #52615d;
  --ink-subtle: #65746f;
  --primary: #0f766e;
  --primary-rgb: 15 118 110;
  --primary-hover: #115e59;
  --primary-active: #134e4a;
  --on-primary: #ffffff;
  --success: #047857;
  --warning: #a34f08;
  --danger: #b91c1c;
  --info: #0369a1;
}

@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
    color-scheme: dark;
    --canvas: #070a0a;
    --surface-1: #0d1212;
    --surface-2: #131a1a;
    --surface-3: #1a2323;
    --border: #253331;
    --border-strong: #3a4b47;
    --ink: #f2f7f5;
    --ink-muted: #a7b4b0;
    --ink-subtle: #82908c;
    --primary: #2dd4bf;
    --primary-rgb: 45 212 191;
    --primary-hover: #5eead4;
    --primary-active: #14b8a6;
    --on-primary: #03201b;
    --success: #34d399;
    --warning: #fbbf24;
    --danger: #f87171;
    --info: #38bdf8;
  }
}
~~~

A forced dark theme uses the same dark tokens at the root. Components reference
semantic variables. Pair status colors with text or an icon. Do not use pure
black, decorative gradients, neon glow, or a second accent. Verify WCAG AA
contrast in both themes.

## 3. Typography rules

Use Geist Sans and Geist Mono through Next.js next/font with display set to
swap. Do not use CSS font imports or add a font package.

| Role | Size | Weight | Line height |
| --- | --- | ---: | ---: |
| Marketing H1 | clamp(2.75rem, 6vw, 4.5rem) | 600 | 1.05 |
| Page H1 | clamp(1.75rem, 3vw, 2.25rem) | 600 | 1.15 |
| Section H2 | clamp(1.375rem, 2vw, 1.875rem) | 600 | 1.2 |
| Body | 1rem | 400 | 1.5 |
| Supporting | 0.875rem | 400 | 1.45 |
| Code and aligned data | 0.8125rem | 450 | 1.45 |

Use one H1 and a logical heading order. Keep hero headlines within two lines,
support copy within 20 words, and text within 65 characters per line. Use mono
only for codes, URLs, timestamps, and aligned numbers. Do not use serif display
faces, decorative eyebrows, heading gradients, or text shadows.

## 4. Component styling

Implement with Tailwind CSS, locally owned shadcn/ui on Radix UI, and Lucide icons.
Controls use an 8px radius, panels use 12px, and pills are limited to status
badges and selected filters. Interactive components need default, hover,
active, focus-visible, disabled, loading, and error states when applicable.
Focus uses a 2px route-teal ring with a 2px canvas offset.

| Component | TinyRoute treatment |
| --- | --- |
| Primary button | Route-teal fill, high-contrast label, 44px minimum height, one per action group |
| Secondary button | Surface fill, hairline border, stronger border on hover |
| Destructive button | Danger color, explicit wording, never the default action |
| Input | Label above, helper text before failure, inline error below, retained value after recoverable failure |
| Panel | Surface 1 and one border; use only when grouping or hierarchy requires it |
| Navigation | 56px top bar, 224px desktop sidebar, Links, Analytics, Account |
| Badge | Text plus semantic color; no decorative status dots |
| Dialog | Focus enters and remains inside, Escape closes when safe, focus returns to trigger |

The create form contains destination, optional custom alias, and optional
expiry. It renders API validation and rate limits. Success shows the complete
short URL, a copy action, and an announced confirmation (FR-CRE-01 through
FR-CRE-08, FR-ABS-01).

Desktop link management uses a semantic table. Mobile uses cards containing
the same critical data and actions. Search and filter state belongs in URL
parameters. Delete confirmation names the code and explains that it cannot be
reused. Missing and non-owned resources look identical
(FR-MGT-01 through FR-MGT-07, NFR-SEC-04).

Analytics leads with total clicks, then the selectable daily trend, then
referrer, device, operating system, browser, country, and city. Default to 30
days. Recharts requires direct labels, reachable tooltips, accessible summaries, and table fallbacks.
Label data as delayed by up to one minute; never expose raw tracking inputs
(FR-ANA-01 through FR-ANA-04, NFR-CON-03, NFR-PRV-02, NFR-PRV-03).

Authentication uses a narrow centered panel. Keep email/password and Google
methods clear. Account deletion is separated from routine settings and states
its consequences (FR-ACC-01 through FR-ACC-05).

Public redirect pages render backend outcomes only. Unknown, deleted, expired,
and case-mismatched codes share a generic not-found treatment. Disabled links
show "Link unavailable" without owner or destination details. Uncertain state
shows a generic service error with no fallback destination
(FR-RED-03 through FR-RED-07, NFR-REL-02).

## 5. Layout principles

- Base spacing is 4px; use 8, 12, 16, 24, 32, 48, 64, and 80px.
- Marketing content max width is 1200px; product content is 1440px.
- Product page padding is 16px mobile, 24px tablet, and 32px desktop.
- Use CSS Grid for page composition and 100dvh for viewport-height regions.
- Use 48px product section gaps and up to 80px for marketing.
- Avoid equal three-card marketing rows. Regular product grids are allowed
  when comparison and scanning benefit.
- Keep the primary marketing CTA in the initial viewport.

## 6. Depth and elevation

| Level | Treatment | Use |
| --- | --- | --- |
| Flat | Canvas, spacing, no shadow | Default regions and data groups |
| Bordered | Surface plus 1px border | Inputs, panels, tables |
| Raised | Small teal-tinted shadow | Menus and popovers |
| Overlay | Stronger tinted shadow plus scrim | Dialogs only |

Use spacing, borders, type, and surface color before elevation. Do not add
shadows to every card, use pure-black shadows, or blur scrolling surfaces.

## 7. Animation and interaction

**Tier:** L1 Refined Static. CSS only.

~~~css
.page-enter {
  animation: content-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes content-enter {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
~~~

Animate only transform and opacity for movement. Use entrance motion once per
page region. Press feedback may move a button by 1px. Copy success changes the
label or adjacent live region. Do not add parallax, scroll hijacking, raw scroll
listeners, cursor replacement, card tilt, confetti, or animated metrics.

## 8. Do and do not

### Do

- Emphasize real link data and clear task progression.
- Use semantic HTML and plain-language labels.
- Preserve input and context after recoverable failures.
- Implement loading, empty, success, validation, rate-limit, and safe-error
  states.
- Use Lucide React as the single interface icon family.

### Do not

- Do not use generic centered heroes, fake screenshots, invented proof, or fake
  precision.
- Do not use glass panels, decorative blobs, purple gradients, or giant
  shadows.
- Do not use emoji, hand-drawn interface icons, ornamental labels, or version
  stamps.
- Do not rely on color, motion, an icon, hover, or a toast as the only signal.
- Do not expose tokens, owner IDs, raw analytics inputs, or internal errors.
- Do not implement backend policy in Next.js.
- Do not introduce V1 or Future features through design work.

## 9. Responsive behavior

| Width | Behavior |
| --- | --- |
| 320-479px | One column, 16px padding, stacked actions, link cards |
| 480-767px | One column, paired actions when labels fit, compact filters |
| 768-1023px | Drawer navigation, reduced grids, selective chart scrolling |
| 1024-1439px | Sidebar, compact link table, two-column analytics |
| 1440px and above | Centered 1440px shell with controlled line lengths |

Touch targets are at least 44 by 44px. Below 1024px, replace the sidebar with a
labeled drawer. Below 768px, collapse columns and replace link tables with
semantic cards without hiding actions. Below 480px, stack filters and actions
when labels would wrap. Dialogs stay within the dynamic viewport. Long URLs may
truncate visually, but copy and reveal controls retain the complete value.

All workflows must be keyboard operable. Announce errors, copy success,
asynchronous completion, and session changes. Test light, dark, and system
themes at 320px, 768px, 1024px, and 1440px. Verify focus order, dialog focus,
screen-reader names and states, contrast, loading and failure states, no
horizontal overflow, and no console or accessibility errors.
