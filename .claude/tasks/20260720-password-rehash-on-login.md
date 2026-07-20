# Password hashes never upgrade — add rehash-on-login

**Status:** TODO — from the password-subsystem review (2026-07-20)
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
  it's dead data for two of three hashers; consider dropping it for self-contained encoders.

## Spec

- [ ] `PasswordHasher.needsRehash(Hash?)` on the interface + all impls (Compound / Argon2 / Bcrypt /
      PBKDF2).
- [ ] Login path rehashes-and-persists with the current primary on successful verify when
      `needsRehash` is true; failures to persist do not fail the login.
- [ ] Never rehashes on a failed check.

## Test evidence

- [ ] Unit: a hash from a non-primary hasher → `needsRehash == true`; from the primary → false.
- [ ] Unit/e2e: logging in with a legacy-hash user rewrites the stored record to the primary `id`;
      a second login reports `needsRehash == false`.
- [ ] e2e via `AppSpec`/`AppUnderTest` on both DB backends (auth touches storage).

## Cross-references

- Interacts with `20260720-auth-error-account-enumeration.md` (dummy-hash timing fix) — both touch
  the sign-in verify path; sequence so the rehash write doesn't reopen a timing channel.
- Depends on the `CompoundPasswordHasher` id-dispatch design (unchanged).
