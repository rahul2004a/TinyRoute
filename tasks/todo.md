# Link creation and redirection checklist

- Feature: `link-creation-and-redirection`
- Branch: `feature/link-creation-and-redirection`
- Specification: [approved spec](../docs/spec/link-creation-and-redirection/spec.md)
- Plan: [implementation plan](plan.md)
- Execution: inline `superpowers:executing-plans` with TDD; one independent final reviewer. No delegated implementation.
- Current state: **planning complete; awaiting saved-plan approval. No application implementation started.**

## Approval gates and baseline

- [x] Read Notion workflow, AGENTS.md, FR/NFR requirements, architecture, ADRs, DESIGN.md and existing authentication contracts.
- [x] Check clean/current base, branch and active tasks; establish feature isolation without discarding unrelated work.
- [x] Approach approved, 2026-10-07.
- [x] Save and self-review written specification; committed as `72a45d2`.
- [x] Written specification approved, 2026-10-07.
- [x] Save/self-review `tasks/plan.md` and `tasks/todo.md`; AC coverage and interface consistency checked.
- [ ] User approves saved plan/checklist; only then start implementation.

Baseline at `ecffb917a489cd18770127ede3b1a96f3a0debfb`: backend offline unit test command passed 63 tests; frontend Vitest passed 48 tests across 13 files. Mockito required execution outside the sandbox due to JVM attachment restrictions; no source workaround. Full integration/build/load evidence is still required for this feature.

## Implementation tasks

Each completion requires the plan's red evidence, green checks, recorded decisions and atomic commit. Mirror the detailed step checkboxes in plan.md; never check a task merely because its code exists.

- [ ] 1. Domain invariants and validated configuration — destination/alias/expiry matrices, generated format, profile checks.
- [ ] 2. Atomic creation and account-deletion fence — real conflict/race/non-reuse/after-lock expiry evidence.
- [ ] 3. Creation budgets and degraded redirect throttling — cap/TTL/retry/isolation/bounded fallback/probe evidence.
- [ ] 4. Authenticated creation HTTP — real JWT/CSRF/strict JSON/body/error/committed-result contract.
- [ ] 5. Expiry-safe redirect cache/resolution — strict snapshots, original-read deadline, outage and cleanup compatibility.
- [ ] 6. Public Spring redirect boundary — exact Location, state matrix, host/path/auth isolation, GET/HEAD/no-store.
- [ ] 7. Accessible create/copy frontend — session/errors/timezone/no retry/clipboard/theme behavior.
- [ ] 8. Browser/live verification — disposable datastores, real flow, state/expiry/deletion, visual evidence.
- [ ] 9. Server timing/load evidence — ≥200 creates and complete 100 rps/600-second measured run.
- [ ] 10. Independent review/full checks/completion readiness — all readiness conditions verified before archive and PR delivery.

## Acceptance checklist

- [ ] AC-01: Signed-in create, unique copyable URL, one-click copy, committed next request works.
- [ ] AC-02: Signed-out/invalid/revoked/stale/deleted credentials and missing CSRF cannot create; sign-in prompt.
- [ ] AC-03: Invalid/non-HTTPS/self-host errors are clear and create no row.
- [ ] AC-04: Alias validity/reservation/conflict, concurrent uniqueness, deleted/expired non-reuse.
- [ ] AC-05: Optional expiry, future validation after locking, exact expiry even when cached.
- [ ] AC-06: Anonymous exact case-sensitive stored destination with path/query/fragment/encoding.
- [ ] AC-07: Unknown/case-mismatched/deleted/expired 404; disabled 403; no details or Location.
- [ ] AC-08: Hourly creation cap and client redirect cap, accurate retry guidance, independent visitors.
- [ ] AC-09: Redis cache fallback, bounded limiter fallback, safe indeterminate-state error.
- [ ] AC-10: Creation p95 <500 ms; complete 100 rps/600-second redirect run p95 <150/p99 <300 ms/errors <0.5%.
- [ ] AC-11: Creation/deletion race fence and compatible bounded deletion cleanup.
- [ ] AC-12: Keyboard/screen-reader/clipboard/error flow; both themes, reduced motion, all four widths.

## Review and verification gates

- [ ] All relevant critical-path tests and full Maven clean verify pass; ≥70% line coverage.
- [ ] Backend formatting/static analysis, frontend lint/format/typecheck/unit/build checks pass.
- [ ] Full contract and live Playwright suites pass with disposable accounts/datastores.
- [ ] Complete local load protocol and harness self-tests pass; raw report summarized in verification.md.
- [ ] Sanitized UI evidence reviewed for layout, keyboard, focus, contrast and target sizes.
- [ ] One independent final review completed; blocking findings fixed and stale checks rerun.
- [ ] No analytics/events/click increments, extra endpoints, secrets, unrelated changes or production provisioning.
- [ ] Documentation and acceptance mapping reflect actual results and material limitations.
- [ ] Every implementation task/AC complete; documentation links checked and records ready for archive.

## Post-completion delivery record

Archive/push/PR follow the completed implementation checklist, avoiding unfinished shipping checkboxes inside an archived task file. Current delivery state: awaiting implementation and verification. The authorized sequence is archive the fully completed files with adjusted links, commit, push without force, create/update the template-based PR, then return its URL and evidence/limitations. No merge, branch deletion or browser launch. Record any delivery blocker accurately.

## Durable execution ledger

| Date       | Task/state                                | Red → green / verification                                                                                                             | Commit / decision                          |
| ---------- | ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------ |
| 2026-10-07 | Specification approved; planning complete | Ten tasks/twelve AC mappings; interface/placeholder/local-link/whitespace review; plan/checklist Prettier pass; no application changes | `72a45d2` spec; saved plan awaits approval |

Add rows during approved execution with exact command outcomes, test counts, failure cause/fix, review findings and commit IDs. Do not record credentials, cookies, raw client addresses/hashes, or real destination/owner data. Before compaction, record current task/step, completed commits, outstanding failures and the next concrete action.

## Material limitations to retain

- Local Testcontainers measurements establish local performance only. Hostinger/nearby Supabase latency remains a production release verification item; this feature does not authorize deployment.
- Redis limiter outages use one backend process's bounded, best-effort redirect window; recovery/restart can start a fresh window. Auth/creation remain fail-closed.
- Link state changes have the approved maximum five-second snapshot bound; expiry is checked at the exact instant without grace.
- Ambiguous creation network failures require deliberate resubmission and may already have committed; no idempotency-key contract is introduced.
