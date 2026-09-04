---
name: trace-requirement
description: Check a proposed feature, change, or diff against docs/requirements/Functional.md and Non-Functional.md before building it. Confirms the matching FR-*/NFR-* ID, its priority (Must/Should/Could) and release (MVP/V1/Future), and flags scope creep. Explicit-only command, invoke with $trace.
---

# Trace requirement ($trace)

A deliberate pre-flight check, run with `$trace` before implementing a
feature or accepting a change request. Does not implicitly trigger — this is
meant to be a conscious gate, not an automatic background check.

## Inputs

Whatever the user just asked for: a feature request, a task description, a
diff, or a plan. If none is given in the current context, ask what to trace.

## Steps

1. Read [docs/requirements/Functional.md](../../../docs/requirements/Functional.md)
   and [docs/requirements/Non-Functional.md](../../../docs/requirements/Non-Functional.md)
   in full (they're short).
2. Find the `FR-*` and/or `NFR-*` ID(s) that best match the request. If more
   than one plausibly applies, list all candidates.
3. Report, for each match:
   - ID, one-line description, actor, **priority** (Must/Should/Could), and
     **release** (MVP/V1/Future).
4. If nothing matches:
   - State that plainly. Do not invent a requirement ID or silently assume
     it's in scope.
5. Apply the scope guardrail from [AGENTS.md](../../../AGENTS.md):
   - MVP Must/Should/Could is in scope by default.
   - V1/Future items (destination blocklist, public API/API keys,
     safe-browsing checks, admin console, or anything else explicitly marked
     V1/Future in the docs) are **out of scope** unless the user explicitly
     asked for that specific item in this conversation.
6. If the request conflicts with a documented behavior (e.g. would make an
   unknown code redirect, or would let Next.js own policy), call that out as
   a blocking issue, citing the exact section of
   [docs/architecture/architecture.md](../../../docs/architecture/architecture.md).

## Output

A short report:

- Matched requirement ID(s), priority, release.
- In-scope / out-of-scope verdict, with the reason.
- Any blocking conflicts with architecture.md, cited by section.
- If out of scope: what the user would need to say to explicitly opt in.

Keep it brief — this is a gate, not an essay.
