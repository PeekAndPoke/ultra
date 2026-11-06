# ULTRA-00291: Funktor auth: store one-shot tokens hashed (SHA-256), never in clear text

> **Status (2026-11-06): DONE.** gate PASS after round 1.** The maintainer decided on hashes on 2026-11-05 and confirmed the plan on 2026-11-06: "yes build it. Implement as you proposed with the 11 and 7 configurable". Open for the maintainer: the default 11 drops the sign-up mail's cancel link on day 6.7 (12 would keep every one).
>
> **Part of:** ULTRA-00248 (Epic: Auth and sessions)

**Security-critical:** yes.

## Why this exists

`AuthRecord.token` holds the raw secret of every one-shot link. `findByToken` matches it as plain equality:

- `db/karango/KarangoAuthRecordsRepo.kt:91`
- `db/monko/MonkoAuthRecordsRepo.kt:96`

Since ULTRA-00231, a live activation link sets the password and signs in. So anyone who can read the auth records
(a backup, a read-only DB leak, an admin tool) holds a working link into every pending account. They also hold a
working reset link into every account with a reset under way. Found while preparing the ULTRA-00231 report; the
maintainer chose hashes over clear text on 2026-11-05.

## The plan

- **In scope:**
  - `PasswordRecoveryToken`
  - `EmailVerificationToken`
  - `SignUpCancelToken`
  - `EmailChangeToken` (defined, but nothing creates one today)
  - `OrgSelectionToken`
- **Out of scope:**
  - `Password` already holds a password hash.
  - `Session` belongs to ULTRA-00136: its token is also the row's `_key`, and nothing creates sessions in production
    yet. A note goes there.
- **Stored as** `SHA-256(raw)`, base64url. The raw token is 256 random bytes, so no salt or slow hash is needed. The
  lookup stays an indexed equality on the hash.
- **One seam, so no caller can forget:**
  - The secret becomes a type the records carry: `TokenHash`, a value class. The field is renamed (`tokenHash`), so a
    raw string cannot be stored by mistake and old rows cannot match.
  - The by-token lookup takes the raw token and hashes it inside `AuthRecordStorage`.
  - The raw token exists only on its way into the mail or the response.
- **The cancel link.** Today every re-send mails the same cancel token again, read back from storage (`findSignUpCancelTokenOf(...).token`,
  `EmailAndPasswordAuth.kt:471`). With hashes it cannot be read back.
  - *Proposed:* each mail mints its own cancel token, and every one lives as long as the account.
  - The growing delays bound the count to about 11 per account over 7 days.
  - Activation, cancel and retire remove all of them; the code removes one today (`EmailAndPasswordAuth.kt:555`).
- **No migration**, as nothing is online. Links issued before the upgrade stop working; signing up again or a new
  reset replaces them.

## Implementation notes

- **The seam:** `domain/TokenHash.kt` (value class, `TokenHash.of(raw)` = base64url SHA-256). The five one-shot
  record types take `tokenHash: TokenHash` and answer `token = null`; `Password` and `Session` keep `token` and answer
  `tokenHash = null`. `AuthRecordStorage.findByRawToken(type, realm, raw)` hashes inside; the old `findByToken`
  stays for sessions and password hashes, and finds nothing for a one-shot type (its `token` is null), so a caller
  that forgets fails closed. `Adapter`/`Vault.Repo` gain `findByTokenHash`; both repos got an index on `tokenHash`
  (there was none on `token`).
- **"The 11 and 7 configurable":** `RealmTokenConfig.maxSignUpCancelTokens = 11` is new; the 7 days are
  `pendingAccountLifetime`, which was already configurable and is what each cancel token lives. Counted exactly, the
  default delays fit the sign-up mail and 11 re-sends in 7 days (the last at about 6.7 days), so the 12th mail drops
  the sign-up mail's link; the KDoc says so.
- **Per mail:** `issueAndSendActivationToken` mints a cancel token for every mail, then
  `Services.trimSignUpCancelTokens(exceptId = new, keep = max)` removes the oldest others. `findSignUpCancelTokenOf`
  is gone; `removeSignUpCancelTokens` replaces the remove-one at activation.
- **A reset that activates** now also removes the activation and cancel links (it did not; harmless before, since a
  retire refuses an active account, but dead weight with up to 11 rows).
- **Session out of scope**, noted in ULTRA-00136.

## Test evidence

- `ActivationHardeningE2eSpec` (Karango and Monko): no stored row holds a raw token after a sign-up, a re-send and
  a reset mail, and each has its hash; the org-selection token likewise; every mailed cancel link works over 6.7
  days, past the cap only the sign-up mail's goes; after an activation no cancel row is left and none cancels.
- `EmailAndPasswordAuthSpec`: sign-up and re-send store hashes only, mint their own cancel token, trim with the cap;
  activation and a reset remove all the links. `AuthRecordStorageBaseSpec` round-trips the changed types on both
  backends (serialization).
