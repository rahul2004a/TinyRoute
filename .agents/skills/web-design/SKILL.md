---
name: web-design
description: Design or redesign TinyRoute marketing and landing pages from a PRD, reference URL, screenshot, or style brief using the existing DESIGN.md. Invoke explicitly with $web-design. Do not use for dashboards, analytics, routine product UI, APIs, backend work, or unrequested design-system changes.
---

# Web design

Use a spec-first workflow to turn product context and visual references into a
coherent TinyRoute web experience. Match the user's requested deliverable: plan,
design specification, implementation, or review.

## TinyRoute authority

This is a Codex adaptation of
[xiaopu-ai/web-design](https://github.com/xiaopu-ai/web-design) at commit
`22a4f482cc4caa2394391c0c31ff0aefd1908774`, used under the bundled
[MIT license](LICENSE).

Follow these sources in order:

1. The user's current request defines scope and output.
2. Read [DESIGN.md](../../../DESIGN.md) before frontend work. It is TinyRoute's
   approved presentation authority and the existing design artifact.
3. Before changing application behavior, read the relevant
   [functional requirements](../../../docs/requirements/Functional.md),
   [non-functional requirements](../../../docs/requirements/Non-Functional.md),
   and [architecture](../../../docs/architecture/architecture.md).
4. Use this skill's references as optional inspiration. They never override the
   sources above.

Keep Next.js presentation-only. Spring Boot services own authorization,
ownership, validation, rate limits, transactions, and redirect policy. Preserve
fail-closed behavior: unknown state must never produce a guessed destination or
authorization result.

Do not regenerate, replace, or materially change the root `DESIGN.md` unless the
user explicitly requests a design-system change. When no such change is
requested, treat `DESIGN.md` as the approved output of the specification phase.

## Workflow

### 1. Ground the request

- Inspect the existing Next.js structure, styling approach, components, assets,
  and installed packages relevant to the requested page.
- Extract the page type, audience, content, desired tone, reference material,
  and requested deliverable from the prompt and project docs.
- State a one-sentence design read when it helps align implementation.
- Ask one focused question only when an unresolved preference would materially
  change the result. Otherwise, make a reasonable assumption and continue.

### 2. Analyze visual inputs

Use only the inputs that are available:

- **Existing design system:** Start from the root `DESIGN.md` and current UI.
- **Screenshot or mockup:** Inspect hierarchy, spacing, typography, color roles,
  responsive implications, and interaction cues. Do not infer product behavior
  from appearance alone.
- **Reference URL:** Use available Codex browser or web tools first. Extract
  design principles rather than copying proprietary assets, branding, or text.
- **Named brand:** Read
  [the design-system index](references/design-systems/INDEX.md), then load only
  the one relevant brand profile.
- **Style words without a reference:** Read
  [style seeds](references/style-seeds.md) and select the closest direction.

Network access, crawling, image fetching or generation, and external services
must be relevant to the current request and follow the active tool-permission
rules. Never hotlink assets from a reference site in production code.

### 3. Define the design direction

- Reuse TinyRoute's existing palette, type, spacing, shape, component states,
  accessibility rules, responsive behavior, and restrained motion.
- Select motion by purpose. Default to the restrained level established by
  `DESIGN.md`; do not force animation quotas, WebGL, parallax, scroll-jacking,
  custom cursors, or third-party animation libraries.
- If the user explicitly requests a new or revised design system, use
  [the design template](references/design-md-template.md) as a checklist and
  adapt it to TinyRoute conventions. Do not import its Google Fonts or other
  upstream mandates when they conflict with `next/font` or project policy.
- Present a concise design summary before implementation only when the user
  asked for review or when a high-impact design decision needs confirmation.

### 4. Implement when requested

- Follow the existing Next.js routing, component, styling, and data-fetching
  conventions. Do not create backend policy in the UI or add Next.js API routes
  for TinyRoute application behavior.
- Reuse installed packages, components, icons, fonts, and assets. Do not add a
  dependency merely because an upstream reference recommends it.
- Prefer user-provided assets, then approved project assets. Use external stock
  imagery only for an authorized mockup, and label it as a placeholder.
- Implement semantic markup, keyboard and focus behavior, loading/empty/error
  states, touch targets, reduced-motion behavior, and responsive layouts.
- Keep effects within TinyRoute's performance and accessibility constraints.

### 5. Verify

- Compare the result with the user's brief and root `DESIGN.md`.
- Check mobile and desktop layouts, horizontal overflow, keyboard navigation,
  focus visibility, contrast, touch targets, and `prefers-reduced-motion`.
- Run the smallest relevant project tests or checks for the files changed.
- If a reference was supplied, report intentional differences rather than
  silently cloning it.

## Reference routing

Read supporting files only when the current task needs them:

| Resource | Use it for |
|---|---|
| [design-md-template.md](references/design-md-template.md) | An explicitly requested design-system creation or revision |
| [style-seeds.md](references/style-seeds.md) | Turning broad style words into a concrete direction |
| [design-systems/INDEX.md](references/design-systems/INDEX.md) | Selecting one named-brand profile for inspiration |
| [scene-defaults.md](references/scene-defaults.md) | Page-type layout and content prompts |
| [interaction-patterns.md](references/interaction-patterns.md) | A requested interaction pattern that fits `DESIGN.md` |
| [motion-library.md](references/motion-library.md) | Comparing motion ideas; its quotas are not TinyRoute requirements |
| [scroll-story-patterns.md](references/scroll-story-patterns.md) | Explicitly requested cinematic storytelling only |
| [text-decoration-rules.md](references/text-decoration-rules.md) | Evaluating text treatments allowed by `DESIGN.md` |
| [icon-library.md](references/icon-library.md) | Icon ideas when compatible with the project's installed icon family |
| [quality-checklist.md](references/quality-checklist.md) | Supplemental visual review after project checks |

The instructional references are adapted to English; brand profiles retain
proper names and technical font identifiers where fidelity requires them. Treat
words such as "must" or "required" inside a brand profile as requirements of
that optional pattern, not as permission to expand the user's task or override
TinyRoute.

## Optional local scripts

Prefer Codex's available browser, web, and image-input capabilities. Use the
bundled scripts only when they add concrete value and the required dependency is
already available or installation is authorized. Resolve all paths relative to
this skill directory; these examples assume the TinyRoute repository root:

```bash
python3 .agents/skills/web-design/scripts/extract_design_tokens.py --url https://example.com --format json
python3 .agents/skills/web-design/scripts/crawl_website.py --url https://example.com --output /private/tmp/tinyroute-web-design-crawl
python3 .agents/skills/web-design/scripts/fetch_unsplash_images.py --keywords "workspace" --use-case placeholder --ack-remote-placeholders
```

The crawler requires Playwright. Do not install Playwright or browser binaries
unless the current task authorizes that dependency change. Unsplash results are
remote placeholder candidates, not cleared production assets.
