# Scroll-Story Patterns

Library of cinematic L3 scroll-story patterns. Reference benchmarks: doubao.com/about, apple.com/vision-pro, stripe.com, tome.app, readme.com, and igloo.inc.

**Trigger conditions**: Use this tier whenever any condition below is met; do not fall back to an "L2 reveal":
- The user explicitly requests "scroll effects like doubao / apple / stripe"
- The user says the result feels "ordinary" or "monotonous," or asks for "scroll-driven motion," "3D," or a "cinematic feel"
- The page presents a methodology, brand, or product manifesto rather than pure information

---

## L3 Hard Requirements (In Addition to the Six Core Motion Categories)

Every L3 page must include at least **three** of these four scroll-story patterns:

| Pattern | Required count |
|------|----------|
| Pin-Scrub scene (fixed section + scroll-driven content transformation) | ≥ 1 |
| Container-replacement narrative (left pin / right swap) | ≥ 1 |
| Convergence/divergence transition (multiple elements fly together or apart) | ≥ 1 |
| WebGL / true 3D signature moment (not CSS 3D alone) | ≥ 1 |

Shared principle: **Include a signature moment every 1-2 viewports.** More than three viewports of monotonous scrolling is not permitted.

---

## Pattern 1 — Card Constellation Hero

**Source**: First viewport of doubao.com/about
**Effect**: 10-15 content sample cards, each representing one instance of a product capability, float around a central message in 3D space with varying Z-depths, rotations, and dynamic blur. Pointer movement drives group parallax.

### Implementation Template

```css
.constellation {
  position: relative;
  perspective: 1400px;
  transform-style: preserve-3d;
  height: 100vh;
}
.star-card {
  position: absolute;
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px;
  transform-style: preserve-3d;
  will-change: transform, filter;
  transition: filter 0.5s var(--ease-cinema);
  box-shadow: 0 20px 50px rgba(0,0,0,0.3);
}
/* Give each card an independent position through inline styles or data attributes:
   --x / --y / --z / --rx / --ry / --rz / --blur
*/
.star-card {
  top: calc(50% + var(--y) * 1px);
  left: calc(50% + var(--x) * 1px);
  transform:
    translate3d(-50%, -50%, var(--z, 0px))
    rotateX(var(--rx, 0deg))
    rotateY(var(--ry, 0deg))
    rotateZ(var(--rz, 0deg));
  filter: blur(var(--blur, 0px));
  opacity: calc(1 - var(--blur, 0) * 0.08);
}
```

```js
// Distribute 12 cards randomly (or according to the design)
const cards = document.querySelectorAll('.star-card');

// 1. Basic floating (independent idle breathing for each card)
cards.forEach((c, i) => {
  gsap.to(c, {
    y: `+=${gsap.utils.random(-16, 16)}`,
    rotate: `+=${gsap.utils.random(-4, 4)}`,
    duration: gsap.utils.random(4, 7),
    ease: 'sine.inOut',
    yoyo: true, repeat: -1,
  });
});

// 2. Pointer parallax (the entire constellation follows)
const stage = document.querySelector('.constellation');
stage.addEventListener('pointermove', (e) => {
  const cx = window.innerWidth / 2, cy = window.innerHeight / 2;
  const dx = (e.clientX - cx) / cx;
  const dy = (e.clientY - cy) / cy;
  cards.forEach((c, i) => {
    const depth = parseFloat(c.style.getPropertyValue('--z') || 0) / 200;
    gsap.to(c, { x: dx * 20 * depth, y: dy * 20 * depth, duration: 0.8, ease: 'power3.out' });
  });
});
```

### Design Guidelines
- Z-axis distribution: foreground (z: 0 to 100) sharp with no blur; middle ground (z: -200 to -400) with 2-4px slight blur; background (z: -500 to -700) with 6-10px blur
- Rotation: |rx, ry, rz| ≤ 15°; beyond that the cards resemble scraps of paper
- Vary card sizes from 180×220 to 320×400 to establish visual rhythm
- Card content must be meaningful (product samples or real screenshots); avoid placeholders

---

## Pattern 2 — Card Collapse Transition

**Source**: The transition from the Hero to "Writing Capability" on doubao.com/about
**Effect**: Once scrolling reaches a threshold, scattered Hero cards fly toward the center and merge into one container for the next section.

### Implementation Template

```js
gsap.timeline({
  scrollTrigger: {
    trigger: '.constellation',
    start: 'bottom 80%',
    end: 'bottom top',
    scrub: 1,
  },
})
.to('.star-card', {
  x: 0, y: 0, z: 0, rotateX: 0, rotateY: 0, rotateZ: 0,
  filter: 'blur(0px)',
  scale: 0,
  opacity: 0,
  stagger: { amount: 0.6, from: 'random' },
  ease: 'power2.in',
})
.from('.next-section .hero-panel', {
  scale: 0.7, opacity: 0, duration: 1, ease: 'power3.out',
}, '-=0.4');
```

