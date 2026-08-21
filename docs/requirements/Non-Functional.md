# Non-Functional Requirements

## 1. Scope

Quality targets for TinyRoute as a personal portfolio project. They are deliberately modest and, more importantly, **verifiable by one person on a small budget** — every number here can be demonstrated with a load test, a script, or a screenshot.

Targets are stated for a single-region deployment on modest hardware. They are engineering goals, not commitments to any customer.

**How to read this**

- **Priority:** `Must` / `Should` / `Could`, within the stated release.
- **Release:** `MVP` (first working deployment) or `V1` (the version to show off).
- Latency is measured server-side, excluding the visitor's network and the destination site's load time.
- Every number below is an assumption chosen for this project, not a business requirement.

## 2. Planning Baseline

| Metric                               |              Target |
| ------------------------------------ | ------------------: |
| Registered users                     |               1,000 |
| Stored links                         |             100,000 |
| Redirects per month                  |           1,000,000 |
| Sustained redirect throughput        | 100 requests/second |
| Peak redirect throughput (load test) | 300 requests/second |
| Link creations per day               |                 500 |

These are sized so the project is interesting to discuss without needing expensive infrastructure. A demo that holds 100 requests/second on a small instance is a better story than an untested claim of 10,000.

## 3. Performance

| ID         | Requirement          | Target                                                                                               | Priority | Release |
| ---------- | -------------------- | ---------------------------------------------------------------------------------------------------- | -------- | ------- |
| NFR-PER-01 | Redirect latency     | p95 under 150 ms and p99 under 300 ms server-side, measured under the sustained throughput baseline. | Must     | MVP     |
| NFR-PER-02 | Page and API latency | p95 under 500 ms for create, list, and management operations.                                        | Must     | MVP     |
| NFR-PER-03 | Sustained load       | Holds 100 redirects/second for 10 minutes while meeting NFR-PER-01, with under 0.5% errors.          | Must     | MVP     |
| NFR-PER-04 | Peak load            | Survives 300 redirects/second for 1 minute without crashing or needing a restart.                    | Should   | V1      |
| NFR-PER-05 | Documented results   | Load test results are reproducible from a committed script and recorded in the repository.           | Must     | V1      |

## 4. Availability and Reliability

| ID         | Requirement          | Target                                                                                                                         | Priority | Release |
| ---------- | -------------------- | ------------------------------------------------------------------------------------------------------------------------------ | -------- | ------- |
| NFR-AVL-01 | Uptime               | 99.5% monthly, best-effort (about 3.6 hours of downtime allowed). Honest for a single-region hobby deployment with no on-call. | Must     | MVP     |
| NFR-AVL-02 | Health check         | An endpoint reports whether the service and its datastore are reachable, suitable for uptime monitoring.                       | Must     | MVP     |
| NFR-AVL-03 | Uptime monitoring    | An external monitor checks the service at least every 5 minutes and alerts the owner by email on failure.                      | Should   | V1      |
| NFR-REL-01 | Redirect correctness | 100% of test codes resolve to their recorded destination. A code never resolves to another link's destination.                 | Must     | MVP     |
| NFR-REL-02 | Fail closed          | If a link's state cannot be determined, the service shows an error rather than redirecting to a guessed destination.           | Must     | MVP     |
| NFR-REL-03 | Safe retries         | Repeating a disable, enable, or delete request produces the same end state and never affects another link.                     | Should   | MVP     |
| NFR-REL-04 | Graceful restart     | Deployments and restarts do not lose in-flight requests or leave links in a broken state.                                      | Should   | V1      |

## 5. Scalability

| ID         | Requirement | Target                                                                                                              | Priority | Release |
| ---------- | ----------- | ------------------------------------------------------------------------------------------------------------------- | -------- | ------- |
| NFR-SCL-01 | Data volume | Meets latency targets with 100,000 links and 1,000,000 recorded clicks in the datastore.                            | Must     | V1      |
| NFR-SCL-02 | Headroom    | Runs at the sustained baseline using under 70% of provisioned CPU and memory.                                       | Should   | V1      |
| NFR-SCL-03 | Growth path | A written note explains what would need to change to handle 10x traffic. This is documentation, not implementation. | Should   | V1      |

## 6. Security

