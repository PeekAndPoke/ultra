# ULTRA-00136: Session revocation — wire the JWT half

> **Status (2026-11-06): NOT STARTED, waiting on the maintainer for open questions 1-5** (recommendations below).
> Decided 2026-11-06: session ids are stored hashed. The storage half exists; the JWT integration does not.
>
> **Part of:** ULTRA-00248 (Auth and sessions epic)

**Plan:** none (follow-up from ULTRA-00001 (code review observations))
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

Key rotation shipped (ULTRA-00176 (jwt kid key rotation)) and, in the course of it, the
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

## Decided by the maintainer (2026-11-06)

- **Session ids are stored hashed** ("Yes i think session ids should be stored hashed too"). The row keeps
  `tokenHash = TokenHash.of(sessionId)` and `token = null`, like the one-shot tokens; the raw id lives only in the JWT
  claim. The `_key` no longer mirrors the raw id: the lookup is `findByTokenHash` (indexed), which ULTRA-00291 built.

## Recommendations on the open questions (2026-11-06, for the maintainer)

1. **Default: `Vault`**, revocation on. A security control that is silently off is what this repo rejects, and no
   app is live, so the extra row per sign-in costs nothing to migrate. `Null` stays for tests.
2. **Device list: none for now.** Keep `userAgent` and `ipAddress` as information on the row; drop the "device"
   meaning of `deviceFingerprint` (keep or remove the field). A real "your devices" feature needs a client-generated
   id and gets its own ticket when wanted.
3. **No reuse:** one row per sign-in, pruned by the TTL. A list, when one exists, sorts by `lastSeenAt`.
4. **Self-service only:** a user revokes their own sessions ("sign out", "sign out everywhere else"). Revoking for
   others (org admins, super users) touches tenant isolation and gets its own ticket.
5. **Two lifetimes, the row wins:** the row's `expiresAt` is an absolute session lifetime (new
   `RealmTokenConfig.sessionLifetime`, default 30 days); the JWT stays short. `refreshToken` refuses when the row is
   gone or expired, which is what stops a refresh from laundering a token; it never extends the row. `touch` updates
   `lastSeenAt` only, debounced by `sessionTouchInterval`.

## Notes

- `AuthRecord.Session`'s KDoc and `SESSION_ID_CLAIM`'s were corrected on 2026-07-28 — both previously
  described this feature in the present tense as though it were wired.
- The `Cached` layer means a revocation is not instant: worst case is one cache TTL. That is a
  deliberate trade and should be documented wherever revocation is exposed to users.

## Note from ULTRA-00291 (2026-11-06)

The one-shot tokens are now stored as `tokenHash` (SHA-256, `domain/TokenHash.kt`) and looked up with
`AuthRecordStorage.findByRawToken`. `AuthRecord.Session` was left out on purpose: its `token` is the raw session id,
mirrored as the row's `_key`. When sessions are issued, decide whether the id is stored hashed as well (then the
`_key` cannot be the raw id), and use the same seam.

## History

- 2026-10-21: numbered ULTRA-00136 (was `docs/tasks/20260728-session-revocation-wiring.md`).
