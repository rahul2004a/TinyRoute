# Interaction Patterns

Three-tier interaction library. All code is directly reusable; combine patterns from the user's selected tier into the generated Style SKILL.

---

## Shared Foundations

### useInView Hook (Shared by L1/L2/L3)

```jsx
function useInView(options = {}) {
  const [ref, setRef] = React.useState(null);
  const [isInView, setIsInView] = React.useState(false);
  React.useEffect(() => {
    if (!ref) return;
    const obs = new IntersectionObserver(([e]) => {
      if (e.isIntersecting) { setIsInView(true); obs.unobserve(ref); }
    }, { threshold: 0.15, ...options });
    obs.observe(ref);
    return () => obs.disconnect();
  }, [ref]);
  return [setRef, isInView];
}
```

### Vanilla JavaScript Version (Without a Framework)

```js
function initScrollReveal(selector = '.reveal', cls = 'in-view') {
  const obs = new IntersectionObserver((entries) => {
    entries.forEach(e => { if (e.isIntersecting) { e.target.classList.add(cls); obs.unobserve(e.target); } });
  }, { threshold: 0.15 });
  document.querySelectorAll(selector).forEach(el => obs.observe(el));
}
document.addEventListener('DOMContentLoaded', () => initScrollReveal());
```

### Smooth-scroll Foundation

```css
html { scroll-behavior: smooth; }
[id] { scroll-margin-top: 80px; }
```