### Design Guidelines
- The convergence endpoint should be the key container in the next section; cards must not disappear arbitrarily
- Use `from: 'random'` for the stagger (more natural and avoids a uniform wave)
- The convergence must span enough distance (80-100vh of scroll) to create a "wind-swept" feeling

---

## Pattern 3 — Left Pin / Right Swap Narrative

**Source**: doubao's four product-capability scenes (writing/drawing/knowledge/answers) and the multi-scene treatment on igloo.inc
**Effect**: Pin the section so the left heading + description + CTA remain fixed while the right container switches among 3-4 scenes (product screenshots / animated demos / 3D models) based on scroll progress.

### Implementation Template

```html
<section class="pin-swap" data-scenes="4">
  <div class="pin-swap-inner">
    <div class="left">
      <h2 class="pin-title">Heading 1</h2>
      <p class="pin-body">Description 1</p>
      <a class="pin-cta">Try it now</a>
    </div>
    <div class="right">
      <div class="scene scene-1">Scene 1 content</div>
      <div class="scene scene-2">Scene 2 content</div>
      <div class="scene scene-3">Scene 3 content</div>
      <div class="scene scene-4">Scene 4 content</div>
    </div>
  </div>
</section>
```

```css
.pin-swap { height: 400vh; /* 100vh per scene */ }
.pin-swap-inner { position: sticky; top: 0; height: 100vh; display: grid; grid-template-columns: 1fr 1.2fr; gap: 48px; align-items: center; }
.scene { position: absolute; inset: 0; opacity: 0; }
.scene:first-child { opacity: 1; }
```

```js
const scenes = gsap.utils.toArray('.scene');
const titles = [/* Array of copy objects for each scene */];

ScrollTrigger.create({
  trigger: '.pin-swap',
  start: 'top top',
  end: 'bottom bottom',
  scrub: 0.5,
  onUpdate: (self) => {
    const progress = self.progress;
    const idx = Math.min(Math.floor(progress * scenes.length), scenes.length - 1);

    // Switch scenes (crossfade)
    scenes.forEach((s, i) => {
      gsap.to(s, { opacity: i === idx ? 1 : 0, duration: 0.4, ease: 'power2.out' });
    });

    // Switch the copy on the left
    document.querySelector('.pin-title').textContent = titles[idx].title;
    document.querySelector('.pin-body').textContent = titles[idx].body;
  },
});
```

### Design Guidelines
- Scene-switch points should create a sense of pause; they must not feel as abrupt as flipping pages
- Scenes on the right must differ substantially (different products or contexts); do not change only the text
- The CTA on the left may remain constant or change by scene (for example, to target a different link)

---

## Pattern 4 — WebGL 3D Signature Moment

**Source**: doubao's Mobius form / Apple Vision Pro headset / igloo.inc's 3D icons
**Effect**: Center a 3D object (glass material, metallic surface, or shader) in the section. Rotate or deform it with scroll, accompanied by orbital particles and a glow.

### Implementation Template (Minimal Viable Three.js)

```html
<section class="webgl-scene">
  <canvas class="webgl-canvas"></canvas>
  <h2 class="webgl-title">Section heading</h2>
</section>
```

```js
import * as THREE from 'https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.js';

const canvas = document.querySelector('.webgl-canvas');
const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.setSize(canvas.clientWidth, canvas.clientHeight);

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(45, canvas.clientWidth / canvas.clientHeight, 0.1, 100);
camera.position.z = 5;

// Glass/iridescent material
const geometry = new THREE.TorusKnotGeometry(1, 0.3, 180, 32);
const material = new THREE.MeshPhysicalMaterial({
  transmission: 0.92,
  thickness: 1.5,
  roughness: 0.15,
  iridescence: 1,
  iridescenceIOR: 1.3,
  clearcoat: 1,
  color: 0xffffff,
});
const mesh = new THREE.Mesh(geometry, material);
scene.add(mesh);

// Ambient light + tinted point lights
scene.add(new THREE.HemisphereLight(0xffffff, 0x202020, 1));
const light1 = new THREE.PointLight(0x5EEAD4, 4, 20); light1.position.set(3, 3, 3); scene.add(light1);
const light2 = new THREE.PointLight(0xFB923C, 4, 20); light2.position.set(-3, -2, 3); scene.add(light2);

// ScrollTrigger-driven rotation
gsap.to(mesh.rotation, {
  y: Math.PI * 2, x: Math.PI,
  scrollTrigger: {
    trigger: '.webgl-scene',
    start: 'top bottom', end: 'bottom top',
    scrub: 1,
  },
});

// Render
function tick() {
  mesh.rotation.z += 0.002; // Subtle baseline self-rotation
  renderer.render(scene, camera);
  requestAnimationFrame(tick);
}
tick();

// Resize
new ResizeObserver(() => {
  renderer.setSize(canvas.clientWidth, canvas.clientHeight);
  camera.aspect = canvas.clientWidth / canvas.clientHeight;
  camera.updateProjectionMatrix();
}).observe(canvas);
```

