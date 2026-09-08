# Scroll-story patterns

Use this reference only for an explicitly requested cinematic or experimental
marketing page. These patterns are optional alternatives, not requirements.
TinyRoute's root `DESIGN.md`, accessibility rules, and performance constraints
remain authoritative.

## Choose one narrative device

Prefer one strong device over several competing effects:

1. A card constellation that settles into a clear hero composition
2. A short card-collapse transition between two related sections
3. A left-pinned explanation with a right-side scene swap
4. A single WebGL product or data metaphor
5. A large section-title bloom used as a transition

Every device needs a static document-flow version for small screens and reduced
motion.

## Pattern 1: card constellation hero

Use when a set of product artifacts communicates value better than one image.
Place each card in the DOM in its final reading order; transforms should only
decorate that structure.

```css
.constellation-card {
  opacity: 0;
  transform: translate3d(var(--start-x), var(--start-y), 0) rotate(var(--start-r));
  animation: settle-card 700ms cubic-bezier(.2, .8, .2, 1) forwards;
  animation-delay: calc(var(--index) * 70ms);
}

@keyframes settle-card {
  to { opacity: 1; transform: translate3d(0, 0, 0) rotate(0); }
}

@media (prefers-reduced-motion: reduce), (max-width: 640px) {
  .constellation-card {
    opacity: 1;
    transform: none;
    animation: none;
  }
}
```

Keep card count low, avoid moving blur, and end the animation completely after
the entrance.

## Pattern 2: card-collapse transition

Use when the same artifacts from the hero become evidence in the next section.
Animate only transform and opacity. The transition must not block scrolling or
hide the destination section.

```ts
const progress = Math.min(1, Math.max(0, rawProgress));
element.style.transform = `translate3d(${x * progress}px, ${y * progress}px, 0)`;
element.style.opacity = String(1 - progress * 0.6);
```

Drive continuous values with an animation library or motion value already in the
project; do not update React state on every scroll event.

## Pattern 3: left pin and right swap

Use for a short, ordered product story with two to four steps. Keep the text in
normal source order and make every scene reachable without JavaScript.

```css
.story-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
  gap: var(--space-xl);
}

.story-copy {
  position: sticky;
  top: var(--header-offset);
  align-self: start;
}

@media (max-width: 800px), (prefers-reduced-motion: reduce) {
  .story-layout { grid-template-columns: 1fr; }
  .story-copy { position: static; }
}
```

Use `IntersectionObserver` to select the active scene. Do not trap wheel or touch
events.

## Pattern 4: one WebGL signature scene

Use WebGL only when a 3D metaphor adds meaning that static media cannot provide.
Load the scene lazily and pause its render loop outside the viewport.

```ts
let running = false;

const visibility = new IntersectionObserver(([entry]) => {
  running = entry.isIntersecting && !reducedMotion.matches;
  if (running) requestAnimationFrame(render);
});

function render() {
  if (!running) return;
  renderer.render(scene, camera);
  requestAnimationFrame(render);
}
```

Provide a static image or CSS surface when WebGL is unavailable, on small
screens, or under reduced motion. Dispose textures, buffers, observers, and event
listeners when the component unmounts.

## Pattern 5: section-title bloom

A large low-contrast title can bridge sections without a library. Keep it
decorative and hidden from assistive technology when the same heading already
exists semantically.

```css
.section-ghost {
  color: color-mix(in srgb, var(--ink) 8%, transparent);
  font-size: clamp(4rem, 16vw, 13rem);
  line-height: .8;
  pointer-events: none;
}
```

## Implementation limits

- Do not combine scroll-jacking with native page navigation.
- Use at most two pinned regions and one WebGL scene.
- Avoid animated `filter: blur()` and permanent `will-change`.
- Throttle pointer work with `requestAnimationFrame`.
- Stop all loops when hidden or off-screen.
- Test keyboard reading order, touch scrolling, resize behavior, and back/forward
  navigation.
- Measure on representative mobile hardware, not only a desktop simulator.

## Reduced-motion document flow

The reduced-motion version should be a complete page, not a frozen frame:

- Render all content in source order.
- Remove pinning and continuous transforms.
- Replace 3D or canvas scenes with approved static media.
- Keep state changes immediate and focus movement predictable.
