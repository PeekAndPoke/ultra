# Email-first sign-up: the password is chosen when activating

**Status:** TODO — AFTER 0.114.0 (maintainer decision 2026-10-19: "B after the release"); the first auth task after it
**Plan:** `.claude/tasks/20261019-insights-auth-parallel-plan.md` (auth tracks); follows
`.claude/tasks-archive/2026-10/20261019-activation-hardening.md` (merged `ed8d3b38`, ships in 0.114.0; its parked row "Activate requires a password" is answered by THIS design instead)
**Security-critical:** yes — red-team: `.claude/tasks/20260727-redteam-account-activation.md`

## Why

Activation hardening leaves one residual: a victim who clicks Activate on an attacker's pre-registration activates the
ATTACKER's password. An unverified account must never hold a credential (Sudhodanan & Paverd, USENIX Security 2022). The
known pattern (Slack, Notion, Atlassian): sign up with the email only; the mailed link opens "Choose your password";
setting it activates the account. The attacker never sees the link, so never holds a credential.

## Spec (re-confirm with the maintainer before implementing — this rewrites the activation flow)

- [ ] `SignUp` (password provider) takes the email and profile fields, NO password; creates the pending account + marker.
- [ ] The activation page asks for a new password (the realm's password policy); setting it activates (marker removed,
      Password record created) — one POST, single-use token.
- [ ] Verified SSO on a pending account: just activates (no password to delete); unverified → refuse (unchanged).
- [ ] Resend: signing up again with a pending address re-sends the link under the growing delays — the resend-token
      flow (`ActivationResendToken`, sign-in → `ActivationRequired` → resend) goes away. Sign-in of a pending account
      answers like a wrong password.
- [ ] Cancel, expiry, retire (hashed key) unchanged.
- [ ] API break (pre-1.0), stated in the docs follow-up: `SignUp` request, the activate request, the removed resend
      endpoint/token.
- [ ] **Owns the auth docs follow-up for BOTH changes** — activation hardening deliberately created no docs task (the
      surface was about to move again). Write ONE docs task when this one is archived, covering: the activation-hardening
      "Docs follow-up" box (in its archived task: stale `auth.astro:123-134` / `llms/funktor.md:185-190`, the breaking-API
      list re-checked against the final code, and every upgrade trap with no compile signal), minus what this task
      removes. The 0.114.0 `CHANGELOG.md` entry is the interim record.
- [ ] E2e on both backends; frontend via `TestBed.preact`; mutation-checked (a sign-up that stores a password must go
      red; activation without the token must go red).

## What this REMOVES — the simplification (recorded 2026-10-19 at the maintainer's request)

File references are to the activation-hardening branch (`wt/auth-activation`) as of its round-1 fix batch; re-grep
before relying on a line — rounds 2–4 moved them (claim-first retire, the sign-in re-read, cancel ordering).

- **The whole resend-token flow.** Today a pending user can only get a new link by signing in with the sign-up password:
  sign-in answers `ActivationRequired` with a short-lived `ActivationResendToken`, which the page then trades for a
  resend. With no password at sign-up there is nothing to sign in with, so it all goes:
  - the `ActivationResendToken` auth-record type (`funktor/auth/src/jvmMain/kotlin/domain/AuthRecord.kt`) and its
    storage cases (`AuthRecordStorageBaseSpec`, both backends);
  - minting it on sign-in, revoking the owner's earlier ones, consuming it only on `count == 1`
    (`AuthRealm.kt`, `EmailAndPasswordAuth.kt`);
  - the `ActivationRequired` sign-in answer (`AuthSignInResponse.kt`, `AuthError.kt`) and the "password check before
    marker check" rule that exists only because of it;
  - the resend endpoint and its request/response (`AuthResendActivationRequest.kt`, `AuthResendActivationResponse`,
    `AuthApiClient.kt`, `AuthLoginApi.kt`, `AuthProvider.resendActivation`);
  - the frontend's in-memory resend carrier and its Retry/"we already sent one" copy (`AuthState.kt`,
    `LoginController.kt`, `ActivateAccountPage.kt`);
  - the tests that pin all of it (`AccountActivationEmailE2eSpec`, `ActivationHardeningE2eSpec` resend cases,
    `AuthRealmActivationBaseSpec`, `EmailAndPasswordAuthSpec`, `ActivateAccountPageSpec`).

  **Resend becomes:** signing up again with a still-pending address re-sends the link, under the same growing delays on
  the marker. One path instead of three.
- **SSO on a pending account** loses its most delicate step: "delete the sign-up password FIRST, then the marker" and the
  fault-injection test pinning that order (`AuthRealm.resolveSsoAccount`, `SsoPendingAccountBaseSpec`). A pending
  account has no password, so a verified SSO sign-in just activates it.
- **The attacker's credential never exists**, so the scenarios built around it stop applying: the one-click Activate
  pre-hijack (the residual decision 1 accepted), "reset supersedes the attacker's password" (`findLatest` ordering), and
  the red-team items about the attacker polling sign-in or rotating the token through resends. Re-read
  `20260727-redteam-account-activation.md` and retire what no longer applies.
- **Retire gets simpler:** there is no Password record on a pending account to remove.

What it ADDS is small: a password field on the activation page (with the realm's policy) and a `Password` record created
by the activation POST.
