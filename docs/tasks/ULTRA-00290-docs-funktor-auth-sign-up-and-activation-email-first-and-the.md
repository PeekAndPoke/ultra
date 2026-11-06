# ULTRA-00290: Docs: funktor auth sign-up and activation, email first and the 0.114 activation hardening

> **Status (2026-11-04): NOT STARTED.** the docs follow-up ULTRA-00231 owns for both changes
>
> **Part of:** ULTRA-00248 (Epic: Auth and sessions)

## Why this exists

ULTRA-00222 (activation hardening, 0.114.0) deliberately wrote no docs because ULTRA-00231 (email first signup) was
about to move the surface again; ULTRA-00231 owns ONE docs task for both, written against settled code. ULTRA-00231
already rewrote the activation section of `docs-site/src/pages/ultra/funktor/auth.astro` and
`docs-site/src/data/llms/funktor.md` (it described the removed resend flow). The rest is open:

- The flows: sign-up (address only), the activation page (choose the password, signed in), signing up again (the
  re-send under growing delays, silent), cancel ("Didn't sign up?"), expiry after `pendingAccountLifetime`, retirement
  (the hashed key, `AccountRetirement`), `PendingAccountsExpiryWorker`, the reset that also activates, SSO on a pending
  account. The endpoint table lacks `POST /auth/{realm}/activate/cancel`.
- The breaking-API lists of both releases, re-checked against the code: `CHANGELOG.md` 0.114.0 and `## Unreleased`
  (ULTRA-00231's entry), and ULTRA-00222's "Implementation notes".
- Every upgrade trap with no compile signal, from both: the four of ULTRA-00222 round 1 and its round 2/3 additions
  (listed in its archived "Docs follow-up" box), and ULTRA-00231's (CHANGELOG `## Unreleased`).
- The library skill (what will bite you), if the docs-site skill asks for one.
- **Added by ULTRA-00289 (2026-11-06), settled:** the activation page asks for the name
  (`AuthActivateAccountRequest.displayName`, required; sign-up takes none); the rule `AuthDisplayName` with
  `refusalOf` and its messages; the refusal answer `AuthActivateAccountResponse.failure` (`WeakPassword`,
  `InvalidDisplayName`, a 200); `AuthUserAdapter.activate(user, ActivateUserParams)`;
  `RealmTokenConfig.activationLinkOverlap` (30 min, 0 = rotate); the org handoff
  (`AuthState.takeActivationOrgSelection()`); its "no compile signal" list in `CHANGELOG.md` `## Unreleased`.
- **Added by ULTRA-00291 and ULTRA-00293 (2026-11-06), settled:** one-shot tokens stored as `TokenHash`, a cancel
  link per mail capped by `RealmTokenConfig.maxSignUpCancelTokens`, links from before the upgrade dead; SSO names
  meet `AuthDisplayName`, else the local part. Both CHANGELOG entries list their silent changes.

## Decide before implementing

## The work

## Acceptance

- `auth.astro` and `llms/funktor.md` move together and agree with the code; the docs site builds; compact beats
  comprehensive (`.claude/skills/docs-site/`).

## History

- 2026-11-04: created.
