# Frontend and backend CI Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task by task.

**Goal:** Implement both supplied CI diagrams with stable, fail-closed checks.

**Architecture:** Two GitHub Actions workflows call small standard-library
Python helpers for change detection, aggregate status, and CodeQL SARIF gating.
Maven owns test phases, coverage and static analysis; Docker consumes the tested
JAR. Test infrastructure is composed into existing Spring tests.

**Tech Stack:** GitHub Actions, Python 3, Node 24/Corepack/pnpm, Java 21/Maven,
Spotless, Surefire/Failsafe, Testcontainers, JaCoCo, SpotBugs/Find Security Bugs,
Gitleaks, CodeQL, Trivy, Docker.

**Spec:** `docs/spec/frontend-backend-ci/spec.md`

## Global constraints

- PR validation has no deployment secrets and never deploys or merges.
- Preserve root development Compose and locked application dependencies.
- Stable check names are `Frontend CI` and `Backend CI`.
- Coverage minimum is 0.70 LINE covered ratio; Trivy blocks HIGH/CRITICAL;
  CodeQL blocks security-severity >= 7.0 or error findings.
- Use branch `feature/frontend-backend-ci`; archive completed tasks only.

## Review focus

- Docs-only changes must complete required checks with success.
- Failed detection or cancelled/unexpectedly skipped jobs must fail gates.
- Renames and deletions must not evade validation.
- Malformed/missing CodeQL reports must fail rather than pass silently.
- Image validation must use the tested JAR and preserve the default Docker build.

## Task 1: CI helpers and workflows

Files: `.github/scripts/{ci.py,test_ci.py,install-tool.sh}`, workflows
`frontend-ci.yml` and `backend-ci.yml`.
Interfaces: helpers write frontend/backend boolean outputs; gate consumes
`NEEDS_JSON`; SARIF consumes a directory; installer accepts gitleaks/trivy.

- [x] Add helper tests; run them and observe failure before implementation.
- [x] Implement conservative diff classification, fail-closed gates and SARIF check.
- [x] Add checksum-verified tool installer and pinned workflows.
- [x] Run helper tests and actionlint; verify missing/cancelled jobs fail.

## Task 2: Backend verification and tested image

Files: `backend/pom.xml`, wrapper, test config and Spring `*IT` test files,
`backend/Dockerfile`, `backend/.dockerignore`.
Interfaces: Maven produces `target/tinyroute-backend-0.0.1-SNAPSHOT.jar`,
Surefire/Failsafe reports and `target/site/jacoco`; Docker target `ci` consumes
`target/application.jar` copied from that artifact.

- [x] Add Maven wrapper, Surefire/Failsafe, coverage enforcement and static analysis.
- [x] Add singleton test containers and move Spring tests into Failsafe naming.
- [x] Apply formatting to changed Java and run wrapper clean verify.
- [x] Add shared Docker runtime target and CI artifact stage; build both targets.
- [x] Run Gitleaks/Trivy and fix applicable findings within this concern.

## Task 3: Verification, review and delivery

Files: `Readme.md`, `docs/development/ci.md`, feature spec and verification record.

- [x] Run frontend frozen install, lint/format/type/tests/build.
- [ ] Record actual hosted validation; local evidence and branch-protection setup are documented.
- [x] Obtain independent review; fix material findings and rerun relevant checks.
- [ ] Push feature branch and create PR without opening browser; inspect CI.
- [ ] Complete criteria and archive plan/todo only when checks pass.
