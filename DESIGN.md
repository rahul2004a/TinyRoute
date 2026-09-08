---
version: alpha
name: TinyRoute
description: "A precise, dark-first design system for TinyRoute: near-black product surfaces, a restrained route-teal accent, compact controls, data-forward link management, and accessible light and dark themes. The interface should feel calm, technical, and trustworthy without borrowing another product's brand, assets, or proprietary typography."

colors:
  primary: "#2dd4bf"
  on-primary: "#03201b"
  primary-hover: "#5eead4"
  primary-pressed: "#14b8a6"
  focus: "#2dd4bf"
  canvas: "#070a0a"
  surface-1: "#0d1212"
  surface-2: "#131a1a"
  surface-3: "#1a2323"
  border: "#253331"
  border-strong: "#3a4b47"
  ink: "#f2f7f5"
  ink-muted: "#a7b4b0"
  ink-subtle: "#74817e"
  light-primary: "#0f766e"
  light-on-primary: "#ffffff"
  light-primary-hover: "#115e59"
  light-primary-pressed: "#134e4a"
  light-focus: "#0d9488"
  light-canvas: "#f6f9f8"
  light-surface-1: "#ffffff"
  light-surface-2: "#edf3f1"
  light-surface-3: "#e3ece9"
  light-border: "#cfddd8"
  light-border-strong: "#9fb3ad"
  light-ink: "#0b1311"
  light-ink-muted: "#52615d"
  light-ink-subtle: "#6b7b77"
  success-dark: "#34d399"
  warning-dark: "#fbbf24"
  danger-dark: "#f87171"
  info-dark: "#38bdf8"
  success-light: "#047857"
  warning-light: "#b45309"
  danger-light: "#b91c1c"
  info-light: "#0369a1"
  overlay: "#000000b8"

typography:
  display-xl:
    fontFamily: Geist Sans
    fontSize: 72px
    fontWeight: 600
    lineHeight: 1.05
    letterSpacing: -2.4px
  display-lg:
    fontFamily: Geist Sans
    fontSize: 48px
    fontWeight: 600
    lineHeight: 1.1
    letterSpacing: -1.4px
  display-md:
    fontFamily: Geist Sans
    fontSize: 36px
    fontWeight: 600
    lineHeight: 1.15
    letterSpacing: -0.8px
  page-title:
    fontFamily: Geist Sans
    fontSize: 30px
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: -0.6px
  section-title:
    fontFamily: Geist Sans
    fontSize: 24px
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: -0.35px
  card-title:
    fontFamily: Geist Sans
    fontSize: 18px
    fontWeight: 550
    lineHeight: 1.3
    letterSpacing: -0.15px
  body-lg:
    fontFamily: Geist Sans
    fontSize: 18px
    fontWeight: 400
    lineHeight: 1.55
    letterSpacing: -0.05px
  body:
    fontFamily: Geist Sans
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0
  body-sm:
    fontFamily: Geist Sans
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.45
    letterSpacing: 0
  caption:
    fontFamily: Geist Sans
    fontSize: 12px
    fontWeight: 450
    lineHeight: 1.4
    letterSpacing: 0.1px
  button:
    fontFamily: Geist Sans
    fontSize: 14px
    fontWeight: 550
    lineHeight: 1.2
    letterSpacing: 0
  mono:
    fontFamily: Geist Mono
    fontSize: 13px
    fontWeight: 450
    lineHeight: 1.45
    letterSpacing: 0

rounded:
  xs: 4px
  sm: 6px
  md: 8px
  lg: 12px
  xl: 16px
  pill: 9999px
  full: 9999px

spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px
  section: 80px

