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
as it already does for `createForSignup`. The realm renames the email AND clears anything derived from it (the demo and
test realms reset a display name defaulted from the local part). The framework owns everything around it, in
`AuthRealm.retirePendingAccount`: refuse unless the PENDING marker exists → remove every auth record of the owner
except the marker → ask the realm to rename → **verify** that `loadByEmail(original)` no longer resolves to this account
(a no-op or throwing `retire` is caught, logged, and the account stays pending) → remove the marker LAST, **by its
count** — the commit point: of a retire and a concurrent activation exactly one wins. Every intermediate state is still
"pending", so a crash leaves a pending account, never a live one.

**Renamed-key format (REVISED 2026-10-19 by the maintainer's decision on the round-1 row: no plaintext):**
`retired-<64 hex>@<32 hex>#retired.invalid`, e.g.
`retired-9c5e…(64)@3f2a9c0e5b7d41a2a8c6e0f1d2b3c4a5#retired.invalid`.
- *No plaintext of the address.* The 64 hex are the full SHA-256 over the CANONICAL address (`EmailAddress.value`).
  Unkeyed, as decided: the hash minimises what the row keeps; it is not a secret (a dictionary of known addresses
  reverses it). No other field may keep the address either — see the realm contract above.
- *An app-level merge still finds an address's retired rows:* `AccountRetirement.retiredKeyPrefix(email)` is what every
  retired key of that address starts with (`retired-<64 hex>@`), for a prefix query; apps never re-derive the hash.
- *Never collides with a sign-up.* Every account-creating path asserts `EmailAddress.isValidFormat`. The domain part is
  `<32 hex>#retired.invalid`; a domain containing `#` matches neither alternative of `EmailRegex` (hostname labels are
  `[a-z0-9-]`; the IP-literal form starts with `[`), so the value always fails it and no sign-up can produce it.
- *Valid stored value.* Fixed length (121), lowercase ASCII, no control or line-separator characters — well within
  `EmailAddress.MAX_LENGTH` (254) whatever the original's length.
- *Unique across repeated retirements.* The 32 hex are the first 16 bytes of SHA-256 over the account's `_id`; each
  retirement retires a DIFFERENT account, and a retired account keeps its `_id`. Deterministic, so two concurrent
  retirements of one account write the same value instead of fighting over the unique index.
- *Undeliverable.* `.invalid` (RFC 2606/6761) never resolves; the framework also refuses to mail a retired key.
- *Recognisable.* `AccountRetirement.isRetired` is a full match on the format.
- No `rename(String)` for other unique fields (dropped in round 1: an unkeyed tag on a user-chosen field can be
  squatted); a realm with another unique field gives it its own non-guessable rename.

**Expiry.** An expired pending account counts as retired at EVERY use: sign-up retires it inline and proceeds; sign-in
by password still answers `ActivationRequired`, but resend refuses; activation, password reset (request and completion)
and SSO retire it inline — SSO sign-up then creates a fresh account. The activation link never outlives the account.
`AuthSystem.retireExpiredPendingAccounts()` frees what nobody touches again; it runs as `PendingAccountsExpiryWorker`,
registered by `funktor/all`'s `Funktor` module (`funktor/auth` cannot depend on `funktor/cluster`: a cycle through
`funktor/inspect`). The sweep pages forward by `createdAt` and passes accounts that fail.

**Cancel** has its own record, `AuthRecord.SignUpCancelToken`: one per pending account, expiring WITH the account (7
days), mailed again by every resend — so a victim can decline long after the 24 h activation link died. The activation
page's own cancel button may still use the activation token (whoever may activate may decline). POST-only for the same
scanner reason as activation, single-use by delete count.

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
      Public API changed (list under Implementation notes) — the follow-up must cover it, and these four upgrade traps
      that give NO compile signal (gate round 1):
      1. an app's own `AccountActivationEmailTemplate` renderer still compiles without `cancelUrl` — it silently mails
         no cancel link;
      2. an app with its own frontend routes mails a `cancel-signup` link its router does not mount;
      3. an app's own activation page that POSTs on load reopens the scanner-detonation hole (decision 1);
      4. an app's own SSO provider must route an existing account through `AuthRealm.resolveSsoAccount` (sign-in AND
         sign-up), or pending accounts are taken over exactly as before.
      Round 2 added (gate, 2026-10-19): GitHub sign-in now asks for the `user:email` scope (a new consent prompt); the
      first sweep after upgrading retires every OLD pending account at once; retired rows stay in the app's user
      collection (lists and admin screens show them); the expiry worker exists only via `funktor/all`'s `Funktor`
      module; a `retire()` left as `TODO()` throws an `Error`, not an `Exception`; an account activated by SSO is
      passwordless until a reset; an app's own `AuthRealm.Messaging` lacks the retired-recipient guard; and the
      breaking-API list under Implementation notes must be re-checked against the final code (`claimedBy`,
      `replaceIfUnclaimed` / `removeIfUnclaimed`, `SignUpCancelToken`, `retiredKeyPrefix`, `save` removed).
      Round 3 added: `AuthRecordStorage.OnAfterSave` listeners never see the conditional writes (`replaceIfClaimedBy`,
      `removeIfClaimedBy`) — a listener auditing auth records misses claims, resend schedules and activations.

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

## Round 1 fix batch (2026-10-19)

Rebased onto master `47c0a7dc` first (clean). Every row dispositioned `fix`, plus the maintainer's decision on the
plaintext row (decided 2026-10-19: keep only a hash). The other parked row (Activate requires a password) is NOT
decided and is untouched. Line references in the sections above are as of `e7910eeb`; current ones are here.

