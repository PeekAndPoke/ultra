# ULTRA-00067: Add a CSPRNG secret/token generator (invites, temp passwords, reset tokens)

> **Status (2026-11-06): DONE.** gate PASS after round 1. Built on the maintainer's word (2026-11-06: "tackle all auth tickets that you can work on where no additional input is needed"); the design below was complete. Earlier status:
> **(2026-07-20): NOT STARTED.** from the password-subsystem review (2026-07-20)
>
> **Part of:** ULTRA-00248 (Auth and sessions epic)

**Plan:** none — password hashing hardening (finding 4)
**Security-critical:** yes (provisioning secrets)

## Problem

There is **no shared secure generator** for random secrets — a repo-wide grep for
`PasswordGenerator` / `generatePassword` / `randomPassword` returns nothing. `ultra/security` exposes
`PasswordHasher.strongRandom` (`PasswordHasher.kt` companion) but no user-facing generator on top.

This is a latent gap rather than a current bug — today's flows let users choose their own passwords,
and account recovery uses `AuthRecord` tokens. But the **b2b invite flow** (see
ULTRA-00063 (b2b realm) — "users are invited into an organisation", self-signup is
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

- [x] `SecretGenerator` (CSPRNG-backed) with token + temp-password modes; configurable length.
- [x] Temp-password mode yields values that pass the active `PasswordPolicy`.
- [x] No modulo bias; entropy documented.
- [x] Existing reset/activation token generation audited and, if weak, switched to it.

## Test evidence

- [x] Unit: distribution/no-bias sanity; length + alphabet honoured; uniqueness across many draws.
- [x] Unit: temp-password output satisfies `PasswordPolicy.default` (and a length-based policy).

## Implementation (2026-11-06)

- `ultra/security/.../SecretGenerator.kt`: `token(byteLength = 32)` (base64url, no padding; below 16 bytes refused),
  `string(length, alphabet)` (each character by `SecureRandom.nextInt(bound)`, which rejects rather than taking a
  modulo; an alphabet that repeats a character, or has fewer than two, is refused), `tempPassword(length = 16,
  alphabet, accept)` (65 unambiguous characters, about 96 bits; redrawn until `accept`, e.g. `PasswordPolicy::matches`,
  takes it, at most 1000 draws). In ultra/security, so it cannot depend on funktor-auth's `PasswordPolicy`: the
  caller passes the check.
- Audit: reset, activation, cancel and org-selection tokens come from `AuthRandom` (256 bytes, `SecureRandom`): not
  weak, left on it. `AuthRandom` moved from `SecureRandom.getInstanceStrong()` (can block) to `SecureRandom()`, as
  `PasswordHasher.strongRandom` did in ULTRA-00066.
- Tests: `SecretGeneratorSpec` (lengths, URL-safe alphabet, 10,000 draws without a repeat, 650,000 characters each
  within ±6% of uniform, refusals, redraw); `AuthRandomSpec` draws 200 temp passwords against
  `PasswordPolicy.default`. Mutants T1 (a byte modulo), T2 (standard base64), T3 (policy ignored), T4 (a repeating
  alphabet), T5 (no 128-bit floor) red.

## Review record — the LEDGER

Round 1 (level 1, `reviewer-high` ×3, 2026-11-06): all three CLEAN. MINORs applied as one batch.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (domain) | `SecretGenerator.kt:64` | `tempPassword` had no strength floor (6 digits = 18 bits passed), `token` has one | fixed | `MIN_TEMP_PASSWORD_BITS = 64`, refused below; test; mutant T6 |
| 1 | MINOR (impl) | `SecretGenerator.kt:52` | A non-BMP character in the alphabet splits into lone surrogates | fixed | Refused; test; mutant T7 |
| 1 | MINOR (impl, domain) | the ticket | "and a length-based policy" ticked, not tested; the default length cannot meet a longer minimum | fixed | `SecretGeneratorPolicySpec`: `^.{20,}$` throws at length 16, always met at 20; the KDoc says to pass a longer length |
| 1 | MINOR (domain) | `SecretGenerator.kt:23` | Symbols stop double-click selection and read as punctuation | fixed (KDoc) / forwarded | KDoc: show monospaced in a select-all box; ULTRA-00065 notes that a length-first policy lets the default drop symbols |
| 1 | NIT (all) | `SecretGeneratorSpec.kt:48` | The bias ratio is 4/3, not 1.25 | fixed | |
| 1 | NIT (impl) | `SecretGenerator.kt:61` | `MAX_DRAWS` throws also for a policy met almost never | fixed | KDoc |
| 1 | NIT (impl, domain) | `SecretGenerator.kt:19` | One KDoc for four constants; "nothing a reader can confuse" overclaims | fixed | One KDoc each; "the worst look-alikes" |
| 1 | NIT (impl) | `SecretGeneratorSpec.kt:67` | The redraw assertion passes 15/16 with the policy ignored | fixed | 200 draws against "starts with a digit"; mutant T3 red |
| 1 | NIT (domain) | `AuthRandomSpec.kt:56` | The policy test sits in `AuthRandomSpec` | fixed | Own spec |
| 1 | NIT (security) | `AuthGenerateJwtSigningSecretCliCommand.kt:16` | The JWT-secret CLI takes `--length 1` | no change | Pre-existing, an operator's own choice; noted for ULTRA-00178's docs |
| 1 | SURPLUS (security) | `AuthRandom.kt:16` | `secureRandom` public | no change | Public before this change; a `setSeed` on NativePRNG/DRBG only adds seed |
| 1 | note (impl, domain) | — | Two CSPRNG holders; `SecretGenerator` not in the kontainer | for the invite flow | ULTRA-00063 decides when it consumes it; red-team scenarios go there too |

**Verdict:** gate PASS after round 1. ultra-security 219/0, funktor-auth 317/0; mutants T1-T7 red.

## Cross-references

- Unblocks the b2b/b2b2c **invite** flow (ULTRA-00063 (b2b realm), self-signup disabled).
- Pairs with ULTRA-00065 (password policy modernization) (temp passwords must satisfy the policy).

## History

- 2026-10-21: numbered ULTRA-00067 (was `docs/tasks/20260720-secure-secret-generator.md`).
- 2026-11-06: built, gate PASS after round 1, DONE. No docs ticket yet: no consumer; the invite flow documents it.
- 2026-11-06: DONE.
