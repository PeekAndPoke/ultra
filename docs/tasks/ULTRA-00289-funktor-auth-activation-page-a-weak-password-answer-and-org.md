# ULTRA-00289: Funktor auth activation page: a weak-password answer, and org realms (OrgSelectionRequired, no org yet)

> **Status (2026-11-04): NOT STARTED.** from the ULTRA-00231 gate, round 1 (2026-11-04)
>
> **Part of:** ULTRA-00248 (Epic: Auth and sessions)

**Security-critical:** no (UX of a security flow; the server side is ULTRA-00231's, gated).

## Why this exists

Two MINORs of the ULTRA-00231 gate (round 1, domain and implementation charters), confirmed against `11f49ed3`:

1. **A weak password is shown as a dead link.** `AuthLoginApi.activateAccount` answers every `AuthError`, including
   `weakPassword`, with `400 {success: false}` and no info — the same body as a used or unknown link. The server left
   the link usable (the policy is checked before the token), but `ActivateAccountPage` shows "This link is not valid
   any more … sign up again", and signing up again inside the delay sends nothing. Reachable only when the page's
   policy check and the server's disagree (a `PasswordPolicy.regex` that JS and the JVM read differently), or a
   custom client skips it. `ActivateAccountPageSpec`'s fake server never answers 400.
2. **Org realms.** `AuthRealm.activate` signs in through `issueSignIn`. In an `OrgPolicy.Required` realm:
   - 0 orgs: `noOrganisationAccess` is caught, `signIn = null`, and the page says "You can sign in now", which is
     untrue for this user.
   - n orgs: `OrgSelectionRequired` mints an `OrgSelectionToken`; `AuthState.activateAccount` applies only `Success`,
     so the token is wasted and the user signs in again to pick the org.
   Untested: every realm with sign-up in this repo is org-less (`TestUserRealm`).

## Decide before implementing

- How the activation endpoint tells a weak password apart without telling anything about the token: a field on
  `AuthActivateAccountResponse`, or a fixed `withInfo` the page reads.
- Whether `AuthState.activateAccount` hands an `OrgSelectionRequired` to `pendingOrgSelection` and routes to the login
  page's picker, or `AuthRealm.activate` should not mint the token at all for n orgs.

## The work

## Acceptance

- A page spec with a 400 weak-password answer; an e2e on an org realm with sign-up (0, 1 and n orgs).

## History

- 2026-11-04: created.
