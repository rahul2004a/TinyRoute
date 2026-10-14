# Paginated link history

Spec: `docs/spec/paginated-link-history/spec.md`.
Continue the existing branch with the user's explicit approval; prior work is
committed at `2a5ad5b`.

1. Add an indexed, owner-scoped cursor query and cohesive listing behavior to
   the existing repository, service, and controller. Validate bounded inputs
   and return immutable records with safe metadata.
2. Reuse the frontend API/result/form files for saved history, account-scoped
   query caching, pagination, copying, and creation-triggered refresh.
3. Verify ownership, cursor boundaries/ties, bad credentials/inputs, reload,
   pagination, empty/error handling, form reset, and responsive layouts.
4. Run appropriate checks, update documentation, review, and archive completed tasks.