- Green: auth JVM 285/0, funktor-all 257/0; compile sweep clean.
- Mutation-checked (2026-11-06), each red in its test, on both backends where storage is involved:
  - the activation link stored raw → red (provider unit tests, every e2e activation)
  - the cancel link stored raw → red (provider unit tests, the no-raw-token e2e, the cancel-links e2e)
  - the reset link stored raw → red (the no-raw-token e2e, the reset e2e)
  - the org-selection token stored raw → red (the org-realm e2e's hash check)
  - the lookup without hashing → red (every flow)
  - the activation removes only some cancel tokens → red (provider unit test, "After an activation…" e2e)
  - no cap → red (12 instead of 11); the cap drops the newest → red (the sign-up mail's link survives)
  - a reset leaves the sign-up links → red (provider unit test)

## Acceptance

- [x] e2e on both backends: no stored row contains a raw token. The test reads the DB row after each mail.
- [x] Each flow still works end to end: activation, reset, cancel, org selection.
- [x] Every mailed cancel link works until the account ends, and none works after an activation. (With the
      default cap of 11 the sign-up mail's goes at the 12th mail, day 6.7: maintainer's call, see the ledger.)
- [x] A serialization test for the changed record types.
- [x] Mutation checks: storing the raw token, looking up without hashing, removing only one cancel token. Each must go
      red.
- [x] CHANGELOG `## Unreleased`.
- [x] ULTRA-00127 (red team) gets the scenario "a read of the auth records yields a live link".

## Review record (the ledger)

### Round 1: level 1 (`reviewer-high`, all three charters): CLEAN (0 CRITICAL, 0 MAJOR)

| # | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1.1 | MINOR (security) | `TokenHash.kt:15` | A public constructor lets a future issuer store `TokenHash(raw)`: the seam is a convention | fixed | `private constructor`; `of` is the only way in. Green on both backends (Slumber and both Vaults read it through the property) |
| 1.2 | MINOR (impl) | the specs | Every hash assertion derives its expectation from `TokenHash.of`: MD5 or a prefix would pass | fixed | `TokenHashSpec` pins SHA-256 vectors computed outside the code; mutants Y1 (MD5), Y2 (a prefix) red |
| 1.3 | MINOR (security) | `RealmTokenConfig.kt:72` | The unsalted fast hash is safe only for long tokens, and `randomTokenByteLength` had no floor | fixed | `require(randomTokenByteLength >= 32)`; mutant Y3 |
| 1.4 | MINOR (domain) | `EmailAndPasswordAuth.kt:224,274`, `CHANGELOG.md` | Old-shape rows (`token`, no `tokenHash`) do not just "match nothing": they cannot be awoken, so listing an owner's tokens may fail | fixed (CHANGELOG) | Traced by the reviewer, not run. Nothing is live; the CHANGELOG now says to delete such dev databases' auth records (Mongo's TTL culls nothing) |
| 1.5 | MINOR (security) | `EmailAndPasswordAuth.kt:273-279` | With `maxSignUpCancelTokens = 1`, two concurrent re-sends remove each other's cancel token | fixed | `require(maxSignUpCancelTokens >= 2)`; with ≥ 2 each keeps its own and the other's (verified by the reviewer, ties included); mutant Y4 |
| 1.6 | NIT | `ActivationHardeningE2eSpec.kt:110-115` | `allRowsOf` checks the decoded record, not the stored document | recorded | Slumber writes only constructor parameters, so the decoded record holds every stored field |
| 1.7 | NIT | `AuthRecordStorageBaseSpec.kt:7` | Import order | fixed | |
| 1.8 | maintainer | `RealmTokenConfig.kt` | 7 days hold 12 mails (the 11th re-send at about 6.72 days), so the default 11 drops the sign-up mail's link: the acceptance line "every mailed cancel link works until the account ends" holds only with 12 | maintainer-decision | The maintainer said 11; the KDoc and CHANGELOG state the effect |

Out of scope, older: `AuthRealm.selectOrg` (`AuthRealm.kt:763`) ignores the `removeById` count, so two concurrent
`select-org` calls with one token both succeed; activation and reset use the count-checked consume. For the maintainer.

**Probed and CLEAN in round 1:** no raw one-shot token stored anywhere (the four creators all hash; nothing creates an
`EmailChangeToken`; funktor-demo, the TS SDK and fixtures do not touch these records); every one-shot lookup goes
through `findByRawToken`, and `findByToken` on a one-shot type cannot match; storage of the value class on both
backends (flattened string, field `tokenHash`, indexed); records' `toString()` holds only the hash; no new side
channel; the trim keeps exactly `keep`; removal at activation, cancel, retire, SSO activation and a reset that
activates; races with activation or retire leave only harmless rows, as before; "11 and 7 configurable" met; KDoc;
tests fail for the right reason; Session out of scope with a note in ULTRA-00136.

Round-1 fixes are MINOR and NIT only, so they do not loop. Green after them: auth JVM 288/0, funktor-all 257/0.

**Verdict:** gate PASS.

## History

- 2026-11-05: created.
- 2026-11-06: built, round 1 CLEAN, its MINORs fixed and mutation-checked; DONE. Docs: ULTRA-00290 notes it ("still
  moving, write last" is now settled for this part).
- 2026-11-06: DONE.
