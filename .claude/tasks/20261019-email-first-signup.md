# Email-first sign-up: the password is chosen when activating

**Status:** TODO — AFTER 0.114.0 (maintainer decision 2026-10-19: "B after the release"); the first auth task after it
**Plan:** `.claude/tasks/20261019-insights-auth-parallel-plan.md` (auth tracks); follows
`20261019-activation-hardening.md` (its parked row "Activate requires a password" is answered by THIS design instead)
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
- [ ] E2e on both backends; frontend via `TestBed.preact`; mutation-checked (a sign-up that stores a password must go
      red; activation without the token must go red).
