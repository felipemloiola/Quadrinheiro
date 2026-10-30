# Account personalization and unique usernames

Status: requirements tracked; backend changes NOT implemented or deployed by this commit.

## Findings verified in repository

- QuadrinheiroAuthManager.signInWithEmail uses SafeStorage.authenticateLocalEmailAccount.
- QuadrinheiroAuthManager.signUpWithEmail uses SafeStorage.registerLocalEmailAccount.
- CloudUserProfile contains name and handle but no custom avatarUrl field.
- CloudUserProfile.toReaderProfile retains a device-local avatar URL supplied by the caller.
- firestore.rules currently has no username reservation collection or uniqueness constraint.
- enterAsGuest currently clears the shared local library and reader profile. Account-scoped isolation must replace this behavior before claiming data is safe across account switches.

## Required implementation and acceptance tests

- [ ] Scope local profile, comics and pending sync operations by authenticated UID; keep guest storage isolated. Never erase another account's data to start a guest session.
- [ ] Restore remote personalized name, handle and photo before writing local defaults or creating new timestamps on login.
- [ ] Persist custom avatar metadata in the cloud. Local content/file URIs must be uploaded to authenticated storage, not treated as portable photo URLs.
- [ ] Resolve local-only email account migration and Firebase authentication explicitly; do not silently claim cross-device persistence.
- [ ] Reserve canonical usernames in an authoritative shared registry using a transaction. Case variants and optional @ prefix must refer to the same name.
- [ ] Enforce reservation ownership and matching profile updates with server-side security rules; UI availability checks alone are insufficient.
- [ ] Reject offline username claims until server confirmation; show a clear error when a name is occupied.
- [ ] Keep current username reserved until replacement succeeds; do not reassign legacy collisions without an explicit migration policy.
- [ ] Test concurrent attempts by two users, profile restoration on a second device, photo restoration, logout, guest entry and account switching.
- [ ] Deploy and test rules against the actual backend separately from committing them to GitHub.
