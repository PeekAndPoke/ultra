# ULTRA-00288: Funktor auth SSO: the pre-existing MINORs of the ULTRA-00118 gate (stale retired user, GitHub OAuth state, 500s)

> **Status (2026-11-04): NOT STARTED.** Found by the security reviewer of the ULTRA-00118 gate, round 1 (2026-11-04).
> All three predate ULTRA-00118 and are out of its scope (`out-of-scope → follow-up task`).
>
> **Part of:** ULTRA-00248 (Epic: Auth and sessions)

**Security-critical:** yes. Red-team: ULTRA-00117 (redteam value class emailaddress) §6 for the 500 bodies.

## Why this exists

1. **A stale user can get one session on a row that was just retired** (`AuthRealm.kt`, `resolveSsoAccount`, the
   `marker == null` branch). `isRetired` is checked on the user the provider loaded earlier. If a retire renames the row
   and removes its claimed marker between that load and the marker read, the stale user goes to `issueSignIn`, and a JWT
   is minted for the retired row's id. The JWT lasts at most one lifetime, because `refreshToken` refuses retired rows.
   Since ULTRA-00118 it also needs a provider-verified address, so only the address's owner can hit it. The password
   path closes the same race by re-reading after the marker read (`EmailAndPasswordAuth.kt:330`).
   Fix: in that branch, reload the user by id and refuse if the row is gone or retired.
2. **GitHub OAuth has no `state` parameter: login CSRF** (`GithubSignInButton.kt:56-57`, `GithubSsoAuth.kt`). A victim
   who opens a callback carrying the attacker's code is signed in to the attacker's account and may enter data there.
   This is not a takeover of the victim's account. Fix: a random `state` in sessionStorage, compared on the callback.
3. **Some SSO failures answer 500, not a refusal.** SSO `createForSignup` is not wrapped the way
   `EmailAndPasswordAuth.kt:366-371` wraps it, so a duplicate-key race escapes. Google's token verification catches only
   `GeneralSecurityException`, so an `IOException` or `IllegalArgumentException` from a malformed token escapes too.
   All of these fail closed, but outside production the 500 body carries a stack trace. Fix: map them to `AuthError`.

## The work

## Acceptance

- One spec per item, mutation-checked. Item 2 needs a frontend spec (`TestBed`) and a server check.

## History

- 2026-11-04: created from the ULTRA-00118 gate, round 1.
