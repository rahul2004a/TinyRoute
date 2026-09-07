---
name: code-review
description: Runs parallel quality and security reviewers against the current tracked and untracked TinyRoute change set, then reports findings before any fix. Explicit-only; invoke with $review [optional spec-slug].
---

# Code review ($review)

Post-implementation, pre-commit gate. Complements `spec-guardian` (which runs
*before* code is written) by reviewing the actual diff once code exists.
Follow [AGENTS.md](../../../AGENTS.md).

User input: an optional spec slug after `$review` (e.g. `$review
custom-alias`), matching a file in `.codex/spec/`.

## Step 1 — Collect the diff

Run `git status --short`, `git diff HEAD`, and
`git ls-files --others --exclude-standard`. Treat `git diff HEAD` as the final
tracked-file state. Represent each untracked text file as an addition from
`/dev/null`; list untracked binary files separately. If there are no tracked or
untracked changes, stop and say: "No changes detected. Implement the feature
before running $review."

Compute a SHA-256 fingerprint over a canonical byte stream: a version marker;
the byte length and bytes of `git diff --binary HEAD`; then, in the bytewise
order returned by `git ls-files -z --others --exclude-standard`, the byte
length and bytes of each path followed by the byte length and raw bytes of its
content. Length-prefix every field so different inputs cannot produce the same
stream by concatenation ambiguity. Do not modify or stage files to compute it.

## Step 2 — Gather context

- If a spec slug was given, read `.codex/spec/<slug>.md`. If it doesn't
  exist, say so and continue without it rather than blocking.
- Identify which files in the change set are backend (`backend/`) vs. frontend
  (`frontend/`) vs. docs/config, so each reviewer has the right scope.

## Step 3 — Parallel review

Spawn `quality-reviewer` and `security-reviewer` (both read-only,
[.codex/agents/](../../agents/)) at the same time. Start both before waiting
for either one. Give both the same inputs:

- The complete change set and reviewed-file list from Step 1.
- The spec file content, if found.
- A reminder of each agent's lane: quality-reviewer skips security;
  security-reviewer skips style/layering.

Each finding must have a severity (`Critical`, `High`, `Medium`, or `Low`), a
file and line, evidence, impact, and a concrete fix. Reviewers may read the
minimum unchanged signatures, configuration, and call sites needed to validate
a changed path, but findings remain limited to changed behavior.

If a reviewer errors, times out, or returns no verdict, retry that reviewer
once. If it still fails, return `REVIEW INCOMPLETE`; never return an approval
verdict from a partial review.

## Step 4 — Unified report

Combine both subagents' findings into one report:

```
Code Review — <spec slug or "untitled change">

Reviewed fingerprint: <sha256>
Reviewed files: <ordered paths>

## Security findings
<security-reviewer output>

## Quality findings
<quality-reviewer output>

## Combined action plan
Ordered checklist, most severe first:
1. [All Critical findings]
2. [All High findings]
3. [All Medium findings]
4. [All Low findings]

## Overall verdict
One of: APPROVED / APPROVED WITH SUGGESTIONS / CHANGES REQUESTED /
REVIEW INCOMPLETE
```

De-duplicate: if both reviewers flag the same file:line for different
reasons, merge into one entry noting both angles.

Set the overall verdict deterministically: any Critical/High finding is
`CHANGES REQUESTED`; only Medium/Low findings is
`APPROVED WITH SUGGESTIONS`; no findings and two completed reviewer verdicts is
`APPROVED`; any unavailable reviewer is `REVIEW INCOMPLETE`.

## Step 5 — Ask before fixing

After presenting the report, ask: "Want me to work through the action plan
now?" Do not edit any files until the user confirms.

## Rules

- Start both reviewers before waiting for either; run them concurrently.
- Never edit files before user approval.
- A reachable stub, disabled test, or placeholder is a finding unless the spec
  explicitly excludes it from the deliverable.
- If either subagent remains unavailable after one retry, report the review as
  incomplete rather than presenting a partial review as complete.
- This skill does not replace `spec-guardian` — use `spec-guardian` before
  writing code, `$review` after.