| Ledger row | What changed | Where |
|---|---|---|
| MAJOR expiry worker | Moved to `funktor/all`, registered by the `Funktor` module; the demo copy and its registration removed; KDocs corrected; the e2e runs the REAL worker and pins the registration through `WorkerRegistry` | `funktor/all/src/jvmMain/kotlin/PendingAccountsExpiryWorker.kt:16`, `funktor.kt:34`, `RealmTokenConfig.kt:26`, `AuthSystem.kt:128` |
| Resend upsert | `AuthRecordStorage.replaceIfExists` — never inserts; Karango: `FOR … FILTER _key … REPLACE … RETURN NEW`, Mongo: `replaceOne(upsert = false)`; a vanished marker answers `sent = false` | `AuthRecordStorage.kt` (`replaceIfExists`), `KarangoAuthRecordsRepo.kt:163`, `MonkoAuthRecordsRepo.kt:175`, `EmailAndPasswordAuth.kt:408` |
| Nothing activates after expiry | An expired pending account is retired at every use: activation, reset request (answered as unknown) and completion, SSO (sign-in → 403; sign-up then creates a fresh account). The final marker delete is the commit point by count in retire, SSO, activation and reset completion (a reset that loses it removes its new password) | `AuthRealm.kt:456,528,544`, `EmailAndPasswordAuth.kt:532,621,690,705`; `resolveSsoAccount` now returns `Stored<USER>?` |
| Cancel lifetime | `AuthRecord.SignUpCancelToken`, one per account, expiring with it, re-mailed by every resend; cancel accepts it or the activation token; Cancel mode has its own dead-link copy | `AuthRecord.kt:149`, `EmailAndPasswordAuth.kt:469,552`, `ActivateAccountPage.kt:286` |
| Sweep head-of-line | Pages forward by `createdAt` (new `after` cursor on both repos), catches per account, stops on a non-advancing page; `CancellationException` rethrown at both catch sites; KDoc corrected for a failed cancel | `AuthRealm.kt:438,473,497`, `AuthSystem.kt:128` |
| Single-use e2e vacuous | Case deleted (pinned by `AccountActivationEmailE2eSpec` and the realm specs); the password clue now signs in with the RETIRED key + password | `ActivationHardeningE2eSpec.kt` |
| Page spec | The fake server records request bodies (token, provider asserted); both deep links mounted through `AuthState.mount` | `ActivateAccountPageSpec.kt` |
| Expiry rule twice | `realm.isExpired(marker.value)` in resend | `EmailAndPasswordAuth.kt:391` |
| `rename(String)` | Dropped | `AccountRetirement.kt` |
| Retired address reachable | `resolveSsoAccount` and `refreshToken` refuse a retired key | `AuthRealm.kt:520,602` |
| Upgrade traps | `AuthProvider` sign-in/sign-up KDoc; the template Params KDoc's false "compile error" claim corrected; all four traps in the docs follow-up box | `AuthProvider.kt:45`, `AccountActivationEmailTemplate.kt:24` |
| Red-team additions | The four collected scenarios added | `20260727-redteam-account-activation.md` |
| **Maintainer decision: no plaintext** | Key `retired-<sha256(canonical address)>@<id tag>#retired.invalid`; `AccountRetirement.retiredKeyPrefix(email)` for merges; the realm contract also clears address-derived fields — demo and test realms reset the name (defaulted from the local part) | `AccountRetirement.kt:39,50`, `AuthUserAdapter.kt` (`retire`), `AdminUserRealm.kt:95` |

**Tests (fresh XML):** `:funktor:auth:jvmTest` **218 / 0 failures**; `:funktor:auth:jsTest` **13 / 0**;
`:funktor:all:jvmTest` **226 / 0** (`ActivationHardening{Karango,Monko}E2eSpec` 19 + 19). New cases: storage
`replaceIfExists`, the `after` cursor and the cancel token on both backends; realm commit point, sweep across pages with
failing accounts, cancellation, refresh of a retired account; SSO expired / retired / retire-wins-the-marker; reset
completion expired / raced; e2e cancel on day 6 and dead after day 7, expired reset, expired SSO sign-in and fresh
SSO sign-up, worker registration; JS refused-cancel copy and routed deep links. `InsightsVaultBindValuesSpec` (from
master) failed once in a baseline run and once under mutant C1 on its log-order race (its own comment documents it)
and passed in the other runs — not touched by this track.

**Full suite** (`console/wt-gradle.sh --full-suite` at `a3fd060a`, base master `47c0a7dc`): no `^e:`; XMLs **15931
tests, 2 failures, 0 errors, 7 skipped**, 49 modules (jvmTest 7459, test 2670, jsBrowserTest 3453, linuxX64Test 2349).
`funktor/auth` 231, `funktor/all` 226, `funktor-demo/server` 47, `monko/core` 262, `karango/core` 1652,
`funktor/insights` 94, `funktor/codegen` 103 — all 0 failures. The two failures are outside this track and untouched by
it: `InsightsVaultBindValuesSpec` "log slice … is real" (expected `[INFO, WARNING]`, got `[INFO]` — the log-order race;
master fixed it meanwhile in `f8dadd86`) and `ultra/cache` `FastCacheRefreshAfterWriteSpec` "only one refresh per key at
a time" on linuxX64 (timing; the parallel plan lists a FastCache linuxX64 flake).
**After rebasing onto master `6b6743ad`** (which carries `f8dadd86`, clean rebase): `:ultra:log:jvmTest` 67/0,
`:funktor:auth:jvmTest` 218/0, `:funktor:all:jvmTest` 226/0 incl. `InsightsVaultBindValuesSpec` 8/0 — fresh XML.

