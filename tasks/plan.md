# Implementation Plan: account-authentication

## Overview

Deliver TinyRoute MVP account authentication: email/password registration with email OTP verification, password and Google sign-in, persisted browser sessions, sign-out, password reset, and account deletion. The backend remains the authority for authentication, ownership, redirect and session policy; Next.js is a credentialed HTTP client only. This plan implements the approved [feature specification](../docs/spec/account-authentication/spec.md) and [public contracts](../docs/spec/account-authentication/contracts.md).

## Preconditions and scope

- The repository currently contains documentation only. Tasks 1–3 create the smallest locked-stack foundation; they do not add unrelated product features.
- The current branch is \`feature/account-authentication\`, and both active task files were empty before this plan was written.
- Scope traces to FR-ACC-01 through FR-ACC-05, FR-ABS-02, FR-CRE-02, and their cited NFRs.
- Each implementation task is S or M (about five files or fewer). Use test-driven implementation and make atomic commits only after focused verification passes.

## Architecture decisions

- Keep the locked \`Controller → Service → Repository/Store/Adapter\` boundary. \`AuthController\` translates HTTP; \`AuthService\` owns transactions and policy; \`Jpa*\` repositories own PostgreSQL; \`Redis*\` adapters own Redis.
- Keep rate limiting at the controller boundary through \`RateLimitService\` and \`RateLimitStore\`. Never put backend policy, ownership, public redirects or tokens in Next.js.
- Use host-only secure HTTP-only access/refresh cookies. \`GET /api/auth/csrf\` issues a no-store CSRF value, which frontend memory sends in \`X-CSRF-TOKEN\` for mutations.
- Retain normalized identity data and the Google \`sub\`, but never Google access/refresh tokens or extended profile data.
- Use hashed, opaque, one-time password-reset tokens; reset URLs carry the raw token only in a fragment.
- Account deletion calls \`LinkService.tombstoneOwnedLinks(userId)\`; authentication neither accesses link repositories nor recreates redirect policy.

## Dependency graph

\`\`\`text
backend bootstrap ─┬─ database/profile baseline ── identity persistence
                   ├─ JWT/password primitives ─────┬─ Redis stores/rate limits
frontend bootstrap ─┴─ HTTP-client shell            └─ Security perimeter
                                                          │
        registration + OTP (API → UI) ────────────────────┤
        password sign-in (API → UI) ──────────────────────┤
        refresh and sign-out (API → UI) ──────────────────┤
        Google OIDC (API → UI) ───────────────────────────┤
        password reset (API → UI) ────────────────────────┘
                                                          │
                   LinkService tombstone contract ─ account deletion (API → UI)
                                                          │
                         contract, security, performance and end-to-end evidence
\`\`\`

## Task list

### Phase 1: executable foundation and security boundary

- [x] Task 1: Bootstrap Spring Boot and its focused test harness.
- [x] Task 2: Bootstrap strict Next.js and its typed HTTP-client shell.
- [ ] Task 3: Add Compose-only PostgreSQL/Redis and externalized dev/prod configuration.
- [ ] Task 4: Add user and authentication-identity persistence.
- [ ] Task 5: Add Argon2id and fixed JWT primitives.
- [ ] Task 6: Add Redis refresh-session, JWT-revocation and rate-limit adapters.
- [ ] Task 7: Wire JWT filtering, exact CORS, CSRF and cookie helpers.

### Checkpoint: security foundation

- [ ] Backend/frontend build and tests run from a clean checkout.
- [ ] Flyway and Redis adapters run against Compose services.
- [ ] A browser can obtain CSRF, while cross-origin or missing-CSRF mutations are rejected.
- [ ] Human reviews origin, local HTTPS, trusted-proxy and key-management configuration before credential flows.

### Phase 2: local account lifecycle

- [ ] Task 8: Deliver registration, verification OTP and resend APIs.
- [ ] Task 9: Deliver accessible registration/OTP UI.
- [ ] Task 10: Deliver password sign-in and current-session APIs.
- [ ] Task 11: Deliver login UI and session bootstrap.
- [ ] Task 12: Deliver atomic refresh rotation and sign-out APIs.
- [ ] Task 13: Deliver browser refresh and sign-out UX.

### Checkpoint: local accounts

- [ ] New users can register, verify, sign in, reload, refresh and sign out end-to-end.
- [ ] Invalid/expired OTP, stale CSRF, wrong password, refresh reuse and rate-limit cases return documented generic errors.
- [ ] Logs, browser storage, mail tests and snapshots contain no passwords, OTPs, token values or OAuth artifacts.
- [ ] Human reviews the completed local-account flow.

### Phase 3: federated sign-in and recovery

- [ ] Task 14: Deliver Google OIDC start/callback validation and identity creation/sign-in.
- [ ] Task 15: Deliver the Google handoff and callback result UI.
- [ ] Task 16: Deliver password-reset request/completion APIs and mail behavior.
- [ ] Task 17: Deliver password-reset UI.

### Checkpoint: federated and recovery flows

- [ ] OIDC tests prove state, nonce, PKCE, issuer/audience/signature, verified email, collision and callback failure behavior.
- [ ] Password reset works once and fails safely on expiry/replay without raw-token URL/log disclosure.
- [ ] Human reviews Google and email configuration before any non-local environment.

### Phase 4: deletion and release evidence

- [ ] Task 18: Deliver account deletion through the LinkService tombstone contract.
- [ ] Task 19: Deliver account-deletion UI and client cleanup.
- [ ] Task 20: Complete observability, contract, security, performance and end-to-end evidence.

### Checkpoint: implementation review

- [ ] Every task acceptance criterion and the project Definition of Done is complete.
- [ ] Relevant Maven, frontend lint/typecheck/test, and Playwright checks pass.
- [ ] Docs are current. Only then archive completed active task files into \`docs/spec/account-authentication/\`.

## Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Untrusted forwarded headers bypass limits. | High | Trust only configured proxy ranges and integration-test direct/forwarded requests. |
| Incomplete OIDC validation or automatic email linking compromises accounts. | High | Validate state, nonce, PKCE, issuer/audience/signature; reject collisions. |
| Refresh races/reuse leave stolen sessions live. | High | Atomic rotate/revoke semantics and concurrent/reuse tests. |
| Deployment origins/cookies differ from local. | High | Externalize origin/HTTPS settings and test in a real browser at checkpoints. |
| Mail behavior leaks accounts or secrets. | High | Generic responses, bounded attempts, fake mail tests, hashed raw secrets only. |
| Link feature is unavailable for deletion. | High | Treat \`LinkService.tombstoneOwnedLinks(userId)\` as a hard dependency; do not duplicate its policy. |
| Cache eviction after deletion fails. | High | Tombstone first, fail closed, queue/retry eviction and expose failures. |

## Open decisions to close at the first checkpoint

- Which exact frontend/API origins and local HTTPS mechanism will be used?
- Which proxy/network ranges may supply forwarded client-IP headers?
- Which transactional email provider, sender and credentials will production use?
- Will link management expose \`LinkService.tombstoneOwnedLinks(userId)\` before Task 18? If not, Task 18 is blocked.

## Parallelization

- Tasks 1–7 are sequential because they establish the shared topology/security boundary.
- Once Task 7 is accepted, API work must precede its UI work; Tasks 8, 10, 12, 14 and 16 can be prepared separately if migrations/contracts do not overlap.
- Task 18 cannot start without the link-service tombstone contract. Task 20 runs only against the fully integrated system.

## Planning verification

- [x] Active plan and todo files contained no incomplete work before initialization.
- [x] Every task in \`tasks/todo.md\` has acceptance criteria, dependencies, focused verification, likely files and S/M sizing.
- [x] Dependencies are bottom-up and checkpoints follow each coherent capability group.
- [ ] Human has reviewed and approved the plan before coding begins.
