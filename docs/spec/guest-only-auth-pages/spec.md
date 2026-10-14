# Guest-only authentication pages

Authenticated visitors to `/login` and `/register` must leave those pages for
`/links`, rather than seeing the signed-in login panel or registration form.
This refines FR-ACC-01/02/03 using the existing Spring-backed session API and
preserves HttpOnly cookie handling (NFR-SEC-06).

## Behavior

- Use `useSession` to determine authentication, including refresh restoration.
- While the session is loading, show the existing session-check status and hide
  the authentication forms.
- Registration waits for the browser session check, including when JavaScript
  is disabled; an unverified session does not render registration controls.
- When authenticated, hide the forms and replace the current history entry with
  `/links`. Apply the same check after successful password sign-in.
- Signed-out visitors retain login, registration, OTP, and OAuth error handling.
- Preserve the existing `/settings` destination after successful OTP verification.
- Keep sign-out available in authenticated account settings after removing the
  signed-in login panel.
- Keep backend authentication and public short-URL redirect policy unchanged.

## Acceptance criteria

- [x] Direct visits to both auth pages with a valid session end at `/links`.
- [x] Refresh-restored sessions also leave both auth pages.
- [x] Pending checks never render login or registration inputs.
- [x] Password login navigates to `/links`; signed-out forms still work.
- [x] OTP verification still navigates to `/settings`.
- [x] Sign-out is available in account settings, after which auth pages work.
- [x] Frontend checks, build, and relevant browser regressions pass.
