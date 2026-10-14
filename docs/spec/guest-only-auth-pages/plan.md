# Guest-only authentication pages

Branch: `fix/guest-only-auth-pages`.
Specification: `docs/spec/guest-only-auth-pages/spec.md`.

1. Add regression coverage for existing sessions, pending checks, and successful
   password login leaving the auth pages.
2. Replace the login signed-in panel with navigation, add registration session
   gating, and move sign-out to account settings. Preserve OTP success routing.
3. Update browser fixtures and live-flow expectations; verify direct visits,
   session refresh, history replacement, OTP, and sign-out.
4. Run frontend validation and build, review the final diff, record evidence,
   and archive completed task files.
