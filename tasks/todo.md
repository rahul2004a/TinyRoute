# Short-link analytics — active checklist

Specification: [spec.md](../docs/spec/short-link-analytics/spec.md).
Active plan: [plan.md](plan.md).

- [x] Check branch/status and fetch current main; confirm a clean baseline.
- [x] Create `feature/short-link-analytics` before changing files.
- [x] Read requirements, architecture, stack decisions, DESIGN.md, and existing flows.
- [x] Locate/read the requested Superpowers workflow and web-design skill.
- [x] Prepare a concrete proposed specification and implementation outline.
- [x] Self-review design for scope, privacy, ownership, date semantics, and failure behavior.
- [x] Receive user review of the proposed design.
- [x] Expand the approved design into a detailed executable plan.
- [x] Add Flyway event schema and atomic count/event persistence with database tests.
- [x] Add UTC range validation, snapshot aggregation, and bounded breakdowns.
- [x] Add isolated bounded async capture and completed-redirect integration.
- [x] Add privacy-safe metadata reduction and local geography with synthetic tests.
- [x] Prove failed redirects, HEAD, health checks, and unrelated 302s never count.
- [x] Add authenticated owner-scoped APIs and identical generic detail 404s.
- [x] Add typed frontend reads, session recovery, account keys, and cache cleanup.
- [x] Build overview/detail, date filters, lifetime totals, and paginated link selection.
- [x] Add daily chart, accessible tables, all six breakdowns, and delay/timestamp copy.
- [x] Complete loading, empty, validation, generic-404, failure, Retry, and Refresh states.
- [x] Integrate actual navigation and saved-link analytics actions.
- [x] Verify desktop/mobile, both themes, reduced motion, keyboard, and no overflow.
- [x] Run backend/frontend checks, targeted browser tests, and affected regressions.
- [x] Inspect existing geography and prepare the requested individual-link map design.
- [x] Receive approval of `world-map-design.md` before implementing the map extension.
- [x] Return complete ISO-coded detail geography and implement the accessible zoomable map.
- [x] Verify map interactions, count consistency, range filtering, themes, and responsive layout.
- [x] Apply the requested India boundary view and verify the generated outline and neighboring borders.
- [x] Diagnose local Unknown geography without storing visitor addresses or weakening proxy trust.
- [x] Recognize advertised Brave browser brands, verify capture/fallback behavior, and reload the local backend.
- [x] Configure and verify the supplied DB-IP City Lite database, keep it outside Git, and include provider attribution on analytics reports.
- [x] Resolve PR 21's Jackson 2 dependency vulnerabilities and verify backend tests, package inventory, and the unchanged local security scan.
- [ ] Measure sustained redirect performance and capture freshness with analytics enabled.
- [x] Update documentation, dependency ADR, geography operation instructions, and evidence.
- [ ] Verify specification AC1–AC12 and complete all task checkboxes.
- [ ] Archive completed plan/checklist under `docs/spec/short-link-analytics/`.
- [ ] Commit/push the completed feature and propose its PR without merging.
