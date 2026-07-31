# Session revocation — wire the JWT half

**Status:** TODO — the storage half exists; the JWT integration does not
**Plan:** none (follow-up from `.claude/tasks/code-review-observations.md`)
**Security-critical:** yes → red-team follow-up required

## What exists today

Built, tested, and unused:

- `AuthRecord.Session` — `token` (the session id, mirrored as the row `_key`), `deviceFingerprint`
  (hash of userAgent + ipAddress), `userAgent`, `ipAddress`, `lastSeenAt`, `expiresAt`.
- `SessionStore` — `create` / `getById` / `touch` / `listForUser` / `revoke` /
  `revokeAllForUser(except)`, with `Null`, `Vault` and `Cached` implementations.
  `SessionStoreCachedSpec` and `SessionStoreVaultBaseSpec` cover them.
- TTL index on `expiresAt` in both `KarangoAuthRecordsRepo` and `MonkoAuthRecordsRepo`, so expired
  rows are pruned by the database.
- `RealmTokenConfig` carries the in-memory TTL for `SessionStore.Cached`.

## Why this is now the highest-value auth item (added 2026-08-01)

Key rotation shipped (`.claude/tasks/20260731-jwt-kid-key-rotation.md`) and, in the course of it, the
maintainer pointed out what it does NOT buy: **rotation cannot contain a compromised key, because
refresh launders a token onto the new one.** An attacker holding a forged token calls `refreshToken`
during the grace window and gets a replacement signed with the *new* key; dropping the old key then
achieves nothing. Verified by reading `AuthRealm.refreshToken`
(`funktor/auth/src/jvmMain/kotlin/AuthRealm.kt:389`) — it authenticates on the token alone, and
re-derives user and permissions from the DB but never asks whether this session is still allowed to
exist.

So today the only revocation this system has is "remove the key", whose granularity is *every user*.
That is what this task fixes, and it is why it outranks any further work on the JWT machinery itself.

It also reframes rotation honestly: a hygiene operation on a schedule, not an incident response.

## What is missing

- `withSessionId()` and `sessionIdClaim()` (`SessionJwtClaims.kt`) have **no callers**.
- No production wiring registers a `SessionStore` — only tests construct one.
- `SessionStore.Vault.touch()` is an explicit no-op ("Phase 1").

So: no JWT carries a session id, no middleware checks one, and `lastSeenAt` never updates. Revocation
works at the storage layer and has no effect on a live token.

## Spec

- [ ] Create a session row on successful sign-in and attach its id via `withSessionId`
- [ ] Resolve `sessionIdClaim()` in the auth middleware through `SessionStore.Cached`; reject when the
      row is gone
- [ ] Register a `SessionStore` in the funktor auth builder, defaulting sensibly (see the open
      question on `Null` below)
- [ ] Implement the debounced `touch()` so `lastSeenAt` is useful without a write per request
- [ ] e2e: sign in, revoke the row, confirm the next request is refused **before** the JWT expires
- [ ] e2e: `revokeAllForUser(except = current)` leaves the calling session alive
- [ ] Mutation-test the middleware check — deleting it must fail a test

## Open questions (from the code review, 2026-07-28)

These are design decisions, not implementation details. Settle them before building.

1. **Default when unconfigured.** If an app installs auth but registers no store, is the default
   `Null` (no revocation, JWT-only — today's behaviour, no migration) or `Vault` (revocation on by
   default, but every app suddenly writes a session row per sign-in)? `Null` silently disables a
   security control, which is the pattern this repo usually rejects.
2. **Device list.** `deviceFingerprint` is `hash(userAgent + ipAddress)`. That changes when the user's
   IP changes — mobile networks, VPNs — so it identifies a *connection*, not a device. If "your
   devices" is a user-facing feature, the fingerprint needs a stable client-generated id instead.
3. **Session reuse per device.** Without reuse, every sign-in adds a row; TTL prunes them eventually,
   but a user signing in daily accumulates a long list in the UI. Reuse by fingerprint, or show only
   the most recent per fingerprint?
4. **Who may revoke.** Self-service only, or also org admins and super users? Cross-org revocation
   touches tenant isolation, so it needs the same treatment as any other cross-org operation.
5. **`expiresAt` vs JWT expiry.** Two lifetimes now exist. Which wins, and does refresh extend the
   session row or only the JWT?

## Notes

- `AuthRecord.Session`'s KDoc and `SESSION_ID_CLAIM`'s were corrected on 2026-07-28 — both previously
  described this feature in the present tense as though it were wired.
- The `Cached` layer means a revocation is not instant: worst case is one cache TTL. That is a
  deliberate trade and should be documented wherever revocation is exposed to users.
