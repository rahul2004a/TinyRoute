# DESIGN.md

> Specification first, code second. — A page designed to express the methodology itself.

**Project**: Open-source introduction page for the web-design SKILL
**Positioning**: When AI builds a website, first produce a readable, editable, portable design specification (DESIGN.md)
**Reference starting point**: hueapp.io (dark editorial style with restrained soft light) — retain its structural foundation while changing accent colors and font pairing to establish a distinct identity

---

## 1. Visual Theme & Atmosphere

**Style**: Dark Editorial × Design System × Cinematic Scroll
**Keywords**: restrained, editorial, methodological, softly luminous, document-like, precise, readable, trustworthy, cinematic
**Tone**: Dark but not austere, technical but not cyberpunk; **Stripe-level information density + Apple-level signature motion + Linear-level structure** — NOT neon, punk, flashy, or sales-driven
**Feel**: An open dark technical manual, with a visual moment worth pausing for on every page — but still a book, not a game

**Interaction Tier**: **L3 Immersive Experience · cinematic scroll story** (matching the narrative density of doubao.com/about while retaining a dark editorial foundation)
**Dependencies**: GSAP 3 + ScrollTrigger + Lenis + **Three.js** (WebGL signature moment) + OGL (Aurora background) + CSS `@property` + CSS 3D transforms

**Scroll-story pattern coverage** (see `references/scroll-story-patterns.md`):
- ✅ **Pattern 1: Card-constellation Hero** → 12 DESIGN.md sample cards floating in 3D space
- ✅ **Pattern 2: Card-convergence transition** → Every card flies toward the center and merges into one between the Hero and Why sections
- ✅ **Pattern 3: Left pin / right swap** → Used in both Three Inputs and Phase A→B→C
- ✅ **Pattern 4: WebGL 3D signature** → "What's Inside" uses a Three.js iridescent torus knot instead of a CSS cube
- ✅ **Pattern 5: Heading glow** → Large section headings include a `::before` ghost
- ✅ **Pattern 6: Abstract-art cap** → Feature cards are topped with mesh-gradient art

