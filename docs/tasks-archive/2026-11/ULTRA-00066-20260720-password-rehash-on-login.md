# ULTRA-00066: Password hashes never upgrade — add rehash-on-login

> **Status (2026-11-06): DONE.** gate PASS after round 2. Built on the maintainer's word (2026-11-06: "tackle all auth tickets that you can work on where no additional input is needed"); the design below was complete. Earlier status:
> **(2026-07-20): NOT STARTED.** from the password-subsystem review (2026-07-20)
>
> **Part of:** ULTRA-00248 (Auth and sessions epic)

**Plan:** none — password hashing hardening (finding 1, the headline gap)
**Security-critical:** yes

## Problem

The hasher is pluggable for *verification* — `CompoundPasswordHasher` hashes with the primary and
verifies by matching the stored `hash.id`
(`ultra/security/src/jvmMain/kotlin/password/CompoundPasswordHasher.kt:33,44,54`). But there is **no
upgrade path**: verification returns a bare `Boolean`.

- `EmailAndPasswordAuth.checkPassword` → `passwordHasher.check(plaintext, hash)` and nothing else
  (`funktor/auth/src/jvmMain/kotlin/provider/EmailAndPasswordAuth.kt:141-143`).
- `validateCurrentPassword` calls it and discards everything but the boolean
  (`EmailAndPasswordAuth.kt:388-396`).

So when the primary is later strengthened (higher Argon2 params → new `id`, e.g.
`argon2-ID-65536-2-4-32` → a stronger id) or a new primary is prepended, **existing users' hashes
stay on whatever was current when each user registered, forever** — until they manually change their
password. Adding a stronger hasher protects only new sign-ups. That is the core of "not future-proof".

## Design

1. Add to `PasswordHasher`:
   ```kotlin
   /** True when [hash] was produced by a hasher other than the current primary (needs upgrade). */
   fun needsRehash(hash: PasswordHasher.Hash?): Boolean
   ```
   `CompoundPasswordHasher.needsRehash` = `hash == null || hash.id != primary.id`. Leaf hashers:
   `hash?.id != id`.
2. On a **successful** password verification in the login path, if `needsRehash(storedHash)`,
   transparently `hash(plaintext)` with the current primary and overwrite the stored
   `AuthRecord.Password` (`createPasswordRecord` already exists as the writer,
   `EmailAndPasswordAuth.kt:~405`). Only ever rehash after a confirmed-correct password, never on
   failure, and never block/failing the login if the rewrite fails (log + continue).
3. This makes "add a stronger hasher" actually migrate the live user base as users log in.

## Related minor cleanups (fold in or split)

- **`SecureRandom.getInstanceStrong()`** (`PasswordHasher.kt` companion `strongRandom`) can *block*
  on low-entropy Linux/containers. Prefer plain `SecureRandom()` (still a CSPRNG, non-blocking on
  modern kernels) unless there's a specific reason for the strong instance.
- **Redundant `salt` field** for Argon2/Bcrypt — the encoded `hash` self-contains the salt and their
  `check()` ignores the stored salt (only PBKDF2 uses it). Now base64'd so it's delimiter-safe, but
  it's dead data for two of three hashers; consider dropping it for self-contained encoders. Note
  (2026-07-22 review): the stored value is also a *lossy* re-encoding (`getSalt()` String →
  UTF-8 → base64, not the raw salt bytes) — harmless while never read back, one more reason to drop
  it rather than ever "wire it up".
- **Malformed stored hash fails as a 500, not a failed check** (2026-07-22 catch-up review,
  pre-existing): a corrupted/adversarial stored hash makes `check()` throw (`fromBase64` /
  password4j `BadParametersException`) instead of returning false; the exception escapes the
  `AuthError`-only catch in the sign-in handler → 500 + error-oracle (and stack trace outside prod).
  Fails closed (never true), so no bypass — but wrap the string-verify boundary to treat
  undecodable input as a normal verification failure when touching this code.

## Colon-fix review record (2026-07-22)

