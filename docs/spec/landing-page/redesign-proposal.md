# Landing page visual revision

Status: implementation authorized on 2026-10-12 (user: "implementation starts").
The latest hero screenshot supersedes the sculpture and URL/route animation
concepts. The final instruction adds independent looping light fades and
explicitly removes playback controls.

## Audit and preservation

The first page was too uniform: hero copy sat apart from the headline, section
headings repeated a split, and thin dividers carried nearly every group. Keep
TinyRoute branding, Geist, semantic light/dark colors, 8px controls, 12px panels,
1200px container, routes, section IDs, tested mobile navigation, Planned labels,
and neutral session behavior. Metadata remains unchanged. Add no tracking,
backend behavior, or invented proof. The later supplied typewriter component
explicitly authorizes Motion for the closing CTA only.

## Selected direction

Reading this as a SaaS landing page for people sharing links, with a spacious
technical visual language inspired by the user's hero screenshot. Retain design
variance 5 and project density; keep the hero copy/grid still and use slow
decorative light fades only. This is native code inspired by the composition, not an Aceternity
component or a copy of its product content.

- Open hero canvas with large two-line copy and actions grouped on the left.
- Static perspective grid and route lines on the right, with six softly lit
  connection points. Use the existing paper/charcoal colors. The scoped DESIGN.md
  exception allows static halos and a fade mask. No text glow or second accent.
- No illustration box, typing demo, or hero entrance. The six light points fade
  independently with uneven delays; each cycle receives a new random 8–14-second
  duration. They repeat indefinitely with no playback button, per user request.
  Reduced motion and JavaScript-disabled rendering show static lights.
- Spacious header with filled icon/wordmark at the left and theme/account
  actions at the right. Remove Features and How it works from the header;
  preserve them in the footer. No divider; 64px mobile/tablet, 80px desktop.
- Reduce background prominence on mobile; preserve text contrast, two-line H1,
  first-viewport CTA, and 44px targets.
- Five varied feature panels: short links leads; aliases and expiry support;
  management and analytics remain visibly Planned.
- Stacked section headings, one connected three-step group, spacious closing
  CTA, and a spacious footer following the user's second screenshot. The closing
  CTA keeps the previous bordered surface panel around the requested smooth
  typewriter heading, intro, and real account links. Footer
  branding/copyright sits beside Pages and Account columns, with an oversized
  subtle TinyRoute wordmark below (14vw, capped at 200px after user sizing feedback). Use a soft faded/2px-blurred upper edge and a clearer lower edge. Keep the watermark decorative and static;
  keep real navigation accessible. Center the wordmark horizontally within the
  footer, keeping both layers coincident. Preserve account labels and destinations.

## Verification

Regenerate and inspect all eight viewport/theme screenshots plus both expanded
mobile menus. Check light timing/reduced motion, contrast, overflow, keyboard/focus,
existing authentication entry routes, theme persistence, reduced motion,
backend failure, and JavaScript-disabled readability. Then finish full checks
and one independent final review. Keep task files active until complete.
