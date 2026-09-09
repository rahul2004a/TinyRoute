# Style Seeds

Keyword-to-initial-token mappings. Use them in Round 2 to quickly match the user's style direction.

Each seed provides a palette, font pairing, radius/shadow baseline, and recommended motion tier. Refine it into complete tokens after user confirmation.

---

## 1. Cream Editorial

**Keywords**: warm, editorial, magazine, restrained, tactile paper
**Feel**: A beautifully typeset magazine unfolding on the screen

```
Palette:
  Background #ECE7DE (warm cream)    Surface #FFFFFF
  Border #D5D0C7 (warm gray)         Strong border #2A2A2A
  Text #1A1A1A / #6B6560             Accent #E8682A (orange)
Fonts:
  heading: Playfair Display (serif, 900)
  body: DM Sans (sans, 400-700)
  mono: JetBrains Mono
Radius: 16px    Shadow: hover only    Tier: L1
```

---

## 2. Dark Tech

**Keywords**: deep, neon, futuristic, technological, cyber
**Feel**: A deep-space station console, with information glowing in the dark

```
Palette:
  Background #0B0B0F        Surface rgba(255,255,255,0.03)
  Border rgba(255,255,255,0.08)
  Text #F0F0F0 / #8B8B8B   Primary #00D4FF    Secondary #8B5CF6
Fonts:
  heading: Space Grotesk (sans, 700)
  body: Inter (sans, 400-600)
  mono: Fira Code
Radius: 12px    Shadow: glow    Tier: L2-L3
Special: Glassmorphism, animated gradient background
```

---

## 3. Minimal Pure

**Keywords**: clean, whitespace, precise, quiet, refined
**Feel**: A single line of text on a white gallery wall

```
Palette:
  Background #FAFAFA        Surface #FFFFFF
  Border #E8E8E8
  Text #1A1A1A / #666666   Accent #0066FF (links only)
Fonts:
  heading: Instrument Serif (serif, 400)
  body: DM Sans (sans, 400-500)
Radius: 8px    Shadow: extremely light    Tier: L1
Special: Establish hierarchy through large type-scale contrast; prohibit decoration
```

---

## 4. Warm Professional

**Keywords**: professional, trustworthy, rounded, friendly, mature
**Feel**: A company that immediately inspires confidence

```
Palette:
  Background #FFFFFF        Surface #F8FAFC
  Border #E2E8F0
  Text #1E293B / #475569   Primary #2563EB    Secondary #F59E0B
Fonts:
  heading: Plus Jakarta Sans (sans, 700-800)
  body: Plus Jakarta Sans (sans, 400-500)
Radius: 12px    Shadow: soft and layered    Tier: L1-L2
```

---

## 5. Playful Creative

**Keywords**: bold, fun, youthful, energetic, handwritten
**Feel**: A birthday-party invitation from a designer friend

```
Palette:
  Background #FFF8F0        Surface #FFFFFF
  Border #FFE0CC
  Text #2D2D2D / #666666   Primary #FF3366    Secondary #FFD700    Tertiary #00CC88
Fonts:
  heading: Sora (sans, 700-800)
  body: Nunito (sans, 400-600)
  accent: Caveat (cursive, handwritten decoration)
Radius: 16-24px (large)    Shadow: colored    Tier: L2-L3
Special: Blob decoration, handwritten annotations, springy animation
```

---

## 6. Chinese Elegant

**Keywords**: ink and letters, Eastern, understated, literary, whitespace
**Feel**: A letter written on xuan paper

```
Palette:
  Background #FAF8F5        Surface #FFFFFF
  Border #E8E0D8
  Text #2C2C2C / #5C5C5C   Primary #C45C3C (ochre)    Secondary #2C5F6E
Fonts:
  heading: Noto Serif SC (Song-style serif, 700)
  body: Noto Sans SC (Hei-style sans serif, 400-500)
  accent: LXGW WenKai (Kai-style script, decorative)
Radius: 4px (very small)    Shadow: extremely light    Tier: L1
Special: Line-height 1.8+, letter-spacing 0.02em, 800px container, first-line indent 2em
```

---

## 7. Cyberpunk

**Keywords**: glitch, grid, glaring, underground, data stream
**Feel**: Neon data racing through a hacker terminal

```
Palette:
  Background #0A0A0A        Surface #111111
  Border #222222
  Text #00FF41 / #888888   Primary #FF0080    Secondary #00FFFF
Fonts:
  heading: Orbitron (sans, 700-900)
  body: IBM Plex Mono (mono, 400)
Radius: 0px (square corners)    Shadow: neon glow    Tier: L3
Special: Glitch effects, scanlines, data-stream animation, typewriter effect
```

---

## 8. Organic Natural

**Keywords**: earthy, handmade, gentle, breathable, sustainable
**Feel**: Handmade soap packaging at a country market

```
Palette:
  Background #F5F0EB        Surface #FEFCF9
  Border #DDD5CA
  Text #3D3228 / #7A6E60   Primary #5B8C5A (moss green)    Secondary #C4956A (terracotta)
Fonts:
  heading: Fraunces (serif, 600-700, optical size)
  body: Source Sans 3 (sans, 400-500)
Radius: 20px+ (very rounded)    Shadow: extremely soft and warm    Tier: L1-L2
Special: Hand-drawn texture backgrounds, irregular edges, soft gradients
```

---

## 9. Swiss Grid

**Keywords**: grid, ordered, rational, black and white, red
**Feel**: A poster in the corridor of the Bauhaus school

```
Palette:
  Background #FFFFFF        Surface #F5F5F5
  Border #000000 (solid)
  Text #000000 / #555555   Accent #FF0000 (sparingly)
Fonts:
  heading: Helvetica Neue / Inter (sans, 700)
  body: Helvetica Neue / Inter (sans, 400)
Radius: 0px    Shadow: none    Tier: L1
Special: Strict grid, bold rules, extensive negative space, asymmetric composition
```

---

## 10. Glassmorphism

**Keywords**: transparent, blurred, light and shadow, layered, dreamlike
**Feel**: Sunlight passing through frosted glass onto a desk

```
Palette:
  Background linear gradient #667eea → #764ba2
  Surface rgba(255,255,255,0.15)
  Border rgba(255,255,255,0.2)
  Text #FFFFFF / rgba(255,255,255,0.7)
Fonts:
  heading: Outfit (sans, 600-700)
  body: Inter (sans, 400-500)
Radius: 16px    Shadow: broad soft glow    Tier: L2
Special: backdrop-filter: blur(12px), translucent layers, animated light effects
```

---

## Seed-mixing Rules

Users often will not match a single seed exactly. Common combinations include:

- "Dark but restrained" → Dark Tech palette + Minimal Pure motion tier and decoration rules
- "Warm but fun" → Warm Professional colors + Playful Creative radius and animation
- "Chinese + technological" → Chinese Elegant fonts + Dark Tech palette
- "Swiss but modern" → Swiss Grid layout and black-and-white palette + larger radii + L2 motion

When mixing seeds, take the **palette and fonts** from one seed and the **motion and decoration rules** from another. Do not combine two palettes.
