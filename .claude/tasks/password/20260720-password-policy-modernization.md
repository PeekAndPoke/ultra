# Modernize the password policy (NIST: length + breach-list over composition rules)

**Status:** TODO — from the password-subsystem review (2026-07-20)
**Plan:** none — password hashing hardening (finding 3)
**Security-critical:** yes (credential strength)

## Problem

The default policy is a **composition rule**
(`funktor/auth/src/commonMain/kotlin/model/PasswordPolicy.kt:12-13`):

```
^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$
```

Good news: it **is enforced server-side**, not just in the UI form rule —
`EmailAndPasswordAuth.kt:243` (`signUp`) and `:274` (`setPassword`) both
`throw AuthError.weakPassword()` on a policy miss. So this is about *modernizing* the rule, not
plugging a bypass.

But **NIST SP 800-63B** now *deprecates* mandatory composition rules in favour of:

- **length** as the primary strength lever (≥ 12, allow long passphrases, don't cap unreasonably),
- **screening against known-breached passwords** (the single highest-value check — a compliant
  `Passw0rd!` passes the current regex yet is on every breach list),
- no forced periodic rotation, allow paste / all Unicode.

The current regex both **over-restricts** (blocks strong passphrases lacking a symbol) and
**under-protects** (accepts breached-but-compliant strings).

## Design

- Keep `PasswordPolicy` pluggable (it already is — realm-configurable, `AuthRealm.passwordPolicy`),
  but make the **default** length-based (e.g. ≥ 12) rather than composition-based; keep the regex
  escape hatch for deployments that must satisfy legacy compliance.
- Add a **breached-password check**: HaveIBeenPwned range API via k-anonymity (SHA-1 prefix, only
  the first 5 hex sent, compare suffixes locally) — no plaintext leaves the server. Make it an
  injectable check so it can be stubbed/offline in tests and disabled by config.
- Wire the breach check into the same two server-side gates (`signUp` `:243`, `setPassword` `:274`)
  as a distinct `AuthError` code (or reuse `WEAK_PASSWORD`).

## Spec

- [ ] Default policy is length-first (composition optional, still configurable per realm).
- [ ] Breached-password screening on sign-up and set-password, server-side, k-anonymity, injectable
      + config-disableable.
- [ ] No plaintext or full hash sent to any third party.

## Test evidence

- [ ] Unit: policy accepts a long passphrase; rejects too-short.
- [ ] Unit: a known-breached password is rejected via a stubbed breach client; unknown accepted.
- [ ] e2e: `signUp` / `setPassword` reject a breached password with the weak-password outcome.

## Cross-references

- `20260720-auth-error-account-enumeration.md` (the `weakPassword` code + the two enforcement sites).
- Realm-configurable policy already exists (`AuthRealmModel.passwordPolicy`); this changes defaults +
  adds a screening step, not the plug-in shape.
