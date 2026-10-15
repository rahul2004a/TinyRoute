# Frontend-domain short links

User-authorized scope: frontend routing only, analytics deferred, existing
`fix/guest-only-auth-pages` branch. Specification:
`docs/spec/frontend-short-links/spec.md`.

1. Add opt-in server-only backend-origin configuration, API rewrites and a
   single-segment shortcode fallback, preserving existing headers and pages.
2. Update safe frontend environment examples, local configuration and setup docs;
   explain local proxy transport versus Spring-owned redirect policy.
3. Verify actual Next.js request/response forwarding with a local fixture backend,
   run frontend checks/build and review the diff. Record evidence and archive the
   completed plan/checklist only after all acceptance criteria are satisfied.

Completed; results are recorded in `verification.md`. Archived on completion
with the checklist, preserving the user's existing branch.
