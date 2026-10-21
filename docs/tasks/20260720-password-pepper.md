# Add an optional server-side pepper to password hashing

**Status:** TODO — from the password-subsystem review (2026-07-20)
**Plan:** none — password hashing hardening (finding 2)
**Security-critical:** yes (defence-in-depth for a DB-only compromise)

## Problem

Hashing mixes only the password and a per-hash salt — there is no server-held **pepper** (a secret
not stored in the database). See `Argon2PasswordHasher.hash`
(`ultra/security/src/jvmMain/kotlin/password/Argon2PasswordHasher.kt`), `BcryptPasswordHasher.hash`,
`PBKDF2WithHmacSHA256PasswordHasher.hash`.

Consequence: a **database-only** leak (SQLi, backup theft, read-replica exposure) hands an attacker
everything needed for offline cracking. Argon2id's cost makes that expensive, but a pepper makes a
DB-only leak *uncrackable* without also compromising the application secret — a materially different
breach bar.

## Design

- Introduce an app-configured secret pepper (from the existing secret/config chain, e.g. alongside
  `funktor.auth.jwt.signingKey`), never persisted with the hash.
- Apply it as a keyed pre-hash (HMAC the password with the pepper before the KDF) or via
  password4j's native pepper support. Keep it hasher-agnostic so it composes with the compound
  design and the rehash-on-login upgrade path (`20260720-password-rehash-on-login.md`).
- **Rotation:** peppers must be rotatable. Store a pepper key-id with the hash (or version the
  format) so old and new peppers can both verify, and let rehash-on-login migrate to the current
  pepper. Decide the storage shape up front — retrofitting rotation later is painful.
- Keep it **optional** (no pepper configured → current behaviour) so it doesn't become a hard
  dependency for simple deployments.

## Spec

- [ ] Optional pepper wired from config; absent → unchanged behaviour.
- [ ] Pepper applied consistently on hash and verify, across all hashers.
- [ ] Pepper key-id / versioning so rotation is possible; rehash-on-login migrates old→new.
- [ ] Secret never logged, never serialized into the stored hash.

## Test evidence

- [ ] Unit: same password + different pepper → non-matching; verify only succeeds with the correct
      pepper.
- [ ] Unit: rotation — a hash made with pepper v1 still verifies after v2 is configured, and is
      rehashed to v2 on next successful login.

## Cross-references

- `20260720-password-rehash-on-login.md` (rotation rides the same upgrade path).
- Reuses the app secret/config mechanism used for JWT signing keys.
