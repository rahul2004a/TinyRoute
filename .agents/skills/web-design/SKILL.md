---
name: web-design
description: Web visual-design SKILL. Given any combination of a PRD, reference URL, screenshot, or keywords, first produce a standardized DESIGN.md specification. After user confirmation, generate web code whose UI/UX, visuals, motion, and responsive behavior all meet the specification. Specializes in web experiences such as landing pages, portfolios, product pages, blogs, personal sites, and SaaS introduction pages. Trigger when the user asks to build a website, design a page, create something based on a reference, turn a screenshot or PRD into a webpage, create a landing page, or produce a design specification. Do not use for backend, database, or pure logic bug-fix work.
---

# Web Design

Web visual-design SKILL. Two-stage workflow: **produce the specification (DESIGN.md) first, then the code.**

DESIGN.md is an explicit file artifact saved in the project directory. It can be reused across projects, edited manually, and consumed by other tools.

## Core Workflow

```
Phase A  Understand requirements (flexible input) ──→  Phase B  Produce DESIGN.md (user confirmation) ──→  Phase C  Generate project code
```

**Check at startup** (in priority order):
1. Does the project already contain DESIGN.md? → If so, reuse, modify, or rebuild it
2. Does the project contain PRD.md? → If so, enter A4 (PRD-driven workflow)
3. Neither exists → Proceed with Phase A normally

---

## Phase A: Understand Requirements

Input is flexible. Accept any combination below; do not force the user through a fixed path:

| Input | Approach |
|------|---------|
| **Reference URL** | Fetch HTML+CSS, extract tokens, and perform a motion audit |
| **Screenshot / design mockup** | Extract mood, color temperature, density, and typographic style from the visuals |
| **Keywords / description** | "Dark, restrained serif style" → match tokens from style-seeds |
| **Brand name** | "Make it similar to Linear" → first check `references/design-systems/`; read the preset if available, otherwise crawl the website |
| **Mixed input** | "Use this URL as a reference, but make the palette warmer" → extract + override |
| **PRD document** | Read product positioning, page structure, and design-handoff sections from PRD.md → analyze competitor styles → derive the design direction |

### A1. URL Analysis (When a Reference URL Is Provided)

**Determine intent**: reference (extract the design language and create an independent new page) vs reproduction (the user explicitly says "reproduce/clone").

**Acquisition workflow** (try in priority order):

1. **Preferred: Playwright crawler** (real browser; can bypass anti-scraping measures, render SPAs, and simulate scrolling)
   ```bash
   python3 scripts/crawl_website.py --url [URL] --output ./crawl-output --scroll-delay 600
   ```
   Output: one screenshot per viewport + tokens.json + structure.json + styles.css

2. **Alternative: Token extraction script** (lightweight and suitable for simple static sites)
   ```bash
   python3 scripts/extract_design_tokens.py --url [URL] --format json
   ```

3. **Fallback: manual curl** (when neither script is available)
   ```bash
   curl -Ls [URL]
   ```

Synthesize all three input types with this priority: actual experience (atmosphere and rhythm) > screenshots (local visual character) > token values (precise parameters). Playwright viewport screenshots can serve as both experiential and screenshot inputs.

**Motion Audit** (mandatory when the reference has scroll-linked motion):
- Trigger signals: effects change continuously with scrolling / sections use stagger, pinning, or parallax / the site references GSAP, Lenis, etc.
- Record the trigger, driver, elements, effect, and timing for each animated region

**Fallback chain**:
- Playwright crawler fails → check `references/design-systems/` for a preset for that brand (58 brands)
- No preset → attempt static extraction with `extract_design_tokens.py`
- Static extraction also fails → curl the CSS manually
- Everything fails → ask the user for screenshots and build the tokens manually from the visuals

### A2. Conversation Guidance (Without a Reference)

Converge quickly on a style direction; do not mechanically walk through a questionnaire:

**Core questions** (ask only those needed, not all of them):
- Light vs dark?
- Serif vs sans serif?
- Restrained whitespace vs rich density?
- Accent-color preference?
- Any websites or styles they like?

Match the closest seed from `references/style-seeds.md` (10 preset directions), then present it for user confirmation or mixing.

When the user mentions a specific brand, consult `references/design-systems/INDEX.md`, search by category, and read the corresponding brand file on demand (58 real-site design specifications, about 300 lines each). Brand files contain only static design systems; motion must still be added according to the interaction tier.

