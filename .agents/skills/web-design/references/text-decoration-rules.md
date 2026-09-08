# Text-decoration decision guide

Text decoration is optional. TinyRoute's root `DESIGN.md` currently favors clear,
unembellished typography and prohibits decorative gradients unless explicitly
revised.

## Gradient text

Consider gradient text only when all of these are true:

- The user explicitly requests a more expressive campaign treatment.
- The approved design system permits gradients.
- The element is a large display heading, not body text.
- Contrast remains sufficient throughout the gradient.

```css
.display-accent {
  background: linear-gradient(135deg, var(--accent-start), var(--accent-end));
  background-clip: text;
  color: transparent;
}
```

Do not combine gradient text with a glow or heavy shadow.

## Text shadow

Use text shadow only to solve a concrete contrast or material problem. Prefer a
subtle shadow on display text over layered novelty effects.

```css
.display-on-media {
  text-shadow: 0 2px 16px rgb(0 0 0 / 35%);
}
```

Do not add text shadows to body copy, controls, table content, or analytics.

## Underlines and highlights

- Use underline offset and thickness to make links recognizable.
- Use a border or background highlight for a short label only when it belongs to
  the approved component language.
- Preserve focus indicators; a hover underline is not a focus ring.

## Quick decision table

| Context | Gradient | Shadow | Underline/highlight |
|---|---:|---:|---:|
| TinyRoute product UI | No | No | Functional links only |
| Standard marketing page | No by default | Rare | Yes, when semantic |
| Expressive campaign | Explicit approval | Optional | Optional |
| Body text | No | No | Links only |
