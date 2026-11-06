# ULTRA-00296: funktor-auth: passwords are at most 256 characters, also at sign-in

> **Status (2026-11-06): DONE.** gate PASS after round 1. The maintainer, 2026-11-06: "Let us only do the max length of 256 characters now. Please write a ticket and implement it." (A request-body size limit for funktor-rest was proposed with it and NOT taken now.)
>
> **Part of:** ULTRA-00248 (Epic: Auth and sessions)
**Security-critical:** yes (a denial-of-service guard on the credential paths). Red team: the scenario goes into
ULTRA-00127 (account activation).

## Why this exists

No password had a maximum: not the policy (`.{8,}`), not a provider, not sign-in. Anyone could send a megabyte
"password" to sign-in, activation, reset or a password change, and the server would match it against the policy
regex and run it through the hasher. Argon2 reads its input once and bcrypt only 72 bytes, so the work is linear,
not amplified, but there is no reason to accept it.

## Decide before implementing

Decided by the maintainer: 256 characters. Counted as Kotlin's `String.length` (UTF-16 units), in every realm,
whatever its own policy regex says.

## The work

- [x] `PasswordPolicy.MAX_LENGTH = 256`; `matches` checks the length first, so every place that sets a password
      (activation, reset, password change) refuses a longer one as it refuses a weak one, server and browser alike;
      `isTooLong` and `TOO_LONG_DESCRIPTION` for the message.
- [x] `asFormRule()` shows "Must be at most 256 characters long" for a too-long password, the policy's description
      otherwise.
- [x] Sign-in refuses a longer password before the user is loaded (the same answer for every address, nothing
      hashed); `validateCurrentPassword` refuses one before hashing (the password change's current password).

## Acceptance

- [x] `PasswordPolicySpec` (common: JVM and browser): 256 passes, 257 does not, for any regex.
- [x] Provider: sign-in with 257 / 100,000 characters refused before `loadByEmail`; a new password of 257 is weak, a
      current one of 257 refused before any hashing.
- [x] E2e, both backends: activation refuses 257 (`WeakPassword`, the link still good), activates and signs in with
      256, refuses a 300,000-character sign-in like a wrong password.
- [x] Browser: the form rule's message names the length. Mutants L1-L4 red.

## Review record — the LEDGER

Round 1 (level 1, `reviewer-high`, two charters by risk: implementation, security; 2026-11-06): both CLEAN.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (impl) | `EmailAndPasswordAuth.kt:868` | "No stored password is longer" is false for passwords stored before the cap; their owners are locked out unawares | fixed | The comment says an older, longer one needs a reset; the CHANGELOG's "no compile signal" line already said so |
| 1 | MINOR (impl) | `ActivationHardeningE2eSpec.kt:295` | The 300,000-character sign-in passes without the cap too (a wrong password is a 403 as well) | no change | True; the cap is pinned by the provider spec, whose fakes throw if reached (mutant L2 red there) |
| 1 | NIT (impl) | `PasswordPolicy.kt:36`, `EmailAndPasswordAuth.kt:357,869` | The rule written three times | fixed | `PasswordPolicy.isTooLong` (companion), used everywhere |
| 1 | NIT (impl, security) | `EmailAndPasswordAuth.kt:357` | `isNotBlank` scans before the length check | fixed | Length first |
| 1 | NIT (impl, security) | `ActivateAccountPage.kt:261`, `IntrospectionApi.kt:194` | A too-long password gets the regex description | fixed | `PasswordPolicy.refusalDescription(password)`, also behind `asFormRule` |
| 1 | NIT (impl) | `EmailAndPasswordAuthSpec.kt:314` | "whatever the address" with one address | fixed | Renamed |
| 1 | note (security) | `EmailAndPasswordAuth.kt:360` | The existing known/unknown-address timing gap (ULTRA-00051 §3) | no change | Not widened; a dummy hash there must come after the length check, which the order allows |

**Verdict:** gate PASS after round 1. funktor-auth JVM 322/0, browser 37/0, funktor-all 266/0, compile sweep clean;
mutants L1-L4 red.

## History

- 2026-11-06: created and built on the maintainer's word.
- 2026-11-06: gate PASS after round 1, DONE. No docs ticket: the auth docs say a password follows the realm's policy;
  the cap is in the CHANGELOG and the KDoc.
- 2026-11-06: DONE.