### A3. Confirm the Interaction Tier (Mandatory)

| Tier | Experience |
|------|--------|
| **L1** Refined Static | Elegant hover + gentle entrances |
| **L2** Fluid Interaction | Scroll reveals, parallax, and navigation changes |
| **L3** Immersive Experience | Scroll-driven timelines, pinning, cursor tracking, and transitions |

For L2, ask about reveal style (fadeInUp/scaleIn), parallax, and navigation changes.
For L3, ask about section pinning, cursor effects, transitions, and whether GSAP/ScrollTrigger/Lenis are allowed.

Consult `references/interaction-patterns.md` for complete code for the selected tier.

### A4. PRD-driven Flow (When a Project-level Specification Is Detected)

**Automatically scan the project root at startup**, in priority order:
1. `PRD.md` / `prd.md`
2. `SPEC.md` / `spec.md`
3. `README.md` or any `.md` file containing key fields such as Positioning / Target Users / Pages / Core Screens

**Do not bind this flow to a specific tool or template.** If the following fields can be extracted from the document, they are sufficient as design input:

| Field (aliases) | Use |
|-------------------|------|
| Product / Product Name / Name | Hero copy |
| Tagline / Pitch / One-line Positioning | Hero subheading and tone |
| Target Users / Audience | Color-temperature and typographic-style decisions |
| Core Pages / Pages / Screens | Phase C generation checklist |
| Competitors / References | Starting point for Phase A style references |
| Tech Stack | Phase C code-generation approach |
| Design Handoff / Design Notes / Tone | Hard constraints to inherit directly |

**Competitor style analysis**:
- PRD mentions competitors → first check `references/design-systems/` for existing presets
- Preset exists → use that brand specification as the starting point
- No preset → call `scripts/crawl_website.py` to crawl and extract the site
- PRD names no competitors → match the closest seed in `references/style-seeds.md` based on product type + tone

**When key fields cannot be extracted**: do not guess. Return to the conversation guidance (A2) to fill the gaps.

**Confirm with the user** (example):
> I found the following in `{filename}`:
> - Product: {name} — {one-line positioning}
> - Target users: {profile}
> - Reference competitors: {list or "none"}
> - Suggested style direction: {1-2 candidates, each with a one-sentence rationale}
>
> Does this direction look right?

After confirmation, enter A3 (interaction tier), then Phase B.

---

## Phase B: Produce DESIGN.md (Mandatory)

**You must generate a DESIGN.md file.** Use the template in `references/design-md-template.md` to produce nine sections:

1. **Visual Theme & Atmosphere** — design philosophy, atmosphere keywords, and a one-sentence direction
2. **Color Palette & Roles** — complete CSS variable definitions (including RGB helper values)
3. **Typography Rules** — font families, Google Fonts URL, type hierarchy, and prohibited fonts
4. **Component Stylings** — complete CSS for buttons, cards, navigation, links, labels, etc. (including all states)
5. **Layout Principles** — grid, spacing scale, and container width
6. **Depth & Elevation** — shadow system
7. **Animation & Interaction** — motion tier plus complete code for entrances, scrolling, hover, and special effects
8. **Do's and Don'ts** — design guardrails and anti-patterns (at least eight)
9. **Responsive Behavior** — breakpoints, touch targets, and collapse strategy

**Text-decoration rules**: After generating the Color Palette and Typography, use the decision table in `references/text-decoration-rules.md` to decide, heading level by heading level, whether to add gradients or shadows.

**Present a summary for user confirmation.** After confirmation, save DESIGN.md in the project directory, then enter Phase C.

---

## Phase C: Generate Project Code (Mandatory)

### C1. Confirm the Context + Collect Content

Ask which page to build and collect its specific content. Consult `references/scene-defaults.md` for the layout and component baseline for that context.

**Content requirements for common contexts**:

| Context | Content to collect |
|------|---------|
| Landing Page | Heading, subheading, Feature list, CTA, social proof |
| Personal Site / Portfolio | Name, title, project list, About section, contact details, blog list |
| Blog | Name, article list/content, author information, category tags |
| Product Page | Product name, selling points, feature screenshots, pricing, FAQ |

Ask proactively when the user's information is incomplete. If the user says "Build it first; I'll add the content later," use reasonable placeholder content.

### C2. Design the Structure + Generate Code