### Reduced-motion Fallback

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
```

---

## L1: Refined Static

> Elegant hover feedback plus gentle entrances. Information remains primary, with no distracting flourishes.

### Entrance: Fade In

```css
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.reveal {
  opacity: 0;
  transition: opacity 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.in-view { opacity: 1; }
```

### Entrance: Slight Rise

```css
.reveal {
  opacity: 0;
  transform: translateY(16px);
  transition: opacity 0.6s cubic-bezier(0.16, 1, 0.3, 1),
              transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.in-view {
  opacity: 1;
  transform: translateY(0);
}
```

### Hover: Slight Lift + Shadow

```css
.card {
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(0,0,0,0.06);
}
```

### Hover: Color Transition

```css
.btn {
  background: var(--color-primary);
  transition: background 0.2s ease, color 0.2s ease;
}
.btn:hover {
  background: var(--color-primary-hover);
}
```

### Hover: Sliding Underline

```css
.link {
  position: relative;
  text-decoration: none;
}
.link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: var(--color-primary);
  transition: width 0.3s ease;
}
.link:hover::after { width: 100%; }
```

### Focus Ring

```css
:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}
```

---

## L2: Fluid Interaction

> Scroll reveals, parallax, and navigation changes. The rhythm makes every section feel like a distinct scene.

### Entrance: fadeInUp + Stagger

```css
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(28px); }
  to { opacity: 1; transform: translateY(0); }
}
.reveal {
  opacity: 0;
  transform: translateY(28px);
  transition: opacity 0.7s cubic-bezier(0.16, 1, 0.3, 1),
              transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.in-view { opacity: 1; transform: translateY(0); }

/* Stagger: offset child-element entrances */
.reveal.in-view > *:nth-child(1) { transition-delay: 0s; }
.reveal.in-view > *:nth-child(2) { transition-delay: 0.1s; }
.reveal.in-view > *:nth-child(3) { transition-delay: 0.2s; }
.reveal.in-view > *:nth-child(4) { transition-delay: 0.3s; }
.reveal.in-view > *:nth-child(5) { transition-delay: 0.4s; }
```

Dynamic JavaScript stagger (supports any number of child elements):
```js
function initStaggerReveal(containerSelector = '.stagger-reveal') {
  const obs = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (!e.isIntersecting) return;
      const children = e.target.children;
      Array.from(children).forEach((child, i) => {
        child.style.transitionDelay = `${Math.min(i * 0.1, 0.6)}s`;
      });
      e.target.classList.add('in-view');
      obs.unobserve(e.target);
    });
  }, { threshold: 0.15 });
  document.querySelectorAll(containerSelector).forEach(el => obs.observe(el));
}
```

### Entrance: scaleIn

```css
.reveal-scale {
  opacity: 0;
  transform: scale(0.92);
  transition: opacity 0.5s cubic-bezier(0.34, 1.56, 0.64, 1),
              transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.reveal-scale.in-view { opacity: 1; transform: scale(1); }
```

### Parallax: CSS Scroll-driven Animation (Modern Browsers)

```css
@supports (animation-timeline: scroll()) {
  .parallax-bg {
    animation: parallaxShift linear both;
    animation-timeline: scroll();
  }
  @keyframes parallaxShift {
    from { transform: translateY(0); }
    to { transform: translateY(-80px); }
  }
}
```

### Parallax: Simple JavaScript Approach (Broad Compatibility)

```js
function initParallax(selector = '.parallax', speed = 0.3) {
  const els = document.querySelectorAll(selector);
  let ticking = false;
  window.addEventListener('scroll', () => {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(() => {
      const scrollY = window.scrollY;
      els.forEach(el => {
        const rect = el.getBoundingClientRect();
        const offset = (rect.top + scrollY - window.innerHeight / 2) * speed;
        el.style.transform = `translateY(${offset}px)`;
      });
      ticking = false;
    });
  }, { passive: true });
}
```

### Navigation: Transparent → Frosted Glass After Scrolling

```css
.nav {
  position: fixed;
  top: 0;
  width: 100%;
  z-index: 100;
  background: transparent;
  backdrop-filter: none;
  border-bottom: 1px solid transparent;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.nav.scrolled {
  background: rgba(var(--color-bg-rgb), 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom-color: var(--color-border);
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
```

```js
const nav = document.querySelector('.nav');
window.addEventListener('scroll', () => {
  nav.classList.toggle('scrolled', window.scrollY > 50);
}, { passive: true });
```

### Scroll-progress Bar

```css
.scroll-progress {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  background: var(--color-primary);
  z-index: 1000;
  transform-origin: left;
  transform: scaleX(0);
}
```

```js
const bar = document.querySelector('.scroll-progress');
window.addEventListener('scroll', () => {
  const pct = window.scrollY / (document.body.scrollHeight - window.innerHeight);
  bar.style.transform = `scaleX(${pct})`;
}, { passive: true });
```

### Hover: Card-image Zoom

```css
.img-card { overflow: hidden; border-radius: var(--radius); }
.img-card img {
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
  display: block;
  width: 100%;
}
.img-card:hover img { transform: scale(1.06); }
```

### Hover: Glowing Border (Dark Style)

```css
.glow-card {
  position: relative;
  transition: box-shadow 0.3s ease;
}
.glow-card:hover {
  box-shadow: 0 0 0 1px var(--color-primary),
              0 0 20px rgba(var(--color-primary-rgb), 0.15);
}
```

### Button-press Microinteraction

```css
.btn {
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(var(--color-primary-rgb), 0.25);
}
.btn:active {
  transform: translateY(0) scale(0.97);
  box-shadow: none;
}
```

### Numeric Counter

```js
function countUp(el, end, duration = 2000) {
  let start = 0;
  const step = (end / duration) * 16;
  const timer = setInterval(() => {
    start += step;
    if (start >= end) { el.textContent = end.toLocaleString(); clearInterval(timer); }
    else { el.textContent = Math.floor(start).toLocaleString(); }
  }, 16);
}

// Trigger with IntersectionObserver
function initCounters(selector = '.count-up') {
  const obs = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (!e.isIntersecting) return;
      const end = parseInt(e.target.dataset.end, 10);
      countUp(e.target, end);
      obs.unobserve(e.target);
    });
  }, { threshold: 0.5 });
  document.querySelectorAll(selector).forEach(el => obs.observe(el));
}
```

```html
<span class="count-up" data-end="1200">0</span>+
```

---

## L3: Immersive Experience

> Scroll-driven timelines, section pinning, cursor tracking, and page transitions. Confirm with the user before introducing dependencies such as GSAP.

### GSAP + ScrollTrigger Foundation

```html
<script src="https://cdn.jsdelivr.net/npm/gsap@3/dist/gsap.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/gsap@3/dist/ScrollTrigger.min.js"></script>
<script>
gsap.registerPlugin(ScrollTrigger);
</script>
```

### Section Pin (Fix the Region While Content Changes on Scroll)

```js
gsap.to('.pinned-content', {
  scrollTrigger: {
    trigger: '.pin-section',
    start: 'top top',
    end: '+=200%',       // Scroll distance for the pin
    pin: true,
    scrub: 1,            // Bind animation progress to scroll
  },
  opacity: 1,
  y: 0,
});
```

### Scroll-driven Timeline

```js
const tl = gsap.timeline({
  scrollTrigger: {
    trigger: '.timeline-section',
    start: 'top center',
    end: 'bottom center',
    scrub: true,
  }
});

