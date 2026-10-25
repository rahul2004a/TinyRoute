# UI polish

The user requested a redesign of the public short-link error page and asked
that subsequent fixes stay together on `fix/ui-polish`. Extend this document
as those changes are supplied; do not merge or open a separate fix branch.

## Public short-link error pages

The existing backend page is a small block of text with a top rule and no way
back to the application. Replace it with a branded, responsive layout using
the existing charcoal/paper palette: a large status number beside a concise
heading, useful generic guidance, and a Back to TinyRoute action. On mobile,
stack the number above the message and use a full-width action below 480px.

Spring Boot continues to render the public route. Use the first validated
frontend origin from `tinyroute.security.allowed-origins` for both home links,
HTML-escaped, rather than the current redirect host or request headers. This
reuses the existing required frontend configuration without another setting.
Production's single frontend origin is the Vercel application. With multiple
allowed origins, place the intended public frontend first.

Follow system light/dark preference, support reduced motion and keyboard focus,
and keep the page self-contained with no JavaScript, remote fonts, or assets.
Use the system font fallback because Next.js fonts are not available to this
independent backend document. Reuse the Lucide Link2 and ArrowLeft SVG paths.

Unknown, deleted, expired, and case-mismatched links retain the same generic
404 page (FR-RED-03, FR-RED-05, FR-RED-06, FR-RED-07). Disabled links retain 403
and generic unavailable copy (FR-RED-04). Throttling retains 429 and Retry-After
(FR-ABS-03). Unknown service state retains a safe 503 (NFR-REL-02). Error
responses retain no-store, no Location header, and empty HEAD bodies. Reveal
no destination, short code, owner, or exception. Navigation only happens when
the visitor explicitly follows a link. Mark error documents noindex,nofollow.

## Acceptance criteria

- All four error states use the new layout and retain their status-specific
  headings and safe generic guidance.
- Both navigation links lead to the validated configured frontend origin.
- No horizontal overflow at 320, 768, 1024, or 1440px in light or dark mode.
- Navigation has visible keyboard focus and at least 44px touch targets.
- Reduced motion disables entrance and interaction animation.
- Existing redirect contracts, including confidentiality, HEAD, throttling,
  outages, and no automatic redirects, still pass.
