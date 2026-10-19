# Account settings redesign

## Scope and requirements

Improve `/settings` using the supplied sidebar reference and the existing
`DESIGN.md`. This is presentation work for FR-ACC-02, FR-ACC-03, and FR-ACC-05;
Spring Boot continues to own authentication, revocation, and deletion
(NFR-SEC-06, NFR-SEC-08, NFR-SEC-09).

## Design

Keep Geist, charcoal/warm-paper themes, Lucide, 8px controls, 12px panels,
and L1 CSS motion. Follow the user's revised ChatGPT settings reference: a
full-height layout with no site header, footer, or outer frame. Use a 224px
desktop sidebar and a 760px content column centered in the remaining space.
The sidebar shows branding, Back to home, Dashboard (linking to `/links`),
active Account settings, the theme control, and the current account identity.
Below 1024px a compact menu button
opens a native modal navigation drawer. Use clear profile and session rows,
then a separate, restrained destructive section. Do not invent editable
profile fields, analytics routes, avatars, or legal pages.

## Acceptance criteria

- [x] Settings has a full-height sidebar with real navigation, no site header,
      and no footer; mobile navigation uses an accessible modal drawer.
- [x] Profile identity and current-session sign-out have a clear hierarchy.
- [x] Deletion retains its separate confirmation, focus management, safe errors,
      pending state, and private-cache cleanup after success.
- [x] Loading, failure, signed-out, and deleted states fit the page layout.
- [x] One visible sidebar theme control replaces the floating control.
- [x] Both themes work at 320, 768, 1024, and 1440px without overflow; navigation
      and destructive confirmation remain keyboard operable.
- [x] Relevant existing tests, lint, typecheck, formatting, and build pass.

## Validation

Run the frontend checks and existing authentication browser regressions.
Inspect desktop/mobile screenshots using intercepted API fixture responses;
do not modify or delete a real account for visual verification.