tl.from('.step-1', { opacity: 0, y: 40 })
  .from('.step-2', { opacity: 0, y: 40 }, '+=0.1')
  .from('.step-3', { opacity: 0, y: 40 }, '+=0.1');
```

### Horizontal-scrolling Section

```js
const panels = gsap.utils.toArray('.h-panel');
gsap.to(panels, {
  xPercent: -100 * (panels.length - 1),
  ease: 'none',
  scrollTrigger: {
    trigger: '.h-scroll-container',
    pin: true,
    scrub: 1,
    end: () => `+=${document.querySelector('.h-scroll-container').scrollWidth}`,
  },
});
```

```css
.h-scroll-container {
  display: flex;
  width: fit-content;
  flex-wrap: nowrap;
}
.h-panel {
  width: 100vw;
  height: 100vh;
  flex-shrink: 0;
}
```

### Cursor Tracking (Magnetic Effect)

```js
const cursor = document.querySelector('.custom-cursor');
document.addEventListener('mousemove', (e) => {
  cursor.style.transform = `translate(${e.clientX}px, ${e.clientY}px)`;
});

// Magnetic button
document.querySelectorAll('.magnetic').forEach(btn => {
  btn.addEventListener('mousemove', (e) => {
    const rect = btn.getBoundingClientRect();
    const x = e.clientX - rect.left - rect.width / 2;
    const y = e.clientY - rect.top - rect.height / 2;
    btn.style.transform = `translate(${x * 0.3}px, ${y * 0.3}px)`;
  });
  btn.addEventListener('mouseleave', () => {
    btn.style.transform = '';
  });
});
```

```css
.custom-cursor {
  position: fixed;
  top: -16px;
  left: -16px;
  width: 32px;
  height: 32px;
  border: 2px solid var(--color-primary);
  border-radius: 50%;
  pointer-events: none;
  z-index: 9999;
  transition: width 0.2s, height 0.2s, border-color 0.2s;
  mix-blend-mode: difference;
}
```

### Cursor Glow (Dark Style)

```css
.cursor-glow {
  position: fixed;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(var(--color-primary-rgb), 0.08) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
  transform: translate(-50%, -50%);
  transition: opacity 0.3s;
}
```

```js
const glow = document.querySelector('.cursor-glow');
document.addEventListener('mousemove', (e) => {
  glow.style.left = e.clientX + 'px';
  glow.style.top = e.clientY + 'px';
});
```

### Word-by-word Text Reveal

```js
function initTextReveal(selector = '.text-reveal') {
  document.querySelectorAll(selector).forEach(el => {
    const text = el.textContent;
    // Split Chinese text by character and English text by spaces
    const isChinese = /[\u4e00-\u9fa5]/.test(text);
    const parts = isChinese ? text.split('') : text.split(' ');
    el.innerHTML = parts.map((p, i) =>
      `<span style="display:inline-block;opacity:0;transform:translateY(100%);transition:all 0.5s cubic-bezier(0.16,1,0.3,1) ${i * (isChinese ? 40 : 80)}ms">${p}${isChinese ? '' : '&nbsp;'}</span>`
    ).join('');
    el.style.overflow = 'hidden';
  });
}

