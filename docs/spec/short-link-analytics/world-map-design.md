# Individual-link country map

Status: approved 2026-10-20; user confirmation: “ok”. Requested as an extension
of `short-link-analytics`. Keep the existing feature branch and incomplete tasks.

## Outcome

An owner viewing `/analytics/[id]` can see which countries contributed successful
redirects during the applied UTC date range, zoom in and out, and inspect each
country's count. The overview retains its existing layout. This presents the
existing geography requirement (FR-ANA-04), preserving date filtering
(FR-ANA-03), ownership (FR-MGT-02; NFR-SEC-04), and anonymous capture
(NFR-PRV-02/03).

## Presentation and interaction

- Add a full-width **Traffic by country** section below all six traffic breakdowns
  in the individual-link report, per the user's placement correction. Reuse the
  existing paper/charcoal theme, Geist type, restrained borders, and responsive
  spacing from DESIGN.md.
- Render a locally bundled SVG world map. Shade countries by redirect count,
  with a labeled low-to-high legend and a distinct neutral no-activity fill.
  Hover, keyboard focus, or tapping a country reveals its name and count.
- Provide labeled 44px Zoom in, Zoom out, and Reset map buttons. Bound zoom to
  1–8×, disable controls at their limits, and allow bounded dragging when zoomed.
  A focusable map viewport supports arrow-key panning, +/- zoom, and Home reset.
  Do not intercept normal page-wheel scrolling or animate map movement.
- Fit the initial map to the available width. Keep controls reachable on mobile
  and prevent page overflow in both themes. Restore the world view when the
  selected link or applied range changes.
- The existing Countries table becomes the complete accessible country list on
  detail pages, using exactly the map's counts and including Unknown. Other
  breakdown tables and the daily chart keep their existing behavior.

## Complete and honest geography

Do not build a map by matching translated names or reverse-geocoding cities.
The detail API adds an optional `geography` object containing
`countries: [{countryCode, label, count}]` and `unknownClicks`. Countries use
stable ISO alpha-2 codes and English labels from the backend. Return every known
country represented in the selected range, sorted by count descending and label
ascending, before the existing top-20 breakdown folding. Reuse the existing
country aggregate query and repeatable-read transaction; add no database schema,
capture, ownership, or redirect-policy change. The overview omits this object.

Map geometry is a static, locally served Natural Earth asset with recorded
source, version, checksum, and public-domain provenance. No remote map tiles,
geocoding requests, map API keys, or visitor data are sent to a map provider.
Geometry has finite detail; retain counts for any countries without drawable
geometry in the table and explicitly label their unplotted total.

User correction (2026-10-20): use the India boundary view. The bundled geometry
now uses the Natural Earth India POV dataset, with shared neighboring edges
simplified together and northern reference locations checked against Survey of
India's published outline. Keep this source choice in the reproducible generator
and the asset notice; do not replace it with the default boundary view.

Unknown clicks are counted and labeled separately, never assigned an invented
point. A zero-activity range shows a neutral map and a clear empty state. If
clicks exist but none have a known country, explain that country information is
unavailable. Country shading represents approximate aggregate geography, not
individual visitors or precise locations. The current optional local City
database remains the enrichment source; localhost/private addresses stay Unknown.

## Verification

- HTTP/database tests prove complete ISO-coded country totals beyond 20,
  conservation with Unknown, correct date/link scoping, omission in overview,
  and existing generic owner-only 404 behavior.
- Component tests exercise map/count association, empty and Unknown-only ranges,
  zoom bounds, reset, and keyboard operation.
- Browser checks exercise zoom, drag, reset, range changes, detail-only
  visibility, table equivalence, mobile/desktop, both themes, and no overflow.
- Run affected backend/frontend checks and record the results. Existing
  performance verification remains incomplete and must not be marked passed.

Source: [Natural Earth terms of use](https://www.naturalearthdata.com/about/terms-of-use/).
