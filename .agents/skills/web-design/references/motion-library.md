# Motion idea library

This catalog names patterns available in projects such as
[react-bits](https://github.com/DavidHDev/react-bits) and
[vue-bits](https://github.com/DavidHDev/vue-bits). It is an idea index, not a
dependency mandate or animation quota. Check licenses, installed packages, and
project fit before reuse.

## Text patterns

| Pattern | Useful for | Cost |
|---|---|---|
| `BlurText` | A restrained display entrance | Low when blur is brief |
| `CountUp` | A measured metric changing once | Low |
| `DecryptedText` | Technical campaign labels | Medium |
| `GradientText` | An approved expressive keyword | Low |
| `RotatingText` | A short rotating phrase | Low |
| `ScrollFloat` | A section-title entrance | Low |
| `ScrollReveal` | Line-level editorial reveal | Low |
| `ShinyText` | A small metallic accent | Low |
| `SplitText` | Word- or line-level hero entrance | Low to medium |
| `TextType` | Command-line or code demonstration | Low |
| `TextPressure` | Experimental variable-font display | Medium |

Prefer line or word granularity. Character-level effects can harm readability,
localization, and screen-reader output.

## Element patterns

| Pattern | Useful for | Cost |
|---|---|---|
| `AnimatedContent` | General one-time reveal | Low |
| `ClickSpark` | Playful confirmation feedback | Low to medium |
| `ElectricBorder` | A campaign highlight | Medium |
| `FadeContent` | Quiet entrance | Low |
| `GlareHover` | Image or card material response | Low |
| `Magnet` | One prominent pointer CTA | Low |
| `Noise` | Static or subtly animated texture | Low |
| `PixelTransition` | A deliberately retro transition | Medium |
| `StickerPeel` | Playful campaign detail | Medium |

## Component patterns

| Pattern | Useful for | Cost |
|---|---|---|
| `AnimatedList` | New list items or filtering | Low |
| `CardSwap` | A compact product demonstration | Medium |
| `Carousel` | User-controlled sequential media | Low |
| `MagicBento` | Uneven capability layout | Medium |
| `Masonry` | Image-led work collection | Low |
| `ScrollStack` | Explicit narrative sequence | Medium to high |
| `SpotlightCard` | Pointer-aware feature emphasis | Low to medium |
| `TiltedCard` | One experimental media card | Medium |

Do not use a component effect when ordinary semantic layout communicates the
same information more clearly.

## Background patterns

| Pattern | Character | Cost |
|---|---|---|
| `DarkVeil` | Quiet dark atmosphere | Low |
| `DotGrid` | Technical structure | Low |
| `FloatingLines` | Minimal ambient movement | Low |
| `Grainient` | Editorial texture | Low |
| `Silk` | Soft material movement | Medium |
| `SoftAurora` | Restrained atmospheric color | Low |
| `Threads` | Fine line structure | Low |
| `Aurora` | Rich WebGL atmosphere | Medium to high |
| `Galaxy` | Technical or spatial campaign | Medium |
| `Hyperspeed` | Deliberately cinematic sequence | High |
| `LetterGlitch` | Cyberpunk campaign | High |
| `LiquidChrome` | Experimental product material | Medium to high |

TinyRoute's current design rules generally favor the low-cost options and may
prohibit gradients or spectacle. Root `DESIGN.md` wins.

## Selection examples

- **Restrained technical:** `FadeContent`, a quiet title reveal, and border-state
  feedback.
- **Editorial light:** line-level `ScrollReveal` with static grain or no
  background effect.
- **Dark campaign:** one `SoftAurora` or `Silk` background plus restrained CTA
  feedback.
- **Playful launch:** one interactive component and one click response; keep the
  rest static.
- **Cinematic story:** one pattern from `scroll-story-patterns.md` with explicit
  mobile and reduced-motion alternatives.

## Performance and accessibility

- Use no more than one WebGL background on a page.
- Replace heavy backgrounds with a static surface on small screens.
- Enable cursor-related effects only for `(hover: hover) and (pointer: fine)`.
- Pause animation outside the viewport.
- Provide a complete `prefers-reduced-motion` path.
- Avoid multiple competing timelines on the main thread.
- Preserve readable text and semantic DOM instead of rendering essential copy
  only to canvas.

## Reuse and attribution

Reuse third-party source only when its license and the current task allow it.
Retain notices required by the source license. If code is derived from
react-bits or vue-bits, record that attribution in the project location agreed
for the task. Do not copy demo assets, marketing text, or brand identity.
