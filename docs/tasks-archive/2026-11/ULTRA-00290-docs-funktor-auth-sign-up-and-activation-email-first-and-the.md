# ULTRA-00290: Docs: funktor auth sign-up and activation, email first and the 0.114 activation hardening

> **Status (2026-11-06): DONE.** Written against settled code, checked by the coordinator (the worker's 15-minute interval, the cancel-link cap, the messaging fix); no feature gate for a docs ticket.
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

Written 2026-11-06 against the working tree (incl. the uncommitted `maxSignUpCancelTokens = 12`). Every claim was
checked against `funktor/auth/src/**` and `funktor/all/.../PendingAccountsExpiryWorker.kt`, not against tickets.

- [x] `auth.astro` — the realm example implements `activate` and `retire` (it no longer compiled without them); the
      activation section rewritten whole as "Sign-up and activation": the flow and the activation answer
      (`failure`, a 200; dead link without a reason; the org handoff `takeActivationOrgSelection()`), "The life of
      a pending account" table (open, sign up again, cancel, reset, SSO, sign-in, expiry with
      `PendingAccountsExpiryWorker`), the `RealmTokenConfig` table (`maxSignUpCancelTokens` 12), Retirement
      (`AccountRetirement`, key format, `retiredKeyPrefix`, the post-check, the mail guard), the display name
      (`AuthDisplayName`, `refusalOf`, a verified message, SSO names), hashed one-shot tokens. The Auth API
      paragraph names org selection and `POST /auth/{realm}/activate/cancel`. New closing section "Upgrading to
      email-first sign-up": the no-compile-signal traps of 0.114.0, ULTRA-00231, -00289, -00291, -00293 (and
      ULTRA-00118's Google/session lines), re-checked against the code; the API list is left to the CHANGELOG
      (compile errors find it). The closing aside no longer says "That's it".
- [x] `llms/funktor.md` — the same content, denser: the `AuthUserAdapter` contract, the section, both tables, the
      three sub-sections, the endpoint table gains `select-org` and `activate/cancel`, the upgrade list.
- [x] `messaging.astro` + the mirror's messaging part — `AccountActivationEmailTemplate.Params` shows `cancelUrl`;
      the "a forgotten value is a compile error" claim corrected (an ignored value is not).
- [x] Library skill: none exists for funktor auth and the docs-site skill does not require one; none created (skills
      change on master by the coordinator).
- Re-check findings: `CHANGELOG.md` 0.114.0 (fixed by the coordinator, 2026-11-06) names the adapter's conditional writes
  `replaceIfClaimedBy`/`removeIfClaimedBy`; on `AuthRecordStorage.Adapter` they are `replaceRecordIfClaimedBy`/
  `removeRecordIfClaimedBy` (the Vault `Repo` has the short names). ULTRA-00222's trap "a `TODO()` retire throws
  an Error" is obsolete: `retirePendingAccount` catches `Throwable` and leaves the account claimed.

## Acceptance

- [x] `auth.astro` and `llms/funktor.md` move together and agree with the code; compact beats comprehensive
      (`.claude/skills/docs-site/`).
- [x] The docs site builds: `pnpm build` in `docs-site/`, 115 pages, no errors (2026-11-06).
- [x] Nothing under `docs-site/public/` edited; no version hardcoded (the changelog link uses `githubRepo`).

## History

- 2026-11-04: created.
- 2026-11-06: docs written (auth page, messaging sample, LLM mirror); site builds. Awaiting the coordinator's review
  and commit.
- 2026-11-06: DONE.
