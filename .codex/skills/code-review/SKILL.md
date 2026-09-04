---
name: code-review
description: Runs parallel quality-reviewer and security-reviewer subagents against the current diff (staged and unstaged) for a TinyRoute feature, then produces a unified report and action plan before any fix is applied. Explicit-only, invoke with $review [optional spec-slug].
---

# Code review ($review)

Post-implementation, pre-commit gate. Complements `spec-guardian` (which runs
*before* code is written) by reviewing the actual diff once code exists.
Follow [AGENTS.md](../../../AGENTS.md).

User input: an optional spec slug after `$review` (e.g. `$review
custom-alias`), matching a file in `.codex/spec/`.

## Step 1 — Collect the diff

Run `git diff` and `git diff --staged`; combine both. If both are empty,
stop and say: "No changes detected. Implement the feature before running
$review."

## Step 2 — Gather context

- If a spec slug was given, read `.codex/spec/<slug>.md`. If it doesn't
  exist, say so and continue without it rather than blocking.
- Identify which files in the diff are backend (`backend/`) vs. frontend
  (`frontend/`) vs. docs/config, so each reviewer has the right scope.

## Step 3 — Parallel review

Spawn `quality-reviewer` and `security-reviewer` (both read-only,
[.codex/agents/](../../agents/)) at the same time — do not wait for one to
finish before starting the other. Give both the same inputs:

- The combined diff from Step 1.
- The spec file content, if found.
- A reminder of each agent's lane: quality-reviewer skips security;
  security-reviewer skips style/layering.

## Step 4 — Unified report

Combine both subagents' findings into one report:

```
Code Review — <spec slug or "untitled change">

## Security findings
<security-reviewer output>

## Quality findings
<quality-reviewer output>

## Combined action plan
Ordered checklist, most severe first:
1. [Critical/High security findings]
2. [Quality CHANGES REQUESTED items]
3. [Remaining security findings]
4. [Quality suggestions]

## Overall verdict
One of: APPROVED / APPROVED WITH SUGGESTIONS / CHANGES REQUESTED
```

De-duplicate: if both reviewers flag the same file:line for different
reasons, merge into one entry noting both angles.

## Step 5 — Ask before fixing

After presenting the report, ask: "Want me to work through the action plan
now?" Do not edit any files until the user confirms.

## Rules

- Never start one reviewer before the other finishes — run in parallel.
- Never edit files before user approval.
- If either subagent returns nothing, report that plainly rather than
  presenting a partial review as complete.
- This skill does not replace `spec-guardian` — use `spec-guardian` before
  writing code, `$review` after.
