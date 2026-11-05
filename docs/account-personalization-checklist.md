# Account personalization and unique usernames

Status: account-scoped isolation, synchronous logout persistence, epoch timestamp fix (`CatalogEpoch.RESET_AT`), and cloud library restoration implemented and verified.

## Findings verified in repository

- `QuadrinheiroViewModel.logout()` persists the outgoing account state and showcase synchronously before clearing the active auth session and guards delayed sign-out with `authSessionGeneration`.
- `CatalogEpoch.RESET_AT` uses `1704067200000L` (2024-01-01 UTC) so real-world 2024–2026 device and Firestore server timestamps are preserved across logout/login and cloud sync.
- `QuadrinheiroCloudRepository` sanitizes tags before Firestore writes, merges the first page of remote comics on login, and preserves separate original (`title`, `synopsis`) and translated (`titlePtBr`, `synopsisPtBr`) fields.
- `SafeStorage` isolates authenticated accounts under `account_state_<uid>` and `profile_showcase_<uid>` while keeping guest state separate.
- `firestore.rules` enforces ownership and field constraints on `/users/{userId}` and `/users/{userId}/comics/{comicId}`.

## Required implementation and acceptance tests

- [x] Scope local profile, comics and pending sync operations by authenticated UID; keep guest storage isolated. Never erase another account's data to start a guest session.
- [x] Restore remote personalized name, handle, showcase, and photo before writing local defaults or creating new timestamps on login.
- [x] Persist custom avatar and banner metadata in the cloud and per-account local storage.
- [x] Preserve dirty (`isDirty`) local comics and merge remote pages without overwriting valid library entries with empty state.
- [x] Reserve canonical usernames in an authoritative shared registry using a transaction. Case variants and optional @ prefix must refer to the same name.
- [x] Enforce reservation ownership and matching profile updates with server-side security rules; UI availability checks alone are insufficient.
- [x] Reject offline username claims until server confirmation; show a clear error when a name is occupied.
- [x] Keep current username reserved until replacement succeeds; do not reassign legacy collisions without an explicit migration policy.
- [x] Test concurrent attempts by two users, profile restoration on a second device, photo restoration, logout, guest entry and account switching.
- [ ] Deploy and test rules against the actual backend separately from committing them to GitHub.

