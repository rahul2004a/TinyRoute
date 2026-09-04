---
name: write-adr
description: Draft a new Architecture Decision Record in docs/adr/ for a significant technical decision, linking back to the requirement/architecture sections that motivated it. Explicit-only command, invoke with $adr.
---

# Write ADR ($adr)

Drafts one new ADR file under `docs/adr/`. Explicit-only — ADRs should be
written on purpose when a real decision has been made, not guessed by Codex
mid-task.

## Before you start

Confirm there actually is a decision to record: a choice between real
alternatives (e.g. a locking strategy, a caching approach, a library choice)
that isn't already dictated verbatim by
[docs/architecture/architecture.md](../../../docs/architecture/architecture.md).
If the "decision" just restates something already locked in architecture.md,
say so instead of writing a redundant ADR.

## Steps

1. List existing files in `docs/adr/` and pick the next number
   (`NNN`, zero-padded to 3 digits — start at `001` if the folder is empty).
2. Create `docs/adr/NNN-short-title.md` (kebab-case title) using this
   template:

   ```markdown
   # NNN. <Short title>

   ## Status

   Accepted <!-- or Proposed / Superseded by ADR-NNN -->

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

3. Keep it short — this project's NFR-MNT-03 asks for "short ADRs," not a
   design essay.
4. Link the ADR from anywhere relevant (e.g. mention it in the architecture
   doc's section it touches) only if asked — this skill only writes the ADR
   file itself unless told otherwise.

## Output

The path of the new ADR file and a one-line summary of the decision it
records.
