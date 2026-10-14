# Paginated link history

Requested after the completed auth-page, links-layout, and form-reset fixes.
The user authorized committing prior changes and continuing on the existing
`fix/guest-only-auth-pages` branch; baseline commit is `2a5ad5b`.

## Scope and requirements

- FR-MGT-01/02: signed-in users can read a paginated list of their own saved
  links, including code, destination, status, and creation date.
- FR-CRE-01/05: preserve the immediate copyable creation result; clear form
  inputs only after confirmed success and refresh history after creation.
- NFR-SEC-04/06/08: Spring owns authentication and owner filtering. No owner
  identifier from the browser, no token access, and safe validation/errors.
- NFR-PER-02: bounded indexed queries; no full-history download or count scan.

Only listing is added. Search, editing, disabling, deleting, and analytics
capture remain separate work. Existing records require no migration.

## Contract and behavior

`GET /api/links?size=10&cursor=<opaque>` requires the existing access cookie.
Size defaults to 10 and must be 1–50. Invalid sizes/cursors return safe 400s.
The response is `{items, nextCursor}`. Items contain the existing creation
fields plus `status` (ACTIVE, DISABLED, EXPIRED) and `clickCount` from storage.
Deleted rows are omitted. Spring determines expiry status. Responses use
`Cache-Control: no-store` and reads do not consume the creation quota.

Use the existing `(owner_id, created_at DESC, id DESC)` index and a cursor
containing the last item's timestamp/id. Fetch size + 1 rows, return at most
size, and generate a next cursor only when another row exists. Cursors are
positions, not authorization credentials: every query filters by JWT owner.

The frontend requests 10 items per page, exposes Previous/Next and a page
indicator, and shows loading, empty, and retryable error states. Reload starts
at the newest page and fetches saved records again. New creation returns to the
first page and refreshes the list. Query caches are scoped to the authenticated
account and hidden when its session is lost. Preserve the shared header/footer
and form width; use a desktop table and mobile cards. Reuse existing frontend
files, with small domain/DTO files for the new Spring response contract.

## Acceptance criteria

- [x] The user's saved links remain visible after a browser refresh.
- [x] Pagination is bounded, deterministic on tied timestamps, and owner-only.
- [x] Missing credentials and forged/invalid inputs fail safely.
- [x] New creation clears inputs, retains its copyable result, and refreshes page 1.
- [x] List loading/errors/empty state and Previous/Next work in both themes/mobile.
- [x] Relevant backend/frontend checks and browser regressions pass.
