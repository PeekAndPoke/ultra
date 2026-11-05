# ULTRA-00291: Funktor auth: store one-shot tokens hashed (SHA-256), never in clear text

> **Status (2026-11-05): DESIGNED.** The maintainer decided on hashes on 2026-11-05. Plan to be re-confirmed before
> building.
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

## Acceptance

- [ ] e2e on both backends: no stored row contains a raw token. The test reads the DB row after each mail.
- [ ] Each flow still works end to end: activation, reset, cancel, org selection.
- [ ] Every mailed cancel link works until the account ends, and none works after an activation.
- [ ] A serialization test for the changed record types.
- [ ] Mutation checks: storing the raw token, looking up without hashing, removing only one cancel token. Each must go
      red.
- [ ] CHANGELOG `## Unreleased`.
- [ ] ULTRA-00127 (red team) gets the scenario "a read of the auth records yields a live link".

## History

- 2026-11-05: created.
