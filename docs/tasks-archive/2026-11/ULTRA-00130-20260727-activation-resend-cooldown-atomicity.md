# ULTRA-00130: The activation-resend delay is a check-then-act

> **Status (2026-11-06): DONE.** gate PASS after round 1. Option A built (the maintainer, 2026-11-06: "tackle all auth tickets that you can work on where no additional input is needed"; A was the recorded preference). Earlier status:
> **(2026-07-27): NOT STARTED.** recorded 2026-07-27 from the review gate on
> ULTRA-00129 (activation resend and frontend); premise REWRITTEN 2026-10-19 by
> ULTRA-00222 (activation hardening) (the July premise was false, see "What the July version got wrong")
>
> **Part of:** ULTRA-00248 (Auth and sessions epic)

**Moved (2026-11-04) by ULTRA-00231 (email first signup):** the resend endpoint is gone. The same check-then-act now
sits in `EmailAndPasswordAuth.reSendIfPending`, reached by signing up again with a pending address — ANONYMOUSLY, so
the "the caller needs the password" half of "Why it is bounded now" no longer holds; the growing delays and the 7-day
expiry still bound it, and option A applies unchanged. Two more from the ULTRA-00231 gate belong with it:
- an anonymous sign-up again rotates the link, so it kills a genuine pending user's live link once the delay is up
  (a nuisance: the newest mail reaches them) — parked for the maintainer in ULTRA-00231;
- the re-send path sends inline, the silent path does not: a timing tell, recorded in ULTRA-00128.

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

- [x] Concurrent resends for one owner produce AT MOST one mail per delay window
- [x] ~~Never two simultaneously live `EmailVerificationToken` rows for one owner~~ — obsolete: since ULTRA-00289 the
      older links live on for `activationLinkOverlap` on purpose
- [x] A concurrent burst can never delete a token that has already been mailed — the burst now sends one mail, and
      the newer link is created before the older ones are capped (ULTRA-00222)
- [x] The resend token is consumed exactly once under concurrency — 2026-10-19, `AuthRealm.resendActivation` acts only
      when its own delete reports `count == 1`; pinned by `AuthRealmActivationBaseSpec` on both backends
- [x] **A concurrency test.** Single-threaded tests cannot see this. Precedent: `CapturedEmailsSpec` fires 200
      concurrent sends and reddens reliably without its lock.
- [x] Both DB backends (the `AuthRecordStorageBaseSpec` pattern), since the fix is storage-level

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

## Implementation (2026-11-06)

- `AuthRecordStorage.replacePendingActivationIfUnchanged(read, next)`: the marker is replaced only while it is
  unclaimed AND its `resends` is still the one `read` holds. `resends` is the schedule's version: every re-send
  increments it, so the second of two writers that read the same marker matches nothing.
- Through the layers: `Adapter.replaceRecordIfClaimedBy` and the Vault `Repo.replaceIfClaimedBy` gain
  `expectedResends: Int?` (null: no such condition, every other caller); Karango adds `&& doc.resends == @resends`
  to its one AQL statement, Monko `eq("resends", …)` to its `replaceOne` filter.
