# DESIGN.md revision checklist

Use this reference only when the user explicitly requests a new design system or
a material revision to TinyRoute's existing `DESIGN.md`. Preserve the current
file format unless the task explicitly requires a migration.

## 1. Visual theme and atmosphere

- Product purpose and target audience
- Three to five concrete atmosphere words
- One sentence describing the intended visual impression
- Clear boundaries against copied brand identity or decorative excess

## 2. Color palette and roles

Define semantic roles rather than page-specific colors:

```yaml
colors:
  primary: "#..."
  on-primary: "#..."
  canvas: "#..."
  surface-1: "#..."
  surface-2: "#..."
  border: "#..."
  ink: "#..."
  ink-muted: "#..."
  focus: "#..."
  success: "#..."
  warning: "#..."
  danger: "#..."
```

Document contrast expectations and light/dark theme relationships. Components
must consume semantic tokens instead of inventing local palette values.

## 3. Typography rules

- Font families and `next/font` loading strategy
- Display, title, body, label, caption, and monospace roles
- Size, weight, line height, and letter spacing for each role
- Fallback stacks and language-specific coverage when needed
- Maximum readable line length for prose

Do not add remote `@import` rules when the project uses `next/font`.

## 4. Component styling

Specify only components the product uses. Cover default, hover, active, focus,
disabled, loading, error, empty, and selected states where applicable.

Typical components include buttons, links, inputs, navigation, panels, dialogs,
status badges, link rows, metric cards, and charts.

## 5. Layout principles

- Container widths and page gutters
- Grid behavior and spacing scale
- Section rhythm and content density
- Sidebar, header, and content relationships
- Mobile reflow rules rather than desktop shrinking

## 6. Depth and elevation

Define border, shadow, overlay, and stacking roles. Use elevation only when it
communicates hierarchy. Keep the radius system consistent.

## 7. Animation and interaction

- Purpose and intensity of motion
- Duration and easing tokens
- Hover, focus, press, entrance, and state-transition behavior
- `prefers-reduced-motion` alternatives
- Performance limits and mobile degradation

Do not require animation libraries or spectacle. TinyRoute defaults to restrained
motion and native CSS where practical.

## 8. Do and do not

Record concrete project safeguards, including accessibility, palette discipline,
component consistency, data clarity, and prohibited decorative patterns.

## 9. Responsive behavior

- Supported viewport ranges and content breakpoints
- Navigation and sidebar collapse behavior
- Table, chart, and card adaptations
- Touch target minimums
- Typography scaling and overflow prevention

## Completion criteria

- The document contains actionable rules rather than placeholders.
- Tokens map cleanly to the existing implementation.
- All interactive states are covered.
- Mobile, keyboard, contrast, and reduced-motion behavior are explicit.
- The revision does not redefine application behavior owned by requirements or
  backend architecture.
