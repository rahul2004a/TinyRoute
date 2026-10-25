# UI polish checklist

- [x] Inspect the renderer, requirements, architecture, and existing design.
- [x] Create `fix/ui-polish` from clean, current `main`.
- [x] Record the requested page redesign and single-branch batch scope.
- [x] Implement responsive branded public error pages and safe home navigation.
- [x] Verify renderer navigation and existing public redirect contracts.
- [x] Verify desktop/mobile, both themes, focus, reduced motion, and overflow.
- [x] Review the diff and record verification evidence.
- [x] Commit the completed error-page fix before starting the header follow-up (`cc297a4`).
- [x] Render landing-header account links from the shared session result.
- [x] Verify signed-in, signed-out, pending/error, and sign-out cache changes.
- [x] Verify desktop/mobile header behavior and run frontend checks/build.
- [x] Review and record the header follow-up results.
- [x] Apply the approved creation-form labels, helper text, placeholders, and action.
- [x] Update existing test selectors and verify the copy-only follow-up.
- [x] Record verification and commit the creation-form copy separately.
- [x] Delegate the analytics 404 presentation update on the existing branch.
- [x] Verify analytics 404 privacy, recovery range, and existing session states.
- [x] Inspect responsive light/dark layouts and run frontend checks/build.
- [x] Review the delegated change, record evidence, and commit separately.
- [x] Unify the three confirmed signed-out workspace prompts.
- [x] Rename the settings sidebar Dashboard entry to Your links everywhere.
- [x] Verify session boundaries, sign-in targets, and signed-in regressions.
- [x] Inspect responsive/theme layouts, review, build, and commit this follow-up.
- [x] Disable and verify the Next.js development indicator configuration.

Keep this batch active for additional changes from the user. The active
follow-up improves signed-out workspace screens and navigation labels. Archive these
files when the batch is ready for a pull request.
