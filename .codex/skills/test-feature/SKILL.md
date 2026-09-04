---
name: test-feature
description: Writes and runs tests for a specific TinyRoute feature by orchestrating the test-writer then test-runner subagents against a spec. Explicit-only, invoke with $test <spec-slug>.
---

# Test feature ($test)

Orchestrates the test-writer -> test-runner pipeline for one feature spec.
Follow [AGENTS.md](../../../AGENTS.md).

User input: the spec slug after `$test` (e.g. `$test custom-alias`),
matching `.codex/spec/<slug>.md`.

If no slug is given, stop and say: "Usage: $test <spec-slug>, e.g. $test
custom-alias."

If `.codex/spec/<slug>.md` does not exist, stop and say: "Spec file not
found at .codex/spec/<slug>.md. Run $spec first, or check the slug."

## Step 1 — Write tests

Spawn `test-writer` ([.codex/agents/](../../agents/)) with:

- The spec file `.codex/spec/<slug>.md`.
- Source files to read for signatures only (not logic): the relevant
  `backend/` or `frontend/` files touched by the spec.
- Instruction: write tests from what the spec says the feature SHOULD do.
  Cover the checklist in test-writer's own instructions (happy path, the
  named redirect failure modes, ownership, rate limiting, validation).

Wait for test-writer to finish and confirm the test file(s) before Step 2.

## Step 2 — Run tests

Spawn `test-runner` ([.codex/agents/](../../agents/)) with:

- The test file(s) test-writer just wrote.
- The spec file, for context on what each test is supposed to validate.
- Instruction: run only the new test file(s), not the full suite. Classify
  every failure as bug vs. missing feature, citing the FR-*/NFR-* ID.

## Handoff rules

- Do not start Step 2 until Step 1 is fully complete.
- test-runner must not attempt to fix any code, regardless of results.
- If test-writer reports it couldn't write tests (e.g. the spec is too
  ambiguous), stop and report why — do not proceed to Step 2.

## Final output

```
Testing Pipeline — <spec-slug>

Step 1 — Tests written
<one line per test, with the FR-*/NFR-* ID it validates>

Step 2 — Results
<test-runner's structured report>

Verdict: ready for $review | needs fixes — <list failing tests and root causes>
```
