# Activation hardening: pre-registration takeover, zero-click activation, resend faucet

**Status:** IMPLEMENTED 2026-10-19 on `wt/auth-activation` (`7826ea5c..`), awaiting `/feature-review` — maintainer
decisions 2026-10-19 (below); worktree track `auth-activation`
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

## Design decisions (track agent, 2026-10-19 — made before building)

**Retire mechanism.** `AuthUserAdapter.retire(user, retirement: AccountRetirement): Stored<USER>` — ABSTRACT, like
`createForSignup`: every realm must spell it out, and a realm without sign-up throws `AuthError.notSupported()` exactly
as it already does for `createForSignup`. The framework owns everything around it, in `AuthRealm.retirePendingAccount`:
refuse unless the PENDING marker exists → remove every auth record of the owner in the realm EXCEPT the marker → ask the
realm to rename → **verify** that `loadByEmail(original)` no longer resolves to this account (a no-op `retire` is caught
here, logged, and the account stays pending) → remove the marker LAST. Every intermediate state is still "pending", so
a crash leaves an account the next sweep retires again, never a live one.

**Renamed-key format:** `<original email>#retired-<32 hex>.invalid`, e.g.
`adam@example.com#retired-3f2a9c0e5b7d41a2a8c6e0f1d2b3c4a5.invalid`.
- *Never collides with a sign-up.* Every account-creating path asserts `EmailAddress.isValidFormat`. The suffix has no
  `@`, so it always lands in the DOMAIN part, and a domain containing `#` matches neither alternative of `EmailRegex`
  (hostname labels are `[a-z0-9-]`; the IP-literal form must end in `]`). So the value fails `isValidFormat` whatever
  the original was — including a legacy, never-validated SSO address — and no sign-up can produce it.
- *Valid stored value.* Lowercase ASCII, no control or line-separator characters, non-blank; the original is truncated
  so the whole stays within `EmailAddress.MAX_LENGTH` (254).
- *Unique across repeated retirements.* The 32 hex are the first 16 bytes of SHA-256 over the account's `_id`. Each
  retirement retires a DIFFERENT account, and a retired account keeps its `_id`, so two retirements of
  `adam@example.com` never share a value. Deterministic, so two concurrent retirements of the same account write the
  same value instead of fighting over the unique index.
- *Undeliverable.* `.invalid` is reserved by RFC 2606/6761 and never resolves, so even a lenient mailer that accepts
  `#` in a domain (RFC 5322 dot-atom does) cannot deliver it. The framework also refuses to mail a retired address.
- *Unique indexes.* Values are distinct strings; nothing about them strains a Karango persistent or a Mongo index.
- The original stays readable, so support can find it and a later app-level merge has something to match on.
- `AccountRetirement.rename(value)` renames any OTHER unique field the same way (`<value>#retired-<hex>`).

**Expiry without a worker dependency.** `funktor/auth` cannot use `funktor/cluster`'s `Worker`: `funktor/inspect`
depends on `funktor/auth` and `funktor/cluster` on `funktor/inspect`, so it would be a cycle. So expiry is enforced at
USE time and the worker is only garbage collection:
- the activation link's lifetime is capped at the pending account's expiry, so nothing activates after it;
- resend refuses an expired account;
- sign-up of an address held by an EXPIRED pending account retires that account inline and proceeds;
- `AuthSystem.retireExpiredPendingAccounts()` sweeps; the demo registers a cluster worker for it. The framework-wide
  home for that worker is `funktor/all`'s `Funktor` module, outside this track's edit scope — recommended there.

**Cancel** is authorized by the activation token itself (whoever can activate may also decline), on its own page
action and endpoint, POST-only for the same scanner reason as activation, and single-use by delete count.

**Growing resend delays** live on the marker (`PendingActivation.resends`, `lastSentAt`), default 5 min, 15 min, 1 h,
4 h, 12 h, then 24 h repeating. The slot is reserved (marker saved) BEFORE the mail goes out. The response stays honest:
`sent` plus `nextResendAt`.

## Spec