components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    padding: 10px 16px
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
  button-primary-pressed:
    backgroundColor: "{colors.primary-pressed}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
  button-secondary:
    backgroundColor: "{colors.surface-2}"
    textColor: "{colors.ink}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    padding: 10px 16px
  button-danger:
    backgroundColor: "{colors.danger-dark}"
    textColor: "{colors.canvas}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    padding: 10px 16px
  focus-ring:
    backgroundColor: "{colors.focus}"
    rounded: "{rounded.md}"
    size: 2px
  divider:
    backgroundColor: "{colors.border}"
    height: 1px
  divider-strong:
    backgroundColor: "{colors.border-strong}"
    height: 1px
  text-input:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    padding: 10px 12px
  panel:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.lg}"
    padding: 24px
  panel-elevated:
    backgroundColor: "{colors.surface-3}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.lg}"
    padding: 24px
  metric-card:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.ink}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.lg}"
    padding: 20px
  status-badge:
    backgroundColor: "{colors.surface-2}"
    textColor: "{colors.ink-muted}"
    typography: "{typography.caption}"
    rounded: "{rounded.pill}"
    padding: 3px 8px
  caption-text:
    textColor: "{colors.ink-subtle}"
    typography: "{typography.caption}"
  top-nav:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.xs}"
    height: 56px
  sidebar:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.ink-muted}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.xs}"
    width: 224px
  modal:
    backgroundColor: "{colors.surface-2}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.lg}"
    padding: 24px
  modal-scrim:
    backgroundColor: "{colors.overlay}"
  feedback-success-dark:
    textColor: "{colors.success-dark}"
    typography: "{typography.body-sm}"
  feedback-warning-dark:
    textColor: "{colors.warning-dark}"
    typography: "{typography.body-sm}"
  feedback-info-dark:
    textColor: "{colors.info-dark}"
    typography: "{typography.body-sm}"
  light-page-shell:
    backgroundColor: "{colors.light-canvas}"
    textColor: "{colors.light-ink}"
    typography: "{typography.body}"
  light-button-primary:
    backgroundColor: "{colors.light-primary}"
    textColor: "{colors.light-on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    padding: 10px 16px
  light-button-primary-hover:
    backgroundColor: "{colors.light-primary-hover}"
    textColor: "{colors.light-on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
  light-button-primary-pressed:
    backgroundColor: "{colors.light-primary-pressed}"
    textColor: "{colors.light-on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
  light-focus-ring:
    backgroundColor: "{colors.light-focus}"
    rounded: "{rounded.md}"
    size: 2px
  light-divider:
    backgroundColor: "{colors.light-border}"
    height: 1px
  light-divider-strong:
    backgroundColor: "{colors.light-border-strong}"
    height: 1px
  light-panel:
    backgroundColor: "{colors.light-surface-1}"
    textColor: "{colors.light-ink}"
    typography: "{typography.body}"
    rounded: "{rounded.lg}"
    padding: 24px
  light-panel-raised:
    backgroundColor: "{colors.light-surface-2}"
    textColor: "{colors.light-ink}"
    typography: "{typography.body}"
    rounded: "{rounded.lg}"
    padding: 24px
  light-menu:
    backgroundColor: "{colors.light-surface-3}"
    textColor: "{colors.light-ink-muted}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: 8px
  light-caption-text:
    textColor: "{colors.light-ink-subtle}"
    typography: "{typography.caption}"
  feedback-success-light:
    textColor: "{colors.success-light}"
    typography: "{typography.body-sm}"
  feedback-warning-light:
    textColor: "{colors.warning-light}"
    typography: "{typography.body-sm}"
  feedback-danger-light:
    textColor: "{colors.danger-light}"
    typography: "{typography.body-sm}"
  feedback-info-light:
    textColor: "{colors.info-light}"
    typography: "{typography.body-sm}"
---

# TinyRoute design system

## Authority and boundaries

This document defines how TinyRoute looks and responds visually. It does not
define product behavior. If it conflicts with
[`Functional.md`](docs/requirements/Functional.md),
[`Non-Functional.md`](docs/requirements/Non-Functional.md), or
[`architecture.md`](docs/architecture/architecture.md), those documents win.

Next.js is a presentation client for the Spring Boot API. The UI may display
authentication, validation, ownership-safe, rate-limit, analytics, and link
state responses, but it must never recreate those policies. `GET /{code}` and
all redirect decisions remain in `RedirectController` and `RedirectService`.

