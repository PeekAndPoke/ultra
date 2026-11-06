# ULTRA-00288: Funktor auth SSO: the pre-existing MINORs of the ULTRA-00118 gate (stale retired user, GitHub OAuth state and URL, 500s)

> **Status (2026-11-06): DONE.** gate PASS after round 1. Built on the maintainer's word (2026-11-06: "tackle all auth tickets that you can work on where no additional input is needed"). Earlier status:
> **(2026-11-04): NOT STARTED.** Found by the security reviewer of the ULTRA-00118 gate, round 1 (2026-11-04).
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

4. **The GitHub authorize URL is built unencoded** (`GithubSignInButton.kt:56-57`, `LoginController.kt:396-426`; ULTRA-00118
   gate, round 2, security). `redirect_uri` is appended raw and ends in `window.location.hash`. Under hash routing
   (Kraft's default) everything after `#` is the authorize URL's fragment, so `&scope=user:email` never reaches GitHub.
   Since ULTRA-00118 every GitHub sign-in needs that scope, so a hash-routed app loses GitHub SSO. The sign-up button's
   `&auth-action=signup` is read as a GitHub parameter too, so "Sign up with GitHub" signs in. Fix: encode `redirect_uri`,
   put `scope` first, and carry the route in `state` (item 2) rather than in a fragment, which RFC 6749 §3.1.2 forbids in
   a redirect URI. UNVERIFIED: whether hash-routed GitHub sign-in ever worked end to end.

## The work

- [x] **1, stale user:** `AuthRealm.resolveSsoAccount`, the no-marker branch, re-reads the row by id (with the id
      round trip that guards Monko's key-only `findById`) and refuses a missing or retired one; it signs in with the
      fresh row. A retire renames the row BEFORE it removes its claimed marker, so "no marker" plus a fresh read is
      enough.
- [x] **2, login CSRF:** `widgets/GithubOAuth` (new): `begin` stores `{state, action, hash}` in sessionStorage (128
      bits from `crypto.getRandomValues`) and builds the authorize URL; `complete(state)` accepts the callback only with
      that state, once (removed whatever the outcome). `LoginController.handleAuthCallback` ignores a GitHub callback
      without it. The check is the browser's: the state never reaches the server, which needs no change (the SPA
      pattern; a server-issued state would need a session the anonymous login page does not have).
- [x] **3, 500s:** Google's `verify` maps `IOException` and `IllegalArgumentException` too; SSO sign-ups create the
      user through `createForSsoSignup` (`provider/SsoSignUp.kt`), which answers a failure, `Throwable`s included (a
      Karango user repo), with "User already exists", as the email sign-up does.
- [x] **4, the authorize URL:** every value encoded, `scope` first, the redirect URI is the page plus
      `?auth-callback=<provider>` with NO fragment; the action (sign in or up) and the route to return to ride in
      the stored state. Under hash routing GitHub's redirect lands on the ROOT route; `AuthState`, when created,
      puts the started route back before the router reads the URL (`GithubOAuth.returnToStartedRoute`), and the login
      page handles the callback there. `AUTH_ACTION_PARAM` and the button's dead `onToken` are gone. UNVERIFIED against GitHub
      itself (no live app): the redirect URI must sit under the GitHub app's registered callback URL, as before.

## Acceptance

- One spec per item, mutation-checked. Item 2 needs a frontend spec (`TestBed`) and a server check. (The server
  check turned out to need no server change: the state is the browser's, see "The work".)

## Review record

Reviewed in one round with ULTRA-00066's round 2 (level 2, three charters, two-phase): CLEAN on all three. The
ledger is in ULTRA-00066 ("Round 2"); every MINOR was applied, the password-records question went to the maintainer.

**Verdict:** gate PASS after round 1.

## History

- 2026-11-04: created from the ULTRA-00118 gate, round 1.
- 2026-11-04: item 4 added from round 2.
- 2026-11-06: built, reviewed with ULTRA-00066's round 2, gate PASS, DONE. The auth docs' GitHub line was corrected
  in place (ULTRA-00290's pages); no new docs ticket.
- 2026-11-06: DONE.
