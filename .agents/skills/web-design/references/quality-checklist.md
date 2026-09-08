# Web-design quality checklist

Use this after the relevant project tests and checks. Root `DESIGN.md` and the
product requirements remain authoritative.

## Design-system alignment

- [ ] The implementation uses existing semantic tokens and typography roles.
- [ ] No unrequested design-system change was introduced.
- [ ] Component shape, density, borders, and elevation are consistent.
- [ ] All visible states match the approved design system.

## Content and reference handling

- [ ] User content and approved project assets take priority.
- [ ] Reference sites informed principles rather than copied identity or assets.
- [ ] No production asset is hotlinked from a reference site.
- [ ] Intentional differences from a supplied reference are documented.

## Accessibility

- [ ] Semantic landmarks and heading order are correct.
- [ ] Keyboard navigation and focus visibility work.
- [ ] Text, controls, status colors, and focus rings meet contrast requirements.
- [ ] Icon-only controls have accessible names.
- [ ] Touch targets meet the project minimum.
- [ ] Meaning is never communicated by color alone.

## Interaction and state

- [ ] Interactive controls have hover, focus, active, disabled, and loading
      states where relevant.
- [ ] Loading, empty, error, and success states preserve layout stability.
- [ ] Motion has a purpose and respects `prefers-reduced-motion`.
- [ ] No scroll-jacking, cursor replacement, or decorative animation was added
      without an explicit brief.

## Responsive behavior and performance

- [ ] Mobile and desktop layouts were checked.
- [ ] Navigation, cards, media, tables, and charts adapt without horizontal
      overflow.
- [ ] Images have dimensions, useful alt text, and appropriate loading behavior.
- [ ] Client-side JavaScript and animation work stay proportional to the task.
- [ ] Effects pause or degrade when off-screen, on mobile, or under reduced
      motion when applicable.

## TinyRoute behavior

- [ ] Next.js remains presentation-only.
- [ ] Ownership, authorization, validation, rate limits, and redirects remain
      backend-owned.
- [ ] Unknown state never produces a guessed redirect or authorization result.