| ID         | Requirement         | Target                                                                                                                              | Priority | Release |
| ---------- | ------------------- | ----------------------------------------------------------------------------------------------------------------------------------- | -------- | ------- |
| NFR-SEC-01 | HTTPS everywhere    | All traffic is served over HTTPS; plain HTTP redirects to HTTPS.                                                                    | Must     | MVP     |
| NFR-SEC-02 | Password storage    | Passwords are stored using a current password-hashing algorithm with per-user salt. Never plaintext or a fast general-purpose hash. | Must     | MVP     |
| NFR-SEC-03 | Secrets handling    | No credentials, API keys, or tokens in the repository. All secrets come from the environment, with a documented `.env.example`.     | Must     | MVP     |
| NFR-SEC-04 | Ownership checks    | Every management operation verifies the caller owns the link. Cross-account access attempts are covered by automated tests.         | Must     | MVP     |
| NFR-SEC-05 | Injection safety    | User input is never interpolated into queries or rendered as raw HTML.                                                              | Must     | MVP     |
| NFR-SEC-06 | Token security      | JWT access tokens and refresh-session tokens are sent only in HttpOnly, Secure, SameSite cookies. Access JWTs expire within 15 minutes. Refresh sessions expire after 30 days of inactivity and are stored in Redis using only a hash of the refresh token. Raw tokens are never stored in PostgreSQL or Redis. | Must     | MVP     |
| NFR-SEC-07 | Dependency scanning | An automated scan runs on every push; known critical vulnerabilities are fixed or explicitly noted before merge.                    | Should   | V1      |
| NFR-SEC-08 | Safe errors         | Error pages and API responses never leak stack traces, queries, or internal paths.                                                  | Must     | MVP     |
| NFR-SEC-09 | Token revocation    | The current refresh session is revoked on logout. All refresh sessions and issued access tokens for a user are invalidated after password reset or account deletion. A revoked token must not authorize protected requests. | Must     | MVP     |
| NFR-SEC-10 | JWT signing keys    | JWT signing keys come from environment-managed secrets, are not committed to the repository, and support key rotation through a key identifier (`kid`). | Must     | MVP     |

## 7. Data Durability, Backup, and Recovery

| ID         | Requirement      | Target                                                                                                        | Priority | Release |
| ---------- | ---------------- | ------------------------------------------------------------------------------------------------------------- | -------- | ------- |
| NFR-BAK-01 | Automated backup | The datastore is backed up automatically at least once per day.                                               | Must     | MVP     |
| NFR-BAK-02 | RPO              | At most 24 hours of data loss. Matched to daily backups; anything tighter is not worth the cost here.         | Must     | MVP     |
| NFR-BAK-03 | RTO              | Service restored within 4 hours of the owner starting recovery, following a written runbook.                  | Must     | MVP     |
| NFR-BAK-04 | Restore test     | A restore into a clean environment is performed and documented at least once before calling the project done. | Must     | V1      |
| NFR-BAK-05 | Retention        | At least 7 days of backups are retained.                                                                      | Should   | V1      |

## 8. Consistency

| ID         | Requirement                    | Target                                                                                        | Priority | Release |
| ---------- | ------------------------------ | --------------------------------------------------------------------------------------------- | -------- | ------- |
| NFR-CON-01 | New link works immediately     | A newly created short URL resolves on the very next request.                                  | Must     | MVP     |
| NFR-CON-02 | State changes take effect fast | Disable, enable, and delete stop or resume redirects within 5 seconds, including any caching. | Must     | MVP     |
| NFR-CON-03 | Analytics lag                  | Click counts may lag actual clicks by up to 1 minute; the UI does not claim to be real-time.  | Should   | V1      |

## 9. Observability

| ID         | Requirement      | Target                                                                                                                    | Priority | Release |
| ---------- | ---------------- | ------------------------------------------------------------------------------------------------------------------------- | -------- | ------- |
| NFR-OBS-01 | Structured logs  | Requests are logged in a structured format with method, path, status, and duration. No passwords, JWTs, refresh tokens, session identifiers, authorization headers, cookies, or password-reset tokens. | Must     | MVP     |
| NFR-OBS-02 | Error visibility | Unhandled errors are captured with enough context to reproduce them, and are reviewable without SSH access.               | Should   | V1      |
| NFR-OBS-03 | Basic metrics    | Request rate, error rate, and latency percentiles are visible for redirects and for management operations separately.     | Should   | V1      |
| NFR-OBS-04 | Audit trail      | Create, disable, enable, and delete events record who did what and when.                                                  | Should   | V1      |

## 10. Maintainability, Testability, and Deployability

