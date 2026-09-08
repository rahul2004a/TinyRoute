# Marketing-page scene defaults

Use these as conversation and layout starting points. Root `DESIGN.md`, the
actual content, and the existing Next.js application decide the final result.

## SaaS landing page

```yaml
structure:
  - focused hero with one primary action
  - problem or value statement
  - product capabilities demonstrated with real UI or approved visuals
  - trust evidence
  - final call to action
layout:
  container: use the existing project container
  rhythm: alternate dense proof with quieter explanation
  grids: vary columns according to content; avoid repeated equal-card rows
components:
  required: navigation, hero, capability sections, call to action, footer
  optional: metrics, testimonials, FAQ, comparison, demo media
interaction:
  baseline: restrained entrance and state transitions
  avoid: mandatory parallax, autoplay media, scroll-jacking
```

## Product feature page

```yaml
structure:
  - feature-specific hero
  - workflow or before-and-after explanation
  - detailed capability sections
  - constraints, privacy, or reliability proof where relevant
  - related action
layout:
  container: reuse landing-page grid and tokens
  density: higher than the home page, but still scannable
components:
  required: feature navigation, examples, states, call to action
  optional: code sample, diagram, benchmark, FAQ
interaction:
  baseline: reveal only when it improves comprehension
```

## Portfolio or case-study page

```yaml
structure:
  - concise identity or project statement
  - selected work or outcome overview
  - process and decisions
  - measurable result
  - contact or next project
layout:
  media: let approved work samples carry the page
  whitespace: generous, with a clear reading path
components:
  required: project summary, media, responsibilities, outcome
  optional: filter, timeline, testimonial, related work
interaction:
  baseline: image and link feedback with subtle reveals
```

## Blog or editorial page

```yaml
structure:
  - article index or article body
  - metadata and navigation
  - related content
layout:
  measure: 65-75 characters for long-form prose
  spacing: use line height and paragraph rhythm instead of cards
components:
  required: title, metadata, content, navigation
  optional: table of contents, code block, quotation, figure, search
interaction:
  baseline: minimal; keep reading stable
```

## Campaign or launch page

```yaml
structure:
  - campaign proposition
  - one memorable visual idea
  - proof or explanation
  - time-sensitive action when applicable
layout:
  variation: may be more expressive than the product UI
  continuity: retain recognizable TinyRoute palette and typography
interaction:
  baseline: one signature moment with a reduced-motion equivalent
  avoid: multiple competing effects
```

## Content collection

Before implementation, identify the actual headline, supporting copy, primary
action, product proof, approved imagery, and legal or trust content. Use concise
placeholders only when the user explicitly accepts placeholder content.
