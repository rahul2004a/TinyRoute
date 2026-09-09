# Quality Checklist

After Phase C code generation, check every item below.

## DESIGN.md Compliance (Required)

- [ ] DESIGN.md has been generated and saved in the project directory
- [ ] DESIGN.md contains all nine sections, with no empty sections
- [ ] Every color in the code references a CSS variable defined in DESIGN.md; there are no hard-coded hex values
- [ ] Fonts follow the Typography Rules in DESIGN.md exactly
- [ ] Interactions implement the tier specified in DESIGN.md (L1/L2/L3) exactly
- [ ] No rule in the DESIGN.md Do's and Don'ts has been violated

## Reference-site Analysis (When a Reference URL Is Provided)

- [ ] Analysis combines experiential assessment, token extraction, and screenshot comparison
- [ ] Original HTML/CSS was fetched with a script or curl; the analysis does not rely solely on an AI summary
- [ ] Every section on the reference site has been counted
- [ ] A motion audit was completed if the site has scroll-linked animation
- [ ] An explicit difference audit compared the generated result with the reference

## Typography

- [ ] Fonts follow DESIGN.md and include a Google Fonts `@import` URL plus fallbacks
- [ ] Decoration decisions for h1/h2/h3 follow `text-decoration-rules.md`
- [ ] **Context-specific type-size requirements** (use the applicable scene sizes from `scene-defaults.md`; do not apply one scale everywhere):
  - Landing / Portfolio Hero: ≥ 60px, weight ≥ 700
  - Blog / article: Hero 32-40px, body 1-1.0625rem, line-height 1.7-1.8
  - Dashboard / App UI: page title 1.25-1.5rem, body 0.875rem
  - Presentation / course material: Hero 3-3.5rem (serif), body 0.82-0.85rem
  - Email: heading 24-28px, body 15-16px
- [ ] Chinese content: the font stack includes a Chinese typeface, line-height is at least 1.7, and letter-spacing is 0.02em

## Visual System

- [ ] Every color uses a CSS variable
- [ ] Icons use the project library, lucide-react, or inline SVG; **emoji decoration is permitted only for a Playful Creative tone**
- [ ] User-provided media has been adapted appropriately; mismatches have been called out
- [ ] Image placeholders use reference-site URLs or Unsplash; solid-color blocks are prohibited

## Interaction

- [ ] Every interactive element has hover and focus states
- [ ] Entrance animation is implemented (at least an L1 fade-in)
- [ ] L2+ scroll behavior is implemented (reveal, parallax, and navigation changes)
- [ ] L3 effects are implemented (pinning, cursor effects, and transitions)
- [ ] L2+ includes a `prefers-reduced-motion` fallback

## Responsive Design

- [ ] Supports at least mobile (< 600px) and desktop layouts
- [ ] Navigation has a mobile collapse strategy
- [ ] Images and containers do not overflow
