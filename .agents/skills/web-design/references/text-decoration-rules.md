# Text-decoration Decision Rules

Before generating code, apply the following checks to each text level to decide whether to add decorative styling.

## Gradient Text

**Trigger conditions** (all must be true):
- Style == "Dark Tech" or "Playful Creative"
- Font size >= 60px, or the element is the primary Hero h1

**Implementation**:
```css
background: linear-gradient(135deg, [primary-color], [accent-color]);
-webkit-background-clip: text;
-webkit-text-fill-color: transparent;
background-clip: text;
```

**Prohibited**:
- Restrained Minimal style (undermines the restraint)
- White background with font size < 40px (poor readability)
- Body paragraphs (`p`); headings only

## Text Shadow

**A. Dark background (background luminance < 30%) and font size >= 80px** → subtle glow:
```css
text-shadow: 0 0 40px rgba([primary-color-RGB], 0.4);
```

**B. Playful Creative style plus primary Hero heading** → layered drop shadow:
```css
text-shadow: 3px 3px 0 [accent-color], 6px 6px 0 rgba(0,0,0,0.15);
```

**C. Warm Professional style plus serif heading and dark text** → soft shadow:
```css
text-shadow: 0 2px 8px rgba(0,0,0,0.12);
```

**Prohibited**:
- Restrained Minimal style
- Do not combine with gradient text (visual overload)
- Body paragraphs (`p`)

## Decorative Underlines / Highlights

- Section eyebrow (11-13px, letter-spacing > 3px) → `border-bottom: 2px solid [primary-color]` or a background highlight
- Link hover → underline offset or color transition; do not add text-shadow

## Decision Table (Quick Reference)

| Context | Restrained Minimal | Dark Tech | Warm Professional | Playful Creative |
|------|---------|---------|---------|---------|
| Hero h1 gradient | -- | Yes | -- | Yes |
| Hero h1 shadow | -- | glow | soft | layered |
| Section h2 gradient | -- | Optional | -- | Yes |
| Section h2 shadow | -- | -- | soft | Optional |
| Any decoration on body `p` | -- | -- | -- | -- |
