# Frontend-domain short links

The user authorized frontend-side implementation without analytics, and explicitly
requested continuing on `fix/guest-only-auth-pages` at baseline `0b15297`.

## Scope and requirements

- FR-CRE-01/05, FR-RED-01..07: open existing short URLs on the HTTPS frontend
  origin; Spring still generates URLs, resolves codes, and returns redirects/errors.
- NFR-REL-02, NFR-SEC-06/08: pass through backend outcomes, safe errors and secure
  cookies without introducing redirect policy or browser-accessible secrets.
- NFR-TST-01, NFR-MNT-02: verify actual Next.js proxy behavior and document setup.

Implement opt-in external rewrites using server-only `BACKEND_ORIGIN`. Route
`/api/*` to Spring for the user's same-origin local API configuration. Forward
unmatched single-segment shortcode candidates only after Next.js pages, dynamic
routes, and static assets. Exclude infrastructure namespaces from shortcode
fallbacks. Preserve case, destination Location, response status/body/headers,
request methods, cookies, and CSRF headers. No client lookup, redirect page,
database access, count recording, or backend source changes.

Existing local backend settings already use HTTP port 8080 and
`SHORT_LINK_BASE_URL=https://localhost:3000`. Spring remains authoritative for
alias reservations, self-reference rejection, link state, throttling and errors.
Frontend routes win collisions; the backend's missing `verify-email` reservation
is a known follow-up outside this frontend-only scope. Production continues to
use the existing direct API/go-host topology unless separately configured; a
production shared-host rollout still needs origin-host and proxy-trust setup.

## Acceptance criteria

- [x] Active short links pass through HTTP 302 and the exact stored Location.
- [x] Unknown/error outcomes retain backend status, HTML body, and no-store.
- [x] Existing pages/assets and nested unknown paths bypass shortcode proxying.
- [x] API requests preserve cookies, CSRF headers, method and body through HTTPS ingress.
- [x] Backend and analytics code remain unchanged; current branch is retained.
- [x] Relevant checks/build and actual Next.js routing tests pass; docs are current.
