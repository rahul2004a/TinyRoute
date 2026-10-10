# Link creation and redirection checklist

- Feature: `link-creation-and-redirection`
- Branch: `feature/link-creation-and-redirection`
- Specification: [approved feature spec](../docs/spec/link-creation-and-redirection/spec.md)
- Plan: [implementation plan](plan.md)
- Execution: inline `superpowers:executing-plans` with TDD; one independent final reviewer. No delegated implementation.
- Current state: **Tasks 1–9, including 7A, complete; the full local load protocol passed 2026-10-10. Original plan approved 2026-10-08; plain-counter revision approved 2026-10-08 ("ok do now"), and salted FF1 revision approved 2026-10-08 ("done"). Independent feature review is complete; Task 10 final evidence/readiness follow-up remains active before archival and PR readiness.**

## Approval gates and baseline

- [x] Read Notion workflow, AGENTS.md, FR/NFR requirements, architecture, ADRs, DESIGN.md and existing authentication contracts.
- [x] Check clean/current base, branch and active tasks; establish feature isolation without discarding unrelated work.
- [x] Approach approved, 2026-10-07.
- [x] Save and self-review written specification; committed as `72a45d2`.
- [x] Written specification approved, 2026-10-07.
- [x] Save/self-review `tasks/plan.md` and `tasks/todo.md`; AC coverage and interface consistency checked.
- [x] User approves saved plan/checklist, 2026-10-08 ("done"); inline implementation authorized.
- [x] Save and self-review user-requested Redis-counter revision in spec, architecture, plan, and checklist, 2026-10-08; retain completed history and Task 8 work.
- [x] User gives written approval of plain-counter spec/plan, 2026-10-08 ("ok do now").
- [x] Save/self-review subsequent salted, collision-free FF1 encoding proposal using the existing dependency.
- [x] User approves salted FF1 written spec/plan revision, 2026-10-08 ("done"); inline implementation authorized.

Baseline at `ecffb917a489cd18770127ede3b1a96f3a0debfb`: backend offline unit test command passed 63 tests; frontend Vitest passed 48 tests across 13 files. Mockito required execution outside the sandbox due to JVM attachment restrictions; no source workaround. Full integration/build/load evidence is still required for this feature.

## Implementation tasks

Each completion requires the plan's red evidence, green checks, recorded decisions and atomic commit. Mirror the detailed step checkboxes in plan.md; never check a task merely because its code exists.

- [x] 1. Domain invariants and validated configuration — destination/alias/expiry matrices, generated format, profile checks.
- [x] 2. Atomic creation and account-deletion fence — real conflict/race/non-reuse/after-lock expiry evidence.
- [x] 3. Creation budgets and degraded redirect throttling — cap/TTL/retry/isolation/bounded fallback/probe evidence.
- [x] 4. Authenticated creation HTTP — real JWT/CSRF/strict JSON/body/error/committed-result contract.
- [x] 5. Expiry-safe redirect cache/resolution — strict snapshots, original-read deadline, outage and cleanup compatibility.
- [x] 6. Public Spring redirect boundary — exact Location, state matrix, host/path/auth isolation, GET/HEAD/no-store.
- [x] 7. Accessible create/copy frontend — session/errors/timezone/no retry/clipboard/theme behavior.
- [x] 7A. HLD Redis-counter generation — fixed-key/salt FF1 Base62 encoding, V6 metadata, atomic allocation/recovery, alias/legacy compatibility, bounded safe failures, and affected regressions.
- [x] 8. Browser/live verification — disposable datastores, real flow, state/expiry/deletion, visual evidence.
- [x] 9. Server timing/load evidence — ≥200 creates and complete 100 rps/600-second measured run.
- [ ] 10. Independent review/full checks/completion readiness — all readiness conditions verified before archive and PR delivery.

## Acceptance checklist