### Design Guidelines
- **Use no more than one WebGL scene per page** (more than one overwhelms the GPU, especially on mobile)
- Recommended geometry: TorusKnot / Möbius strip / shader sphere / BoxGeometry, depending on the style
- Material: `transmission + iridescence` on `MeshPhysicalMaterial` matches doubao's recipe
- Use a camera FOV of 40-55° and place the object 3-5 units from the camera (too close causes fisheye distortion)
- Particles/glow: add 50-200 small spheres (sprites) around the object to create a "dust" effect
- Mobile fallback: detect `navigator.hardwareConcurrency < 4` or `matchMedia('(max-width: 640px)')` and replace the scene directly with a large static image

### Dependency Size
- Three.js core: ~150KB gzipped
- OGL (lightweight alternative): ~15KB, but requires custom shaders
- Prefer Three.js because its community materials and loaders are mature

---

## Pattern 5 — Section Title Bloom / Ghost

**Source**: doubao's large "Research Areas" heading
**Effect**: Place a blurred, offset, low-opacity duplicate behind a large section heading to create visual depth.

```css
.ghost-title {
  position: relative;
  font-size: clamp(56px, 8vw, 120px);
  font-weight: 900;
  color: var(--text-1);
}
.ghost-title::before {
  content: attr(data-ghost);
  position: absolute; left: 4px; top: 4px;
  color: var(--accent-warm);
  opacity: 0.3;
  filter: blur(8px);
  z-index: -1;
}
```

---

## Pattern 6 — Abstract Gradient Art Top

**Source**: doubao's three Research Areas cards (speech / large language models / multimodal)
**Effect**: The upper half of a feature card is abstract art (gradient orb / ribbon / particles), and the lower half is text. The artwork may be a static PNG/SVG or a CSS mesh gradient.

```css
.art-card { background: var(--surface-1); border-radius: 16px; overflow: hidden; }
.art-top {
  aspect-ratio: 4 / 3;
  background:
    radial-gradient(circle at 30% 40%, var(--accent-cool) 0%, transparent 50%),
    radial-gradient(circle at 70% 60%, var(--accent-warm) 0%, transparent 45%),
    radial-gradient(circle at 50% 80%, #c084fc 0%, transparent 40%);
  filter: blur(0) saturate(140%);
}
.art-top.animated {
  background-size: 200% 200%;
  animation: artShift 18s ease-in-out infinite alternate;
}
@keyframes artShift {
  to { background-position: 100% 100%; }
}
```

Add rivet decoration (doubao places small squares on all four corners and edges, evoking old photo paper or identification cards):

```css
.art-card { position: relative; }
.art-card::before, .art-card::after,
.art-card > .rivet-tl, .art-card > .rivet-tr,
.art-card > .rivet-bl, .art-card > .rivet-br {
  content: ''; position: absolute; width: 6px; height: 6px;
  background: var(--surface-2); border: 1px solid var(--border);
  transform: rotate(45deg);
}
.art-card::before { top: 8px; left: 8px; }
.art-card::after { top: 8px; right: 8px; }
.rivet-bl { bottom: 8px; left: 8px; }
.rivet-br { bottom: 8px; right: 8px; }
```

---

## Fallbacks and Performance

| Device condition | Fallback strategy |
|----------|----------|
| `prefers-reduced-motion: reduce` | Replace every pin-scrub with a simple fade; disable WebGL |
| `matchMedia('(max-width: 640px)')` | Reduce the Constellation to 3-4 static cards; retain pin-swap but simplify it to a one-time entrance; replace WebGL with a static image |
| `navigator.hardwareConcurrency < 4` | Disable WebGL, blur filters, and the custom cursor |
| Safari (weaker performance) | Automatically disable filter blur if FPS < 40 |

Performance detection:

```js
const perf = {
  isMobile: matchMedia('(max-width: 640px)').matches,
  isLowCore: navigator.hardwareConcurrency < 4,
  reduceMotion: matchMedia('(prefers-reduced-motion: reduce)').matches,
  noHover: !matchMedia('(hover: hover)').matches,
};
document.documentElement.dataset.perf = JSON.stringify(perf);
// CSS can use [data-perf*='"isMobile":true'] for conditional styling
```
