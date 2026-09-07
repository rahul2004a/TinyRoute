---
name: write-adr
description: Draft an Architecture Decision Record for a significant TinyRoute decision and link its motivation. Explicit-only; invoke $adr followed by the decision title and chosen alternative.
---

# Write ADR ($adr)

Drafts one new ADR file under `docs/adr/`. Explicit-only — proposed or accepted
decisions should be recorded on purpose, not guessed by Codex mid-task.

User input: the decision title and chosen alternative after `$adr`. If the
decision context, meaningful alternatives, consequences, or status cannot be
established from the request, ask before writing rather than inventing them.

## Plan Mode

Inspect existing ADRs and relevant documentation, then return the proposed ADR
number, title, status, and content outline. Do not create or edit files.

## Before you start

Confirm there actually is a decision to record: a choice between real
alternatives (e.g. a locking strategy, a caching approach, a library choice)
that isn't already dictated verbatim by
[docs/architecture/architecture.md](../../../docs/architecture/architecture.md).
If the "decision" just restates something already locked in architecture.md,
say so instead of writing a redundant ADR.

## Steps

1. Read existing ADR titles and decisions. If one already records substantially
   the same decision, stop and ask whether it should be superseded or updated.
   Otherwise pick the next number (`NNN`, zero-padded to 3 digits — start at
   `001` if the folder is empty). Recheck that the target path does not exist
   immediately before writing; never overwrite an ADR.
2. Create `docs/adr/NNN-short-title.md` (kebab-case title) using this
   template:

   ```markdown
   # NNN. <Short title>

   ## Status

   <Proposed or Accepted>

   ## Context

   <What problem or requirement forced this decision. Cite the FR-*/NFR-* ID
   or architecture.md section that motivated it.>

   ## Decision

   <What was decided, stated plainly.>

   ## Alternatives considered

   <Other options and why they were rejected, if relevant.>

   ## Consequences

   <What this makes easier/harder, and any follow-up it implies.>
   ```

3. Use `Accepted` only when the user explicitly states that the decision is
   made; otherwise use `Proposed`. Use `Superseded by ADR-NNN` only when the
   referenced replacement ADR exists. Do not leave template comments in the
   finished file.
4. Keep it short — this project's NFR-MNT-03 asks for "short ADRs," not a
   design essay.
5. Link the ADR from anywhere relevant (e.g. mention it in the architecture
   doc's section it touches) only if asked — this skill only writes the ADR
   file itself unless told otherwise.

## Output

The path of the new ADR file and a one-line summary of the decision it
records.