1. **Inspect the project environment**: framework, routing, styling approach, existing components, and assets
2. **Choose the framework**: existing project → continue with it; no project → use the minimal viable approach
3. **Generate strictly from DESIGN.md**:
   - Reference every color through CSS variables
   - Use fonts as defined in DESIGN.md
   - Implement interactions at the tier specified in DESIGN.md
   - Do not violate the Do's and Don'ts
4. **Image strategy**: user-provided media > project assets > reference-site URL placeholders > Unsplash
5. **Icon strategy**: existing project library > lucide-react > inline SVG (see `references/icon-library.md`)

### C3. Audit

- **DESIGN.md compliance check**: verify that the code follows the specification exactly
- **Reference difference audit** (when a reference exists): compare tokens, layout, typography, interaction, and media item by item
- **Responsive verification**: at least mobile + desktop
- **Quality checklist**: read `references/quality-checklist.md` and check every item

---

## Landing-page Impact Principles (Landing Pages Only)

The **landing page (home page)** is the most important page of a web platform. Other pages need to remain consistent and follow the specification, but the home page alone must satisfy the following two non-negotiable principles.

### Principle 1: Three "Wow" Moments + One Delightful Detail

The locations of the impact moments are **fixed**. Each location must deliver a visual surprise that makes the user say "wow":

| # | Location | Requirement | Typical techniques |
|---|------|------|---------|
| **1** | **First viewport / Hero** | Motion, 3D, or a giant gradient creates immediate impact on load | Bursting card constellation, WebGL 3D object, giant-heading mask reveal, pointer-following spotlight, 3D card stack |
| **2** | **First scroll** | The first section after the Hero must contain a hook that sustains attention | Manifesto marquee (large type), number burst, split-screen cut, card-convergence transition, full-screen video |
| **3** | **List / array / enumeration / showcase** | Must not be a conventional equal-size grid | Uneven Bento layout, SpotlightCard hover tracking, 3D tilted cards, abstract-art caps |

**One delightful design detail**: a detail that is easy to miss but makes the user smile when noticed. Candidates:
- Feedback microanimation (sparkle + tooltip after copying)
- Easter egg (Konami code / Ctrl+K command palette)
- Meta joke (hover a keyword to reveal "This is the specification used by this page →")
- A small surprise at the end of the page (such as "Thanks for making it this far")

### Principle 2: Fluid, Unobstructed Scrolling (Non-negotiable)

The following performance limits have **equal priority** with the impact design. Spectacular moments that stutter receive a score of zero.

- **3D / WebGL**: at most one instance per page, and it must pause through IntersectionObserver while not visible
- **`filter: blur()` on moving elements**: prohibited. Use opacity + scale to create depth instead
- **`backdrop-filter: blur()`**: value ≤ 14px and must not cover a large scrolling region
- **Lenis / scroll-jacking**: enable only when pin-scrub is genuinely necessary; use native `scroll-behavior: smooth` on ordinary pages
- **ScrollTrigger pin**: ≤ 2 per page (reserved for doubao-level narrative density)
- **pointermove listeners**: must be throttled with rAF
- **Global custom-cursor replacement**: use only on design or tool sites with a strong visual character; prohibit it on ordinary SaaS sites

### Implementation Recommendations (Performance-friendly Impact Recipes)

These combinations have been measured at 60fps on mid-range hardware:

| Impact moment | Implementation | Performance cost |
|------|------|---------|
| **Pointer spotlight** | CSS var `--mx/--my` + `radial-gradient(at var(--mx) var(--my))` | ⭐ Extremely low (one repaint) |
| **Card-burst entrance** | One-shot GSAP timeline (1.5s stagger on load, then release) | ⭐⭐ Low (once only) |
| **Manifesto marquee** | Pure CSS `transform: translateX` keyframes | ⭐ Extremely low |
| **SpotlightCard** | Per-card `--mx/--my` + `::before` radial-gradient (rAF-throttled) | ⭐⭐ Low |
| **Large-type mask reveal** | One-shot pure CSS `clip-path` animation | ⭐ Extremely low |
| **Flowing keyword gradient** | `background-position` animation | ⭐ Extremely low |

Combinations to avoid:
- ❌ 12+ elements using `filter: blur()` + `will-change` simultaneously (exhausts GPU memory)
- ❌ Lenis + multiple pin-scrub scenes + WebGL (blocks the main thread)
- ❌ Continuous Three.js rendering that never pauses (consumes frame time even offscreen)

