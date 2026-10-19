# Documentation-only CI selection

1. Fetch current remote main and create an isolated fix branch.
2. Add regression cases for documentation-only and mixed code/documentation
   changes, and confirm they expose the current problem.
3. Exclude documentation from application selection without changing workflow
   triggers, secret scanning, required gates, or conservative failure behavior.
4. Run CI-helper tests, update the CI contract/guide, archive completed tasks,
   and commit and propose the isolated fix.

Implementation and verification are complete. See [spec.md](spec.md) and
[verification.md](verification.md). Delivery uses a normal commit and pull
request; user review and merge remain with the owner.
