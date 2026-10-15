# Frontend-domain short links verification

Completed 2026-10-15 on `fix/guest-only-auth-pages`, as explicitly requested by
the user. Baseline: `0b15297`. Requirements: FR-CRE-01/05, FR-RED-01..07,
NFR-REL-02, NFR-SEC-06/08, NFR-TST-01 and NFR-MNT-02.

## Result and scope

Added opt-in Next.js external rewrites with a validated, server-only
`BACKEND_ORIGIN`. API requests use `/api/*`; unmatched single-segment shortcode
candidates use a fallback after pages, dynamic routes and static assets.
Infrastructure namespaces are excluded from shortcode fallback. There is no
Next.js redirect page or browser-side destination lookup. Spring's HTTP status,
Location, HTML errors, no-store, Retry-After and Secure cookies pass through.

Updated the safe frontend environment example and preserved the existing local
API origin while adding `BACKEND_ORIGIN=http://localhost:8080` to ignored local
configuration. The user's backend environment already configured port 8080,
HTTP transport and `SHORT_LINK_BASE_URL=https://localhost:3000`. No backend
source, analytics, database, dependency manifest or lockfile was changed.
README and architecture document the local shared-origin setup and the unchanged
production topology. Existing browser/live test configurations explicitly select
their separate verification API, independent of the new local environment.

## Checks

- `pnpm exec playwright test --config playwright.routing.config.ts`: 11 passed.
  Uses actual Next.js HTTPS routing and a disposable HTTP upstream. Covers one
  302 with exact path/query/fragment, unknown/case-mismatched codes, disabled,
  deleted, expired, throttled and unavailable outcomes, HEAD, no-store, HTML,
  Retry-After, existing pages/assets, nested unknown paths, infrastructure
  exclusions, existing referrer policy, multiple Secure HttpOnly cookies, and
  POST method/body/CSRF forwarding. Requests are counted to detect unexpected
  upstream calls; external redirects are not followed.
- `pnpm test --run`: all 99 tests in 18 files passed.
- Two existing Chromium journeys passed: signed-out/unknown sessions cannot
  submit, and keyboard creation plus one-click copying uses the exact API URL.
- `pnpm run lint`, `pnpm run format:check`, `pnpm run typecheck` and
  `pnpm run build`: passed.
- Checked that an unset upstream disables rewrites and that non-HTTP origins,
  credentials, paths, query strings and fragments are rejected.
- Reviewed the final diff, confirmed no backend changes, and passed
  `git diff --check`. Acceptance criteria and task checkboxes are complete;
  plan/checklist are archived beside this record.

The sandbox blocked pnpm's dependency restoration and the temporary server
listener. Approved unrestricted commands restored the existing locked packages
and ran the routing/browser checks; no dependency versions were changed.

The routing gate uses a fixture upstream, not the live Spring/PostgreSQL/Redis
stack. No production deployment, real OAuth journey or load measurement was
performed. The known backend alias-reservation gap and production origin-host
and forwarded-client trust work remain outside this frontend-only change.
