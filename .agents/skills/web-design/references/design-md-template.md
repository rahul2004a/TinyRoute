# DESIGN.md Template

Use this template when producing DESIGN.md in Phase B. All nine standard sections must contain substantive content.

---

## Template

```markdown
# DESIGN.md

> {{one-sentence design manifesto}}

## 1. Visual Theme & Atmosphere

**Style**: {{style name}}
**Keywords**: {{5-8 design keywords}}
**Tone**: {{tone description}} — NOT {{opposing keywords}}
**Feel**: {{one evocative sentence using a metaphor}}

**Interaction Tier**: {{L1 Refined Static / L2 Fluid Interaction / L3 Immersive Experience}}
**Dependencies**: {{CSS only / GSAP + ScrollTrigger / GSAP + ScrollTrigger + Lenis}}

## 2. Color Palette & Roles

```css
:root {
  /* Backgrounds */
  --bg: {{primary background}};              /* Page background */
  --surface: {{surface}};                    /* Cards/containers */
  --surface-alt: {{secondary surface}};      /* Alternating sections */
  --surface-hover: {{hover surface}};        /* Hover-state surface */

  /* Borders */
  --border: {{border}};                      /* Default border */
  --border-hover: {{hover border}};          /* Hover border */

  /* Text */
  --text: {{primary text}};                  /* Headings and important text */
  --text-secondary: {{secondary text}};      /* Body copy and descriptions */
  --text-tertiary: {{tertiary text}};        /* Labels and supporting information */

  /* Accent */
  --accent: {{primary accent}};              /* CTAs, links, and active states */
  --accent-hover: {{accent hover}};

  /* RGB variants for rgba() */
  --bg-rgb: {{r,g,b}};
  --accent-rgb: {{r,g,b}};

  /* Semantic */
  --success: {{success color}};
  --error: {{error color}};
  --warning: {{warning color}};
}
```

**Color Rules:**
- {{rule 1, e.g. "Reference every color through a CSS variable; do not hard-code hex values"}}
- {{rule 2, e.g. "Use only one accent color within a section"}}
- {{rule 3}}

## 3. Typography Rules

**Font Stack:**
```css
@import url('{{Google Fonts URL}}');
```

| Role | Font | Size | Weight | Line Height | Letter Spacing |
|------|------|------|--------|-------------|----------------|
| Hero H1 | {{font}} | {{size}} | {{weight}} | {{lh}} | {{ls}} |
| Section H2 | {{font}} | {{size}} | {{weight}} | {{lh}} | {{ls}} |
| H3 | {{font}} | {{size}} | {{weight}} | {{lh}} | — |
| Body | {{font}} | {{size}} | {{weight}} | {{lh}} | — |
| Label | {{font}} | {{size}} | {{weight}} | {{lh}} | {{ls}} |
| Mono/Code | {{font}} | {{size}} | {{weight}} | {{lh}} | — |

**Typography Rules:**
- {{rule, e.g. "Heading weight ≥ 700"}}
- **NEVER use**: {{prohibited font list}}

**Text Decoration:**
- {{result from the decision table in text-decoration-rules.md, e.g. "Hero h1: no gradient or shadow (restrained style)"}}

## 4. Component Stylings

### Buttons
```css
{{complete CSS, including default / hover / active / focus / disabled}}
```

### Cards
```css
{{complete CSS, including default / hover / focus}}
```

### Navigation
```css
{{complete CSS, including a scrolled state when applicable}}
```

### Links
```css
{{complete CSS, including hover animation}}
```

### Tags / Badges
```css
{{complete CSS}}
```

### {{other components required by the context}}

## 5. Layout Principles

**Container:**
- Max width: {{width}}
- Padding: {{padding}}
- Narrow variant (text-heavy): {{width}}

**Spacing Scale:**
- Section padding: {{value}}
- Component gap: {{value}}
- Card internal padding: {{value}}

**Grid:**
```css
{{grid CSS}}
```

## 6. Depth & Elevation

| Level | Treatment | Use |
|-------|-----------|-----|
| Flat | {{no shadow}} | {{context}} |
| Subtle | {{light shadow}} | {{context}} |
| Elevated | {{medium shadow}} | {{context}} |
| {{additional levels}} | | |

## 7. Animation & Interaction

**Motion Philosophy**: {{one sentence, e.g. "Restrained and elegant; use only opacity and transform"}}
**Tier**: {{L1/L2/L3}}

### Dependencies
```html
{{CDN links, if any}}
```

### Base Setup
```js
{{GSAP/Lenis initialization code, if any}}
```

### Entrance Animation
```css
{{entrance-animation keyframes and class names}}
```

### Scroll Behavior
```js
{{scroll reveal / parallax / pinning code}}
```

### Hover & Focus States
```css
{{hover/focus rules for every interactive element}}
```

### Special Effects
{{include as needed: cursor tracking / page transitions / text reveal / parallax / other effects}}

### Reduced Motion
```css
@media (prefers-reduced-motion: reduce) {
  {{fallback rules}}
}
```

## 8. Do's and Don'ts

### Do
- {{positive rule, at least five total}}

### Don't
- {{prohibited item, at least eight total, prefixed with ❌}}

## 9. Responsive Behavior

**Breakpoints:**
| Name | Width | Key Changes |
|------|-------|-------------|
| Desktop | > {{bp}} | {{layout}} |
| Tablet | {{bp}}-{{bp}} | {{layout}} |
| Mobile | < {{bp}} | {{layout}} |

**Touch Targets:** minimum {{size}}
**Collapsing Strategy:** {{collapse rules}}

```css
{{responsive CSS}}
```
```

---

## Generation Guidance

Follow these rules when generating DESIGN.md:

1. **Every section must contain substantive content**; a heading-only placeholder is not enough
2. **All CSS must be runnable**, not pseudocode
3. **Components must include every state** (default / hover / active / focus / disabled)
4. **The Animation section cannot be empty**; include at least entrance and hover animation
5. **Do's and Don'ts is a core section**—anti-patterns constrain AI behavior more effectively than positive advice
6. **L2+ must include a Reduced Motion fallback**
7. **Define every color as a CSS variable**; do not hard-code hex values in components
8. **Fonts must include a Google Fonts `@import` URL plus fallbacks**
9. **Responsive behavior must cover at least Desktop and Mobile**
10. **Save the file as DESIGN.md in the project root**
