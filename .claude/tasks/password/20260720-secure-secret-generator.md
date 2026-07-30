# Add a CSPRNG secret/token generator (invites, temp passwords, reset tokens)

**Status:** TODO — from the password-subsystem review (2026-07-20)
**Plan:** none — password hashing hardening (finding 4)
**Security-critical:** yes (provisioning secrets)

## Problem

There is **no shared secure generator** for random secrets — a repo-wide grep for
`PasswordGenerator` / `generatePassword` / `randomPassword` returns nothing. `ultra/security` exposes
`PasswordHasher.strongRandom` (`PasswordHasher.kt` companion) but no user-facing generator on top.

This is a latent gap rather than a current bug — today's flows let users choose their own passwords,
and account recovery uses `AuthRecord` tokens. But the **b2b invite flow** (see
`.claude/tasks-archive/2026-07/20260720-b2b-realm.md` — "users are invited into an organisation", self-signup is
disabled) will need to provision a secret: an invite token or a one-time temp password. Without a
vetted generator, each call site risks rolling its own with a weak RNG or a biased alphabet.

## Design

- A small `SecretGenerator` in `ultra/security` built on a CSPRNG (`SecureRandom`), producing:
  - URL-safe tokens (base64url / unambiguous alphabet), configurable length/entropy;
  - optionally, human-typable temp passwords (unambiguous charset, no `0/O`/`1/l`) that satisfy the
    configured `PasswordPolicy`.
- Uniform, unbiased selection (no modulo bias). Document the entropy per generated secret.
- Reuse for reset/activation token generation if those currently roll their own (audit
  `AuthRecord` token creation for reset/activation while here).

## Spec

- [ ] `SecretGenerator` (CSPRNG-backed) with token + temp-password modes; configurable length.
- [ ] Temp-password mode yields values that pass the active `PasswordPolicy`.
- [ ] No modulo bias; entropy documented.
- [ ] Existing reset/activation token generation audited and, if weak, switched to it.

## Test evidence

- [ ] Unit: distribution/no-bias sanity; length + alphabet honoured; uniqueness across many draws.
- [ ] Unit: temp-password output satisfies `PasswordPolicy.default` (and a length-based policy).

## Cross-references

- Unblocks the b2b/b2b2c **invite** flow (`20260720-b2b-realm.md`, self-signup disabled).
- Pairs with `20260720-password-policy-modernization.md` (temp passwords must satisfy the policy).
