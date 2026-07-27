# The activation-resend cooldown is a check-then-act

**Status:** TODO — recorded 2026-07-27 from the review gate on
`.claude/tasks/20260727-activation-resend-and-frontend.md`
**Security-critical:** yes (it is a rate limit) → red-team scenario already recorded as
`.claude/tasks/20260727-redteam-account-activation.md` §8
**Severity:** LOW as it stands, and it is LOW only because of the single-use token in front of it —
see "Why this is not urgent". It becomes the whole defence if that token is ever relaxed.

## The gap

`EmailAndPasswordAuth.resendActivation` (`funktor/auth/src/jvmMain/kotlin/provider/EmailAndPasswordAuth.kt`):

```
findActivationResendToken   ─┐
removeAuthRecord            │  four separate operations,
findPendingActivation       │  no transaction, no lock,
findLatestEmailVerificationToken → compare to the window
issueAndSendActivationToken ─┘  no uniqueness constraint
```

Every request whose *read* completes before the first *write* commits passes the window check. So K
concurrent requests can all decide "the cooldown has elapsed" and all send.

`letTheBotsWait()` (`api/AuthApi.kt`) is a 250–500 ms per-request `delay`; it spreads arrivals and
damps this in practice but bounds nothing — the race window is the read→write round trip, and the
attacker simply raises K.

The same shape applies to the token consumption: two requests can both read the resend token before
either `removeAuthRecord` lands, so "single-use" is itself only single-use under sequential access.

## Why this is not urgent

The endpoint is authorized by `AuthRecord.ActivationResendToken`, which is single-use, short-lived,
and mintable ONLY by passing the password check for that account. So:

- this is not reachable anonymously — the attacker must know the password;
- an attacker who knows the password can already sign in, so the interesting target is a burst aimed
  at the account's own mailbox rather than a takeover;
- each burst needs one token, and getting another means another sign-in.

It is recorded rather than fixed because closing it properly means a storage-level constraint on both
DB backends, which is more than the feature it guards is worth today.

## Options

### A. Unique index + conditional write (preferred)

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

## Acceptance

- [ ] Concurrent resends for one owner produce AT MOST one mail per cooldown window
- [ ] Never two simultaneously live `EmailVerificationToken` rows for one owner
- [ ] A concurrent burst can never delete a token that has already been mailed
- [ ] The resend token is consumed exactly once under concurrency
- [ ] **A concurrency test.** The existing single-threaded tests cannot see any of this — that is why
      it survived the gate's mutation round. Precedent for the shape: `CapturedEmailsSpec` fires 200
      concurrent sends and reddens reliably without its lock.
- [ ] Both DB backends (`MatrixTest2d` / the `AuthRecordStorageBaseSpec` pattern), since the fix is
      storage-level

## Related, deliberately NOT in scope

`.claude/tasks/20260727-signup-mail-throttle.md` — a different problem with a different shape. That
one is about an ANONYMOUS endpoint with no per-account anchor at all (every aliased address is a new
user, so no per-user cooldown can bite). Atomicity would not help it, and a suppression point in the
send chain would not help this one.