// Trigger with IntersectionObserver
function triggerTextReveal(selector = '.text-reveal') {
  const obs = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (!e.isIntersecting) return;
      e.target.querySelectorAll('span').forEach(s => {
        s.style.opacity = '1';
        s.style.transform = 'translateY(0)';
      });
      obs.unobserve(e.target);
    });
  }, { threshold: 0.3 });
  document.querySelectorAll(selector).forEach(el => obs.observe(el));
}
```

### Page Transition

```css
.page-transition {
  animation: pageEnter 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes pageEnter {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Exit (use with route changes) */
.page-exit {
  animation: pageExit 0.25s ease-in both;
}
@keyframes pageExit {
  to { opacity: 0; transform: translateY(-8px); }
}
```

### 3D Perspective Card

```css
.perspective-card {
  perspective: 1000px;
}
.perspective-card-inner {
  transition: transform 0.4s ease;
  transform-style: preserve-3d;
}
```

```js
document.querySelectorAll('.perspective-card').forEach(card => {
  const inner = card.querySelector('.perspective-card-inner');
  card.addEventListener('mousemove', (e) => {
    const rect = card.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;
    inner.style.transform = `rotateY(${x * 12}deg) rotateX(${-y * 12}deg)`;
  });
  card.addEventListener('mouseleave', () => {
    inner.style.transform = '';
  });
});
```

### Lenis Smooth Scrolling (Optional Enhancement)

```html
<script src="https://cdn.jsdelivr.net/npm/lenis@1/dist/lenis.min.js"></script>
<script>
const lenis = new Lenis({ lerp: 0.1, smoothWheel: true });
function raf(time) { lenis.raf(time); requestAnimationFrame(raf); }
requestAnimationFrame(raf);

// Synchronize with GSAP ScrollTrigger
lenis.on('scroll', ScrollTrigger.update);
gsap.ticker.add((time) => lenis.raf(time * 1000));
gsap.ticker.lagSmoothing(0);
</script>
```

---

## Decorative Effects (Select by Style)

### Flowing Gradient Background

```css
@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
.gradient-bg {
  background: linear-gradient(-45deg, var(--color-primary), var(--color-secondary), var(--color-accent));
  background-size: 400% 400%;
  animation: gradientShift 12s ease infinite;
}
```

### Glow Pulse (Dark Style)

```css
@keyframes glowPulse {
  0%, 100% { box-shadow: 0 0 20px rgba(var(--color-primary-rgb), 0.2); }
  50% { box-shadow: 0 0 40px rgba(var(--color-primary-rgb), 0.4); }
}
.glow { animation: glowPulse 3s ease-in-out infinite; }
```

### Floating Blob (Playful Background)

```css
@keyframes blobMorph {
  0%, 100% { border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%; }
  50% { border-radius: 50% 60% 30% 60% / 30% 60% 70% 40%; }
}
.blob {
  width: 300px;
  height: 300px;
  background: var(--color-primary);
  opacity: 0.08;
  filter: blur(60px);
  animation: blobMorph 8s ease-in-out infinite;
  position: absolute;
  z-index: 0;
  pointer-events: none;
}
```

### Typewriter Effect

```css
@keyframes typing { from { width: 0; } to { width: 100%; } }
@keyframes blink { 50% { border-color: transparent; } }
.typewriter {
  overflow: hidden;
  white-space: nowrap;
  border-right: 3px solid var(--color-primary);
  width: fit-content;
  animation: typing 3s steps(30) 1s both, blink 0.7s step-end infinite;
}
```