---

## 100-point Quality Baseline (Hard Acceptance Gate)

DESIGN.md and the generated code must **both** satisfy every condition below. If any condition is unmet, the work is incomplete. See `references/quality-checklist.md` for the full checklist; these are the core non-negotiables:

### DESIGN.md Non-negotiables
- All nine sections contain substantive content rather than template placeholders
- Every component style includes **all states**: default / hover / active / focus / disabled
- Every color is defined as a CSS variable, including RGB helper values for rgba
- Fonts include an `@import` URL + fallback stack
- The motion tier (L1/L2/L3) is explicit, with complete dependency declarations
- Do's and Don'ts contains at least eight items, including at least five Don'ts
- L2+ includes a `prefers-reduced-motion` fallback
- Responsive behavior covers at least Desktop + Mobile

### Code Non-negotiables
- Every color references a CSS variable, with **zero hard-coded hex values**
- Fonts implement the Typography Rules in DESIGN.md exactly
- Every interactive element has hover + focus states
- Entrance animation is implemented (at least an L1 fade-in)
- L2+ implements scroll reveal + scrolled navigation states + parallax
- L3 implements pinning / cursor tracking / transitions
- **Solid-color image placeholders are prohibited** (use reference-site URLs / Unsplash / user-provided media)
- Icons use the project library / lucide-react / inline SVG (emoji are prohibited outside a Playful tone)
- No DESIGN.md Do or Don't is violated
- At ≤ 600px, there is no horizontal overflow and touch targets are ≥ 44×44px

### Signature-motion Non-negotiables (Mandatory for L2+, Stronger for L3)

