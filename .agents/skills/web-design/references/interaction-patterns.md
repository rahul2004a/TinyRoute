# Interaction patterns

Choose the lightest pattern that communicates state or hierarchy. TinyRoute
defaults to restrained motion. Every pattern must preserve keyboard use, focus
visibility, readable content, and a reduced-motion path.

## Shared foundation

### Viewport reveal

Use `IntersectionObserver` for a small number of one-time reveals. Content must
remain available when JavaScript fails.

```ts
const observer = new IntersectionObserver(
  (entries) => {
    for (const entry of entries) {
      if (!entry.isIntersecting) continue;
      entry.target.setAttribute("data-visible", "true");
      observer.unobserve(entry.target);
    }
  },
  { threshold: 0.16 },
);

document.querySelectorAll<HTMLElement>("[data-reveal]").forEach((element) => {
  observer.observe(element);
});
```

```css
[data-reveal] {
  opacity: 0;
  transform: translateY(12px);
  transition: opacity 420ms ease, transform 420ms ease;
}

[data-reveal][data-visible="true"] {
  opacity: 1;
  transform: none;
}

@media (prefers-reduced-motion: reduce) {
  [data-reveal] {
    opacity: 1;
    transform: none;
    transition: none;
  }
}
```

### Focus and press feedback

```css
.action:focus-visible {
  outline: 2px solid var(--focus);
  outline-offset: 3px;
}

.action:active {
  transform: translateY(1px);
}

@media (prefers-reduced-motion: reduce) {
  .action:active {
    transform: none;
  }
}
```

## L1: restrained interaction

Use L1 for ordinary TinyRoute marketing work.

- Short opacity or 4-12px position entrances
- Color, border, and underline transitions
- Small press feedback on controls
- No continuous animation
- No behavior that delays reading or interaction

### Link underline

```css
.text-link {
  background: linear-gradient(currentColor, currentColor) 0 100% / 0 1px no-repeat;
  transition: background-size 180ms ease;
}

.text-link:hover,
.text-link:focus-visible {
  background-size: 100% 1px;
}
```

### Card emphasis

Prefer border and surface changes over large movement:

```css
.feature-card {
  transition: border-color 180ms ease, background-color 180ms ease;
}

.feature-card:hover,
.feature-card:focus-within {
  border-color: var(--border-strong);
  background: var(--surface-2);
}
```

## L2: guided motion

Use L2 only when the brief benefits from stronger narrative movement.

- Stagger a small group once; avoid animating every descendant.
- Keep scroll effects independent of critical navigation or reading.
- Use native browser APIs before adding a library.
- Pause off-screen work and disable pointer effects on touch devices.

### Staggered group

```css
.reveal-group[data-visible="true"] > * {
  animation: reveal-item 420ms both;
  animation-delay: calc(var(--item-index, 0) * 70ms);
}

@keyframes reveal-item {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: none; }
}
```

### Sticky navigation state

Toggle a data attribute after the page crosses a small sentinel. Do not run a
scroll handler on every frame.

```ts
const header = document.querySelector<HTMLElement>("[data-site-header]");
const sentinel = document.querySelector<HTMLElement>("[data-header-sentinel]");

if (header && sentinel) {
  new IntersectionObserver(([entry]) => {
    header.toggleAttribute("data-scrolled", !entry.isIntersecting);
  }).observe(sentinel);
}
```

### Pointer spotlight

Only enable this pattern for accurate pointing devices. Throttle updates with
`requestAnimationFrame`.

```ts
if (matchMedia("(hover: hover) and (pointer: fine)").matches) {
  let frame = 0;
  card.addEventListener("pointermove", (event) => {
    cancelAnimationFrame(frame);
    frame = requestAnimationFrame(() => {
      const bounds = card.getBoundingClientRect();
      card.style.setProperty("--pointer-x", `${event.clientX - bounds.left}px`);
      card.style.setProperty("--pointer-y", `${event.clientY - bounds.top}px`);
    });
  });
}
```

## L3: cinematic interaction

Use L3 only when the user explicitly requests a cinematic or experimental page.
Read [scroll-story-patterns.md](scroll-story-patterns.md) before choosing a
pattern.

- Isolate browser-only motion in small client components.
- Limit pinned sequences and avoid hijacking native scroll.
- Use at most one WebGL scene, pause it off-screen, and provide a static mobile
  and reduced-motion alternative.
- Load heavy code only where the effect appears.
- Measure layout, scripting, and paint cost on representative mobile hardware.

## Avoid

- Animating `filter: blur()` on many moving elements
- Permanent `will-change`
- State updates on every pointer or scroll event
- Replacing the system cursor for ordinary SaaS pages
- Motion that hides content until a script completes
- Simultaneous smooth-scroll, pinned timelines, and WebGL without a demonstrated
  need and performance budget
