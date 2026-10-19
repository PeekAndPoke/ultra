# Activation hardening: pre-registration takeover, zero-click activation, resend faucet

**Status:** TODO — maintainer decisions 2026-10-19 (below); worktree track `auth-activation`
**Plan:** `.claude/tasks/20261019-insights-auth-parallel-plan.md` → the first auth track, taken ahead of T4 (maintainer:
"next free slot" — live code with a security hole)
**Fixes:** the round-2 gate FAIL of `.claude/tasks/20260727-activation-resend-and-frontend.md` (its ledger is the input)
and the pending-account half of `.claude/tasks/20260726-sso-email-verification.md`
**Security-critical:** yes — red-team: `.claude/tasks/20260727-redteam-account-activation.md` (extended 2026-10-19)

## Why

An attacker who knows only `victim@corp.com` can pre-register it with a password of their choice, then keep a genuine
activation mail flowing to the victim (resend), and the activation link activates on page load — so a mail scanner, or
one click, opens the account to the attacker's password. If the victim used Google sign-in meanwhile, they now share an
account. Industry name: account pre-hijacking, "classic-federated merge" (Sudhodanan & Paverd, USENIX Security 2022).

## Decisions (maintainer, 2026-10-19)

1. **Activation is an explicit action.** The link opens a page with an **Activate** button; only the click (a POST)
   activates. Opening the link changes nothing.
2. **SSO sign-in on a PENDING password account** (password sign-up, never activated):
   - provider says the email is verified → the account is ACTIVATED and its sign-up **password is deleted** — it becomes
     passwordless (SSO-only); the owner can add a password later through password reset. Log a security event.
   - provider does not say verified → refuse (`invalidCredentials`).
   Already-active accounts keep today's behaviour (the rest of the SSO policy stays open in its own task).
3. **Pending accounts expire** (7 days after sign-up) and can be **cancelled** from the activation mail ("Didn't sign
   up? Cancel"). Expiry/cancel does NOT hard-delete: other collections may reference the account. The account is
   **retired** — everything in its unique key (the email) is renamed so the address is free again: `adam@example.com`
   can sign up, expire, sign up, expire … Adam's data stays with the retired account and is lost to him; merging old data
   into a new account is an app-by-app concern later, not the framework's. Its password and auth records go.
4. **Resend is rate-limited with growing delays** per pending account (e.g. 5 min, 15 min, 1 h, 4 h, …), stored on the
   pending marker — no hard cap needed; expiry (3) bounds it.

## Spec

- [ ] **Activate button** — `ActivateAccountPage` no longer redeems on load; an explicit button POSTs. Copy: "if you did
      not create this account, cancel it" with the cancel action.
- [ ] **SSO on a pending account** — in the SSO providers (`GoogleSsoAuth`, `GithubSsoAuth`, any other `EmailAndPassword`-
      linked SSO): verified → activate + delete the Password records + clear the marker (one place, shared by all SSO
      providers); unverified → refuse. Read the provider's verified claim correctly per provider (Google `email_verified`,
      GitHub: the verified primary email from the emails API).
- [ ] **Retire** — a framework mechanism to retire an account: rename its unique-key fields so the address can sign up
      again, remove its auth records (password, tokens, marker). User storage is app-owned, so this is an explicit,
      required operation on the realm's user storage (the demo and test realms implement it). **Decide the renamed-key
      format before implementing** and record it here: it must never collide with a real sign-up, stay a valid stored
      value for the field's type (`EmailAddress` canonicalisation and its forbidden characters), and stay unique across
      repeated retirements of the same address.
- [ ] **Expiry** — a worker retires pending accounts older than 7 days (configurable). **Cancel** — a mailed, single-use
      cancel link retires the pending account.
- [ ] **Resend growing delays** — per pending account, stored on the marker; the response stays honest.
- [ ] **Round-2 MAJORs that are plain fixes:** `ActivationResendToken` storage cases in `AuthRecordStorageBaseSpec` on
      both backends (round trip, expiry, type scoping); revoke the owner's earlier resend tokens when minting; consume a
      token only when its delete removed it (`count == 1`). Correct the cooldown-atomicity task's premise and red-team §8.
- [ ] **Round-2 MINORs** (maintainer: fix MINORs): `sent = false` when the mail failed; pin the resend-token lifetime;
      pin "password check before marker check" (e2e + unit); surface activation failures (the dead Retry branch) and keep
      the `NotSent` copy honest; the factually wrong KDoc/comments (the interface's email-keyed contract, "no resend
      endpoint", `resend-activation`, `AuthApi`, Mongo "culls"); frontend tests (`TestBed.preact`); the wildcard import;
      pin `removeAllByOwner(exceptId)` at storage level; consume the resend token in `AuthRealm` (mirroring
      `OrgSelectionToken`); add the tenth `AuthError.message` echo site to the error-disclosure inventory.
- [ ] **Tests:** e2e through `AppSpec` on BOTH backends (the auth e2e is Karango-only today — add a Monko app as
      `OrgIsolationE2eSpec` does); mutation-check every security and persistence change.
- [ ] **Docs follow-up at archive time:** `docs-site/src/pages/ultra/funktor/auth.astro:123-134` and
      `docs-site/src/data/llms/funktor.md:185-190` over-claim the resend token and state a rule activation did not meet.

## Out of scope (tracked elsewhere)

- Monko's auth-record TTL never expires anything (plan backlog, `e4076e46`).
- Session revocation, refresh-token renewal after a reset (`20260728-session-revocation-wiring.md`).
- The rest of the SSO linking policy for already-active accounts (`20260726-sso-email-verification.md`).
- Sign-up mail throttling per recipient (`20260727-signup-mail-throttle.md`).

## Implementation notes

## Test evidence

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**
