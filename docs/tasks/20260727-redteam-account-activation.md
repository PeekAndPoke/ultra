# Red-team scenarios: account activation

**Status:** COLLECTED 2026-07-27 — **do NOT execute during feature work.** These are for a dedicated
penetration-test session.
**Feature:** `docs/tasks-archive/2026-07/20260727-account-activation.md` (commit `31667449`)
**Related:** ULTRA-00117 (redteam value class emailaddress) §1–2 (SSO auto-link),
ULTRA-00125 (redteam messaging)

Ordered by expected severity. Each was raised or confirmed during the review gate and verified to be
reachable in the code; none has been attempted.

## 1. Sign-up as an outbound-mail amplifier

`POST /login/{realm}/signup` is `public()`, and as of this feature it SENDS AN EMAIL to an
attacker-chosen address. There is no rate limiting anywhere in the repo (grep for
`ratelimit|throttl` returns only an unrelated kraft signature-pad option); `letTheBotsWait()` is a
250–500 ms per-request `delay` that bounds neither rate nor concurrency.

A given address string can only be signed up once ("User already exists" → no mail), so the
amplification comes from ALIASING: `EmailAddress.of` trims and lowercases but folds neither
subaddress tags nor dots, so `victim+1@gmail.com` … `victim+N@gmail.com` are N distinct accounts
delivering to one inbox.

- **Attempt:** drive N sign-ups with subaddress variants of one victim address; measure delivered
  mail, `system_sent_messages` rows, and provider complaint rate.
- **Attempt:** the same with random addresses — measure user-row and `PendingActivation` growth. The
  marker has `expiresAt = null` BY DESIGN, so the TTL indexes
  (`db/karango/KarangoAuthRecordsRepo.kt`, `db/monko/MonkoAuthRecordsRepo.kt`) never cull it.
- **Impact if it works:** mail-bomb from the customer's verified sending identity; SES/SendGrid
  reputation damage up to sending suspension; unbounded DB growth.

## 2. Pre-registration phishing — the victim's click completes the setup

The activation mail (`AuthRealm.DefaultMessaging.sendAccountActivationEmail`) is a pure
call-to-action: "Welcome! Click the link below to activate your account." There is no "if you did not
sign up, ignore this", no requesting IP, no time.

- **Attempt:** register `victim@corp.com` with a password the attacker chooses; observe whether the
  victim, receiving an unsolicited but entirely affirmative mail from a real vendor domain, activates
  it. Then sign in with the attacker's password.
- **Chain:** combine with the SSO auto-link gap (ULTRA-00118 (sso email verification))
  so the victim's later SSO sign-in lands on the same row and the attacker retains co-access.
- **Note:** the diff still strictly IMPROVES on the previous behaviour, which handed the attacker a
  session with no victim interaction at all. This is residual risk, and it is a copy change to close.

## 3. Sign-up as an account-enumeration oracle

A fresh address returns `200 {success=true, requiresActivation=true}`; an existing one returns `400`
with `withInfo("User already exists")` (`provider/EmailAndPasswordAuth.kt`, `api/AuthApi.kt`). Both
status and body differ. Sign-in, recover-account and activate are all correctly uniform — sign-up is
the odd one out. **Pre-existing**, but it compounds §1: enumeration tells the attacker which
addresses will generate mail.

- **Attempt:** enumerate a customer list by probing sign-up.

## 4. Denial of service by pre-registration

Registering a victim's address before they do leaves them with an account they cannot use until they
prove the mailbox.