- [x] **Activate button** — `ActivateAccountPage` no longer redeems on load; an explicit button POSTs. Copy: "if you did
      not create this account, cancel it" with the cancel action.
      *(`ActivateAccountPage.kt:124` — a token only selects the `Choose` state; `activateNow` `:134` / `cancelNow` `:150`
      run on a click. The mail's cancel link mounts the same page in `Mode.Cancel`, `AuthFrontend.kt:50`.)*
- [x] **SSO on a pending account** — verified → activate + delete the Password records + clear the marker (one place,
      shared by all SSO providers); unverified → refuse. Google `email_verified`, GitHub the verified primary email.
      *(`AuthRealm.resolveSsoAccount` `AuthRealm.kt:474`, password first `:487`, marker last; Google `GoogleSsoAuth.kt:172,
      176,207`; GitHub `GithubSsoAuth.kt:219,228,268`, `/user/emails` `:160`, `user:email` scope `GithubSignInButton.kt:57`.
      The claim is read lazily — an active account never triggers the `/user/emails` call.)*
- [x] **Retire** — rename the unique-key fields, remove the auth records; an explicit, required operation on the realm's
      user storage; the renamed-key format decided and recorded above.
      *(`AuthUserAdapter.retire` `AuthUserAdapter.kt:89`; format `AccountRetirement.kt:32,49`; framework side
      `AuthRealm.retirePendingAccount` `AuthRealm.kt:413`. Implemented by `AdminUserRealm` (renames), `B2bRealm`,
      `B2b2cRealm`, `OperatorRealm` (no sign-up: `notSupported`), `TestUserRealm`, `ActivationHardeningRealm`,
      `TestAppUserRealm`, `MinimalTestRealm`.)*
- [x] **Expiry** — pending accounts older than 7 days (configurable) are retired. **Cancel** — a mailed, single-use cancel
      link retires the pending account.
      *(`RealmTokenConfig.pendingAccountLifetime` `RealmTokenConfig.kt:29`; enforced at use — link lifetime capped
      `EmailAndPasswordAuth.kt:420`, resend refused `:371`, sign-up retires inline `:320`; swept by
      `AuthSystem.retireExpiredPendingAccounts` `AuthSystem.kt:126`, registered as a worker in the demo
      (`PendingAccountsExpiryWorker.kt`). Cancel: `POST /auth/{realm}/activate/cancel` `AuthLoginApi.kt:137`,
      `EmailAndPasswordAuth.cancelSignUp` `:492`, single-use by delete count `:501`; link in the mail
      `AccountActivationEmailTemplate.kt:72`, built at `EmailAndPasswordAuth.kt:446`.)*
- [x] **Resend growing delays** — per pending account, stored on the marker; the response stays honest.
      *(`PendingActivation.resends/lastSentAt` `AuthRecord.kt:77,79`; `RealmTokenConfig.activationResendDelays` `:37`,
      `activationResendDelay` `:70`; check `EmailAndPasswordAuth.kt:379`, slot reserved before the mail `:384`;
      `AuthResendActivationResponse.nextResendAt`.)*
- [x] **Round-2 MAJORs that are plain fixes:** `ActivationResendToken` storage cases on both backends (round trip, expiry,
      type scoping); revoke the owner's earlier resend tokens when minting; consume a token only when its delete removed
      it (`count == 1`). Correct the cooldown-atomicity task's premise and red-team §8.
      *(Storage cases in `AuthRecordStorageBaseSpec`; revoke `AuthRealm.kt:296`; consumption `AuthRealm.kt:385`; premise
      corrected in `20260727-activation-resend-cooldown-atomicity.md` and red-team §8.)*
- [x] **Round-2 MINORs:** `sent = false` when the mail failed (`EmailAndPasswordAuth.kt:392`); resend-token lifetime pinned
      (e2e, production lifetime against an advanced clock); "password check before marker check" pinned (unit + e2e);
      activation failures surfaced (`ActivateAccountPage.kt:142,158` — a working Retry) and the "recently sent" copy
      only when the server names the delay (`:339`); factually wrong KDoc/comments corrected (`AuthProvider` resend
      contract, both "no resend endpoint" comments and the spec's, `resend-activation` in `AuthRecord` and
      `AuthSignInResponse`, `AuthApi` → `AuthLoginApi`, Mongo TTL "culls"); frontend tests (`ActivateAccountPageSpec`,
      `TestBed.preact`); the wildcard import (and `GithubSsoAuth`'s); `removeAllByOwner(exceptId)` pinned at storage
      level; the resend token consumed in `AuthRealm` (`AuthRealm.kt:376`); the tenth echo site added to the
      error-disclosure inventory.
- [x] **Tests:** e2e through `AppSpec` on BOTH backends (`ActivationHardeningE2eSpec` — a Karango app and a Monko app,
      auth records AND users on each backend, unique email index on both); mutation table below.
- [ ] **Docs follow-up at archive time:** `docs-site/src/pages/ultra/funktor/auth.astro:123-134` and
      `docs-site/src/data/llms/funktor.md:185-190` over-claim the resend token and state a rule activation did not meet.
      Public API changed (list under Implementation notes) — the follow-up must cover it.

## Out of scope (tracked elsewhere)

- Monko's auth-record TTL never expires anything (plan backlog, `e4076e46`).
- Session revocation, refresh-token renewal after a reset (`20260728-session-revocation-wiring.md`).
- The rest of the SSO linking policy for already-active accounts (`20260726-sso-email-verification.md`).
- Sign-up mail throttling per recipient (`20260727-signup-mail-throttle.md`).

## Implementation notes

- **Breaking public API** (pre-1.0; the docs follow-up must cover every item): `AuthUserAdapter.retire` is new and
  abstract; `AuthRealm.Messaging.sendAccountActivationEmail` takes `cancelUrl`, and
  `AccountActivationEmailTemplate.Params` carries it; `AuthProvider.resendActivation(realm, owner: UserId)` (the realm
  consumes the token now) and new `AuthProvider.cancelSignUp`; `RealmTokenConfig.activationResendCooldown` is replaced by
  `activationResendDelays` + `pendingAccountLifetime`; `AuthResendActivationResponse.nextResendAt`; new
  `AuthRealm.cancelSignUp` / `retirePendingAccount` / `retireExpiredPendingAccounts` / `resolveSsoAccount` /
  `isExpired`, `AuthSystem.cancelSignUp` / `retireExpiredPendingAccounts`; `EmailAndPasswordAuth.Services` lost
  `findLatestEmailVerificationToken` and `findActivationResendToken`, gained `savePendingActivation`, and
  `removeAuthRecord` returns whether it removed; `GithubSsoAuth.RemoteClient.getEmails` (defaults to null = unverified);
  storage adapter gained `saveRecord` / `removeAllOfOwner` / `findCreatedBefore` (`AuthRecordStorage.kt:148-157`, both
  repos); route `AuthFrontendRoutes.cancelSignUp`, endpoint `AuthApiClient.CancelSignUp`; `ActivateAccountPage(mode)`.
- **Order is the safety argument, twice.** `resolveSsoAccount` deletes the password before the marker
  (`AuthRealm.kt:487`), so a failure in between leaves a pending, password-less account — never a live one with the
  pre-registering attacker's password. `retirePendingAccount` removes credentials and tokens first (`:422`), renames,
  checks the address is free (`:440`), and removes the marker last (`:448`): every intermediate state is still pending
  and the next sweep finishes it. Both orders are pinned by mutation (fault injection for the SSO one).
- **The retire post-condition** (`AuthRealm.kt:440`): a realm whose `retire()` renames nothing is caught — the account
  stays pending and an error is logged — instead of looking retired while the address stays taken forever. A throwing
  `retire()` is caught the same way (`:432`), so the sweep and cancel never 500.
- **Expiry does not depend on the worker.** `funktor/auth` cannot use `funktor/cluster`'s `Worker` (cycle through
  `funktor/inspect`), so the guarantee lives at use time and the demo's `PendingAccountsExpiryWorker` only collects.
  **Recommended:** register the worker in `funktor/all`'s `Funktor` module so every app gets it — outside this track's
  edit scope.
- **Cancel is authorized by the activation token** (no new record type): whoever may activate may decline, and a resend
  rotates both links together. The endpoint does NOT echo `AuthError.message` (`AuthLoginApi.kt:137`).
- **Two layers keep a retired account unmailed:** the reset path answers a retired address like an unknown one
  (`EmailAndPasswordAuth.kt:558`) and `DefaultMessaging` refuses a retired recipient (`AuthRealm.kt:121`). The second was
  added because the first is easy to bypass from a future flow; the mutation round showed a test must pin each.
- **The e2e drives a clock**, not shortened config: `ActivationHardeningE2eApps.kt` registers `Kronos.systemUtc.mutable()`
  and the production `RealmTokenConfig`, so the 7-day lifetime, the 15-min resend-token lifetime and the 5 min / 15 min /
  1 h delays are what is tested. JWTs keep the system clock (the verifier uses it).
- **GitHub identity** stays the `/user` `email` (today's lookup, unchanged for active accounts); "verified" means that
  address is the PRIMARY and VERIFIED entry of `/user/emails`. A verified non-primary address therefore reads as
  unverified — fail-closed, and only for pending accounts.
- **Deviation:** the resend-token lifetime is pinned with the production lifetime and an advanced clock rather than a
  short lifetime in `TestUserRealm` (the ledger's suggestion) — same property, closer to production.
- **Deviation:** the SSO e2e uses GitHub with a fake `RemoteClient`; Google's client library is not on `funktor/all`'s
  test classpath. Google is covered against REAL storage on both backends in `SsoPendingAccountBaseSpec`.

## Test evidence

Counts from `build/test-results/**/TEST-*.xml` (kotest ignores `--tests`); failing cases read from the console.

- [x] `:funktor:auth:jvmTest` — **196 tests, 0 failures** (was 163 before the new specs). New: `AccountRetirementSpec` 8,
      `AuthRealmActivation{Karango,Monko}Spec` 5 + 5 (resend consumption by delete count, retire guards — real storage),
      `SsoPendingAccount{Karango,Monko}Spec` 7 + 7 (Google and GitHub against real storage, incl. a fault-injected
      delete-order test), `DefaultMessagingRetiredRecipientSpec` 1; `AuthRecordStorage{Karango,Monko}Spec` 11 → 18 each;
      `EmailAndPasswordAuthSpec` 26 → 33 (resend rewritten for the delays, cancel, password-before-marker).
- [x] `:funktor:auth:jsTest` — **11 tests, 0 failures**; new `ActivateAccountPageSpec` 7 (`TestBed.preact`, real
      browser, ktor `MockEngine` as the server): opening a link sends nothing, the click POSTs, cancel mode, refused link,
      unreachable server + Retry, the carrier read-and-cleared, a stale carrier cleared by the next login, honest resend
      copy.
- [x] `:funktor:all:jvmTest` — **210 tests, 0 failures**. New `ActivationHardening{Karango,Monko}E2eSpec` 15 + 15: auth
      records AND users on each backend, unique email index on both, production `RealmTokenConfig` against an advanced
      clock. `AccountActivationEmailE2eSpec` 8 and `AuthApiSpec` 23 adapted to the two-link mail and the delays.
- [x] `:funktor-demo:server:compileKotlin` green (all four demo realms implement `retire`; the worker compiles).
- [x] **Full suite** (`console/wt-gradle.sh --full-suite`, `clean build --continue`, at `0618d85e`): BUILD SUCCESSFUL in
      14m 15s, no `^e:`. XMLs: **15861 tests, 0 failures, 0 errors, 7 skipped** across 49 modules — jvmTest 7422, test
      2639, jsBrowserTest 3451, linuxX64Test 2349. `funktor/auth` 207 (jvm 196 + js 11), `funktor/all` 210,
      `funktor-demo/server` 47. No tracked generated file changed.

### Mutation evidence

Backed up with `cp` outside the worktree (source path in the name), restored with `cp`, verified with `cmp`. Every mutant
went RED for the right reason; "module" = `:funktor:auth`, "e2e" = `:funktor:all`, both backends unless noted.

| # | Mutation | Result |
|---|---|---|
| 1 | Activation on page load (the pre-decision behaviour) | ✅ RED `ActivateAccountPageSpec` "opening … sends NOTHING" and the cancel-mode case (requests seen on load) |
| 2 | SSO keeps the sign-up password | ✅ RED `SsoPendingAccount*` Google + GitHub activation cases (password still present) |
| 3 | SSO accepts an unverified address | ✅ RED `SsoPendingAccount*` "refused, changes nothing" (Google false/absent; GitHub cases) |
| 4a | Retire never asks the realm to rename | ✅ RED `AuthRealmActivation*` "retiring again later works" (`retirePendingAccount` stays false) |
| 4b | Retire keeps the auth records | ✅ RED e2e sweep and cancel cases (`hasPassword` still true) |
| 5 | Flat resend delay (always the first entry) | ✅ RED unit "the delay GROWS" and "once due" (`nextResendAt`) |
| 6 | Minting a resend token does not revoke earlier ones | ✅ RED e2e "Minting a resend token revokes the owner's earlier ones" — exactly that case |
| 7a | Resend consumption ignores the delete count | ✅ RED `AuthRealmActivation*` "the delete COUNT decides" (provider reached) |
| 7b | Cancel consumption ignores the delete count | ✅ RED unit "go no further when ANOTHER request consumed the token first" |
| 8 | `sent = true` on a failed mail | ✅ RED unit "sent=false when the mail FAILED" — exactly that case |
| 9 | SSO clears the marker BEFORE deleting the password | ✅ RED `SsoPendingAccount*` fault-injection "password goes BEFORE the marker" — exactly that case |
| 10 | Activation link no longer capped at the account's expiry | ✅ RED unit "the new link never outlives the pending account" |
| 11 | Sign-up never frees an expired address | ✅ RED e2e "Without the sweep, signing up an EXPIRED pending address …" |
| 12 | Sweep query ignores the cutoff — Karango / Monko separately | ✅ RED `AuthRecordStorage{Karango,Monko}Spec` "findCreatedBefore …" |
| 13 | Retire without the pending check | ✅ RED `AuthRealmActivation*` "an ACTIVATED account is never retired" AND unit sign-up "user already exists" (an active account's address would be retired) |
| 14 | No check that the rename freed the address | ✅ RED `AuthRealmActivation*` "renames NOTHING is caught" |
| 15 | `removeAllOfOwner` ignores the kept type — Karango / Monko separately | ✅ RED storage case on that backend + the realm guards (marker gone early) |
| 16 | Marker `save` inserts a duplicate | ✅ RED storage "resend schedule must survive save and reload" (`_id` changed) |
| 17 | Resend ignores expiry | ✅ RED unit "nothing for an EXPIRED pending account" |
| 18 | Pending marker checked before the password | ✅ RED unit "check the PASSWORD before the pending marker" AND e2e "A WRONG password … plain 403" |
| 19 | Resend slot reserved AFTER the mail | ✅ RED unit "reserves the slot BEFORE mailing" (event order) |
| 20 | GitHub verified claim ignores `primary` | ✅ RED `SsoPendingAccount*` GitHub, clue "verified but not primary" |
| 21 | Reset path accepts a retired address | ✅ RED e2e "A retired address is answered like an unknown one" — **only after the token-row assertion was added**; before it, the mailer's own refusal hid this mutant (found by this round, fixed in `8d5b4f3a`) |
| 22 | Activation mail without the cancel link | ✅ RED template specs (links, "way out") |
| 23 | `AccountRetirement.rename` a no-op / no truncation / constant digest | ✅ RED `AccountRetirementSpec` (format, isValidFormat, MAX_LENGTH, uniqueness, case) |
| 24 | Mailer guard for a retired recipient removed | ✅ RED `DefaultMessagingRetiredRecipientSpec` (reached the mailer) |
| 25 | `expiredOnly` ignored in `retirePendingAccount` | ✅ RED e2e "… before expiry the address is still taken" (sign-up would retire a FRESH pending account) |
| 26 | A FAILED resend mail still reports `nextResendAt` (the page would then say "we already sent one recently") | ✅ RED unit "sent=false when the mail FAILED" — a defect found in self-review after the first round, fixed and pinned in the same commit |
| — | Sweep passes `expiredOnly = false` | not run: EQUIVALENT — the sweep's query already filters on the cutoff; `expiredOnly` there is defence in depth |

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**