| ID         | Requirement          | Target                                                                                                       | Priority | Release |
| ---------- | -------------------- | ------------------------------------------------------------------------------------------------------------ | -------- | ------- |
| NFR-MNT-01 | Automated checks     | Linting, formatting, and tests run in CI on every push. A red build blocks merge.                            | Must     | MVP     |
| NFR-MNT-02 | Local setup          | A new developer can run the project locally from a clean clone in under 15 minutes using the README.         | Must     | MVP     |
| NFR-MNT-03 | Documented decisions | Significant technical choices are recorded as short ADRs in `docs/adr/`.                                     | Should   | MVP     |
| NFR-TST-01 | Critical-path tests  | Automated tests cover create, resolve, unknown, disabled, deleted, ownership enforcement, and rate limiting. | Must     | MVP     |
| NFR-TST-02 | Coverage             | At least 70% line coverage on application code, with the critical paths fully covered.                       | Should   | MVP     |
| NFR-TST-03 | Load test script     | A committed script reproduces the throughput and latency measurements.                                       | Should   | MVP     |
| NFR-DEP-01 | Repeatable deploy    | Deployment is a single documented command or an automatic push-to-deploy pipeline.                           | Must     | MVP     |
| NFR-DEP-02 | Migrations           | Schema changes are applied through versioned migrations, never manual edits.                                 | Must     | MVP     |
| NFR-DEP-03 | Rollback             | A bad deployment can be reverted within 15 minutes using a documented procedure.                             | Should   | V1      |
| NFR-DEP-04 | No redirect downtime | Routine deployments do not interrupt working redirects.                                                      | Should   | V1      |

## 11. Privacy

| ID         | Requirement           | Target                                                                                                                    | Priority | Release |
| ---------- | --------------------- | ------------------------------------------------------------------------------------------------------------------------- | -------- | ------- |
| NFR-PRV-01 | Minimal collection    | Only an email address and password hash are stored for accounts. No name, phone, or profile data. JWT claims contain only user ID, token ID, issue/expiry timestamps, token type, and token version — not email, password data, or OAuth tokens. | Must     | MVP     |
| NFR-PRV-02 | IP handling           | Visitor IP addresses are used only for rate limiting and are not stored beyond 24 hours.                                  | Must     | MVP     |
| NFR-PRV-03 | Anonymous analytics   | Click analytics store no visitor IP address or identifier that could single out a person.                                 | Must     | MVP     |
| NFR-PRV-04 | Account deletion      | Deleting an account removes personal data from the live datastore within 24 hours; it ages out of backups per NFR-BAK-05. | Should   | MVP     |
| NFR-PRV-05 | Plain-language notice | A short privacy note states what is collected and why.                                                                    | Should   | V1      |

## 12. Non-Functional Requirements Summary

**MVP quality bar** — redirects at p95 under 150 ms holding 100 requests/second, 99.5% best-effort uptime with a health check, HTTPS with properly hashed passwords, JWT + refresh-session cookies, token revocation, and enforced ownership checks, daily backups with a 24-hour RPO and 4-hour RTO, structured logs, CI with critical-path tests, and repeatable deploys with migrations.

**V1 additions** — documented load test results, uptime monitoring and alerting, error tracking and metrics, dependency scanning, a verified restore, an audit trail, rollback procedure, and anonymous analytics with a privacy note.

**Deliberately not doing** — multi-region deployment, active/active failover, four-nines uptime, sub-minute RPO, formal security audits, and on-call rotation. Each is real production work with real cost, and none is defensible for a portfolio project. The growth-path note in NFR-SCL-03 is where to discuss them instead.

## 13. Assumptions

1. Single region, single small instance or container, with a managed datastore.
2. No on-call. The owner responds when they notice, which is why uptime is 99.5% and best-effort.
3. Monthly hosting budget is small; every target is chosen to fit low-cost tiers.
4. Real traffic will be minimal. Throughput numbers are validated by load tests, not production traffic.
5. No formal compliance obligations, since there are no real customers or paid users.
6. Backups are provided by the hosting platform or a scheduled job, not custom infrastructure.

## 14. Open Questions

1. What is the actual monthly hosting budget? It decides whether NFR-PER-03 and NFR-SCL-02 are achievable as stated.
2. Is uptime monitoring worth adding, or is the health check enough for a project nobody depends on?
3. Should analytics be built at all, given NFR-PRV-03 rules out the most interesting dimensions?
4. Are the load-test numbers the headline of the README, or just an appendix?

## 15. Recommended Next Step

Confirm the hosting budget and the MVP targets in Sections 3, 4, and 7, since those drive every later choice. Then start high-level design in [docs/architecture/](docs/architecture/), tracing each design decision back to a requirement ID from this document or [Functional.md](docs/requirements/Functional.md).
