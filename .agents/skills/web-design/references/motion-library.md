# Motion Library — vue-bits / reactbits

For L2+ pages, select motion effects from [vue-bits](https://github.com/DavidHDev/vue-bits) or [reactbits](https://github.com/DavidHDev/react-bits) by default (the same effects for Vue and React, both by DavidHDev under the MIT License). **Reuse instead of rewriting whenever possible.**

For a plain-HTML project, adapt each effect according to its source stack:
- CSS-only → copy the class and keyframes directly
- GSAP-driven → copy the logic directly (GSAP itself is framework-agnostic)
- Three.js / OGL-driven → copy the canvas initialization logic
- Deeply dependent on the Vue/React lifecycle → extract paired `init()` / `destroy()` functions

---

## Hard Requirements (Mandatory for L2+)

Every L2+ page must include all six motion categories below. **Missing any category fails the requirement.**

| Category | Minimum count | Suggested placement |
|------|----------|----------|
| Text Animation — Hero H1 | 1 | Large first-viewport heading (entrance / continuous state) |
| Text Animation — Section H2 | 1 | Section headings (scroll-triggered) |
| Text Animation — Body / Label | 1 | Body copy / eyebrow / code line |
| Animation — element level | ≥ 1 | Magnetic button, card hover, cursor, or decorative element |
| Component — interactive structure | ≥ 1 | Card stack, gallery, menu, 3D card, etc. |
| Background — atmosphere layer | 1 | Hero background or section underlay |

**A page must contain at least four signature moments in total.** Use 6-8 on an L3 page, but never more than 10 (it becomes restless).

---

## Catalog (as of 2026-04)

### Text Animations (24)

| Name | Summary | Best for | Tags |
|------|--------|------|------|
| ASCIIText | Rebuilds text from ASCII characters | Technical, terminal style | Heavy, decorative |
| BlurText | Characters enter one by one, blurred → sharp | General entrances | Light, elegant |
| CircularText | Arranges text along a circular path | Logo surrounds, decorative badges | Light, decorative |
| CountUp | Rolls a number up to its target value | Statistics, KPIs | Light, functional |
| CurvedLoop | Loops text along a curve | Scrolling marquees, quotations | Light, decorative |
| DecryptedText | Scrambled characters → decoded text | Technical or cryptographic slogans | Medium, technical |
| FallingText | Text falls and rearranges with simulated physics | Highlighted paragraph decoration | Heavy, entertaining |
| FuzzyText | Text trembles with noise | Glitch art | Heavy, decorative |
| GlitchText | Glitch displacement + chromatic aberration | Cyberpunk | Heavy, stylized |
| GradientText | Fills text with a gradient (optionally animated) | Keywords, logos | Light, general |
| RotatingText | Cycles through words (an alternative to typewriter text) | Rotating Hero verbs | Light, functional |
| ScrambleText | Scrambled characters → stable text | Technical/document-style eyebrows | Light, technical |
| **ScrollFloat** | Floats into place on scroll and holds a precise position | **Preferred for Section H2** | Light, general |
| **ScrollReveal** | Reveals words/lines progressively on scroll | **Preferred for body paragraphs** | Light, general |
| ScrollVelocity | Links text speed to scroll velocity | Decorative strips, parallax | Medium, decorative |
| ShinyText | Sweeps a metallic sheen across text | CTAs, key slogans | Light, branded |
| Shuffle | Shuffles character positions | Transitions | Medium, entertaining |
| **SplitText** | Staggers characters/words/lines into view | **Preferred for Hero H1** | Light, classic |
| TextCursor | Cursor-based typing effect | Terminal feel, instructional copy | Light, functional |
| TextPressure | Changes font weight based on pointer distance | Interactive Hero headings | Medium, experimental |
| TextTrail | Leaves a visual trail behind characters | Parallax decoration | Medium, decorative |
| TextType | Types text character by character | Code blocks, command lines | Light, functional |
| TrueFocus | Keeps the focused word sharp while blurring the rest | Directing the reader's gaze | Medium, functional |
| VariableProximity | Increases font weight near the pointer | Variable-font demonstrations | Medium, experimental |

### Animations — Element Level (29)

| Name | Summary | Tags |
|------|--------|------|
| AnimatedContent | General entrance (fade/slide/scale) | Light, general |
| Antigravity | Repels elements as the pointer approaches | Medium, experimental |
| BlobCursor | Morphing blob cursor | Medium, decorative |
| ClickSpark | Bursts particles on click | Light, feedback |
| Crosshair | Crosshair cursor | Medium, experimental |
| Cubes | Self-rotating 3D cube grid | Heavy, decorative |
| ElectricBorder | Electric halo around a border | Medium, branded |
| FadeContent | Simple fade-in | Light, foundational |
| GhostCursor | Cursor with a trailing afterimage | Light, decorative |
| GlareHover | Sweeps a highlight across a card on hover | Light, branded |
| GradualBlur | Progressive blur | Light, transitional |
| ImageTrail | Pulls an image trail behind the pointer | Heavy, entertaining |
| LaserFlow | Flowing laser | Heavy, decorative |
| LogoLoop | Infinite horizontal brand-logo loop | Light, functional |
| MagicRings | Decorative halos | Medium, decorative |
| **Magnet** | Magnetically attracts an element to the pointer | **Preferred for CTAs** (light) |
| MagnetLines | Decorative magnetic field lines | Medium, decorative |
| MetaBalls | Fluid metaballs | Heavy, decorative |
| MetallicPaint | Metallic coating sheen | Medium, branded |
| Noise | Noise texture | Light, ambient texture |
| OrbitImages | Images rotating in orbit | Medium, decorative |
| PixelTrail | Pixelated pointer trail | Medium, stylized |
| PixelTransition | Pixelated transition | Medium, transitional |
| Ribbons | Floating ribbons | Medium, decorative |
| ShapeBlur | Animated shape blur | Medium, decorative |
| SplashCursor | Pointer splash effect | Heavy, entertaining |
| StarBorder | Flowing star points around a border | Medium, branded |
| StickerPeel | Peeling sticker edge | Medium, entertaining |
| TargetCursor | Targeting-reticle cursor | Medium, experimental |

### Components — Interactive Structures (30)

| Name | Summary | Tags |
|------|--------|------|
| AnimatedList | Animated list-item entrances | Light, general |
| BorderGlow | Border glow that follows the pointer | Medium, branded |
| BounceCards | Cards expand with spring motion | Medium, playful |
| BubbleMenu | Bubble menu | Medium, playful |
| CardNav | Card-based navigation | Light, general |
| **CardSwap** | 3D card switching and stacking | **Preferred for Hero card showcases** |
| Carousel | Basic carousel | Light, general |
| ChromaGrid | Chromatic-aberration grid | Medium, stylized |
| CircularGallery | Circular gallery | Medium, decorative |
| Counter | Animated counter | Light, functional |
| DecayCard | Decaying-card animation | Medium, decorative |
| Dock | macOS-style dock | Light, functional |
| DomeGallery | Spherical gallery | Heavy, decorative |
| ElasticSlider | Elastic slider | Light, functional |
| FlowingMenu | Flowing menu | Medium, decorative |
| FlyingPosters | Flying posters | Heavy, entertaining |
| Folder | Animated folder | Light, playful |
| GlassIcons | Glass icons | Light, decorative |
| GlassSurface | Glass surface | Medium, decorative |
| GooeyNav | Gooey navigation | Medium, playful |
| InfiniteMenu | Infinite menu | Medium, decorative |
| **InfiniteScroll** | Infinite horizontal/vertical work strip | **Preferred for showcases** |
| **MagicBento** | Bento grid + hover lighting | **Preferred for Feature grids** |
| Masonry | Masonry layout | Light, general |
| PillNav | Pill navigation | Light, general |
| PixelCard | Pixelated card | Medium, stylized |
| ProfileCard | 3D flipping profile card | Light, playful |
| RollingGallery | Wheel-controlled gallery | Medium, decorative |
| **ScrollStack** | Scroll-driven card stack (pin + overlap) | **Preferred for narrative cards** |
| **SpotlightCard** | Pointer-following spotlight card | **Preferred for Feature cards** |
| Stack | Card stack | Light, general |
| StaggeredMenu | Staggered entrance menu | Light, general |
| Stepper | Stepper | Light, functional |
| **TiltedCard** | 3D tilt (gyroscope / pointer) | **Preferred for work showcases** |

### Backgrounds — Atmosphere Layer (38)

| Name | Summary | Performance | Tags |
|------|--------|------|------|
| **Aurora** | Soft flowing aurora | Medium (WebGL) | **Preferred for dark editorial** |
| Balatro | Playing-card texture | Medium | Stylized |
| Ballpit | Physics-based ball pit | Heavy | Playful |
| Beams | Sweeping beams of light | Medium | Technical |
| ColorBends | Bending color fields | Medium | Artistic |
| DarkVeil | Dark veil | Light | Dark-editorial alternative |
| Dither | Dithered pixels | Light | Retro |
| DotGrid | Dot grid | Light | Minimal |
| EvilEye | Eye tracking | Medium | Experimental |
| FaultyTerminal | Glitching terminal | Medium | Cyber |
| FloatingLines | Floating lines | Light | Minimal |
| Galaxy | Galaxy | Medium | Technical |
| GradientBlinds | Gradient blinds | Light | Stylized |
| Grainient | Grainy gradient | Light | Editorial |
| GridDistortion | Distorted grid | Medium | Experimental |
| GridMotion | Flowing grid | Light | Technical |
| GridScan | Scan-line grid | Medium | Cyber |
| Hyperspeed | Hyperspeed tunnel | Heavy | Cyber |
| Iridescence | Iridescent sheen | Medium | Branded |
| LetterGlitch | Wall of glitching characters | Heavy | Cyber |
| LightPillar | Pillar of light | Medium | Dramatic |
| LightRays | Radiating light rays | Medium | Dramatic |
| Lightning | Lightning | Medium | Dramatic |
| LineWaves | Line waves | Light | Minimal |
| LiquidChrome | Liquid chrome | Medium | Branded |
| LiquidEther | Liquid ether | Medium | Artistic |
| Orb | Energy orb | Medium | Decorative |
| Particles | Particle system | Medium | General |
| PixelBlast | Pixel explosion | Heavy | Cyber |
| PixelSnow | Pixel snow | Light | Seasonal |
| Plasma | Plasma | Medium | Artistic |
| Prism | Prismatic dispersion | Medium | Branded |
| PrismaticBurst | Prismatic burst | Heavy | Dramatic |
| Radar | Radar scan | Light | Technical |
| RippleGrid | Ripple grid | Medium | Decorative |
| **Silk** | Flowing silk | Medium | **Works for dark or light editorial** |
| SoftAurora | Soft aurora (lightweight version) | Light | Dark-editorial alternative |
| Squares | Square grid | Light | Minimal |
| Threads | Threads | Light | Minimal |
| Waves | Waves | Light | Minimal |

---

## Recommended Combinations by Style × Context

### Dark Editorial (hueapp / Linear-style structure)
- Background: **Aurora** / **Silk** / **SoftAurora** / **Grainient**
- Hero H1: **SplitText** or **ShinyText** (keywords) + **GradientText** (keywords)
- H2: **ScrollFloat** or **BlurText**
- Body: **ScrollReveal**
- Animation: **Magnet** (CTA) + **GlareHover** (cards)
- Component: **CardSwap** / **ScrollStack** / **SpotlightCard** / **MagicBento**

### Dark Tech / Cyber (cursor / warp / cyber)
- Background: **LetterGlitch** / **Beams** / **Hyperspeed** / **FaultyTerminal**
- Hero H1: **GlitchText** / **DecryptedText** / **ShinyText**
- H2: **SplitText**
- Body: **TextType** (code blocks) + **ScrambleText** (eyebrows)
- Animation: **ElectricBorder** / **ClickSpark**
- Component: **TiltedCard** / **PixelCard**

### Minimal Pure / Editorial Light
- Background: **DotGrid** / **FloatingLines** / **Grainient**
- Hero H1: **SplitText** (restrained) + **GradientText** (keywords only)
- H2: **ScrollFloat**
- Body: **ScrollReveal**
- Animation: **Magnet** (CTA, the only motion effect) + **FadeContent**
- Component: **CardNav** / **InfiniteScroll** / **Masonry**

### Playful Creative
- Background: **Iridescence** / **LiquidChrome** / **Plasma**
- Hero H1: **TextPressure** / **FallingText** / **BounceCards** copy
- H2: **Shuffle** / **SplitText**
- Body: **TextTrail** / **ScrollReveal**
- Animation: **ClickSpark** / **BlobCursor** / **StickerPeel**
- Component: **BounceCards** / **BubbleMenu** / **GooeyNav** / **FlyingPosters**

### Chinese Elegant
- Background: **Threads** / **FloatingLines** / **Grainient** (low contrast)
- Hero H1: **BlurText** (slow) + **GradientText** (ochre gradient)
- H2: **ScrollFloat**
- Body: **ScrollReveal** (line-level, not character-level—character-level Chinese animation is too fragmented)
- Animation: **Magnet** (restrained)
- Component: **CircularGallery** / **Masonry**

### Warm Professional
- Background: **Silk** / **SoftAurora** / **Grainient**
- Hero H1: **SplitText** + **GradientText**
- H2: **ScrollFloat**
- Body: **ScrollReveal**
- Animation: **Magnet** + **GlareHover**
- Component: **MagicBento** / **SpotlightCard** / **InfiniteScroll**

---

## Performance Principles

- Never use more than **two heavy backgrounds** on one page (and use only one WebGL background)
- On mobile (< 640px), fall back automatically: heavy background → static gradient; 3D component → 2D version
- Enable cursor-related effects only under `matchMedia('(hover: hover)')`
- Every effect must have a `prefers-reduced-motion` fallback path
- Limit each page to three GSAP timelines (more will contend for the main thread)

## Reuse Rules

1. **Prefer direct reuse**: vue-bits / reactbits source is MIT-licensed and can be used with author attribution
2. **Adaptation layer**: When porting Vue / React code to vanilla HTML, expand props into data attributes or function parameters
3. **Attribution placement**: Add "Motion effects inspired by / derived from [vue-bits](https://github.com/DavidHDev/vue-bits) by DavidHDev (MIT)" to the page footer or README