`3fcdfb26` (positional `Hash.fromString` + base64 salt) passed its 3-agent catch-up gate: no
CRITICAL/HIGH; legacy raw-salt hashes verified as *repaired*, dispatch fail-closed, PBKDF2 salt
round-trip symmetric. The one confirmed MEDIUM — the colon-free invariant on `Hash.id`/`hash` was
implicit and unguarded (the same failure class, reintroducible by a future hasher) — is fixed:
KDoc contract on `Hash`, `asString()` now rejects a colon-bearing id/hash at write time (parse stays
total/fail-closed), plus end-to-end legacy-salt regression tests and guard tests in
`PasswordHashRoundTripSpec`.

## Spec

- [x] `PasswordHasher.needsRehash(Hash?)` on the interface + all impls (Compound / Argon2 / Bcrypt /
      PBKDF2).
- [x] Login path rehashes-and-persists with the current primary on successful verify when
      `needsRehash` is true; failures to persist do not fail the login.
- [x] Never rehashes on a failed check.

## Test evidence

- [x] Unit: a hash from a non-primary hasher → `needsRehash == true`; from the primary → false.
- [x] Unit/e2e: logging in with a legacy-hash user rewrites the stored record to the primary `id`;
      a second login reports `needsRehash == false`.
- [x] e2e via `AppSpec`/`AppUnderTest` on both DB backends (auth touches storage).

## Cross-references

- Interacts with ULTRA-00051 (auth error account enumeration) (dummy-hash timing fix) — both touch
  the sign-in verify path; sequence so the rehash write doesn't reopen a timing channel.
- Depends on the `CompoundPasswordHasher` id-dispatch design (unchanged).

## Implementation (2026-11-06)

- `PasswordHasher.needsRehash(Hash?)` / `needsRehash(String?)`: a leaf hasher, any other id; `CompoundPasswordHasher`,
  any id but its primary's. A default method, so every hasher has it without code.
- `EmailAndPasswordAuth.signIn`, after the credential proved LIVE: `rehashIfOutdated` hashes the proven password with
  the current primary and replaces the SAME record in place (`replaceIfUnclaimed`: never inserts, so a password
  changed meanwhile is never brought back; `createdAt` and `_id` stay, so "latest" and the live check are
  unaffected). A failure is logged as a warning and the sign-in goes on. `Services` gains `passwordNeedsRehash` and
  `replacePasswordRecord`.
- Not on `changePassword` / `validateCurrentPassword`: the new password is hashed fresh anyway.
- ULTRA-00051 (timing): the rewrite runs only after a correct password, which the answer itself already tells; it
  opens no new channel on a failure.
- Folded in from "Related minor cleanups": `PasswordHasher.strongRandom` is a plain `SecureRandom()` (the strong
  instance can block); a stored hash a leaf hasher cannot decode checks `false` in `CompoundPasswordHasher` instead of
  throwing out of the sign-in (a 500; mutant H1 showed Argon2 throws on an empty hash). NOT done: dropping the
  redundant `salt` field of Argon2/Bcrypt hashes, a storage-format change with no reader and no harm; it stays a note.

## Review record — the LEDGER

