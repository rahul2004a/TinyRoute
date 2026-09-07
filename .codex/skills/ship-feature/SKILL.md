---
name: ship-feature
description: Commits the current feature branch with a conventional commit message, pushes it, and opens a PR against main via the gh CLI, using the feature's spec as the PR description. Does not merge. Explicit-only, invoke with $ship.
---

# Ship feature ($ship)

Gets a finished feature from a local branch to an open, reviewable PR.
Follow [AGENTS.md](../../../AGENTS.md). This skill stops at "PR opened" —
merging is a manual, deliberate step for a solo portfolio repo, not
something to automate away.

## Plan Mode

Perform read-only branch, review, authentication, and PR preflight checks and
return the exact shipping plan. Do not stage, commit, push, or open a PR.

## Step 0 — Preconditions

- `git branch --show-current` must return a named branch other than `main`.
  Refuse detached HEAD and `main`.
- Require a completed `$review` with an approval verdict, reviewed-file list,
  and SHA-256 fingerprint. Recompute the fingerprint over the exact current
  tracked and untracked change set using `$review`'s algorithm. Missing,
  incomplete, rejected, or mismatched review evidence blocks shipping.
- Before staging, verify there are no unrelated staged changes, `origin` is
  configured, `gh auth status` succeeds, and no PR already exists for the
  current branch. Stop before mutation if any precondition fails.

## Step 1 — Generate the commit message

Read the already verified current change set, `git status --short`, and
`git log main..HEAD --oneline`.
Find the matching spec under `.codex/spec/` for the current branch (by slug
match against the branch name).

Generate a Conventional Commit message:
- `feat:` new feature, `fix:` bug fix, `chore:` config/tooling,
  `docs:` documentation only.
- Lowercase, no trailing period, under 72 characters.
- Describes what the user can now do, not what the code does.
  Good: "feat: add custom alias support to link creation"
  Bad: "feat: added alias field to LinkController"

## Step 2 — Commit and push

```
git add <reviewed-file>...
git commit -F <commit-message-file>
git push -u origin <current-branch>
```

Stage only files that belong to the reviewed feature. Never use `git add .`
or `git add -A`: inspect `git status` after staging and stop if unrelated
changes are staged. Write the generated message to a securely created
temporary file; do not interpolate generated or user-derived text into shell
syntax.

## Step 3 — Open the PR

Build the body from the spec and write it to a securely created temporary
file. Use `gh pr create --base main --head <current-branch> --title
<plain-English-title> --body-file <body-file>`. Pass arguments without shell
interpolation; never place spec-derived content directly in shell syntax.
Remove temporary message/body files after their command completes or fails.

```markdown
## What this PR does
<one paragraph from the spec's Overview>

## Requirement traceability
<the FR-*/NFR-* IDs from the spec>

## Changes
<one bullet per changed file, one-line description each>

## Definition of done
<the spec's Definition of Done checklist, checked off for items verified>

## How to test
<specific steps derived from the spec>
```

If no spec is found, build the body from the diff and say the PR
description is diff-derived, not spec-derived.

## Step 4 — Report

```
✓ Committed — <message>
✓ Pushed — <branch>
✓ PR opened — <url>
Next: review the PR, then merge manually (squash recommended) once green.
```

## Rules

- Never commit directly to `main`.
- Never merge or delete branches — that's a manual step the user takes from
  the PR itself.
- If `gh` is not authenticated, stop and say: "gh CLI is not authenticated.
  Run `gh auth login` first."
- If push fails because there's no upstream yet, retry with
  `git push -u origin <branch>` (already the default above).
- Never force-push.
- On any failure, report separately whether staging, commit, push, and PR
  creation completed, then give only non-destructive recovery commands. Never
  claim the feature shipped when a later phase failed.
