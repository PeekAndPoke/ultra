# Red-team scenarios: account activation

**Status:** COLLECTED 2026-07-27 — **do NOT execute during feature work.** These are for a dedicated
penetration-test session.
**Feature:** `.claude/tasks/20260727-account-activation.md` (commit `31667449`)
**Related:** `.claude/tasks/20260726-redteam-value-class-emailaddress.md` §1–2 (SSO auto-link),
`.claude/tasks/20260727-redteam-messaging.md`

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
- **Chain:** combine with the SSO auto-link gap (`.claude/tasks/20260726-sso-email-verification.md`)
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

Registering a victim's address before they do leaves them with an account they cannot use: sign-in
returns 403 until activation, and there is no resend endpoint. Their only route in is "forgot
password", which requires them to know the account exists.

- **Attempt:** pre-register a target address, then measure whether the victim can self-serve back in
  from the login page alone (today: the login page renders the 403 as a generic "Login failed", so
  they are not even told what happened).

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
- **Expected:** SSO succeeds by design (see the rule documented on `AuthRecord.PendingActivation` —
  clearing on SSO would reopen the pre-hijack chain, so the marker deliberately SURVIVES and the
  attacker's password stays blocked). `selectOrg` and `refreshToken` are downstream of a completed
  provider sign-in. Confirm both.
- **The real target:** any provider an app adds to the same realm inherits no marker check. Try a
  hand-rolled magic-link or API-key provider.

## 7. Weak password through the recovery path

`recoverAccountSetPasswordWithToken` never enforced `realm.passwordPolicy` (`setPassword` does).
Pre-existing — but this feature makes that method the designated, and currently ONLY, recovery route
for a lapsed activation, so it is now the path locked-out users are guaranteed to take.

- **Attempt:** complete a reset with a one-character password.

## 8. Resend: the cooldown is a check-then-act

Added 2026-07-27 after the gate on the resend feature. `EmailAndPasswordAuth.resendActivation` reads
the newest verification token, compares it to the window, then rotates and writes — with no
transaction, no lock and no unique index on `(realm, ownerId, _type)`.

- **Attempt:** fire K concurrent `POST /login/{realm}/activate/resend` sharing ONE valid resend token.
  Every request whose read completes before the first write commits passes the window check.
- **Bounding it:** the resend token is single-use and consumed before the send, and it can only be
  obtained by passing the password check, so this is not anonymous and not unbounded — the question is
  how many sends one token can produce under a burst.
- **Real fix if it reproduces:** a unique index on `(realm, ownerId, _type)` for
  `EmailVerificationToken` plus a conditional upsert, or a compare-and-set on the newest `createdAt`.

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
