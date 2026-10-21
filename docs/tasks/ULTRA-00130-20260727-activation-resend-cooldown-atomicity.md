# ULTRA-00130: The activation-resend delay is a check-then-act

**Status:** TODO — recorded 2026-07-27 from the review gate on
ULTRA-00129 (activation resend and frontend); premise REWRITTEN 2026-10-19 by
ULTRA-00222 (activation hardening) (the July premise was false, see "What the July version got wrong")
**Security-critical:** yes (it is a rate limit) → red-team scenario ULTRA-00127 (redteam account activation) §8
**Severity:** LOW, bounded by the hardening of 2026-10-19 — see "Why it is bounded now".

## The gap

`EmailAndPasswordAuth.resendActivation` (`funktor/auth/src/jvmMain/kotlin/provider/EmailAndPasswordAuth.kt`) reads the
`PendingActivation` marker, compares `lastSentAt + activationResendDelay(resends)` to now, then writes the advanced marker
with `replaceIfUnclaimed` and sends. That write is conditional on `claimedBy` ONLY — it loses to a retire or an SSO
claim, not to another resend — so two requests that both read the marker before either write lands both pass the check
and both send.

## What the July version got wrong

It called this LOW because "K concurrent requests must share one token". That was false: every refused sign-in minted a
fresh `ActivationResendToken` and none was ever revoked, so K parallel sign-ins gave K tokens and K resends raced the
check (round-2 MAJOR, ULTRA-00129 (activation resend and frontend)). Token consumption also ignored the delete count, so
even one token was single-use only under sequential access.

## Why it is bounded now (2026-10-19)

- Minting a resend token revokes the owner's earlier ones (`AuthRealm.signIn`), and the realm consumes a token only when
  ITS delete removed it (`AuthRealm.resendActivation`, `count == 1`). One token buys at most one resend, even under a
  burst. K concurrent resends need K tokens that all survived each other's revocation — a narrow interleaving of K
  sign-ins, each of which needs the password.
- The delay grows per account (5 min, 15 min, 1 h, 4 h, 12 h, then daily) and the account expires after 7 days, so the
  total mail one pending account can cause is bounded even if this race is won repeatedly: a won race adds a mail inside
  one window, it does not reset the schedule.
- The attacker this matters for is the account's own password holder (e.g. someone who pre-registered a victim's
  address). Their mailbox target is the victim's; the burst is a handful of extra mails, not a faucet.

## Acceptance (unchanged in intent)

- [ ] Concurrent resends for one owner produce AT MOST one mail per delay window
- [ ] Never two simultaneously live `EmailVerificationToken` rows for one owner
- [ ] A concurrent burst can never delete a token that has already been mailed
- [x] The resend token is consumed exactly once under concurrency — 2026-10-19, `AuthRealm.resendActivation` acts only
      when its own delete reports `count == 1`; pinned by `AuthRealmActivationBaseSpec` on both backends
- [ ] **A concurrency test.** Single-threaded tests cannot see this. Precedent: `CapturedEmailsSpec` fires 200
      concurrent sends and reddens reliably without its lock.
- [ ] Both DB backends (the `AuthRecordStorageBaseSpec` pattern), since the fix is storage-level

## Options

### A. Conditional save of the marker (preferred since 2026-10-19)

The schedule lives on ONE row per account (`PendingActivation`), and the conditional primitive exists on both backends:
`AuthRecordStorage.replaceIfClaimedBy` / `replaceIfUnclaimed` (one atomic AQL `REPLACE` / Mongo `replaceOne`), today
filtering on `claimedBy` only. Option A extends that filter to the `resends` / `updatedAt` the check read, so the
second concurrent writer matches nothing and loses at the database.

### A'. Unique index + conditional write (the July option)

A unique index on `(realm, ownerId, _type)` for `EmailVerificationToken` makes the second concurrent
writer lose at the database rather than in application logic. Index declaration already has a home on
both backends — `buildIndexes()` in `db/karango/KarangoAuthRecordsRepo.kt` and
`db/monko/MonkoAuthRecordsRepo.kt`, which already declare a `persistentIndex` and a `ttlIndex`.

Complication worth checking first: the rotation currently *keeps* the row it just created and deletes
the rest (`removeEmailVerificationTokens(..., exceptId = issued._id)`), so a unique index would have
to tolerate the brief overlap, or rotation must become an upsert of a single row per owner. The
second is probably cleaner and also removes the delete entirely.

### B. Compare-and-set on the newest `createdAt`

Write conditional on the newest token still being the one the check read. No schema change, but it
needs a conditional-update primitive that `AuthRecordStorage` does not currently expose.

### C. Per-owner advisory lock

Simplest to reason about, and the worst fit for a multi-JVM deployment — the very case where the race
is reachable. Listed for completeness; do not pick it without a distributed lock.

## Related, deliberately NOT in scope

ULTRA-00128 (signup mail throttle) — a different problem with a different shape. That
one is about an ANONYMOUS endpoint with no per-account anchor at all (every aliased address is a new
user, so no per-user cooldown can bite). Atomicity would not help it, and a suppression point in the
send chain would not help this one.

## History

- 2026-10-21: numbered ULTRA-00130 (was `docs/tasks/20260727-activation-resend-cooldown-atomicity.md`).
