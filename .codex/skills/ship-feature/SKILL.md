---
name: ship-feature
description: Commits the current feature branch with a conventional commit message, pushes it, and opens a PR against main via the gh CLI, using the feature's spec as the PR description. Does not merge. Explicit-only, invoke with $ship.
---

# Ship feature ($ship)

Gets a finished feature from a local branch to an open, reviewable PR.
Follow [AGENTS.md](../../../AGENTS.md). This skill stops at "PR opened" —
merging is a manual, deliberate step for a solo portfolio repo, not
something to automate away.

## Step 0 — Preconditions

- Refuse to run on `main`: `git branch --show-current` must not be `main`.
  If it is, stop and say so.
- If `$review` hasn't been run for this diff (no evidence of it in the
  conversation), recommend running it first, but don't block — ask the user
  whether to proceed anyway.

## Step 1 — Generate the commit message

Run `git diff --staged`, `git diff`, and `git log main..HEAD --oneline`.
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
git commit -m "<generated message>"
git push -u origin <current-branch>
```

Stage only files that belong to the reviewed feature. Never use `git add .`
or `git add -A`: inspect `git status` after staging and stop if unrelated
changes are staged.

## Step 3 — Open the PR

Use the `gh` CLI (`gh pr create --base main --head <current-branch> --title
"<plain-English title>" --body "<body>"`). Build the body from the spec:

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