- [x] AC-01: Signed-in create, unique copyable URL, one-click copy, committed next request works.
- [x] AC-02: Signed-out/invalid/revoked/stale/deleted credentials and missing CSRF cannot create; sign-in prompt.
- [x] AC-03: Invalid/non-HTTPS/self-host errors are clear and create no row.
- [x] AC-04: Alias validity/reservation/conflict, atomic counter and recovery concurrency, legacy compatibility, deleted/expired non-reuse.
- [x] AC-05: Optional expiry, future validation after locking, exact expiry even when cached.
- [x] AC-06: Anonymous exact case-sensitive stored destination with path/query/fragment/encoding.
- [x] AC-07: Unknown/case-mismatched/deleted/expired 404; disabled 403; no details or Location.
- [x] AC-08: Hourly creation cap and client redirect cap, accurate retry guidance, independent visitors.
- [x] AC-09: Redis cache fallback, bounded limiter fallback, counter/recovery safe failure, safe indeterminate-state error.
- [x] AC-10: Creation p95 <500 ms; complete 100 rps/600-second redirect run p95 <150/p99 <300 ms/errors <0.5%.
- [x] AC-11: Creation/deletion race fence and compatible bounded deletion cleanup.
- [x] AC-12: Keyboard/screen-reader/clipboard/error flow; both themes, reduced motion, all four widths.

## Review and verification gates

- [x] All relevant critical-path tests and full Maven clean verify pass; ≥70% line coverage.
- [x] Backend formatting/static analysis, frontend lint/format/typecheck/unit/build checks pass.
- [x] Full contract and live Playwright suites pass with disposable accounts/datastores.
- [x] Complete local load protocol and harness self-tests pass; raw report summarized in verification.md.
- [x] Sanitized UI evidence reviewed for layout, keyboard, focus, contrast and target sizes.
- [x] One independent final review completed; minor findings fixed and affected checks rerun. Final evidence/readiness follow-up is pending.
- [x] No analytics/events/click increments, extra production endpoints, secrets, unrelated changes or production provisioning.
- [x] Documentation and acceptance mapping reflect actual results and material limitations.
- [ ] Every implementation task/AC complete; documentation links checked and records ready for archive.

## Post-completion delivery record

Archive/push/PR follow the completed implementation checklist, avoiding unfinished shipping checkboxes inside an archived task file. Current delivery state: implementation, browser/full checks, independent feature review and local sustained-load acceptance complete; Task 10 final evidence/readiness follow-up remains active. Keep PR #15 draft until that audit passes. The authorized sequence is archive the fully completed files with adjusted links, commit, push without force, update the template-based PR, then return its URL and evidence/limitations. No merge, branch deletion or browser launch. Record any delivery blocker accurately.

## Durable execution ledger