**Motion Library selection** (ported directly from [vue-bits](https://github.com/DavidHDev/vue-bits), MIT):

| Category | Selection | Placement |
|------|------|------|
| Background (atmosphere layer) | **Aurora** | Full-screen Hero background (soft flowing aurora, ideal for dark editorial design) |
| Text — Hero H1 | **SplitText** + **ShinyText** | Character-staggered H1 entrance; the keywords "spec" and "code" receive a Shiny metallic light sweep |
| Text — Section H2 | **ScrollFloat** | Each section H2 floats in character by character as it enters the viewport |
| Text — Body / Label | **ScrollReveal** (body paragraphs) + **ScrambleText** (eyebrow labels) + **TextType** (Install code block) | Three granularities cover body copy, labels, and code |
| Animation — element level | **Magnet** + **GlareHover** + **ClickSpark** | Magnetic CTA; light sweep on card hover; particle burst on button click |
| Component — interactive structure | **CardSwap** (3D card stack on the right of the Hero) + **SpotlightCard** (What's Inside cards) + **InfiniteScroll** (Showcase strip) + **MagicBento** (Why comparison grid) | Four components carry four narrative segments |

**Total signature moments: 10+** (3 in the Hero + 1 Phase pin + 1-2 in each section)

---

## 2. Color Palette & Roles

```css
:root {
  /* Backgrounds — half a step darker than hueapp to reinforce the "dark manual" feel */
  --bg: #0a0b0e;
  --bg-2: #07080b;
  --surface-1: #111217;
  --surface-2: #171921;
  --surface-3: #1f222d;
  --surface-hover: #242836;

  /* Borders */
  --border: rgba(67, 70, 81, 0.5);
  --border-strong: rgba(67, 70, 81, 0.9);
  --border-accent: rgba(94, 234, 212, 0.3);

  /* Text — four-level hierarchy */
  --text-1: #ebecef;
  --text-2: #c6c9d2;
  --text-3: #8d909c;
  --text-4: #60636f;

  /* Accent — cool cyan + warm orange (specification × warmth) */
  --accent-cool: #5EEAD4;
  --accent-cool-hover: #8CF5E3;
  --accent-cool-soft: rgba(94, 234, 212, 0.12);

  --accent-warm: #FB923C;
  --accent-warm-hover: #FDB874;
  --accent-warm-soft: rgba(251, 146, 60, 0.12);

  /* Gradient — reserved for keyword decoration; do not use broadly */
  --gradient-key: linear-gradient(135deg, #5EEAD4 0%, #FB923C 100%);

  /* RGB variants */
  --bg-rgb: 10, 11, 14;
  --accent-cool-rgb: 94, 234, 212;
  --accent-warm-rgb: 251, 146, 60;

  /* Semantic */
  --success: #5EEAD4;
  --error: #F87171;
  --warning: #FB923C;
}
```

**Color Rules**:
- Reference every color through a CSS variable; hard-coded hex values are strictly prohibited
- Use cool cyan and warm orange **only for emphasis** (CTAs, keywords, selected states, and hover accents); use surfaces for every large area of color
- Use the **`--gradient-key` gradient for only 1-2 core phrases** ("specification," "100 score," and "DESIGN.md") — no more than one instance per viewport
- Only one accent family may dominate within a section; when cool and warm accents appear together, separate them with whitespace
- Follow the text-color hierarchy: headings `--text-1`, body `--text-2`, supporting text `--text-3`, labels `--text-4`

---

## 3. Typography Rules

**Font Stack**:
```css
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:opsz,wght@9..40,400..700&family=Instrument+Serif:ital@0;1&family=JetBrains+Mono:wght@400;500&display=swap');

--font-ui: 'DM Sans', system-ui, -apple-system, 'Segoe UI', Roboto, 'Noto Sans SC', sans-serif;
--font-serif: 'Instrument Serif', 'Noto Serif SC', Georgia, serif;
--font-mono: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
```

| Role | Font | Size | Weight | Line Height | Letter Spacing |
|------|------|------|--------|-------------|----------------|
| Hero H1 | DM Sans | `clamp(48px, 6vw, 84px)` | 700 | 1.05 | -0.02em |
| Hero Serif Accent | Instrument Serif (italic) | inherit | 400 | 1.05 | 0 |
| Section H2 | DM Sans | `clamp(28px, 3.2vw, 42px)` | 700 | 1.15 | -0.015em |
| H3 | DM Sans | 22px | 600 | 1.3 | -0.01em |
| Eyebrow Label | DM Sans | 13px | 500 | 1.4 | 0.08em (uppercase) |
| Body Lg | DM Sans | 17px | 400 | 1.6 | 0 |
| Body | DM Sans | 15px | 400 | 1.65 | 0 |
| Small | DM Sans | 13px | 500 | 1.5 | 0 |
| Micro | DM Sans | 11px | 500 | 1.4 | 0.06em (uppercase) |
| Code / Mono | JetBrains Mono | 14px | 500 | 1.5 | 0 |
| Serif Quote | Instrument Serif | 28-36px | 400 (italic) | 1.3 | 0 |

**Typography Rules**:
- All body headings use DM Sans; **Instrument Serif is reserved for italic keyword decoration** (such as "*design*.md" and "*100 score*"), with no more than three instances per page
- Heading weight ≥ 600 (Hero 700)
- For Chinese content, the font stack automatically falls back to Noto Sans SC, with line-height ≥ 1.7 and letter-spacing 0.02em
- **NEVER use**: Arial, Times New Roman, Comic Sans, or any decorative display font

**Text Decoration** (decide according to `text-decoration-rules.md`):
- Hero H1: Fill the keywords ("spec" and "code") with `--gradient-key`; **no shadow** (large type on a dark background does not need one)
- Section H2: Solid `--text-1`, with no gradient or shadow
- H3 / Body: Solid color, with no decoration

---

## 4. Component Stylings

### Buttons

```css
/* Primary — cool-cyan fill for key CTAs */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 9999px;
  background: var(--accent-cool);
  color: var(--bg);
  font: 500 14px/1 var(--font-ui);
  letter-spacing: -0.005em;
  border: 1px solid var(--accent-cool);
  cursor: pointer;
  transition: transform 0.22s cubic-bezier(.2,0,0,1),
              background 0.12s cubic-bezier(.2,0,0,1),
              box-shadow 0.22s cubic-bezier(.2,0,0,1);
}
.btn-primary:hover {
  background: var(--accent-cool-hover);
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(var(--accent-cool-rgb), 0.25);
}
.btn-primary:active { transform: translateY(0); }
.btn-primary:focus-visible { outline: 2px solid var(--accent-cool); outline-offset: 3px; }
.btn-primary:disabled { opacity: 0.45; cursor: not-allowed; transform: none; box-shadow: none; }

/* Ghost — bordered treatment for secondary CTAs */
.btn-ghost {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 11px 19px;
  border-radius: 9999px;
  background: transparent;
  color: var(--text-1);
  border: 1px solid var(--border-strong);
  font: 500 14px/1 var(--font-ui);
  cursor: pointer;
  transition: background 0.12s, border-color 0.12s, transform 0.22s;
}
.btn-ghost:hover { background: var(--surface-2); border-color: var(--text-3); transform: translateY(-1px); }
.btn-ghost:active { transform: translateY(0); }
.btn-ghost:focus-visible { outline: 2px solid var(--accent-cool); outline-offset: 3px; }

/* Pill — small label button for the three-step summary */
.pill {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 12px;
  border-radius: 9999px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  color: var(--text-2);
  font: 500 12px/1 var(--font-mono);
}
```

### Cards

```css
.card {
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 24px;
  transition: background 0.22s cubic-bezier(.2,0,0,1),
              border-color 0.22s cubic-bezier(.2,0,0,1),
              transform 0.22s cubic-bezier(.2,0,0,1);
}
.card:hover {
  background: var(--surface-2);
  border-color: var(--border-strong);
  transform: translateY(-2px);
}
.card:focus-within { border-color: var(--accent-cool); }

/* Feature card with accent icon slot */
.card .card-icon {
  width: 36px; height: 36px;
  border-radius: 8px;
  background: var(--accent-cool-soft);
  color: var(--accent-cool);
  display: inline-flex; align-items: center; justify-content: center;
  margin-bottom: 16px;
}
.card .card-title { font: 600 18px/1.3 var(--font-ui); color: var(--text-1); margin: 0 0 6px; }
.card .card-body  { font: 400 14px/1.6 var(--font-ui); color: var(--text-3); margin: 0; }
```

### Navigation

```css
.nav {
  position: sticky; top: 0; z-index: 50;
  padding: 14px 0;
  background: transparent;
  border-bottom: 1px solid transparent;
  transition: background 0.22s, border-color 0.22s, backdrop-filter 0.22s;
}
.nav.is-scrolled {
  background: rgba(var(--bg-rgb), 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom-color: var(--border);
}
.nav-link {
  color: var(--text-3);
  font: 500 14px/1 var(--font-ui);
  transition: color 0.12s;
}
.nav-link:hover { color: var(--text-1); }
.nav-link.is-active { color: var(--accent-cool); }
```

### Links

```css
.link {
  color: var(--accent-cool);
  text-decoration: none;
  background-image: linear-gradient(var(--accent-cool), var(--accent-cool));
  background-size: 0% 1px;
  background-position: 0 100%;
  background-repeat: no-repeat;
  transition: background-size 0.28s cubic-bezier(.2,0,0,1), color 0.12s;
}
.link:hover { background-size: 100% 1px; color: var(--accent-cool-hover); }
.link:focus-visible { outline: 2px solid var(--accent-cool); outline-offset: 3px; border-radius: 2px; }
```

### Tags / Badges

```css
.tag {
  display: inline-flex; align-items: center;
  padding: 4px 10px;
  border-radius: 9999px;
  background: var(--accent-cool-soft);
  color: var(--accent-cool);
  font: 500 11px/1.4 var(--font-mono);
  letter-spacing: 0.04em;
}
.tag.tag-warm { background: var(--accent-warm-soft); color: var(--accent-warm); }
.tag.tag-neutral { background: var(--surface-2); color: var(--text-3); border: 1px solid var(--border); }
```

### Code Block / Install Command

```css
.codeblock {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 18px;
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: 12px;
  font: 500 14px/1 var(--font-mono);
  color: var(--text-2);
}
.codeblock .prompt { color: var(--text-4); user-select: none; }
.codeblock .copy-btn {
  margin-left: auto;
  padding: 6px 10px;
  font: 500 11px/1 var(--font-mono);
  color: var(--text-3);
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.12s, border-color 0.12s;
}
.codeblock .copy-btn:hover { color: var(--accent-cool); border-color: var(--accent-cool); }
.codeblock .copy-btn.is-copied { color: var(--accent-cool); }
```

### Step Indicator (for the three-step summary)

```css
.steps { display: flex; flex-wrap: wrap; gap: 24px; justify-content: center; }
.step {
  display: inline-flex; align-items: center; gap: 8px;
  font: 400 13px/1 var(--font-mono);
  color: var(--text-3);
}
.step .step-num { color: var(--text-4); letter-spacing: 0.04em; }
.step .step-sep { color: var(--text-4); margin: 0 4px; }
```

---

## 5. Layout Principles

**Container**:
- `max-width: 1120px`
- Side padding: `clamp(20px, 4vw, 40px)`
- Narrow variant (body/install): `max-width: 720px`

**Spacing Scale** (matching hueapp's 4→128 progression):
```
--sp-1: 4px    --sp-6: 32px
--sp-2: 8px    --sp-7: 48px
--sp-3: 12px   --sp-8: 64px
--sp-4: 16px   --sp-9: 96px
--sp-5: 24px   --sp-10: 128px
```

- Section vertical padding: `96px → 128px` (desktop), `64px → 80px` (mobile)
- Card padding: 24px
- Component gap: 16-24px
- Spacing between Hero elements: 16-24-32 progression

**Grid**:
```css
.grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.grid-4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 900px) { .grid-3, .grid-4 { grid-template-columns: 1fr 1fr; } }
@media (max-width: 600px) { .grid-3, .grid-4 { grid-template-columns: 1fr; } }
```

---

## 6. Depth & Elevation

In a dark context, **use fewer shadows and rely more on surface hierarchy**.

| Level | Treatment | Use |
|-------|-----------|-----|
| Flat | No border or shadow | Background decoration and large color fields |
| Bordered | `1px solid var(--border)` | Default cards and inputs |
| Bordered Hover | `1px solid var(--border-strong)` + `translateY(-2px)` | Card hover |
| Glow (CTA Hover only) | `box-shadow: 0 8px 24px rgba(var(--accent-cool-rgb), 0.25)` | Primary-button hover |
| Blurred Overlay | `backdrop-filter: blur(14px) + rgba bg` | Sticky navigation while scrolled |

Do not use multiple overlapping shadows or enormous blur effects.

---

## 7. Animation & Interaction

**Motion Philosophy**: Editorial foundation + cinematic signature moments. Most areas remain quiet; **2-3 signature moments deliver the wow factor**. Motion serves the narrative: the Hero establishes atmosphere, Phase A→B→C explains the methodology, and the Showcase presents the output.
**Tier**: **L3 Immersive Experience**

### Dependencies

```html
<!-- GSAP + ScrollTrigger + Lenis (all via CDN, no build step) -->
<script src="https://cdn.jsdelivr.net/npm/gsap@3.12.5/dist/gsap.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/gsap@3.12.5/dist/ScrollTrigger.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/lenis@1.1.14/dist/lenis.min.js"></script>
```

### Timing Tokens

```css
--ease: cubic-bezier(.2, 0, 0, 1);
--ease-soft: cubic-bezier(.2, .8, .2, 1);
--ease-cinema: cubic-bezier(.16, 1, .3, 1);
--dur-fast: 0.12s;
--dur-mid: 0.22s;
--dur-slow: 0.36s;
--dur-reveal: 0.8s;
--dur-signature: 1.4s;
```

### Base Setup — Lenis + GSAP

```js
// 1. Lenis smooth scrolling
const lenis = new Lenis({
  duration: 1.2,
  easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
  smoothWheel: true,
  smoothTouch: false,
});
function raf(time) { lenis.raf(time); requestAnimationFrame(raf); }
requestAnimationFrame(raf);

// 2. Bridge GSAP + ScrollTrigger with Lenis
gsap.registerPlugin(ScrollTrigger);
lenis.on('scroll', ScrollTrigger.update);
gsap.ticker.add((time) => lenis.raf(time * 1000));
gsap.ticker.lagSmoothing(0);

// 3. Top scroll-progress bar
gsap.to('.scroll-progress', {
  scaleX: 1,
  ease: 'none',
  scrollTrigger: { trigger: document.body, start: 'top top', end: 'bottom bottom', scrub: true },
});
```

### Entrance Animation

```css
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(24px); filter: blur(6px); }
  to   { opacity: 1; transform: translateY(0); filter: blur(0); }
}
.reveal { opacity: 0; }
.reveal.in-view { animation: fadeUp 0.8s var(--ease-cinema) forwards; }

/* Stagger child elements */
.reveal.in-view > * { animation: fadeUp 0.8s var(--ease-cinema) backwards; }
.reveal.in-view > *:nth-child(1) { animation-delay: 0.00s; }
.reveal.in-view > *:nth-child(2) { animation-delay: 0.08s; }
.reveal.in-view > *:nth-child(3) { animation-delay: 0.16s; }
.reveal.in-view > *:nth-child(4) { animation-delay: 0.24s; }
.reveal.in-view > *:nth-child(5) { animation-delay: 0.32s; }
```

### Hover & Focus States

- Every button, card, and link must have hover and focus-visible states
- Card hover: brighten the surface + strengthen the border + `translateY(-2px)` + slightly enlarge the internal icon
- Primary-button hover: rise 1px + cyan glow + magnetic cursor tracking within ±8px
- Link hover: animate the underline from 0% to 100%

### Magnetic Button

```js
document.querySelectorAll('[data-magnetic]').forEach(btn => {
  const strength = 0.35;
  btn.addEventListener('pointermove', (e) => {
    const r = btn.getBoundingClientRect();
    const x = (e.clientX - r.left - r.width / 2) * strength;
    const y = (e.clientY - r.top - r.height / 2) * strength;
    gsap.to(btn, { x, y, duration: 0.4, ease: 'power3.out' });
  });
  btn.addEventListener('pointerleave', () => {
    gsap.to(btn, { x: 0, y: 0, duration: 0.6, ease: 'elastic.out(1, 0.4)' });
  });
});
```

### Custom Cursor (dot + ring)

```css
* { cursor: none; }
@media (hover: none) { * { cursor: auto; } .cursor-dot, .cursor-ring { display: none; } }
.cursor-dot, .cursor-ring {
  position: fixed; top: 0; left: 0; pointer-events: none; z-index: 9999;
  border-radius: 50%; transform: translate(-50%, -50%);
  mix-blend-mode: difference;
}
.cursor-dot  { width: 6px;  height: 6px;  background: #fff; transition: transform 0.08s linear; }
.cursor-ring { width: 36px; height: 36px; border: 1px solid #fff; transition: transform 0.2s var(--ease-cinema), width 0.22s, height 0.22s; }
.cursor-ring.is-hover { width: 60px; height: 60px; border-color: var(--accent-cool); }
```

```js
const dot = document.querySelector('.cursor-dot');
const ring = document.querySelector('.cursor-ring');
let rx = 0, ry = 0, dx = 0, dy = 0;
window.addEventListener('pointermove', (e) => {
  dx = e.clientX; dy = e.clientY;
  dot.style.transform = `translate(${dx}px, ${dy}px) translate(-50%, -50%)`;
});
(function followRing() {
  rx += (dx - rx) * 0.18; ry += (dy - ry) * 0.18;
  ring.style.transform = `translate(${rx}px, ${ry}px) translate(-50%, -50%)`;
  requestAnimationFrame(followRing);
})();
document.querySelectorAll('a, button, [data-magnetic], .card, .codeblock .copy-btn').forEach(el => {
  el.addEventListener('pointerenter', () => ring.classList.add('is-hover'));
  el.addEventListener('pointerleave', () => ring.classList.remove('is-hover'));
});
```

### Hero Background — Animated Grid Gradient (CSS-only)

```css
@property --grad-angle {
  syntax: '<angle>';
  initial-value: 0deg;
  inherits: false;
}
@keyframes gradRotate {
  to { --grad-angle: 360deg; }
}
.hero-bg {
  position: absolute; inset: 0; z-index: 0;
  background:
    radial-gradient(60% 50% at 30% 20%, rgba(var(--accent-cool-rgb), 0.18), transparent 70%),
    radial-gradient(50% 40% at 75% 35%, rgba(var(--accent-warm-rgb), 0.14), transparent 65%),
    conic-gradient(from var(--grad-angle) at 50% 50%,
      rgba(var(--accent-cool-rgb), 0.08),
      rgba(var(--accent-warm-rgb), 0.08),
      rgba(var(--accent-cool-rgb), 0.08));
  filter: blur(40px) saturate(120%);
  animation: gradRotate 30s linear infinite;
  opacity: 0.85;
}
.hero-bg::after {
  content: ''; position: absolute; inset: 0;
  background: radial-gradient(ellipse at 50% 100%, var(--bg) 0%, transparent 70%);
}
```

### Signature #1 — Hero Card Constellation (Pattern 1)

**Twelve DESIGN.md sample cards float in 3D space**. Each card is a slice of a real design specification (palette, type table, component CSS, or Do/Don't item). They surround the central heading "Specification first, code second," respond to the pointer with group parallax, and breathe independently.

```css
.constellation {
  position: absolute; inset: 0;
  perspective: 1500px; transform-style: preserve-3d;
  pointer-events: none; /* Keep Hero text clickable */
}
.star-card {
  position: absolute; top: 50%; left: 50%;
  background: var(--surface-1);
  border: 1px solid var(--border-strong);
  border-radius: 14px;
  padding: 16px 18px;
  box-shadow: 0 24px 60px rgba(0,0,0,0.5);
  will-change: transform, filter;
  pointer-events: auto;
  transform:
    translate3d(calc(-50% + var(--x) * 1px), calc(-50% + var(--y) * 1px), var(--z, 0px))
    rotateX(var(--rx, 0deg)) rotateY(var(--ry, 0deg)) rotateZ(var(--rz, 0deg));
  filter: blur(var(--blur, 0px));
  opacity: calc(1 - var(--blur, 0) * 0.06);
}
/* Near / middle / far groups */
.star-card.near   { --blur: 0;  --z:  0; }
.star-card.mid    { --blur: 3;  --z: -250; }
.star-card.far    { --blur: 7;  --z: -500; opacity: 0.55; }
/* Expand x/y/rx/ry/rz for all 12 cards through data-i and CSS variables; generate exact values in Phase C */
```

Example card content (12 cards representing 12 types of specification excerpts):
1. Palette (4 swatches + variable names)
2. Type table (3 rows of font-family + weight)
3. Button CSS (5-6-line code excerpt)
4. Shadow scale (3 box-shadow previews)
5. Spacing scale (8 scale bars)
6. Radius samples (4 blocks with different border-radius values)
7. Do/Don't checklist (4-5 items)
8. Motion curve (SVG easing-curve chart)
9. Grid system (12-column preview)
10. Icon collection (6 lucide icons)
11. Code-block preview (3 lines of monospaced code)
12. Responsive breakpoints (three viewport-size diagrams)

```js
// 1. Initialize positions (12 points distributed along two elliptical rings)
const positions = [
  { x: -380, y: -180, rx:  8, ry: -12, rz: -4, cls: 'near' },
  { x:  360, y: -200, rx: -6, ry:   8, rz:  6, cls: 'near' },
  { x: -460, y:   40, rx:  4, ry: -10, rz: -8, cls: 'mid' },
  { x:  480, y:   60, rx: -4, ry:  10, rz:  8, cls: 'mid' },
  { x: -280, y:  220, rx:  6, ry:  -6, rz: -2, cls: 'near' },
  { x:  320, y:  240, rx: -8, ry:   6, rz:  4, cls: 'near' },
  { x: -600, y: -100, rx:  2, ry: -14, rz: -6, cls: 'far' },
  { x:  620, y: -120, rx: -2, ry:  14, rz:  6, cls: 'far' },
  { x: -150, y: -260, rx: 10, ry:   4, rz: -2, cls: 'mid' },
  { x:  180, y: -240, rx:-10, ry:  -4, rz:  2, cls: 'mid' },
  { x:    0, y:  340, rx: -6, ry:   0, rz:  0, cls: 'far' },
  { x:    0, y: -360, rx:  6, ry:   0, rz:  0, cls: 'far' },
];
document.querySelectorAll('.star-card').forEach((el, i) => {
  const p = positions[i];
  el.classList.add(p.cls);
  Object.entries(p).forEach(([k, v]) => {
    if (['x','y','rx','ry','rz'].includes(k)) el.style.setProperty(`--${k}`, v);
  });
});

// 2. Idle breathing for each card
gsap.utils.toArray('.star-card').forEach((c) => {
  gsap.to(c, {
    '--y': `+=${gsap.utils.random(-18, 18)}`,
    '--rz': `+=${gsap.utils.random(-3, 3)}`,
    duration: gsap.utils.random(5, 8),
    ease: 'sine.inOut', yoyo: true, repeat: -1,
  });
});

// 3. Group pointer parallax
const stage = document.querySelector('.hero');
stage.addEventListener('pointermove', (e) => {
  const cx = innerWidth/2, cy = innerHeight/2;
  const dx = (e.clientX - cx) / cx, dy = (e.clientY - cy) / cy;
  gsap.utils.toArray('.star-card').forEach((c) => {
    const depth = Math.abs(parseFloat(c.style.getPropertyValue('--z') || 0)) / 500;
    gsap.to(c, { x: dx * 30 * (1-depth), y: dy * 30 * (1-depth), duration: 0.8, ease: 'power3.out' });
  });
});
```

### Signature #2 — Card-convergence Transition (Pattern 2)

Between the Hero and Why sections, the scattered cards fly toward the center and merge into one large DESIGN.md card.

```js
gsap.timeline({
  scrollTrigger: {
    trigger: '.hero',
    start: 'bottom 70%',
    end: 'bottom top',
    scrub: 1,
  },
})
.to('.star-card', {
  '--x': 0, '--y': 0, '--z': 0, '--rx': 0, '--ry': 0, '--rz': 0,
  '--blur': 0,
  scale: 0.2,
  opacity: 0,
  stagger: { amount: 0.8, from: 'random' },
  ease: 'power2.in',
})
.from('.why-hero-card', { scale: 0.6, opacity: 0, duration: 1, ease: 'power3.out' }, '-=0.5');
```

### Signature #3 — Phase A→B→C Pinned Scrollytelling (Enhanced Pattern 3)

**Core mechanism**. Pin this section for roughly 150vh of scroll distance so the transformation from "input → DESIGN.md → code" follows scroll progress.

```js
const phaseTl = gsap.timeline({
  scrollTrigger: {
    trigger: '.phase-scene',
    start: 'top top',
    end: '+=1500',
    scrub: 1,
    pin: true,
    anticipatePin: 1,
  },
});

// Phase A: three input icons float → merge
phaseTl
  .from('.phase-input-prd',    { y: 40, opacity: 0, duration: 1 })
  .from('.phase-input-url',    { y: 40, opacity: 0, duration: 1 }, '-=0.7')
  .from('.phase-input-shot',   { y: 40, opacity: 0, duration: 1 }, '-=0.7')
  .to(['.phase-input-prd', '.phase-input-url', '.phase-input-shot'], {
    x: (i) => [-40, 0, 40][i] * -1, y: 0, scale: 0.8, opacity: 0.3, duration: 1.2,
  })
  // Phase B: the DESIGN.md card floats in from the right; token lines populate one by one
  .from('.phase-design-card', { x: 120, opacity: 0, duration: 1.2, ease: 'power3.out' }, '<')
  .from('.phase-token-row', { width: 0, opacity: 0, stagger: 0.12, duration: 0.6 }, '-=0.6')
  // Phase C: code lines flow out of DESIGN.md
  .to('.phase-design-card', { x: -80, scale: 0.9, opacity: 0.5, duration: 1 }, '+=0.4')
  .from('.phase-code-line', { x: -40, opacity: 0, stagger: 0.05, duration: 0.5 }, '-=0.8')
  // Final: finished webpage thumbnail
  .from('.phase-final', { y: 40, opacity: 0, duration: 1 }, '+=0.3');
```

A progress bar appears along the left side of the section, with three color segments (cyan → cyan-orange → orange) indicating the phases.

### Signature #4 — WebGL 3D Iridescent Knot (Pattern 4 · Replaces the CSS Cube)

The visual centerpiece of the "What's Inside" section. **Three.js TorusKnotGeometry + MeshPhysicalMaterial** (transmission 0.92, iridescence 1, clearcoat 1). Scroll drives its rotation; cyan and orange point lights tint it; 50 particle sprites create "dust"; and orbital rings are overlaid with SVG/CSS.

```js
import * as THREE from 'https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.js';

const canvas = document.querySelector('.knot-canvas');
const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
const resize = () => {
  renderer.setSize(canvas.clientWidth, canvas.clientHeight);
  camera.aspect = canvas.clientWidth / canvas.clientHeight;
  camera.updateProjectionMatrix();
};

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
camera.position.z = 5;

const mat = new THREE.MeshPhysicalMaterial({
  transmission: 0.92, thickness: 1.5, roughness: 0.15,
  iridescence: 1, iridescenceIOR: 1.3, clearcoat: 1,
  color: 0xffffff,
});
const knot = new THREE.Mesh(new THREE.TorusKnotGeometry(1, 0.3, 180, 32), mat);
scene.add(knot);

scene.add(new THREE.HemisphereLight(0xffffff, 0x101020, 0.6));
const l1 = new THREE.PointLight(0x5EEAD4, 4, 20); l1.position.set(3, 3, 3); scene.add(l1);
const l2 = new THREE.PointLight(0xFB923C, 4, 20); l2.position.set(-3, -2, 3); scene.add(l2);

// Dust particles
const pgeo = new THREE.BufferGeometry();
const positions = new Float32Array(150 * 3);
for (let i = 0; i < 150; i++) {
  positions[i*3]   = (Math.random()-0.5) * 8;
  positions[i*3+1] = (Math.random()-0.5) * 6;
  positions[i*3+2] = (Math.random()-0.5) * 4;
}
pgeo.setAttribute('position', new THREE.BufferAttribute(positions, 3));
const particles = new THREE.Points(pgeo, new THREE.PointsMaterial({ color: 0xaaaaaa, size: 0.03, transparent: true, opacity: 0.7 }));
scene.add(particles);

gsap.to(knot.rotation, {
  y: Math.PI * 2, x: Math.PI,
  scrollTrigger: { trigger: '.whats-inside', start: 'top 80%', end: 'bottom 20%', scrub: 1 },
});

function tick() {
  knot.rotation.z += 0.002;
  particles.rotation.y += 0.0008;
  renderer.render(scene, camera);
  requestAnimationFrame(tick);
}
resize(); tick();
new ResizeObserver(resize).observe(canvas);
```

The **card portion of What's Inside** remains (showing six asset categories such as 58 brands / 10 seeds / 3 tiers / 7 scenes). Place it below the WebGL scene and top each card with a **Pattern 6 abstract-art cap** (cyan-orange gradient mesh).

### Signature #5 — Three Inputs: Left Pin / Right Swap (Pattern 3)

Pin the Three Inputs section for 300vh. Keep the heading fixed on the left ("Start from anywhere") while the right container switches through three scenes on scroll: **PRD document → reference URL screenshot → user sketch**. Each scene is an enlarged, realistic card with sheen and detail. Transition with a crossfade and slight scale.

```js
const scenes = gsap.utils.toArray('.input-scene');
const labels = ['PRD document', 'Reference URL', 'Screenshot / Sketch'];

ScrollTrigger.create({
  trigger: '.three-inputs',
  start: 'top top', end: 'bottom bottom',
  pin: '.three-inputs-inner',
  scrub: 0.5,
  onUpdate: (self) => {
    const idx = Math.min(Math.floor(self.progress * scenes.length), scenes.length - 1);
    scenes.forEach((s, i) => {
      gsap.to(s, { opacity: i === idx ? 1 : 0, scale: i === idx ? 1 : 0.94, duration: 0.4 });
    });
    document.querySelector('.input-label').textContent = labels[idx];
  },
});
```

### Signature #6 — Large Section-heading Glow (Pattern 5)

```css
.section-h2 {
  position: relative; display: inline-block;
  font: 700 clamp(36px, 5vw, 64px)/1.1 var(--font-ui);
  color: var(--text-1);
}
.section-h2::before {
  content: attr(data-ghost);
  position: absolute; left: 0; top: 0;
  background: var(--gradient-key);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: blur(24px); opacity: 0.45;
  z-index: -1;
  transform: translate(6px, 6px);
}
```

### Signature #7 — Abstract-art-capped Feature Cards (Pattern 6)

```css
.art-card { background: var(--surface-1); border: 1px solid var(--border); border-radius: 16px; overflow: hidden; }
.art-top {
  aspect-ratio: 4 / 3;
  background:
    radial-gradient(circle at 30% 40%, var(--accent-cool) 0%, transparent 48%),
    radial-gradient(circle at 72% 60%, var(--accent-warm) 0%, transparent 44%),
    radial-gradient(circle at 50% 85%, #c084fc 0%, transparent 40%);
  background-size: 200% 200%;
  filter: saturate(130%);
  animation: artShift 20s ease-in-out infinite alternate;
}
@keyframes artShift { to { background-position: 100% 100%; } }
.art-bottom { padding: 20px 22px 22px; }
```

### Signature #8 — Showcase 3D Perspective Marquee

Two rows of work thumbnails move in opposite directions while the whole group tilts in 3D space (x-axis -8°), creating the parallax impression that they are "flying past behind the frame."

```css
.marquee {
  perspective: 1200px;
  overflow: hidden;
  -webkit-mask-image: linear-gradient(to right, transparent, #000 12%, #000 88%, transparent);
          mask-image: linear-gradient(to right, transparent, #000 12%, #000 88%, transparent);
}
.marquee-track {
  display: flex; gap: 24px;
  transform: rotateX(-8deg) rotateZ(-2deg);
  animation: marqueeSlide 40s linear infinite;
}
.marquee.reverse .marquee-track { animation-direction: reverse; animation-duration: 50s; }
@keyframes marqueeSlide { to { transform: rotateX(-8deg) rotateZ(-2deg) translateX(-50%); } }
```

### Signature #5 — Palette-token Convergence Animation (Why DESIGN.md Section)

As the section enters the viewport, 12 scattered floating swatches fly into grid points and form a `:root { --bg: ... }` code block.

```js
ScrollTrigger.create({
  trigger: '.token-assemble',
  start: 'top 70%',
  onEnter: () => {
    gsap.from('.token-chip', {
      x: () => gsap.utils.random(-200, 200),
      y: () => gsap.utils.random(-100, 100),
      rotate: () => gsap.utils.random(-30, 30),
      opacity: 0,
      stagger: 0.05,
      duration: 1,
      ease: 'power3.out',
    });
    gsap.from('.token-code-line', { opacity: 0, x: -20, stagger: 0.06, duration: 0.5, delay: 0.8 });
  },
});
```

### Signature #6 — Text Scramble (Eyebrow Labels)

When each eyebrow label enters the viewport, run a 0.8s scrambled-characters-to-stable-text animation.

```js
function scrambleText(el, duration = 800) {
  const final = el.dataset.scramble || el.textContent;
  el.dataset.scramble = final;
  const chars = '!<>-_\\/[]{}—=+*^?#________';
  let start = performance.now();
  function tick(now) {
    const p = Math.min(1, (now - start) / duration);
    const out = final.split('').map((c, i) => {
      if (p * final.length > i) return c;
      if (c === ' ') return ' ';
      return chars[Math.floor(Math.random() * chars.length)];
    }).join('');
    el.textContent = out;
    if (p < 1) requestAnimationFrame(tick);
    else el.textContent = final;
  }
  requestAnimationFrame(tick);
}
document.querySelectorAll('[data-scramble]').forEach(el => {
  new IntersectionObserver(([e], obs) => {
    if (e.isIntersecting) { scrambleText(el); obs.unobserve(el); }
  }, { threshold: 0.8 }).observe(el);
});
```

### Signature #7 — Flowing Hero Keyword Gradient

The gradient on the Hero words "**spec**" and "**code**" is not static; it flows slowly across the text surface on a 30-second cycle.

```css
.key-gradient {
  background: linear-gradient(135deg, var(--accent-cool), var(--accent-warm), var(--accent-cool));
  background-size: 300% 100%;
  -webkit-background-clip: text; background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: gradientSlide 18s linear infinite;
}
@keyframes gradientSlide { to { background-position: -300% 0; } }
```

### Signature #8 — Top Scroll-progress Bar

```css
.scroll-progress {
  position: fixed; top: 0; left: 0; right: 0; height: 2px;
  background: var(--gradient-key);
  transform-origin: 0 50%; transform: scaleX(0);
  z-index: 100;
}
```

### Reduced Motion

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
  .reveal { opacity: 1; animation: none; }
  .hero-bg, .cube, .marquee-track, .key-gradient { animation: none !important; }
  .stack-card { transform: none !important; }
  * { cursor: auto !important; }
  .cursor-dot, .cursor-ring { display: none !important; }
}
```

```js
// Apply the same fallback in JavaScript
if (matchMedia('(prefers-reduced-motion: reduce)').matches) {
  lenis.destroy?.();
  ScrollTrigger.getAll().forEach(t => t.kill());
}
```

---

## 8. Do's and Don'ts

### Do
- ✅ Keep the dark background and four-level text hierarchy, building information rhythm entirely through **weight, spacing, and hierarchy**
- ✅ Use cool cyan and warm orange **as a pair separated by whitespace**; never join them directly
- ✅ Limit keyword-gradient decoration to one instance per viewport and treat it as a "pointer," not "decoration"
- ✅ Give every section the trio of eyebrow label (uppercase, letter-spacing) + H2 + subheading
- ✅ Accent 2-3 keywords with **italic** Instrument Serif to create a "document-like" feel
- ✅ Use JetBrains Mono for code blocks and commands; never substitute plain body text
- ✅ On card hover, only "brighten the surface + strengthen the border + rise 2px"; do not change color
- ✅ L2 motion must include a `prefers-reduced-motion` fallback

### Don't
- ❌ Do not use neon colors, large saturated color fields, or glowing borders (this is dark **editorial** design, not cyberpunk)
- ❌ Do not stack more than four colors; cool cyan + warm orange + four grays are the complete system
- ❌ Do not apply gradients to body text, buttons, or borders (limit them to 1-2 keyword fills plus the top progress bar)
- ❌ Do not give cards heavy shadows (`box-shadow` is limited to primary CTA hover and the 3D card stack)
- ❌ Do not use emoji (they do not suit a methodology product)
- ❌ Use **pinning / custom cursor only in explicit signature areas**; do not enable full-page scroll-jacking or replace the cursor across the entire page (this page has one pin in Phase A→B→C; the global custom cursor uses mix-blend-mode to remain subtle)
- ❌ Do not set body copy in serif (use it only for keyword accents)
- ❌ Do not make Latin fonts carry Chinese text **on their own** (the fallback stack must include Noto Sans SC)
- ❌ Do not hard-code hex values; every color must use a CSS variable
- ❌ Do not decorate the install command box (it must look like a real command, not a decorative strip)
- ❌ Do not keep 3D effects in constant high-intensity motion (Cube: 24s/cycle; Stack: low-frequency breathing — the page must not feel restless)
- ❌ On low-performance devices (`matchMedia('(max-width: 640px)')` + `navigator.hardwareConcurrency < 4`), do not retain the 3D cube or complex pinning; fall back to static cards

---

## 9. Responsive Behavior

**Breakpoints**:

| Name | Width | Key Changes |
|------|-------|-------------|
| Desktop | ≥ 1024px | 3-4-column grid, two-line Hero heading, section padding 96-128px |
| Tablet | 640-1023px | 2-column grid, two-line Hero at one type-size step smaller, simplified collapsed navigation |
| Mobile | < 640px | Single column, Hero type at the clamp minimum, section padding 64-80px, vertical step summary |

**Touch Targets**: minimum 44×44px
**Collapsing Strategy**:
- Navigation: full desktop set → hide secondary links on tablet → collapse to a hamburger on mobile (or retain only the Logo + GitHub icon)
- Grid: 3 columns → 2 columns → 1 column
- Hero three-step summary: horizontal → vertical

```css
@media (max-width: 1023px) {
  :root { --sp-section: 80px; }
  .grid-3, .grid-4 { grid-template-columns: 1fr 1fr; }
  .nav .nav-link-secondary { display: none; }
}

@media (max-width: 639px) {
  :root { --sp-section: 64px; }
  .container { padding-left: 20px; padding-right: 20px; }
  .grid-3, .grid-4 { grid-template-columns: 1fr; }
  .steps { flex-direction: column; gap: 12px; }
  .hero-title { font-size: clamp(36px, 9vw, 48px); }
}
```

---

## Appendix: Difference Audit Against the Reference Site (hueapp.io)

| Dimension | hueapp.io | This page | Rationale |
|------|-----------|--------|------|
| Base color | #0d0e12 | #0a0b0e (darker) | Reinforces a "dark manual" rather than a "product page" |
| Accent colors | Cool blue #63b3ed + warm pink #ec6cb9 | Cool cyan #5EEAD4 + warm orange #FB923C | Avoids imitation and echoes the "specification × warmth" methodology |
| Typography | DM Sans only | DM Sans + Instrument Serif (decorative) | Establishes a recognizable "document-like" feel |
| Hero size | 56-72px | 48-84px | Creates more distance from hueapp and greater impact |
| Section structure | Hero → Proofs → System → Install | Hero → Why DESIGN.md → Three Inputs → A→B→C workflow → Assets → Demo → Install | A different story arc (methodology vs brand-to-UI) |
| Motion tier | L1-L2 | L2 | Substantially aligned |
| Distinctive element | Horizontal-scrolling showcase | Comparison graphic (AI codes directly vs spec first, then code) | The methodology needs comparative evidence, so the work showcase becomes secondary |