- `EmailAndPasswordAuth.Services.replacePendingActivation(read, next)` (was `(marker)`).
- Not `updatedAt` as the version: two writes in the same millisecond, or under a frozen test clock, would match.
- **Found on the way, pre-existing: Karango answered overlapping conditional writes with a 500.** A burst of 50
  parallel writers on one document made Arango abort the slower ones with `1200 write-write conflict` (3 runs of 3)
  instead of letting their FILTER miss. It hit every conditional write of the Karango auth repo (the re-send, retire
  and SSO claims, activation's remove) and the bulk removes. `KarangoAuthRecordsRepo.retryOnConflict` runs such a
  statement again on a 1200 (the aborted run wrote nothing), up to 5 times, so it decides against the committed
  document; anything else, or a conflict that persists, still throws. (Round 1 answered a 1200 as "lost"; the gate
  showed why that is wrong, see the ledger.) The same burst without the resends condition (mutant R3) showed
  it end to end: Karango answered a loser with a 500, Monko sent a second mail.

## Test evidence

- `AuthRecordStorageBaseSpec` (Karango and Monko): a second write from the same read matches nothing and writes
  nothing; a claimed marker loses too; 50 parallel writers from one read: exactly one lands, none throws; 50 parallel
  `removeIfUnclaimed`: exactly one removes, none throws.
- `AuthRecordStorageBaseSpec`, after round 1: an activation's remove overlapping 10 re-send writes still removes
  (10 rounds); 20 parallel `removeAllByOwner`: every row goes, none throws.
- `ActivationHardeningE2eSpec` (both backends): two sign-ups held by `ResendGate` until both read the marker and passed
  the delay: one mail (deterministic); 20 concurrent sign-ups at a due re-send: one mail, every answer 200, `resends` 1.
- funktor-auth 303/0, funktor-all 261/0 (after the round-1 fixes), 2026-11-06.
- Mutants, each red for the intended reason: R1 Karango ignores `resends` (storage test; the burst e2e on Karango),
  R2 Monko ignores it (storage test), R3 the provider back to the plain replace (burst e2e on both: Monko "one slot,
  one mail", Karango a 500), R4 the storage passes no `resends` (storage test on both), R5 / R6 the conflict not
  caught on replace / remove (the burst tests on Karango). After round 1: R1-R4 re-run, each now also red in the
  `ResendGate` e2e on both backends; R5 the conflict never retried (three Karango burst tests: 1200); R6 a conflict
  counted as lost, the round-1 version (the remove-vs-re-send test, round 0); R7 the bulk remove not retried.

## Review record — the LEDGER

Round 1 (level 1, `reviewer-high` ×3, 2026-11-06): all three charters CLEAN.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (all three) | `KarangoAuthRecordsRepo.kt:218` | A 1200 means the writes OVERLAPPED, not that this one's condition failed: an activation's remove (or a reset, a retire or SSO claim) overlapping a re-send's REPLACE, which leaves the marker unclaimed, was refused, the link burnt | fixed | Verified: re-send writes keep `claimedBy == null`. On a 1200 the statement now runs again (≤ 5), deciding against the committed document as Mongo does; new test: a remove overlapping 10 re-send writes, 10 rounds, always removes; mutant R6 (the round-1 "lost") red |
| 1 | MINOR (impl) | `ActivationHardeningE2eSpec.kt:451` | The provider's switch to the conditional write (R3) is pinned only by a timing-dependent burst | fixed | `ResendGate` holds each re-send's write until two arrived: both read the marker and passed the delay; one mail with the fix, two with R3, every run |
| 1 | MINOR (impl, out of scope) | `KarangoAuthRecordsRepo.kt:143-174` | The bulk removes (`removeAllByOwner`, `removeAllOfOwner`) also throw 1200 on overlap | fixed here | Same helper, cheap; test: 20 parallel `removeAllByOwner`; mutant R7 |
| 1 | NIT (impl) | `AuthRecordStorage.kt:296` | `next` may name another record than `read` | fixed | `require(next._id == read._id)` |
| 1 | NIT (impl, domain) | `RealmTokenConfig.kt:77` | The reason for `>= 2` ("two concurrent re-sends") no longer holds | fixed | Reworded: the new mail's token and the previous mail's |
| 1 | NIT (impl) | `RealmTokenConfig.kt:52`, the storage spec | Long lines, import order | fixed | |

Probed and CLEAN (from the reports): `resends` only grows and claims never revert, so no ABA; the condition cannot
be bypassed (Mongo and Arango compare numbers by value; a missing field fails closed); one slot, one mail on both
backends, losers answer the same 200; every `expectedResends = null` caller unchanged; `KarangoDriver` wraps exactly
the `ArangoDBException`; `EntityRepository.remove` fails safe for token consumption; no new denial of service. SURPLUS:
nothing; the Karango conflict handling is the bug found on the way.

**Verdict:** gate PASS after round 1 (no CRITICAL or MAJOR). The fixes re-run: see "Test evidence".

## Related, deliberately NOT in scope

ULTRA-00128 (signup mail throttle) — a different problem with a different shape. That
one is about an ANONYMOUS endpoint with no per-account anchor at all (every aliased address is a new
user, so no per-user cooldown can bite). Atomicity would not help it, and a suppression point in the
send chain would not help this one.

## History

- 2026-10-21: numbered ULTRA-00130 (was `docs/tasks/20260727-activation-resend-cooldown-atomicity.md`).
- 2026-11-04: premise moved to the sign-up re-send path by ULTRA-00231.
- 2026-11-06: option A built with the Karango conflict fix, gate PASS after round 1, DONE. No docs ticket: no
  public API a user of the library calls changed (the storage adapter and `Services` are extension points; CHANGELOG).
- 2026-11-06: DONE.