This system is adapted from the Linear analysis in
[VoltAgent's awesome-design-md](https://github.com/VoltAgent/awesome-design-md/tree/main/design-md/linear.app),
then rewritten for TinyRoute. It uses no Linear trademarks, assets, copy, or
proprietary fonts. The upstream MIT notice is preserved in
[`docs/licenses/awesome-design-md-MIT.txt`](docs/licenses/awesome-design-md-MIT.txt).

## Visual direction

TinyRoute should feel like a carefully made developer tool: calm, compact,
fast, and data-forward. Near-black or quiet off-white canvases establish the
base. Route teal is the only decorative accent and marks primary actions,
links, selection, focus, and the TinyRoute brand. Semantic colors are reserved
for status and feedback.

Use surfaces and hairline borders for hierarchy. Avoid atmospheric gradients,
glass effects, oversized shadows, decorative blobs, and excessively rounded
controls. Product UI, short-link data, and analytics are the visual focus.

## Themes

Support both `prefers-color-scheme` themes. Use dark when a preference is not
available. A manual theme control may override the system preference once the
frontend exists, but it must be accessible by keyboard and expose its state.

| Role | Dark | Light |
| --- | --- | --- |
| Primary | `#2dd4bf` | `#0f766e` |
| On primary | `#03201b` | `#ffffff` |
| Canvas | `#070a0a` | `#f6f9f8` |
| Surface 1 | `#0d1212` | `#ffffff` |
| Surface 2 | `#131a1a` | `#edf3f1` |
| Surface 3 | `#1a2323` | `#e3ece9` |
| Border | `#253331` | `#cfddd8` |
| Strong border | `#3a4b47` | `#9fb3ad` |
| Ink | `#f2f7f5` | `#0b1311` |
| Muted ink | `#a7b4b0` | `#52615d` |
| Subtle ink | `#74817e` | `#6b7b77` |

Do not achieve light mode by simply inverting dark colors. Preserve the same
surface hierarchy and contrast relationships. Never use color as the only
signal: pair status colors with labels, icons, and meaningful text.

## Typography

Use `Geist Sans, Inter, ui-sans-serif, system-ui, sans-serif`. Use `Geist Mono,
ui-monospace, SFMono-Regular, Menlo, monospace` for short codes, URLs, dates,
counts when alignment matters, and technical identifiers. Load Geist through
Next.js font tooling when the frontend is implemented; do not add a font
package solely for this document.

Display text is concise, sentence case, weight 600, and negatively tracked.
Product headings use 24–36px; reserve 48–72px for a marketing hero. Body text
is 16px, compact supporting text is 14px, and captions are never smaller than
12px. Do not use uppercase for paragraphs or controls.

## Layout and responsive behavior

- Base spacing unit: 4px. Prefer 8, 12, 16, 24, 32, 48, and 80px gaps.
- Marketing content max-width: 1200px. Dashboard content max-width: 1440px.
- Desktop app shell: 224px sidebar, 56px top bar, fluid content area.
- At 1024px, reduce multi-column grids. Below 768px, collapse the sidebar to a
  menu and turn data tables into readable cards. At 480px, use one column.
- Keep form controls at least 44px tall on touch screens and avoid horizontal
  scrolling for primary actions or link metadata.
- Let long destinations truncate visually with an accessible full-value
  affordance. Never mutate or silently shorten the stored value.

## Elevation, shape, and motion

Default cards use `surface-1`, a 1px border, and a 12px radius. Hovered or
selected cards move to `surface-2` with a stronger border. Modals use
`surface-2` over the overlay and may use one restrained shadow. Buttons and
inputs use 8px radii; reserve pills for compact status badges and filters.

Use 120–180ms transitions for color, border, opacity, and small transforms.
Avoid bouncing or large parallax effects. Honor `prefers-reduced-motion` by
removing nonessential movement while keeping state changes understandable.

## Core components

### Navigation and shell

Keep primary navigation short: Links, Analytics, and Account. The TinyRoute
mark and create action remain easy to find. Selected navigation uses a subtle
surface lift, a teal indicator, and semibold text—not a large filled accent.

### Buttons and links

- Primary: teal fill, high-contrast label, 8px radius, 44px minimum touch
  height. Use once per action group.
- Secondary: surface fill with hairline border. Tertiary: text-only with a
  visible hover surface.
- Destructive: semantic danger color and explicit wording. Never style delete
  as a routine primary action.
- Focus: 2px teal ring with a 2px offset on every interactive element.
- Disabled controls remain legible and cannot rely on reduced opacity alone.

### Forms

Labels sit above inputs. Supporting text explains accepted formats before an
error occurs. Validation messages appear next to the field and are announced
to assistive technology. Do not clear user input after a recoverable error.

The create form covers destination URL, optional custom alias, and optional
expiry. It presents HTTPS validation and rate-limit responses from the API; it
does not decide those policies. After success, show the complete short URL in
a mono result panel with a one-click Copy action and an announced "Copied"
state (FR-CRE-01..08, FR-ABS-01).

### Link list and management

Desktop uses a compact table with code, destination, status, clicks, created
date, and an actions menu. Mobile uses one link per card with the same
information and no hidden critical action. Search/filter appears above the
list. Pagination controls stay below it (FR-MGT-01, FR-MGT-06).

Use labeled Active, Disabled, and Expired badges. Disable and re-enable are
reversible actions. Destination edits use a focused modal or side panel.
Permanent deletion requires a confirmation that names the short code and
states that it cannot be reused (FR-MGT-03..07). The UI never exposes ownership
identifiers or distinguishes missing resources from resources owned by someone
else (FR-MGT-02, NFR-SEC-04).

### Analytics

Lead with total clicks, then the selected-range daily trend, followed by
referrer, device, operating system, browser, country, and city breakdowns
(FR-ANA-01..04). Default the trend to 30 days. Charts use teal shades plus
neutrals, direct labels, accessible summaries, and tabular fallbacks. Tooltips
must be keyboard reachable.

Label analytics as potentially delayed by up to one minute. Never describe it
as real-time. Do not depict raw IP addresses, full referrer URLs, raw user
agents, individual visitors, or tracking identifiers (NFR-CON-03,
NFR-PRV-02/03).

### Authentication and account

Use a narrow, centered card for email/password and Google registration or
sign-in. Keep method choice clear without making one look unsafe. Provide
distinct verification, session-refresh, password-reset, signed-out, and
account-deletion states (FR-ACC-01..05). Never imply that access or refresh
tokens are visible to JavaScript or browser storage; they are secure cookies
(NFR-SEC-06).

Account deletion is visually separated from routine settings and requires an
explicit confirmation explaining that owned short URLs stop redirecting.

### Feedback and system states

- Loading: use skeletons for page structure and a spinner only for local,
  short actions. Preserve layout to avoid shifts.
- Empty: explain the state and offer one relevant action.
- Success: confirm the completed action near its origin; do not rely only on a
  disappearing toast.
- Validation and rate limiting: show clear retry guidance returned by the API.
- Server error: use generic language, retain recoverable input, and never show
  stack traces, queries, or internal paths (NFR-SEC-08).

### Public redirect outcomes

The backend owns these responses. If a public HTML state is rendered, keep it
minimal and never offer or imply a guessed destination:

- Unknown, deleted, expired, and case-mismatched codes use the same generic
  not-found treatment (FR-RED-03, FR-RED-05..07).
- Disabled codes use a distinct "Link unavailable" treatment without owner or
  destination details (FR-RED-04).
- An uncertain datastore state uses a generic service error and no `Location`
  header or fallback link (NFR-REL-02).

## Accessibility

- Meet WCAG AA contrast for text, controls, borders that convey state, and
  focus indicators in both themes.
- Use semantic HTML, a logical heading order, explicit labels, and useful link
  text. Icon-only controls require accessible names and tooltips.
- All workflows must be operable by keyboard, including menus, modals, date
  controls, chart tooltips, copy actions, and confirmation dialogs.
- Announce form errors, copy success, async completion, and session changes.
- Never rely on hover, color, motion, or icon shape alone to convey meaning.

## Do and do not

Do keep the interface dense but breathable, emphasize actual product data,
use teal sparingly, write plain-language labels, and preserve user context
through recoverable failures.

Do not add decorative gradients, glassmorphism, multiple brand accents,
pill-shaped primary buttons, oversized shadows, fake live indicators, hidden
ownership logic, or client-side redirect decisions.

Do not introduce destination blocklists, public APIs or API keys, safe-browsing
checks, or an admin console through design work. Those are V1 or Future scope.

## Agent implementation guide

Before building a frontend surface:

1. Read the matching FR/NFR requirements and architecture flow.
2. Select the theme surface level and typography token before styling.
3. Reuse the component and state patterns in this document.
4. Keep backend policy out of Next.js and render API outcomes faithfully.
5. Check keyboard behavior, both themes, mobile layout, contrast, loading,
   empty, success, validation, rate-limit, and safe-error states.
6. Run `npx --yes @google/design.md@0.4.0 lint DESIGN.md` after editing this
   document.

## Known gaps

This contract does not choose a charting or component library, define
application routes, or prescribe backend response shapes. Those decisions must
come from an approved feature spec and the locked architecture. The manual
theme-control persistence mechanism should be selected when the frontend is
bootstrapped; until then, system preference with dark fallback is sufficient.