**Every L2+ page must include all six motion categories below. None may be omitted.** Prefer selections from [vue-bits / reactbits](https://github.com/DavidHDev/vue-bits) (see `references/motion-library.md`), and reuse source rather than rewriting when possible.

| Category | Count | Placement |
|------|------|------|
| Text Animation — Hero H1 | ≥ 1 | Large-heading entrance or continuous state (such as SplitText / ShinyText / GradientText) |
| Text Animation — Section H2 | ≥ 1 | Scroll-triggered section heading (such as ScrollFloat / BlurText) |
| Text Animation — Body / Label | ≥ 1 | Body copy, eyebrow, or code block (such as ScrollReveal / TextType / ScrambleText) |
| Animation — element level | ≥ 1 | Magnetic CTA / card hover / cursor / click feedback (such as Magnet / GlareHover / ClickSpark) |
| Component — interactive structure | ≥ 1 | Card stack / gallery / navigation / 3D card (such as CardSwap / ScrollStack / SpotlightCard / MagicBento) |
| Background — atmosphere layer | ≥ 1 | Hero or global background (such as Aurora / Silk / Grainient / Threads) |

**An L3 page must contain at least six signature moments in total**, but no more than 10 per page (more becomes restless).

### Additional L3 Cinematic Scroll-story Requirements

In addition to the six motion categories above, an L3 page (for requests such as "like doubao / apple / stripe," "include 3D," "this feels ordinary," or "make it cinematic") **must cover at least three of the four scroll-story patterns**. See `references/scroll-story-patterns.md` for detailed templates:

| Pattern | Required | Description |
|------|------|------|
| Pin-Scrub scene | ≥ 1 | Section pin + scroll-driven content transformation/timeline |
| Left pin / right swap container replacement | ≥ 1 | Fix the heading on the left while the right container switches among scenes on scroll (matching doubao's four product scenes) |
| Convergence/divergence transition | ≥ 1 | Multiple elements fly from scattered positions into a merged form, or the reverse (matching doubao's Hero-to-next-viewport transition) |
| WebGL / true 3D signature moment | ≥ 1 | Three.js / OGL / Babylon; CSS 3D alone does not count |

**Golden rule**: **Include a signature moment every 1-2 viewports.** Three consecutive viewports containing only fade reveals is a failure.

**Performance baseline**:
- WebGL scenes per page ≤ 1
- Fall back automatically on mobile (see the performance section in scroll-story-patterns.md)
- `prefers-reduced-motion` must have a complete fallback path

**Style fit**: Follow the "Recommended Combinations by Style × Context" in `references/motion-library.md` exactly—do not use GlitchText for Dark Editorial or Aurora for Playful. Tonal consistency matters more than spectacle.

**Performance baseline**:
- Heavy backgrounds (WebGL) per page ≤ 1
- Fall back automatically on mobile: heavy background → static gradient; 3D component → 2D
- Enable cursor effects only under `matchMedia('(hover: hover)')`
- Every effect must have a `prefers-reduced-motion` fallback path

**Attribution**: When using vue-bits / reactbits source, add "Motion effects derived from [vue-bits](https://github.com/DavidHDev/vue-bits) by DavidHDev (MIT)" to the page footer or README.

### Reference-alignment Audit (When a Reference Exists)
- Count every section on the reference site
- Align palette / typography / spacing with reference values (or state the design rationale for intentional divergence)
- Reference site has scroll-linked motion → perform a motion audit and implement the corresponding tier
- After generation, perform an explicit difference audit listing what aligns and what intentionally differs

**The audit is not optional.** After writing the Phase C code, review every item and fix failures in place.

---

## Technology-stack Principles

- **Framework**: continue with the project's existing stack; if none exists, use the minimal viable approach (static HTML or Vite)
- **Styling**: continue with the existing approach; do not force in new dependencies
- **Components**: reuse an existing library; without one, write semantic components directly
- **Animation**: prefer CSS; L2 may use IntersectionObserver; L3 may use GSAP/ScrollTrigger/Lenis
- **Images**: prohibit solid-color block placeholders; manage placeholder images centrally in `const IMG` at the top of the code

---

## Language Rules

**Follow the input language by default**; do not impose a language:

| Input signal | Default output language |
|----------|--------------|
| Reference URL / screenshot / PRD is in Chinese | Chinese |
| Reference URL / screenshot / PRD is in English | English |
| User's conversation language | Fallback signal |

**Add multilingual switching only when needed**:
- Add i18n only when the user explicitly requests it or the PRD says the product serves multiple markets
- **Do not add locale objects and language switchers to every page by default**—they are noise in a single-language product
- When i18n is needed:
  - Centralize copy in a separate locale object (`const messages = { zh: {...}, en: {...} }`) and reference it in components with `t('hero.title')`
  - Switching must take effect immediately without a refresh
  - Simple project: top-level locale + React state; Next.js: `next-intl` / `i18next`; plain HTML: `<select>` + `data-lang`

**Requirements specific to Chinese-language pages** (mandatory when generating Chinese content):
- Use a Chinese typeface such as Noto Sans SC / Noto Serif SC / LXGW WenKai; do not configure only Latin fonts and rely on system fallback
- Line-height ≥ 1.7 and `letter-spacing: 0.02em`
- Body font size ≥ 15px and long-form reading size ≥ 16px
- Mixed Chinese and English: put the Chinese typeface first, followed by a Latin typeface (Inter / DM Sans) as fallback in the `font-family` chain

---

## Reference-file Index

| File | Contents | When to read |
|------|------|----------|
| `references/design-md-template.md` | DESIGN.md output template (nine sections) | Phase B |
| `references/interaction-patterns.md` | Three-tier L1/L2/L3 interaction code library | Phase A + B |
| `references/motion-library.md` | vue-bits/reactbits effect catalog + recommended combinations by style | Phase B (selection) + Phase C (implementation) |
| `references/scroll-story-patterns.md` | L3 cinematic scroll-story pattern library (card constellation / left pin and right swap / convergence transition / WebGL signature) | Phase B (after selecting L3) + Phase C |
| `references/style-seeds.md` | 10 style seeds → token mappings | During Phase A conversation |
| `references/scene-defaults.md` | Layout/component/interaction baselines for seven contexts | Phase C |
| `references/text-decoration-rules.md` | Text-gradient/shadow decision table | Phase B |
| `references/icon-library.md` | lucide-react icon quick reference | Phase C |
| `references/quality-checklist.md` | Quality checklist | Phase C audit |
| `references/design-systems/INDEX.md` | Index of 58 brand design systems | In Phase A when the user mentions a brand |
| `references/design-systems/{name}.md` | Complete design specification for one brand | Read on demand; never load all files |
| `scripts/crawl_website.py` | Playwright crawler (screenshots + tokens + structure) | Preferred in Phase A |
| `scripts/extract_design_tokens.py` | Lightweight token extraction (static-site alternative) | Alternative in Phase A |
| `scripts/fetch_unsplash_images.py` | Candidate placeholder-image URLs | Phase C |