Round 1 (level 1, `reviewer-high` ×3, 2026-11-06), also covering the ULTRA-00130 retry follow-up. NOT CLEAN: one
MAJOR, found by all three charters.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR (all three) | `EmailAndPasswordAuth.kt:398-402` | `rehashIfOutdated` caught only `Exception`; Karango's failures are `KarangoException : Throwable`, so a failed rewrite failed the sign-in (a 500) on Karango; the unit fake threw an `IllegalStateException`, the one kind that is caught | fixed | Verified (`karango/core/.../exceptions.kt:7-10`). Now `Throwable`, rethrowing `CancellationException` and `Error`; the fake throws a plain `Throwable`; mutant G4 (back to `Exception`) red |
| 1 | (MAJOR, same root, outside the change set) | `EmailAndPasswordAuth.kt:436-441`, `provider/SsoSignUp.kt` | The "User already exists" wrap around `createForSignup` caught only `Exception`: a Karango user repo's unique-index failure stayed a 500 | fixed | Same `Throwable` catch; tests with a plain `Throwable` for the email and the Google sign-up; mutants G5, G6 red |
| 1 | MINOR (domain, security) | `CompoundPasswordHasher.kt:56-60` | The catch-all turned a BROKEN hasher into a silent "wrong password" for every user | fixed | Narrowed to `IllegalArgumentException` (password4j's `BadParametersException`, bad base64): the undecodable-input spec stays green, a broken hasher fails loudly; mutant H4 red |
| 1 | MINOR (impl, security) | `CHANGELOG.md:172` | "A stored hash … checks false" holds only for `CompoundPasswordHasher` | fixed | The entry names it, and says a directly bound leaf still throws |
| 1 | MINOR (impl) | `AuthRecordStorageBaseSpec.kt:703` (ULTRA-00130) | The overlap test changed; R5/R6 not re-checked on it | no change needed | They were: after the change, R5 red in four Karango burst tests, R6 red ("round 3"), R7 red (the run before this gate) |
| 1 | NIT (domain) | `CompoundPasswordHasher.kt:64` | Compare via `primary.needsRehash(hash)`, not `primary.id`, so a wrapping primary (a pepper) works | fixed | Delegates; the interface KDoc says when to override |
| 1 | NIT (impl) | `ActivationHardeningE2eSpec.kt:56` | Import order | fixed | |
| 1 | NIT (impl) | `EmailAndPasswordAuth.kt:397` | `replacePasswordRecord`'s Boolean is ignored | no change | Kept for callers of `Services`; the rehash does not care whether the record was gone |
| 1 | NIT (security) | `CHANGELOG.md:159` | A long line | fixed | |

Probed and CLEAN (from the reports): the rewrite runs only after a correct password, the pending check and the
live check; it never inserts and keeps `createdAt`/`_id`, so no password comes back as latest and none survives a
removal; concurrent rehashes write valid hashes of one password; which id counts as current; an unknown or corrupt
id is never rehashed; no new timing channel (ULTRA-00051); `SecureRandom()` for salts; the retry re-runs only 1200s,
≤ 8 runs and ~140 ms, re-runs of losing conditional writes cannot conflict again; the log line holds no secret.

Round 2 (level 2, `reviewer-xhigh` ×3, two-phase, 2026-11-06), one round over ULTRA-00066 (its round 2), the
ULTRA-00130 retry follow-up and ULTRA-00288 (its round 1). All three charters CLEAN in phase 1 and phase 2; every
round-1 fix held (b), no rejected CRITICAL/MAJOR to audit (c). MINORs applied as one batch, no re-review (review-loop).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 | MINOR (all three; kept in phase 2 against row 1-2) | `EmailAndPasswordAuth.kt:439`, `SsoSignUp.kt` | The `Throwable` wrap also caught the adapter's OWN `AuthError` (a realm without sign-up: `notSupported()`) and relabelled it "User already exists" | fixed | Verified (`AuthError : Throwable`). One helper for all three sign-ups, `provider/SignUpUser.kt` `createForSignupOrRefuse`, rethrows `AuthError`; tests on email, Google, GitHub; mutant B1 |
| 2 | MINOR (security) | `CompoundPasswordHasher.kt`, `EmailAndPasswordAuth.kt:397` | A compound nested in a compound cannot check its primary's hashes: the rehash replaced a working hash with an unverifiable one, a lockout | fixed | `rehashIfOutdated` writes only a fresh hash that `checkPassword` verifies; test; mutant B2 |
| 2 | MINOR (security) | `GoogleSsoAuth.kt:177` | A crafted token still escaped as a 500 (a numeric `aud`: `ClassCastException`) | fixed | `verify` maps any `Exception` but cancellation; the REAL verifier is fed five broken tokens (no network); mutant B3 |
| 2 | MINOR (all three) | `CHANGELOG.md:18`, `auth.astro:496`, `llms/funktor.md:475` | Still said GitHub's scope does not reach GitHub under hash routing | fixed | All three rewritten; grep for "scope lands" / "hash routing" clean |
| 2 | MINOR (domain, impl) | `LoginController.kt:428-469`, `GithubOAuth.kt` | Under hash routing the callback lands on the ROOT route; the stored hash was applied only on the login page, only with a `code`, and by `pushState`, which the router does not observe | fixed | `GithubOAuth.returnToStartedRoute`, called when `AuthState` is created (before the router reads the URL), puts the started route back, for any matching round trip (a denial too); the login page then only cleans the query. A second restore in the controller was built first and dropped: mutant J6 survived because `AuthState` had already done it. Tests incl. a denial; mutant J7 red |
| 2 | MINOR (domain, impl) | `CHANGELOG.md:174` | Not listed: an app's own authorize URL is ignored now; the hash-routing precondition | fixed | Both in "no compile signal" |
| 2 | MINOR (impl) | the specs | Gaps: no GitHub "User already exists" case, the id guard untested, the e2e never checked its legacy seed landed, the button's action untested | fixed (3 of 4) | GitHub case, id and address guard tests (B4, B5), the seed asserted; the button's action: NOT tested — clicking navigates the test page away (`location.href`); the action's effect is pinned by `GithubCallbackSpec` via `begin` |
| 2 | MINOR (impl) | `EmailAndPasswordAuthSpec.kt:611`, `index_jvmTest.kt:192` | `MinimalTestRealm`'s `error()` defaults are `IllegalStateException`s, which the sign-up wrap turns into the expected answer | fixed | The defaults throw `AssertionError` (an `Error`, rethrown) |
| 2 | (probe, impl; withdrawn as unverified) | `CompoundPasswordHasher.kt:58` | password4j may throw `ArrayIndexOutOfBoundsException` for too few Argon2 parameters | fixed | Confirmed by running it (`$argon2id$v=19$m=1$AAAA$AAAA`: "Index 1 out of bounds"); `IndexOutOfBoundsException` caught too; mutant B6 |
| 2 | NIT (domain, security) | `AuthRealm.kt:575` | The re-read could compare the address | fixed | Also the address the provider verified |
| 2 | NIT (all) | `GithubSignInButton.kt:20` | `onToken` never called | fixed | Removed, with the dead lambdas |
| 2 | NIT (impl) | `GithubOAuth.kt` | `Pending`, `begin`, `complete`, `authorizeUrl` could be internal | fixed | Internal; `Action` stays public (the button's parameter) |
| 2 | NIT (impl) | `CompoundPasswordHasherSpec.kt:108`; ULTRA-00288 Acceptance | Long line; "a server check" vs no server change | fixed | |
| 2 | NIT (security) | `EmailAndPasswordAuth.kt:394` | `needsRehash` outside the try | withdrawn in phase 2; moved inside anyway | |
| 2 | out of scope (domain) | `EmailAndPasswordAuth.kt:693` | A password change keeps the superseded records with their old hashes | maintainer-decision | Removing them races two concurrent changes into an account with NO password; how to remove (older than the new one, by `createdAt`) is a design choice: asked |
| 2 | observation (domain) | `GithubSsoAuth.kt:128` | The code exchange sends no `redirect_uri` | red team | Added to ULTRA-00117's SSO scenarios |

**Verdict:** gate PASS after round 2 (ULTRA-00066), after its first round (ULTRA-00288).

## History

- 2026-10-21: numbered ULTRA-00066 (was `docs/tasks/20260720-password-rehash-on-login.md`).
- 2026-11-06: built, gate PASS after round 2 (one MAJOR in round 1, fixed), DONE. No docs ticket: `needsRehash` is a
  small hasher extension point, named in the CHANGELOG; the auth docs (ULTRA-00290) cover nothing it changes.
- 2026-11-06: DONE.
