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

Keep this batch active for additional changes from the user. The active
follow-up hides guest header links for an authenticated user. Archive these
files when the batch is ready for a pull request.
