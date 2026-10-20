# Short-link analytics — active plan

Feature: `short-link-analytics`.
Specification: [spec.md](../docs/spec/short-link-analytics/spec.md).
Status: design prepared; user review pending. This is the implementation outline,
not an approved executable plan. Expand it with the Superpowers writing-plans
workflow after design approval; execute inline unless the user requests delegation.

## Constraints

Use Java/Spring MVC/JPA/PostgreSQL and bounded `@Async`; retain the layer-first
packages. Frontend remains Next.js/React, strict TypeScript, Node 24, pnpm,
TanStack Query, React Hook Form/Zod, Recharts, Radix-based shadcn/ui where needed,
and existing semantic design tokens. Spring owns all analytics policy. Implement
FR-ANA-01..04, FR-MGT-02, NFR-SEC-04/05/08, NFR-CON-03, and NFR-PRV-02/03.
Keep unrelated incomplete work intact; `tasks/` was empty at branch creation.

## Ordered deliverables

1. Review the proposed specification and resolve any requested changes. Expand
   this outline into exact interfaces, file changes, test cases, and commands
   using `superpowers:writing-plans` before editing application code.
2. Implement event persistence and UTC range rules. Add
   `backend/src/main/resources/db/migration/V7__create_click_events.sql`,
   `model/ClickEvent.java`, `model/AnalyticsRange.java`, application contracts
   under `repository/`, and JPA implementations under `repository/jpa/`.
   Prove atomic event/count writes and owner-scoped snapshot queries against
   actual PostgreSQL, including concurrent increments and transaction rollback.
3. Implement bounded asynchronous capture. Add `ClickCountService`, its separate
   async worker, metadata normalizer, and analytics executor. Connect only
   completed GET redirects through the HTTP completion seam. Expose the existing
   trusted resolved client address for transient local geography. Add the
   `GeoLocationLookup` interface and local MaxMind adapter, config, pinned
   dependency, and an ADR. Verify warm-cache capture, exclusions, saturation,
   queue-age bounds, safe failures, and privacy independently of analytics reads.
4. Implement `AnalyticsController`, `AnalyticsService`, `OwnershipGuard`, response
   DTOs, and safe exception mappings. Add `/api/analytics` and the architectural
   `/api/links/{id}/analytics` contract. Validate ownership before detail ranges,
   read a consistent snapshot, fill daily gaps, and bound breakdowns. Exercise
   two-account HTTP contracts, invalid inputs, fallbacks, and datastore failures.
5. Implement typed analytics reads and authenticated overview/detail routes in
   `frontend/src/features/analytics/` and `frontend/src/app/(account)/analytics/`.
   Add Recharts with a pinned lockfile resolution; use existing request/session
   recovery. Read the installed Next.js routing/client-boundary guides before
   coding. Build labeled range filters, range-keyed queries, charts with tables,
   all state treatments, account cache cleanup, and explicit snapshot freshness.
6. Add navigation and saved-link detail actions. Reuse the settings sidebar/drawer
   for analytics with correct active states, preserve the existing links shell,
   and retain its pagination for selection/lifetime counts. Record the analytics
   presentation in DESIGN.md without changing its palette or motion tier.
7. Verify end to end. Add `frontend/e2e/short-link-analytics.spec.ts` and targeted
   component/API tests; run the backend's real-datastore suite and all frontend
   quality commands. Check both themes at 320/768/1024/1440px, keyboard navigation,
   table equivalence, UTC boundaries, retries, and session changes. Run the
   existing sustained redirect protocol with capture enabled and measure
   recording delay; retain evidence and any environment limits honestly.
8. Update architecture/README, the landing analytics availability text, geo setup
   instructions, and verification evidence. Finish acceptance criteria and all
   active checkboxes, archive this plan/checklist under the feature spec, commit,
   push, and create the PR under the project workflow. Do not merge, delete a
   branch, deploy, or open a PR browser page.

## Verification commands for execution

Run the checks appropriate to each deliverable first; after application work,
use the full project gates:

```sh
./backend/mvnw -f backend/pom.xml -B -ntp clean verify
pnpm --dir frontend lint
pnpm --dir frontend format:check
pnpm --dir frontend typecheck
pnpm --dir frontend test --run
pnpm --dir frontend build
pnpm --dir frontend exec playwright test e2e/short-link-analytics.spec.ts
git diff --check
```

Backend integration tests require Docker. Frontend build may need network access
for fonts. Runtime/browser evidence uses existing local HTTPS fixture conventions;
no production credentials or services are used. Broaden browser checks to the
existing affected authentication/link suites after the analytics checks pass.
