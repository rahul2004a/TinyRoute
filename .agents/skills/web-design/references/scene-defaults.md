# Scene Defaults

Default layout, component, and interaction baselines for each context. Load the applicable section after confirming the context in Round 1, then use it as the starting point for subsequent discussion.

---

## Landing Page / Product Page

```yaml
layout:
  structure: Vertical multi-section narrative (Hero → Features → Social Proof → CTA)
  container: 1200px max, centered
  section_spacing: 80-120px
  grid: Flexible composition (switch among 1, 2, or 3 columns based on content)

components:
  required: Hero (large heading + subheading + CTA), navigation bar, Feature cards, CTA button, Footer
  common: Customer-logo marquee, pricing cards, FAQ accordion, Testimonial cards
  optional: Embedded video, statistics, comparison table

interaction_baseline: L2
  - navigation: sticky, with background blur after scrolling
  - entrance: section-level fadeInUp plus internal stagger
  - Hero: 2-3 staggered entrance layers
  - cards: hover lift + shadow
  - CTA: hover scale + color change
  - scroll: progress bar (optional)

typography:
  hero_size: 3.5-5rem
  section_title: 2-2.5rem
  body: 1-1.125rem
  density: Medium (alternate headings, paragraphs, and whitespace)
```

---

## Portfolio

```yaml
layout:
  structure: Hero + project grid + About Me + Contact
  container: 1100-1400px
  grid: 2-3-column masonry or equal-height grid
  project_details: Modal / new page / expansion

components:
  required: Project cards (cover + title + tags), filtering/categories, contact information
  common: Skill tags, timeline, social links
  optional: Custom cursor, project-detail pages, dark-mode toggle

interaction_baseline: L2-L3
  - project_cards: image zoom + information reveal on hover
  - images: lightbox or full-screen preview
  - scroll: reveal + parallax
  - page_transitions: cross-page animation (L3)
  - cursor: custom cursor or magnetic effect (L3)

typography:
  hero_size: 4-6rem (may be very large)
  body: 0.95-1rem
  density: Low (generous whitespace, image-led)
```

---

## Blog / Content Site

```yaml
layout:
  structure: Article list / individual article
  container: 720-800px (narrow container for optimal reading)
  sidebar: Optional (table of contents / tags / recommendations)
  spacing: 1.5-2em between paragraphs

components:
  required: Article cards, article-body typography, dates/tags, pagination
  common: Table of contents (TOC), code blocks, blockquotes, image lightbox
  optional: Comments, search, RSS, dark mode

interaction_baseline: L1
  - entrance: subtle fadeIn
  - article_cards: color change or slight lift on hover
  - table_of_contents: highlight the current section while scrolling
  - code_blocks: one-click copy
  - images: click to enlarge

typography:
  article_title: 2-2.5rem
  body: 1-1.0625rem, line-height 1.7-1.8
  chinese_additions: letter-spacing 0.02em, first-line indent 2em
  density: High (text-dense, with breathing room from line-height and paragraph spacing)
```

---

## Dashboard

```yaml
layout:
  structure: Sidebar + top bar + content area
  container: Full width (240-280px sidebar, fluid content area)
  grid: Dense grid (2-4 columns, 12-16px gap)
  spacing: Compact (16-24px between sections)

components:
  required: Side navigation, data cards, chart containers, tables, filters
  common: KPI figures, status badges, breadcrumbs, notifications, loading skeletons
  optional: Live-update indicator, dark mode, draggable cards

interaction_baseline: L1
  - data_cards: highlight border on hover (do not lift; it disrupts information density)
  - table_rows: background color change on hover
  - sidebar: collapsible, with the current item highlighted
  - loading: pulsing skeleton
  - status: animated numeric transitions (number rolling)
  - note: do not use scroll reveal (information must be immediately visible)

typography:
  page_title: 1.5rem
  card_title: 0.875-1rem
  body: 0.8125-0.875rem
  figures: 1.5-2.5rem, font-weight 700-800
  density: High (information-dense, separated by grids and borders)
```

---

## Presentation / Course Material / Knowledge Map

```yaml
layout:
  structure: Hero + section navigation + Bento-grid content area
  container: 1280px
  grid: 3-column Bento (mix span-1/span-2/span-3)
  gap: 14px
  spacing: 48-64px between sections

components:
  required: Navigation pills, module cards, Hero statistics, section headings
  common: Progress indicator, difficulty labels, knowledge-point cards, timeline
  optional: Table of contents, download button, assessment form

interaction_baseline: L1
  - navigation: anchor scrolling + current-item highlight (IntersectionObserver)
  - cards: darker border + light shadow on hover
  - labels: invert colors on hover
  - print: hide navigation with @media print, use a single-column layout
  - note: avoid scroll animation (course material must support quick browsing and printing)

typography:
  hero_title: 3-3.5rem (serif, decorative)
  section_title: 1.5rem (serif)
  card_title: 0.95rem (sans, bold)
  body: 0.82-0.85rem
  labels: 0.65-0.72rem, uppercase, letter-spacing
  density: Medium-high (substantial information organized by card boundaries)
```

---

## App UI / Admin Console

```yaml
layout:
  structure: Top bar + sidebar (optional) + content area + modal/drawer
  container: Full width, content area max 1400px
  grid: Grouped by function, fluid 1-3 columns
  spacing: Medium (24-32px)

components:
  required: Buttons (multiple states), forms (input/select/checkbox), tables, modal/drawer
  common: Toast notifications, stepper, Tabs, dropdown menus, pagination
  optional: Drag and drop, rich-text editing, file uploads, skeleton screens

interaction_baseline: L1-L2
  - buttons: hover/active/disabled/loading states
  - forms: focus ring, validation feedback, error highlighting
  - modal: background blur + content scaleIn
  - Toast: slideIn + automatic dismissal
  - list: drag-to-reorder when needed
  - transitions: fade between pages (do not slide; it conflicts with navigation expectations)

typography:
  page_title: 1.25-1.5rem
  body: 0.875rem
  label: 0.75rem
  density: Medium (function-first, with moderate whitespace)
```

---

## Email Template

```yaml
layout:
  structure: Logo header + body copy + CTA + footer
  container: 600px max (email-client constraint)
  grid: Single column or 2 columns (table-based, not CSS grid)
  spacing: 20-30px padding

components:
  required: Header (Logo + heading), body paragraphs, CTA button, footer (unsubscribe link)
  common: Product cards, side-by-side image and text, divider
  note: All styles must be inline; CSS classes are unsupported

interaction_baseline: None (email does not support JavaScript)
  - CTA_button: implement rounded corners with table+td and inline background-color
  - links: inline color and text-decoration
  - images: must set width, alt text, and display:block

typography:
  title: 24-28px
  body: 15-16px, line-height 1.5-1.6
  safe_fonts: Arial, Helvetica, Georgia, Times New Roman
  prohibited: Google Fonts and custom fonts (unsupported by some clients)
  density: Low (email reading environments are distracting, so use large type and generous whitespace)

constraints:
  - Inline every style (`<style>` is not supported in every client)
  - Use `<table>` for layout, not flexbox/grid
  - Limit image width to 580px
  - Use a total width of 600px
  - Do not use JavaScript
  - Use a VML fallback for background images (Outlook)
```