**Mutation evidence (round-1 batch).** `cp` backup outside the worktree (path in the name), `cp` restore + `cmp`.

| # | Mutant | Result |
|---|---|---|
| R2a / R2b | `replaceIfExists` upserts — Karango / Monko separately | ✅ RED storage "must NEVER bring a deleted one back" on that backend |
| R2c | Resend ignores a vanished marker | ✅ RED unit "an activation that removed the marker meanwhile wins" |
| R3a | SSO activates an EXPIRED pending account | ✅ RED `SsoPendingAccount*` "EXPIRED pending account is retired" |
| R3b | Retire's final marker delete unconditional | ✅ RED `AuthRealmActivation*` "activation DURING the retire wins" |
| R3c | SSO's marker delete unconditional | ✅ RED `SsoPendingAccount*` "a retire that wins the marker meanwhile" |
| R3d / R3e | Reset completion: expired check / race cleanup removed | ✅ RED the two new unit cases |
| X1 | Reset request ignores expiry | ✅ RED e2e "A reset of an EXPIRED pending account" |
| C1 | Cancel token lives 24 h (the activation link's lifetime) | ✅ RED e2e "cancel link works for the account's WHOLE pending life" |
| R5a | Sweep without the per-account catch | ✅ RED `AuthRealmActivation*` "does not hold up the rest, across pages" |
| R5b / R5c | `after` cursor ignored — Karango / Monko | ✅ RED storage `findCreatedBefore` + the sweep test (timed out: the page never advanced — hence the non-advancing-page guard) |
| R5d | `CancellationException` swallowed | ✅ RED "cancellation is NOT swallowed" |
| R10a / R10b | SSO / refresh accept a retired key | ✅ RED the SSO and refresh cases |
| W1 | Worker not registered in `Funktor` | ✅ RED e2e registration case + both sweep cases |
| J1 | The page POSTs a different token | ✅ RED page body assertions (direct and routed) |
| J2 | `cancelSignUp` mounted in Activate mode | ✅ RED the routed deep-link case |
| J3 | Cancel mode shows the generic dead-link copy | ✅ RED "a refused CANCEL link speaks to someone who did not sign up" |
| H1 | Retired key keeps the plaintext address | ✅ RED `AccountRetirementSpec` (format, no-plaintext, prefix, length) |
| H2 | Realm retire keeps the address-derived name | ✅ RED e2e sweep case — **only after the fixture was fixed**: the e2e signed up with an explicit display name, so the name never carried the local part and H2 first SURVIVED; it now signs up without one and asserts the precondition |

## Review record — the LEDGER (filled by /feature-review)

Round 1 (2026-10-19), blind, `reviewer-high` ×3, base `41d6900d`, head `e7910eeb`. **1 MAJOR → not clean.**
Coordinator checks: no framework caller of `retireExpiredPendingAccounts` (grep: the demo worker + the e2e only);
`funktor/all/build.gradle.kts:41-42` depends on auth AND cluster (a cycle-free home); the cancel token is the
activation token, capped at `now + 24h` (`EmailAndPasswordAuth.kt:421-424`, `:497-499`); `savePendingActivation` is a
save (upsert) of the read marker (`:384-386`); the single-use e2e advances 15 min against a 15-min token
(`ActivationHardeningE2eSpec.kt:304-322`, `RealmTokenConfig.kt:38,45`) — all as claimed.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `funktor-demo/.../PendingAccountsExpiryWorker.kt:12-14`, `admin_user_module.kt:24`, `funktor/all/src/jvmMain/kotlin/funktor.kt:27-39` (domain) | The expiry sweep (decision 3) runs only in the demo; no app on funktor gets it, while KDocs (`RealmTokenConfig.kt:26`, `AuthSystem.kt:122-125`) say accounts ARE retired | fix | The module cycle is real for `funktor/auth`, but `funktor/all` depends on both: move the worker there, register it in `Funktor`, drop the demo registration (else twice). Coordinator widens the edit scope to `funktor/all/src/jvmMain` |
| 1 | MINOR | `EmailAndPasswordAuth.kt:365,384-386` (implementation) | A resend's upsert re-inserts a marker deleted by a concurrent activation; the active account reads pending again and the day-7 sweep RETIRES it (password and records gone) | fix | Conditional replace (no upsert) — "only if this `_id` still exists"; or re-read and answer `sent = false` |
| 1 | MINOR | `EmailAndPasswordAuth.kt:419-420,630`, `AuthRealm.kt:448,482-491` (domain) | "Nothing activates after expiry" is false: reset and verified SSO activate an expired, unswept account; the outcome depends on sweep timing; the marker delete is unconditional, so an activation racing a retire gets a session on a renamed account | fix | Expired = retired at use: reset/SSO retire inline (SSO sign-up then creates a fresh account, reset answers as unknown); make the final marker delete conditional on its count |
| 1 | MINOR | `EmailAndPasswordAuth.kt:497-499`, `:421-424` (security) | The cancel link dies with the 24 h activation token, but the account lives 7 days; a Wednesday cancel fails and the page's advice ("try signing in") is wrong for a victim | fix | Cancel honours the pending lifetime (its own record, expiring with the account — Karango's TTL culls at `expiresAt`); Cancel mode gets its own "link not usable" text |
| 1 | MINOR | `AuthRealm.kt:456-462`, `findCreatedBefore` (both repos), `AuthRealm.kt:433`, `AuthSystem.kt:130` (domain + implementation) | Head-of-line blocking: a marker whose retire keeps failing (or whose user load throws, aborting the batch) stays oldest forever; the "retried by the next sweep" KDoc is false for a failed cancel; `catch (Exception)` swallows `CancellationException` | fix | Catch per account; move past failures (attempt stamp / backoff on the marker); rethrow cancellation; correct the KDoc |
| 1 | MINOR | `ActivationHardeningE2eSpec.kt:304-322`, `:461-463` (implementation) | "A resend token is single-use" passes because the token has EXPIRED (15 min = 15 min), never by single use; the retired-password clue gets its 403 from the rename | fix | Delete or reword the case (the property is pinned in `AccountActivationEmailE2eSpec` and the realm specs); fix the clue |
| 1 | MINOR | `funktor/auth/src/jsTest/kotlin/pages/ActivateAccountPageSpec.kt:65`, `AuthFrontend.kt:50` (implementation) | The fake server records only method + path — a page POSTing a wrong/empty token passes; the `cancelSignUp` → `Mode.Cancel` wiring is untested | fix | Capture the body (token, provider); one routed mount per deep link |
| 1 | MINOR | `EmailAndPasswordAuth.kt:371` vs `AuthRealm.kt:402` (domain + implementation) | The expiry rule is written twice, on two clocks (`services.instantNow()` vs `deps.kronos`) | fix | `realm.isExpired(marker.value)` |
| 1 | MINOR | `AccountRetirement.kt:33-37,57-61` (security) | `rename(value: String)` (no production caller) produces a predictable tag (unkeyed SHA-256 of `_id`); a user-chosen unique field allowing `#` can be squatted, so the victim's retire collides forever | fix | Drop the `String` overload until an app needs it (email is safe: `#` lands in the domain part) |
| 1 | MINOR | `GoogleSsoAuth.kt:169`, `GithubSsoAuth.kt:216`, `AuthRealm.kt:547` (security) | Only reset guards retired addresses; `resolveSsoAccount` and `refreshToken` would hand a session to a retired row if anything presented the retired string | fix | Refuse `AccountRetirement.isRetired(email)` in `resolveSsoAccount` (shared by all SSO); refresh refuses a retired account |
| 1 | MINOR | `AuthProvider.kt:39-49,60-67`, `AccountActivationEmailTemplate.kt:22-25` (domain) | Upgrade traps with no compile signal: custom templates silently mail no cancel link (the Params KDoc's "compile error" claim is false), custom frontends mail an unmounted route, app pages POSTing activate on load, app SSO providers not told to use `resolveSsoAccount` | fix | One line on the `AuthProvider` sign-in/sign-up KDoc; correct the Params KDoc; list all four in the docs follow-up |
| 1 | — | `AccountRetirement.kt:49-55` (domain) | The disowned address stays in plaintext forever on the retired row ("I did not sign up"); data minimisation (GDPR Art. 5(1)(c)) — a hash would still let a later merge match | **maintainer-decision** | Decision 3 renames the key and leaves merging to apps; whether the original stays readable is the maintainer's call |
| 1 | — | `ActivateAccountPage.kt` / activate endpoint (security) | Classic-federated merge survives ONE victim click on Activate (accepted by decision 1); closing it cheaply: the Activate POST requires the account's password — the real owner knows it, a pre-registration victim does not and is steered to cancel/reset/SSO | **maintainer-decision** | Changes decision 1's UX; the maintainer's call |

Red-team additions (security, collected): re-registration of an expired victim sign-up (attacker polls sign-up, the
victim then clicks Activate on the attacker's account while EXPECTING the mail); cancel → immediate re-registration
loops; resend racing a cancel (stale `Stored` user → a live link for a retired row, the retired-recipient guard does
not fire); SSO racing the sweep/cancel (does `refreshToken` renew a session for a retired row).

**Verdict:** round 1 NOT clean (1 MAJOR). Fix batch (the MAJOR + the MINORs, maintainer's standing direction), then
round 2 (`reviewer-xhigh`, two-phase). Two maintainer decisions parked (they do not stop the loop).

Round 2 (2026-10-19), two-phase, `reviewer-xhigh` ×3, head `f8135e70` on master `6b6743ad`. **3 MAJOR → not clean;
safety valve fired (two rounds without a clean one) → maintainer asked before round 3.**
Coordinator checks: `AuthRealm.kt:418-464` read — credentials go at `:427`, the rename at `:437`, the conditional
marker delete (the commit point) only at `:456`, so the reviewers' interleaving is real; `ActivateAccountPage.kt:246-251`
posts the cancel token to activate while `EmailAndPasswordAuth.kt:511-513` accepts only the activation token — the
button cannot succeed; `:552-554` cancel falls back to the activation token with no test. **A coordinator claim that did
not survive:** from a truncated copy of the security report I relayed its MAJOR as "sign-in reads the password before
the marker"; the full report lists SSO password-before-marker as CLEAN and its MAJOR is the retire ordering below.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 | MAJOR | `AuthRealm.kt:418-464` (`:427`, `:437` before `:456`) (all three) | Round 1's conditional marker delete fixes only WHO wins, not WHAT is left: retire strips credentials and renames the email before its commit point, so an activation that wins the race leaves an ACTIVE account with no credentials under a retired key — permanent lockout (reset/SSO/refresh refuse retired rows). The KDoc's "changing nothing / never a live one" is false. Reachable via Cancel-vs-Activate and sweep-vs-activate at day 7. The commit-point test (`AuthRealmActivationBaseSpec.kt:181`) asserts only the boolean, on a non-persisting mock | fix (design: maintainer) | Claim first: the commit point must come BEFORE anything destructive, and a claimed-but-unfinished retire must stay non-activatable and retryable. Same pattern in `resolveSsoAccount` (`:537-544`) |
| 2 | MAJOR | `ActivateAccountPage.kt:246-251`, `EmailAndPasswordAuth.kt:511-513` (implementation + domain) | Cancel mode's "Activate my account instead" posts the cancel token to activate, which accepts only the activation token — it always fails | fix | Drop the button (text pointing at the activation mail); the cancel token must NOT activate |
| 2 | MAJOR | `EmailAndPasswordAuth.kt:552-554` (implementation) | Cancel with the ACTIVATION token (the fallback) has no test on either backend | fix | e2e on both backends + jsTest; mutation-checked (remove the fallback → red) |
| 2 | MINOR | `AuthRealm.kt:537-544` (security) | `resolveSsoAccount` deletes passwords/tokens before its commit; no rename, reset still recovers | fix | Same claim-first ordering as the retire MAJOR |
| 2 | MINOR | five KDocs (implementation) | Still say cancel uses the activation token | fix | Rewrite |
| 2 | MINOR | `AuthLoginApi.kt:113,131` (security) | activate/resend echo `e.message`; `cancelSignUp` suppresses it | fix | Same treatment as cancel |
| 2 | MINOR | reset completion; `signIn` (domain) | Reset completion ignores the token delete count; `signIn` does not refuse `isRetired` | fix | Act on the count; refuse retired |
| 2 | MINOR | reset-loses-marker path (domain) | Removes ALL passwords, not the stale one | fix | Narrow it |
| 2 | MINOR | sweep e2e (implementation) | The assertion passes because the token expired | fix | Assert on the retire itself |
| 2 | MINOR | `[Mode.*]` KDoc links (implementation) | Unresolved | fix | Resolve or plain code |
| 2 | MINOR | sweep query (domain) | No index on (realm, `_type`, `createdAt.ts`) | fix | Add it, both backends |
| 2 | MINOR | `AuthRecordStorage.save`, `Adapter.saveRecord` (implementation, SURPLUS) | No caller left after `replaceIfExists` | fix | Remove |
| 2 | MINOR | docs follow-up (domain) | New upgrade traps: GitHub `user:email` consent; the first sweep retires OLD pending accounts; retired rows in the user collection; the worker only via `Funktor`; `TODO()` retire throws an Error; passwordless after SSO; custom Messaging lacks the guard; stale API list | docs task | Listed in the docs follow-up on archive |
| 2 | — | sent-message store refs, failed-mail logs (domain) | Keep the plaintext address that the retired row now hashes | **maintainer note** | Out of this track's scope; raise with the maintainer |

CLEAN (security, round 2): retired-key forging/squatting, revival paths, token single-use/owner/type/realm scoping,
cancel cannot hit an active account, link lifetimes, SSO verified-claim handling, SSO password-before-marker order,
enumeration on cancel/activate/resend. Bounded races already in the red-team task: SSO racing retire (refresh fails
closed), resend racing retire (stray mail, no session), custom-provider `e.userId` trust.

**Verdict:** round 2 NOT clean (3 MAJOR). Safety valve: stop and ask the maintainer before the round-2 fix batch and
round 3 (`reviewer-top`).

Round 3 (2026-10-19), two-phase, `reviewer-top` ×3, head `8d3e01dd` on master `d9016c81`. **2 MAJOR → not clean;
maintainer asked again (the safety valve already fired after round 2).** Every round-2 `fix` row: HOLDING, as judged by
all three reviewers, except the SSO half of the retire row (see the first MAJOR below). Coordinator checks:
`EmailAndPasswordAuth.kt:305-325` and `:748-752` were read — the Password row is read, then Argon2id runs, then the marker
is read at `:315`, and nothing is re-checked before `issueSignIn`; `AuthRealm.kt:580-586` deletes the password before the
marker. `AuthRealm.kt:451` (`else -> return false`) refuses an `sso` claim on every retire, the sweep included. Both
MAJORs are real. **A coordinator claim that did not survive:** in round 2 I withdrew "sign-in reads the password before
the marker" because the full security report listed "SSO password-before-marker order" as CLEAN. That note covered the
WRITE order inside `resolveSsoAccount`, not a concurrent password sign-in READING across it — the race had never been
reviewed.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 3 | MAJOR | `EmailAndPasswordAuth.kt:305-325,748-752`, `AuthRealm.kt:319,580-586`, `EmailAndPasswordAuth.kt:718-727` (security) | A password sign-in in flight across a one-way flow gets a session: it reads the Password row, hashes it, then reads the marker. SSO activation and reset completion change the password BEFORE removing the marker, so a sign-in that read the old password and then finds no marker gets a refreshable session on the victim's now-ACTIVE account. The pre-registering attacker loops sign-ins with their own password, which reopens decision 2. Via retire: a 1 h JWT on a dead row, refresh refuses (already-recorded bounded race). No test has a sign-in in flight | fix | After the marker check, re-read `findLatestPasswordRecord` and require the same `_id` that was checked — "the credential proven must still be live at commit", as token delete counts already do. Sound because every one-way flow touches the password before the marker. Test: a real-storage interleave on both backends; mutation: drop the re-read → red |
| 3 | MAJOR (domain) / MINOR (implementation) | `AuthRealm.kt:451,559,571-586`, `EmailAndPasswordAuth.kt:350,633-639,708` | An SSO claim that stops part-way (a crash or a DB error between the claim and the marker delete) has no exit except the same provider verifying the same address again. Sign-in, reset, sign-up and the sweep all refuse it, and the sweep does so silently, forever. The round-2 Reason "reset still recovers" was invalidated by the claim itself | fix | The sweep takes over an EXPIRED `sso` claim by a conditional write (`claimedBy == sso → retire`); SSO's final marker delete becomes conditional on its own claim, so a takeover is not undone. Generalise `IfUnclaimed` to "if claimedBy == expected" on both backends. Realm spec on both backends; mutation-checked. Severity disputed; fixed either way |
| 3 | MINOR | `AuthError.kt:6`, `AuthRealm.kt:468,512`, `AuthSystem.kt:134` (security) | `AuthError` is a `Throwable`, not an `Exception`: a realm whose `retire()` throws `AuthError.notSupported()` (as `AuthUserAdapter.kt:89` advises) escapes the per-account catch, and the Epoch-dated claim then aborts every later sweep | fix | Catch `Throwable` except `CancellationException` at the three nets (covers `TODO()` too) |
| 3 | MINOR | `EmailAndPasswordAuth.kt:364,372` (implementation) | Sign-up inserts the password before the marker; a failed marker insert leaves an ACTIVE account with an unproven address (the pre-hijack outcome). Pre-existing order | fix | Marker first, then the password |
| 3 | MINOR | `AuthRealm.kt:441`, `Timestamped.kt:29-38` (domain) | Back-dating the claim to Epoch reuses the value `withTimestamps` treats as "unset, re-stamp". Any future hooked save would un-back-date it, and the real sign-up time is lost | fix | Keep `createdAt` honest: the sweep also selects `retire`-claimed markers of any age (and expired `sso` ones, per the MAJOR) |
| 3 | MINOR | `AuthSystem.kt:124-125`, `RealmTokenConfig.kt:26-27` (implementation + domain) | The KDocs say expiry is enforced at sign-in; sign-in answers `ActivationRequired`, and only resend refuses | fix | Correct the KDocs (the behaviour is the spec's) |
| 3 | MINOR | `AuthRecordStorage.kt:14` (domain) | `OnAfterSave` listeners never see the conditional writes | docs task | One line in the docs follow-up |
| 3 | MINOR | `AccountRetirement.kt:20`, `EmailAndPasswordAuthSpec.kt:839-921`, `AdminUserRealm.kt:93-94`, `EmailAndPasswordAuth.kt:537-544` (implementation + domain, SURPLUS) | Public `userId` with no reader; mangled test formatting; a duplicate comment; a stale half-comment | fix | Tidy |
| 3 | — | `AuthRealm.kt:404-406,646` (implementation + domain, SURPLUS) | `isBeingRetired` costs a query per refresh, for a state no framework path produces | keep | Defence in depth for custom providers (round-1 row); say so in its KDoc |
| 3 | — | resend × retire stray mail / false "activated" page (security, withdrawn in phase 2) | — | already recorded | Red-team bounded race; no session |

Probed CLEAN in round 3: claim-first on both backends — AQL bind params, BSON filters, a missing `claimedBy` reads as null,
`_type` survives REPLACE; every marker writer is conditional; every pair of activate, retire, SSO, reset, resend and
cancel settles on the one conditional write, and no interleaving needs a lock (domain walked them all). Concurrent
multi-node sweeps are safe (deterministic rename, resume). Back-dated markers do not head-of-line-block. Old records
decode to defaults. Retired-key refusal holds at every entry point. Realm isolation holds. No error echo. Code style is
clean across all 45 files. Serialized types have serializing tests.

**Verdict:** round 3 NOT clean (2 MAJOR). Fix batch proposed to the maintainer; round 4 (`reviewer-top`) after it.

## Round 2 fix batch (2026-10-19, maintainer-approved design: CLAIM FIRST)

Every round-2 row dispositioned `fix`; the `docs task` row is listed in the docs follow-up box; the `maintainer note`
row (sent-message refs and failed-mail logs keep the plaintext address) is skipped as instructed. Activate stays as
it is (the password-at-activation question is answered by `20261019-email-first-signup.md`). No GlobalLocks: the
commit point is a conditional write on the pending marker itself.

**The claim.** `AuthRecord.PendingActivation.claimedBy` (`AuthRecord.kt:86`) is null until a one-way change takes the
account: `"retire"` or `"sso"`. Two atomic, conditional storage steps, both backends:
- `AuthRecordStorage.replaceIfUnclaimed` (`AuthRecordStorage.kt:87`) — replace only while the record exists AND is
  unclaimed. Karango: one AQL `FOR doc … FILTER doc._key == @key && doc.claimedBy == null REPLACE … RETURN NEW`
  (`KarangoAuthRecordsRepo.kt:170`); Mongo: `replaceOne(and(_id, claimedBy == null), upsert = false)`
  (`MonkoAuthRecordsRepo.kt:182`). A missing `claimedBy` reads as null, so other records and pre-existing markers
  qualify. This is also resend's write, so a resend can never undo a claim.
- `AuthRecordStorage.removeIfUnclaimed` (`:89`) — the same filter with `REMOVE` / `deleteOne`: the activation and
  reset commit point.

| Ledger row | What changed | Where |
|---|---|---|
| MAJOR retire ordering | `retirePendingAccount` CLAIMS FIRST (`replaceIfUnclaimed` with `claimedBy = retire`); a lost claim changes NOTHING and returns false; a claim already held by `retire` is resumed; only then credentials go, the email is renamed, and the marker goes last. The claimed marker is back-dated to the epoch, so the next sweep finishes a retire that stopped part-way (a failing or no-op `retire()` now leaves the account CLAIMED and blocked, not pending). KDoc rewritten to match | `AuthRealm.kt:426,439,455` |
| … blocked while claimed | sign-in refuses a claimed account (no resend token) (`EmailAndPasswordAuth.kt:320`); resend refuses (`:400`, and its conditional write); activation refuses and commits only by `removeIfUnclaimed` (`:529,545`); reset request (`:634`) and completion refuse; SSO refuses a retire claim; refresh refuses via `isBeingRetired` (`AuthRealm.kt:404,646`) | as listed |
| MINOR SSO ordering | `resolveSsoAccount` claims first (`claimedBy = sso`), then deletes the password and tokens, then the marker; losing the claim to a retire touches nothing; a claim stopped part-way is finished by the next verified SSO sign-in | `AuthRealm.kt:541` |
| MAJOR Cancel mode activate | Removed; Cancel mode points at the mail's "Activate account" link instead — its token cannot activate | `ActivateAccountPage.kt:245` |
| MAJOR cancel by activation token | e2e on both backends + page spec (Activate mode's cancel posts the activation token to `/activate/cancel`) | `ActivationHardeningE2eSpec.kt`, `ActivateAccountPageSpec.kt` |
| Five stale KDocs | `AccountActivationEmailTemplate.Params.cancelUrl`, `AuthProvider.cancelSignUp`, `AuthFrontendRoutes.cancelSignUp`, `AuthCancelSignUpRequest`, `AuthRecord.EmailVerificationToken` (+ `AuthRealm.cancelSignUp`) | — |
| `e.message` echo | activate and resend no longer echo (as cancel); inventory updated | `AuthLoginApi.kt:112,132`, `error-disclosure/20260720-auth-error-account-enumeration.md` |
| Reset completion / signIn | completion consumes its token by count (`EmailAndPasswordAuth.kt:699`); `signIn` refuses a retired key before any lookup (`:299`) | — |
| Narrowed delete | a reset that loses the marker takes back exactly the password it wrote (`:728`) | — |
| Sweep e2e | asserts the retire itself: exact renamed key and every raw auth row DELETED (expired rows included) | `ActivationHardeningE2eSpec.kt` (`rawRowsOf`) |
| `[Mode.*]` links | plain code | `ActivateAccountPage.kt:31` |
| Sweep index | `(realm, _type, createdAt.ts)` on both backends | `KarangoAuthRecordsRepo.kt:65`, `MonkoAuthRecordsRepo.kt:63` |
| Surplus `save` | `AuthRecordStorage.save` / `Adapter.saveRecord` removed | `AuthRecordStorage.kt` |

**Tests (fresh `TEST-*.xml`):** `:funktor:auth:jvmTest` **232 / 0**, `:funktor:auth:jsTest` **14 / 0**,
`:funktor:all:jvmTest` **234 / 0** (`ActivationHardening{Karango,Monko}E2eSpec` 23 + 23). New: storage "a CLAIMED marker
can be neither replaced nor removed" (both backends); realm (both backends) — claim beats a mid-retire activation, a
retire that lost the claim changes nothing (credentials kept), partial retire stays claimed/blocked/refresh-refused and
the next sweep finishes it, the stale-read activation race; SSO claim-first; unit — reset token count, claimed reset,
retired-key and claimed sign-in; e2e (both backends, through a test-only adapter gate right after the claim, and a gate
inside the realm's `retire()`) — activation wins (credentials kept, address not renamed, signs in), retire wins
(fully retired, activation link dead), partial retire then sweep, cancel by activation token; JS — no activate action in
Cancel mode, Activate mode's cancel posts the activation token.

**Full suite** (`console/wt-gradle.sh --full-suite`, rebased onto master `d9016c81`): BUILD SUCCESSFUL in 11m 27s, no
`^e:`; XMLs **15955 tests, 0 failures, 0 errors, 7 skipped**, 49 modules (jvmTest 7482, test 2670, jsBrowserTest 3454,
linuxX64Test 2349); `funktor/auth` 246, `funktor/all` 234, `funktor-demo/server` 47.

**Mutation evidence (round 2).** `cp` backups outside the worktree (path in the name), `cp` restore + `cmp`.

| # | Mutant | Result |
|---|---|---|
| MA | Retire strips credentials BEFORE its claim | ✅ RED realm "a retire that LOST the claim … changes NOTHING" (both backends) |
| MB1 | Activation ignores the claim (claim check removed AND unconditional delete) | ✅ RED realm "an activation that READ the marker before a retire claimed it cannot commit after" (both) — **only after that test was added**: in the e2e gate the claimed marker is back-dated, so activation took its EXPIRY branch and the mutant survived; the e2e's first gate (inside `retire()`) ran after the token was already gone and could not express the race at all |
| MB0 | Only the explicit claim check removed | survives, by design — the conditional delete (and the expiry branch) still guard; defence in depth |
| MC | Cancel without the activation-token fallback | ✅ RED e2e "the ACTIVATION token cancels too" (both) |
| MD | SSO deletes the password before its claim | ✅ RED `SsoPendingAccount*` "CLAIM FIRST … NOTHING is deleted" (both) |
| ME | Cancel mode offers "Activate instead" again | ✅ RED page spec "cancel link leads with Cancel" (no activate action) |
| MF | Claim not back-dated | ✅ RED e2e "A retire that stops part-way … next sweep finishes it" (both) |
| MG | Refresh ignores a retire claim | ✅ RED realm partial-retire case (both) |
| MH | Reset completion ignores its token delete count | ✅ RED unit "a token another completion consumed first does nothing" |
| MI | A reset losing the marker keeps its password | ✅ RED unit "takes back exactly the password this reset wrote" |
| MJ / MK | `signIn` accepts a retired key / ignores a claim | ✅ RED the two unit cases |
| MM | `replaceIfUnclaimed` ignores the claim — Karango / Monko separately | ✅ RED storage + realm + SSO claim cases on that backend |


## Round 3 fix batch (2026-10-20, maintainer-approved)

Every round-3 row dispositioned `fix`; the `docs task` row (OnAfterSave listeners never see the conditional writes) is in
the docs follow-up box; the `keep` row got its KDoc (`AuthRealm.isBeingRetired`, `AuthRealm.kt:409`: refresh pays the
query as defence in depth for custom providers).

| Ledger row | What changed | Where |
|---|---|---|
| MAJOR sign-in race | `validateCurrentPassword` returns the Password record it proved (`EmailAndPasswordAuth.kt:749`); after the marker check, sign-in re-reads the newest Password record and refuses unless its `_id` is the proven one (`:330`). KDoc: every one-way flow changes the password BEFORE it removes the marker, so a missing marker means the change is visible | `EmailAndPasswordAuth.kt:330,749` |
| MAJOR stuck SSO claim | Conditional writes generalised to `claimedBy == expected` (null = unclaimed), one statement each: Karango `FILTER … && doc.claimedBy == @expected` (`KarangoAuthRecordsRepo.kt:171`), Mongo `and(_id, eq(claimedBy, expected))` (`MonkoAuthRecordsRepo.kt:184`); `AuthRecordStorage.replaceIfClaimedBy` / `removeIfClaimedBy`. The retire takes over an EXPIRED `sso` claim (`sso → retire`, only with `expiredOnly`) (`AuthRealm.kt:450`, `claimForRetire` `:500`); SSO's final marker delete is conditional on its own claim and refuses the session when taken over (`:618`). KDocs rewritten | as listed |
| MINOR `AuthError` is a Throwable | `Throwable` minus `CancellationException` at the three nets | `AuthRealm.kt:473,541`, `AuthSystem.kt:134` |
| MINOR sign-up order | The marker is written BEFORE the password | `EmailAndPasswordAuth.kt:372` |
| MINOR Epoch back-dating | Dropped; `createdAt` stays the sign-up time. The sweep query selects markers created before the cutoff OR claimed by `retire` (any age) — one query, still ordered and paged by `createdAt` (`AuthRealm.kt:531`; Karango hand-written AQL `KarangoAuthRecordsRepo.kt:148`, Mongo `Filters.or` `MonkoAuthRecordsRepo.kt:160`). Expired `sso` claims are selected by age | as listed |
| MINOR KDocs | Where expiry is enforced: sign-up, SSO, activation, reset retire; resend refuses; a password sign-in answers `ActivationRequired` | `AuthSystem.kt:122`, `RealmTokenConfig.kt:26` |
| MINOR tidy | `AccountRetirement.userId` dropped (private tag only); the mangled resend-spec formatting fixed; the duplicate `AdminUserRealm` comment merged; the stale half of the activation comment trimmed; dead imports removed | — |

**Tests (fresh `TEST-*.xml`):** `:funktor:auth:jvmTest` **247 / 0** — new `SignInRace{Karango,Monko}Spec` 3 + 3 (real
storage: a sign-in in flight across a verified SSO activation, across a reset completion, and the plain sign-in);
`AuthRealmActivation*` 16 each (stuck SSO claim not retired before expiry — by the sweep, a cancel-style or an
expired-only retire — taken over after it, and an SSO finishing its claim while the sweep takes it over loses
cleanly; the sweep passes a realm whose `retire()` throws `AuthError.notSupported()`); storage 21 each
(`claimedBy == expected` takeover, `orClaimedBy`); unit sign-up marker-first. `:funktor:all:jvmTest` **234 / 0** — the
sweep e2e and the partial-retire case hold without the back-dating.

**Mutation evidence (round 3).** `cp` backups outside the worktree (path in the name), `cp` restore + `cmp`.

| # | Mutant | Result |
|---|---|---|
| N1 | Sign-in without the password re-read | ✅ RED `SignInRace*` SSO and reset cases (both backends) |
| N2 | SSO-claim takeover not gated on expiry | ✅ RED "a stuck SSO claim … is NOT retired before the account expires" — **only after that case also called `retirePendingAccount` directly**: the sweep never selects a fresh `sso` claim, so through the sweep alone the mutant survived; the gate guards cancel and the sign-up's expired-only retire |
| N3 | SSO's final marker delete unconditional | ✅ RED "an SSO sign-in finishing its claim while the sweep takes it over loses cleanly" (both) |
| N4 / N5 | Conditional writes ignore `expected` — Karango / Monko separately | ✅ RED storage + realm claim cases on that backend |
| N6 | The sweep ignores retire claims (`orClaimedBy = null`) | ✅ RED realm "a retire that stops part-way … the next sweep finishes it" (both) |
| N7 | The retire nets back to `catch (Exception)` | ✅ RED "a realm whose retire() throws an AuthError … does not stop the sweep" (both) |
| N8 | Sign-up writes the password before the marker | ✅ RED unit "a failing password insert leaves a PENDING (passwordless) account" (+ the sign-up success case's order) |