UPDATED 2026-10-19: resend is reachable only for someone who knows the password, so a pre-registered victim cannot
use it. Their routes in: "forgot password" (activates, and supersedes the attacker's password), SSO with a verified
address (activates and deletes the attacker's password), or the mail's "Didn't sign up? Cancel" link (retires the
account and frees the address). Untouched, the account expires after 7 days and the address is free again.

- **Attempt:** pre-register a target address, then walk the victim's real options end to end and
  measure whether any of them is discoverable without already knowing what happened.

## 5. Token replay across realm / provider / account

Verified scoped in code, so these are confirmation attempts rather than suspicions:
`findByToken` filters by realm AND record type AND expiry, and `removePendingActivations` uses the
owner from the TOKEN RECORD rather than from the request.

- **Attempt:** redeem a realm-A verification token against realm B; redeem it with
  `provider = "google-sso"`; redeem another user's token and check whose marker is dropped.
- **Attempt:** replay a consumed token, and replay one whose 24 h lifetime has passed.
- **Note:** `EmailVerificationToken` carries NO provider field. Only `EmailAndPasswordAuth` issues
  them today, so nothing is exploitable — but a second sign-up-capable provider minting the same
  record type would make cross-provider redemption live.

## 6. Bypassing the gate through another session-minting path

The check lives in `EmailAndPasswordAuth.signIn`, not in `AuthRealm.issueSignIn`. Every other route
into `issueSignIn` ignores the marker.

- **Attempt:** with a pending marker on an account, obtain a session via Google SSO, GitHub SSO,
  `selectOrg`, or `refreshToken`.
- **Expected (2026-10-19):** SSO on a pending account succeeds ONLY with a provider-verified address, and then
  activates it after deleting the sign-up password (`AuthRealm.resolveSsoAccount`); unverified is refused.
  `selectOrg` and `refreshToken` are downstream of a completed provider sign-in. Confirm all three.
- **The real target:** any provider an app adds to the same realm inherits no marker check. Try a
  hand-rolled magic-link or API-key provider.

## 7. Weak password through the recovery path

`recoverAccountSetPasswordWithToken` never enforced `realm.passwordPolicy` (`setPassword` does).
Pre-existing — but this feature makes that method the designated, and currently ONLY, recovery route
for a lapsed activation, so it is now the path locked-out users are guaranteed to take.

- **Attempt:** complete a reset with a one-character password.

## 8. Resend: the growing delay is a check-then-act

REWRITTEN 2026-10-19 (`docs/tasks-archive/2026-10/20261019-activation-hardening.md`). The July premise — "K concurrent requests must
share ONE token" — was false: every refused sign-in minted a fresh token and none was revoked. Now minting revokes the
owner's earlier tokens, a token is consumed only by the request whose delete removed it (`count == 1`), and the delay
lives on the `PendingActivation` marker: read, compare, write, send. The write (`replaceIfUnclaimed`) is conditional on
`claimedBy` only — it loses to a retire or an SSO claim, never to another resend.

- **Attempt:** fire K concurrent sign-ins for one pending account, then K concurrent resends with whatever tokens
  survived each other's revocation. Count the mails, and whether the marker's `resends` advanced once or K times.
- **Attempt:** K concurrent resends sharing ONE token — expected: exactly one passes the delete count.
- **Bounding it:** the schedule grows per account and the account expires after 7 days, so a won race adds mail inside
  one window; it does not reset the schedule. Fix if it reproduces: a compare-and-set save of the marker
  (`20260727-activation-resend-cooldown-atomicity.md`, option A).

## 9. Resend authorization

The property to attack: `resendActivation` must be unreachable without a live
`AuthRecord.ActivationResendToken`, which is minted ONLY in `AuthRealm.signIn` after a correct
password for a pending account.

- **Attempt:** call resend with a forged/guessed token, with another realm's token, with an
  already-consumed token, and with an expired one.
- **Attempt:** obtain a resend token for account A and use it to trigger mail for account B (the owner
  comes from the token record, not the request — confirm).
- **Attempt:** harvest a resend token from a browser (it is held in memory on `AuthState`, not in
  localStorage or the URL — confirm it does not leak into history or `Referer`).

## Added by the activation-resend gate, round 2 (2026-10-19)

Collected, not executed. §2's "a copy change to close" is wrong once activation is zero-click.

- **Scanner detonation** — pre-register an address on a Defender / Mimecast / Proofpoint tenant; does the sandbox fire the
  activation? Then sign in with the attacker's password; retry via resend.
- **Sustained resend stream** — from an attacker-owned pending account, loop sign-in + resend at the cooldown cadence;
  mails per day to a non-aliasable address, and does anything ever stop it?
- **Persistence after the victim's reset** — take a session after a zero-click activation; let the victim reset the
  password; does `refresh-token` keep renewing the attacker's JWT?
- **Resend-token row growth** — N refused sign-ins give N rows; Mongo TTL culling; `findByToken` latency under growth.
- **Type confusion** — a resend token at `/activate`, a verification token at `/activate/resend`; a resend token redeemed
  through a second `EmailAndPasswordAuth` with other `FrontendUrls`.
- **Custom providers** — a provider that throws `AccountNotActivated(userId)` before its own credential check makes the
  realm mint a resend token for an arbitrary user (`AuthRealm.kt:269-287` trusts `e.userId`).
- **Concurrent burst** — K parallel sign-ins mint K resend tokens; K parallel resends race the non-atomic cooldown.

## Added by the activation hardening (2026-10-19)

Collected, not executed. The design: `docs/tasks-archive/2026-10/20261019-activation-hardening.md`.

- **Scanner detonation, again** — the page now needs a click. Does a sandbox that runs JS and clicks the first primary
  button (some do) activate a pre-registered account? Does any gateway follow the CANCEL link and click it, retiring
  legitimate sign-ups at scale?
- **Retired-key collisions** — sign up the exact retired key (`retired-<sha256(addr)>@<tag>#retired.invalid`, revised
  2026-10-19: the address is hashed, not kept), its upper-case form, with surrounding whitespace, with a Unicode
  look-alike of `#`; every path must refuse. Then try to make a retirement collide on the unique index (two accounts
  whose `_id`s hash to the same 16 bytes is out of reach; look for a realm whose `retire()` renames from something
  other than the account).
- **Reversing the address hash** — the hash is unkeyed by decision: measure how fast a dictionary of a customer's known
  addresses maps retired rows back to addresses, and whether any other field on a retired row (name, profile, audit
  data) still carries the address.
- **Reviving a retired account** — password reset to the retired string, SSO whose provider reports it, an admin or
  app path that loads users by email; any session for a retired row is a finding.
- **Cancel as a weapon** — the cancel token IS the activation token. Can anyone but the mailbox holder obtain it
  (Referer, browser history of the `/cancel-signup/{token}` route, stored mail anonymization)? Can a cancel retire an
  ACTIVE account through a race with activation or reset?
- **SSO verified-claim spoofing** — Google: a Workspace on an attacker domain asserting `email_verified` for a victim
  address on that domain; GitHub: an unverified secondary set as primary, a token without the `user:email` scope, an
  `/user/emails` error path. Each must leave the pending account untouched.
- **Sweep head-of-line blocking** — the sweep takes the 100 OLDEST expired markers per realm; seed 100 accounts whose
  `retire()` keeps failing and check whether younger expired accounts are ever retired.
- **Expiry boundary** — activate, cancel and resend at `pendingAccountLifetime ± 1 s`, with the app clock and the DB
  clock skewed; the link's capped `expiresAt` and the marker's `createdAt` come from the same Kronos only if the repo
  hook and the provider share it.
- **Retire vs activate race** — click Activate while the sweep (or a cancel) retires the same account; possible outcomes
  include an account reported activated that is in fact retired.


## Added by the activation-hardening gate, round 1 (2026-10-19)

Collected, not executed.

- **Re-registration of an expired victim sign-up** — the victim signs up and lets it lapse; the attacker polls sign-up
  for that address, re-registers it the moment it expires, and the victim — who is EXPECTING an activation mail —
  clicks Activate on the attacker's account. Measure the window and whether the mail gives the victim any cue.
- **Cancel → immediate re-registration loops** — the victim cancels, the attacker re-registers within seconds,
  repeatedly. Does anything slow the loop down, and does the victim's mailbox become a mail stream?
- **Resend racing a cancel** — a resend that loaded the user just before a cancel retired it: does it issue a live link
  for a retired row, with a stale `Stored` user whose address is still the original (so the retired-recipient guard
  does not fire)? It also finds no `SignUpCancelToken` (the retire deleted them) and MINTS a new one on the retired
  account (`EmailAndPasswordAuth.resendActivation`): check what that orphan token can still reach.
- **SSO racing the sweep or a cancel** — an SSO sign-in that resolved the user before a retire renamed it: is a session
  issued for the retired row, and does `refreshToken` keep renewing it?
