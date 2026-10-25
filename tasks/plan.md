# UI polish implementation plan

Scope: [the UI polish specification](../docs/spec/ui-polish/spec.md).
Branch: `fix/ui-polish`; keep subsequent user-supplied fixes on this branch.

1. Update `RedirectPageRenderer` in place with responsive branded error
   markup, semantic palette, generic guidance, keyboard states, reduced
   motion, and navigation to the first validated frontend origin.
2. Verify the navigation and supported states with focused renderer tests.
   Run existing redirect contracts to protect status codes, no-store, HEAD,
   rate limiting, outage handling, and non-disclosure.
3. Render the actual Java output in Chromium at 320, 768, 1024, and 1440px in
   both themes; inspect screenshots, focus, motion preferences, and overflow.
4. Record verification and review the diff. Keep the batch active for the
   user's next changes; archive plan/checklist before making the combined PR
   ready, once all requested changes have been completed.
