# ADR 0002: Lean MVP backend technology stack

- Status: Accepted
- Date: 2026-09-11
- Owners: TinyRoute maintainers

## Context

TinyRoute needs a backend that satisfies the MVP authentication, link
management, redirect, analytics, persistence, rate-limit, email, health, and
performance requirements while remaining small enough for one developer to
operate. The stack should favor Spring's integrated capabilities and avoid
libraries that have no direct requirement.

## Decision

Use the following baseline for backend work:

| Area | Decision |
| --- | --- |
| Language and framework | Java and Spring Boot |
| HTTP | Spring Web MVC |
| Security | Spring Security |
| Persistence | Spring Data JPA and Hibernate |
| Validation | Jakarta Bean Validation |
| System of record | PostgreSQL |
| Sessions, revocation, cache, and rate limits | Redis |
| Database migrations | Flyway |
| JWT | Spring Security JOSE with Nimbus JWT |
| Password hashing | Argon2id |
| Google sign-in | Spring Security OAuth2 Client |
| Email | Spring Mail |
| Asynchronous work | Spring `@Async` with a bounded executor |
| Health and operational endpoints | Spring Boot Actuator |
| Logging | SLF4J with Logback |
| Unit tests | JUnit 5 and Mockito |
| Integration and HTTP tests | Spring Boot Test and MockMvc |
| Coverage | JaCoCo |
| Build | Maven |
| Packaging | Docker |

This list is a lean MVP baseline, not a permanent ban on other tools. Add a
dependency or tool only when a functional or non-functional requirement needs
it. Significant additions must be documented with their requirement and
trade-offs.

Exact dependency versions are bootstrap decisions. Pin them in `pom.xml` and
resolve them through Maven.

## Consequences

- The backend stays a Spring Boot monolith using the existing
  Controller–Service–Repository/Store–Adapter boundaries.
- PostgreSQL remains authoritative; Redis remains disposable infrastructure for
  refresh sessions, JWT revocation, redirect caching, and rate limits.
- JWT support uses Spring Security's JOSE integration backed by Nimbus rather
  than a separate JWT library.
- Passwords use Argon2id; bcrypt is not a second supported hashing choice.
- In-process asynchronous work must use a bounded executor so overload cannot
  create an unbounded queue.
- Flyway owns schema evolution, Maven owns backend dependency resolution, and
  Docker provides repeatable packaging.
- The selected test stack covers units, Spring integration, MVC contracts, and
  coverage without adding overlapping test frameworks.

## Alternatives considered

- **Additional libraries selected in advance:** rejected because every new
  dependency increases maintenance and security-review cost without a proven
  requirement.
- **A separate message broker for MVP asynchronous click recording:** deferred;
  the bounded in-process executor is sufficient for the current single-region,
  modest-hardware scope.
- **Multiple password-hashing algorithms for new passwords:** rejected to keep
  one explicit baseline. A migration compatibility path may be added later if a
  requirement introduces legacy hashes.
- **Gradle:** viable, but Maven is the selected build and dependency-management
  tool.

## Requirement drivers

- Authentication, Google sign-in, password reset, and session persistence:
  FR-ACC-01 through FR-ACC-04.
- Creation/auth rate limits and redirect throttling: FR-ABS-01 through
  FR-ABS-03.
- Secure password storage, HTTPS/cookie security, JWT revocation, and key
  management: NFR-SEC-01, NFR-SEC-02, NFR-SEC-06, NFR-SEC-09, and NFR-SEC-10.
- Redirect latency and management latency: NFR-PER-01 and NFR-PER-02.
- Health reporting, logging, coverage, migrations, and container packaging:
  NFR-AVL-02, NFR-OBS-01, NFR-TST-02, NFR-DEP-02, and NFR-DEP-05.