Historical delivery checkpoint (2026-10-08): pushed the reviewed feature branch and created
[draft PR #15](https://github.com/rahul2004a/TinyRoute/pull/15). Implementation
and review fixes are committed through `2f9bdfa`; Tasks 9–10 and AC-10 remain
open because the required loopback alias and sustained-load report are absent.
Active task files are retained. No merge, deletion, force-push or browser launch.

| Date       | Task/state                                | Red → green / verification                                                                                                             | Commit / decision                          |
| ---------- | ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------ |
| 2026-10-07 | Specification approved; planning complete | Ten tasks/twelve AC mappings; interface/placeholder/local-link/whitespace review; plan/checklist Prettier pass; no application changes | `72a45d2` spec; saved plan awaits approval |

| 2026-10-08 | Task 1 complete | Red: missing domain types; green: 90 focused tests, 0 failures/errors; Maven Spotless applied | HTTPS/host/alias/expiry invariants; injectable clock; no schema change |

| 2026-10-08 | Task 2 complete | Red: missing create/repository methods; green: 6 unit + 9 PostgreSQL/Redis integration tests | Task 1 `ede8684`; corrected test catch outside rollback boundary; native conflict-safe insertion and owner fence |

| 2026-10-08 | Task 3 complete | Red: missing limiter/backoff methods; green: 14 focused unit tests + Redis store integration | Task 2 `a571164`; unchanged auth keys; redirect-only bounded fallback; ceil retry |

| 2026-10-08 | Task 4 complete | Red: six HTTP failures (missing endpoint); green: 16 security/datastore integration + 7 error/body-limit unit tests | Task 3 `554d671`; strict DTO-local Jackson decoding; shared real-security fixture; safe commit error mapping |

| 2026-10-08 | Task 5 complete | Red: missing resolver/cache types; boundary regressions observed stale-cache 302 and expiry-during-put 302 before fixes. Green: 20 unit + 5 datastore/rollback integration tests | Task 4 `bd88d82`; check time after external calls, preserve original deadline; actual creation rollback returns 503 with no row; parallel cache-outage backoff verified |

| 2026-10-08 | Task 6 complete | Red: missing matcher, JWT auth-isolation failure, seven HTTP failures; encoded-namespace regression three failures. Green: 26 security unit + 20 creation/public/perimeter integration tests | Task 5 `a9e0269`; fixed double-header visitor fixture; shared raw matcher excludes decoded private namespaces; literal malformed nested paths get fixed 404; no analytics |

| 2026-10-08 | Task 7 complete | Red: three absent feature suites + API-error/navigation failures; prior-result loss regression observed. Green: full Vitest 73 tests, then 26 focused feature tests after three additional checks; lint/typecheck pass | Task 6 `16e23e1`; retain last successful result after failure; local expiry conversion; no POST retry; awaited clipboard/manual fallback; corrected assertion for TanStack context |

| 2026-10-08 | Task 8 partial, paused for allocation revision | Contract link Playwright: 18 tests passed; disposable fixture/isolation integration: 3 tests passed. 24 UI artifacts captured, eight inspected; dev indicator overlays still need clean captures. Live application did not launch; no live/load completion claimed | Task 7 `613325f`; preserve uncommitted fixture/browser work. Next after revision approval: Task 7A, then isolated-port/test-compile live setup and remaining Task 8 checks |

| 2026-10-08 | Counter revision saved, awaiting written approval | HLD confirms Redis global counter; original code uses random eight-character candidates with conflict-safe INSERT and no availability SELECT. Self-review covers V6, recovery/collision/capacity/error tests and AC mappings. Four-document Prettier check, local links/fenced blocks/numeric range check, placeholder scan, and git diff --check passed; no generator code changes | Task 7A replaces generator after approval. Committed numeric metadata excludes aliases/legacy rows from recovery floor; permanent PostgreSQL uniqueness protects published codes after reset/restore |

| 2026-10-08 | Salt/uniqueness revision saved | User rejected truncated HMAC to avoid generated-code collisions. Existing Bouncy Castle 1.86 FF1 API verified read-only; AES-FF1 proposed as a fixed-key/salt eight-digit radix-62 permutation. No generator code/new tests added; prior attempted test patch was rejected atomically before changes or any test run | Awaiting written spec/plan approval; retain Task 8 work. Stable encoding configuration required; PostgreSQL protects alias/legacy/recovery races |

| 2026-10-08 | Salted FF1 revision approved; Task 7A verification in progress | User written approval "done". Red: focused tests failed compilation on missing encoder/counter/recovery contracts. Green: 54 focused unit tests and 37 real PostgreSQL/Redis/security regressions; 100 concurrent initialization and allocation calls each remain unique. Migration-upgrade/configuration regressions running next | V6 nullable metadata, fixed AES-FF1 key/salt, atomic non-TTL Redis counter, bounded conflict recovery; preserve Task 8 work |

| 2026-10-08 | Task 7A complete | Green: 54 encoder/counter/service units; 37 real datastore/security regressions; 64 configuration/domain/error units plus one V5→V6 migration integration; final explicit ten-allocation/no-eleventh regression: 8 service units. All pass, no failures/errors. Spotless applied and diff whitespace checked | Redis recovery retains deleted/expired allocations; alias and legacy NULL metadata remain intact; fixed key/salt required. Resume Task 8 on isolated ports |

| 2026-10-08 | Task 8 green; Task 9 support in progress | Full contracts: 27 Playwright tests; new eight theme/viewport contrast checks (≥4.5:1), 24 clean screenshots. Full live auth + links: 2 tests passed in 1.1 min. Fixture/isolation: 4 integration tests. Live stale-CSRF red → new regression red → 23 focused API/form tests green; link requests bootstrap fresh CSRF once, no POST retry | Isolated 3001/8444 and disposable datastores; production auth unchanged. Load source alias awaits user admin setup. Timing red → 3 filter/6 Node/14 CI helper checks green; full checks pending |

| 2026-10-08 | Full-check failures under systematic debugging | Frontend lint/format/typecheck/build, full 77 Vitest tests and 27 Playwright contracts passed. Full Maven run exposed fixture rows blocking legacy auth cleanup, plus cached test-context default pools exhausting the shared PG connection limit. Fix: remove only fixture accounts after its class; test datastores use max pool 4/min idle 0 | No production pool/security changes. Rerun full Maven after observed integration red; load alias still pending |

| 2026-10-08 | Full gates green; Task 8 ready for checkpoint | Full Maven clean verify: 238 unit + 106 integration, zero failures/errors/skips; JaCoCo 90.27%; SpotBugs/FindSecBugs and Spotless pass. Frontend lint/format/typecheck/77 Vitest/build/27 contracts pass. Full live 2 journeys passed; 6 Node + 14 CI helpers pass | Scope-safe fixture teardown and test-only max pool 4/min idle 0 fixed observed full-suite failures. No versions or production security/pools changed. Load alias and independent final review outstanding |

| 2026-10-08 | Independent final review complete; minor fixes verified | Reviewer inspected base through `26dfcda`: no critical implementation defects; independently confirmed 344 backend tests and 90.27% coverage. Report preflight regression red on missing export → all 9 Node tests green; output directory/file prepared before traffic, old evidence preserved, bad targets rejected. Current approval/Task 8 status reconciled | Task 8/check support checkpoint `26dfcda`. Full sustained load remains blocked by unavailable macOS loopback alias; retain active tasks and draft readiness. No product/security policy changes |

Add rows during approved execution with exact command outcomes, test counts, failure cause/fix, review findings and commit IDs. Do not record credentials, cookies, raw client addresses/hashes, or real destination/owner data. Before compaction, record current task/step, completed commits, outstanding failures and the next concrete action.

2026-10-09 PR CI follow-up: run `37782401827` at `1cf4e04` failed before the
creation request with `UnfinishedStubbingException` while spying on the final
transaction-manager commit method. Backend source was identical to the previous
successful CI run. Replacing the spy with an owner-scoped deferred PostgreSQL
failure preserves the real commit-boundary contract (NFR-CON-01; NFR-REL-02).
Observed regression red without the fault fixture: expected 503, received 201.
Focused green: four handler units and one commit integration test, with no row,
Location, destination, owner or private cause leakage. Full coverage-enabled
`spotless:check clean verify` passed: 238 unit + 106 integration tests, zero
failures/errors/skips, 90.27% line coverage, zero SpotBugs findings. Independent
review found no issues in the test-only change. PR CI rerun follows the fix commit. No production
code, schema migration, dependencies or acceptance scope changed.

## Material limitations to retain

2026-10-09 completion run: the macOS alias remained absent. The approved protocol
is executing in an isolated native Linux Docker namespace, using Java
21.0.12.1/Node 24.21.0 and disposable PostgreSQL 17.2/Redis 7.4.2. All normal
limits, TLS verification and the exact trusted source are unchanged. The first
cold Redis health connection timed out; a later health request returned 200 UP
without configuration changes. All 200 creates succeeded (server p95 16.24 ms).
The first full measured run failed: 60,000 offered, 59,954 issued/completed;
46 scheduling drops, maximum lateness 203.26 ms. Issued requests all returned
302, server p95/p99 5.68/18.41 ms, zero errors. This report does not satisfy AC-10.
A 120-second diagnostic reproduced scheduling stalls; measured GC pauses did
not account for them. Separate idle Linux and host timers had no >50 ms stalls.
The reviewer identified unnecessary retention of completed driver promises.
The driver now keeps only active promises, with deterministic cap/drain and
outcome-accounting tests: missing-export red, 11 green harness tests, 14 green
CI helper tests, independent follow-up review without findings. CPU/TLS tracing localized pauses to native handshake initialization. Context,
trust-store, group and session-cache experiments did not fix sustained issuance
and were not adopted. The native host driver will use an owned local TCP
pass-through proxy: end-to-end TLS, exact backend proxy source and all caps/
thresholds unchanged. The native-host 60-second diagnostic passed 6,000/6,000 requests at 100 rps,
zero errors/drops/late arrivals, maximum lateness 15.67 ms. Health and guarded
fixture preflight passed; the independent reviewer inspected the byte-only
relay and found no issues. The fresh full run remains required. No completion
claim is based on the rejected diagnostics.

PR CI follow-up: frontend CI passed; backend pre-check initially stopped on the
known public FF1 fixture in `.env.example`. Gitleaks 8.30.1 locally reproduced
the one finding (red); an exact historical fingerprint exception for the verified
sequential-byte fixture made the full-history scan green. No broad scan exclusions
or production policy changes. The rerun at `22f241b` passed all backend and
frontend CI gates, including CodeQL and dependency/image scans.

Checklist reconciliation: Step 10.1 is complete because its full checks and
acceptance mapping are verified. Task 9's actual load/evidence steps (9.4/9.5),
final readiness (10.3) and task finalization (10.4) remain open. AC-10 has no
passing complete report yet; this dependency prevents marking Task 10 complete or
archiving the active task files.

Final-review follow-up confirmed the report preflight fix and identified one
remaining stale Task 8 paragraph in the spec; that paragraph is now reconciled.
Review fix commit: `db22899`. Node self-tests (9), CI helper tests (14), touched
file Prettier checks and `git diff --check` pass. The loopback alias is still absent;
no sustained-load completion is claimed.

- Local Testcontainers measurements establish local performance only. Hostinger/nearby Supabase latency remains a production release verification item; this feature does not authorize deployment.
- Redis limiter outages use one backend process's bounded, best-effort redirect window; recovery/restart can start a fresh window. Auth/creation remain fail-closed.
- Link state changes have the approved maximum five-second snapshot bound; expiry is checked at the exact instant without grace.
- Ambiguous creation network failures require deliberate resubmission and may already have committed; no idempotency-key contract is introduced.
- Implemented salted FF1 codes hide the counter sequence but remain finite public identifiers. The generated-code bijection requires fixed key/salt/alphabet/width; rotation is outside this feature. Alias/legacy conflicts still require PostgreSQL uniqueness. Missing/stale counters recover from committed numeric metadata; malformed/unavailable counters fail creation safely. Allocation/rollback gaps are permitted; published codes are never reused.

2026-10-09 native-host full attempt failed strict issuance: 200 creates p95
23.30 ms, 59,995/60,000 redirects, five late drops (max 125.26 ms), zero HTTP/
transport errors; server p95/p99 8.29/69.63 ms. The failed raw report and runtime/
relay setup are retained explicitly as failed evidence. A worker-pool experiment
performed worse and was discarded; no worker implementation or TLS-option
experiment remains in the working tree. The committed driver remains `d3124b7`
(bounded retention and local pass-through support), with 11 passing self-tests.
A quiet uninterrupted host or isolated test machine is needed for a new full run;
the user has been asked. Tasks 9–10/AC-10 stay active, PR stays draft, no archive.
Independent follow-up review recomputed the failed report's percentiles, counts
and checksums, checked the saved setup and confirmed no private data leakage;
no findings. This review approves recording progress, not feature completion.
The two owned verification containers were stopped and removed after the run;
the owner's Compose services were left untouched.

2026-10-10 quiet-host retry: creation passed (200 successes, server p95 23.37 ms),
but measured redirects failed: 55,998/60,000 issued/completed, 4,002 late drops,
maximum scheduling lateness 39,843.27 ms, two transport failures, server p95/p99
5.59/27.39 ms and incomplete server-sample accounting. macOS power history records
`Clamshell Sleep` at 15:34:16 IST and wake at 15:34:56 IST, matching the large stall.
The failed private report and sanitized sleep correlation are retained. The Mac
is now on AC power with the lid open; the user was told to keep it open throughout
the fresh-data retry. `caffeinate -i` and all protocol/limit/TLS thresholds remain
unchanged. No acceptance completion or archival is claimed before a passing report.

2026-10-10 Task 9 complete at source checkpoint `a44929d`: all 200 counter-generated
creates returned 201 (server p95 35.78 ms); all 60,000 redirects were issued,
completed and timed over 600.003 seconds (99.999 rps), each returning 302.
Server p95/p99 8.48/40.59 ms; client p95/p99 17.81/115.36 ms; zero transport/
HTTP errors, dropped or excessively late arrivals, no collector overflow; maximum
lateness 77.42 ms. A separate Python audit recomputed percentiles and checked all
counts, finite samples, status distributions and the harness hash before saving
compressed numeric evidence and refreshed runtime/hardware metadata. TLS health
passed and wrong-hostname/untrusted-certificate probes were rejected. No sleep
event overlapped the passing attempt. Default caps, trust, TLS, cache policy and
thresholds stayed unchanged. Eleven harness tests pass; prior source-identical
full backend/frontend checks remain applicable. Final evidence/readiness review
and Task 10 completion follow before archival.

2026-10-10 independent final evidence review: the reviewer recomputed all server/
client percentiles, counts, throughput and lateness; checked passing/failed report
hashes, harness identity, gzip metadata, privacy, setup syntax and local links.
No critical/important/product/harness findings. Minor stale status text in spec,
architecture and plan was reconciled. Fresh checks: 11 Node harness tests and
14 CI helper tests pass; touched-file Prettier, local links and whitespace checks
pass. Backend/frontend product source is unchanged from the fully verified
`4864ecc`; harness is unchanged from `d3124b7`; all eight CI checks passed on
`a44929d`. Passing-attempt backend/relay containers were removed; the owner's
Compose services were untouched. Local AC-10 is supported; production topology
performance remains a release limitation. Finalize the task records and archive
after the final readiness/link audit.
