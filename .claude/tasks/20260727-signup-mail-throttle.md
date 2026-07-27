# Sign-up is an unthrottled outbound-mail primitive — needs a suppression point

**Status:** TODO — deferred by the user 2026-07-27, to be handled later
**Found via:** the security review of `.claude/tasks/20260727-account-activation.md` (commit
`31667449`), which is also what introduced the exposure
**Security-critical:** yes → attack scenarios already recorded, see the bottom
**Supersedes:** `.claude/tasks/20260727-messaging-followups.md` §5 — that item asked for the same
mechanism for a different consumer (activation RESEND). This is the same task with a second, already
shipped, consumer.

## The exposure

`POST /login/{realm}/signup` is anonymous and `public()` (`funktor/auth/src/jvmMain/kotlin/api/AuthApi.kt`).
As of account activation it **sends an email to a caller-chosen address** from the app's verified
sending identity (`provider/EmailAndPasswordAuth.kt`, `sendAccountActivationEmail`).

Before that commit, sign-up mailed nothing. The tripwire that asserted exactly this — `emails.capturedTo(email)
shouldBe emptyList()` in `PasswordResetEmailE2eSpec` — was written to go red when activation landed,
and did.

**There is no rate limiting anywhere in the repo.** `grep -riE "ratelimit|rate-limit|throttl"` over
all `*.kt` returns one unrelated hit (a kraft signature-pad option). `letTheBotsWait()`
(`api/AuthApi.kt`) is a 250–500 ms `delay` per request — it bounds neither request rate nor
concurrency, and on a coroutine it costs the server nothing to hold.

### Why it amplifies

A given address STRING can only be signed up once — the second attempt hits `"User already exists"`
(`provider/EmailAndPasswordAuth.kt`) and mails nothing. So the multiplier is **aliasing**:
`EmailAddress.of` (`ultra/security/src/commonMain/kotlin/user/EmailAddress.kt`) trims, lowercases and
rejects non-ASCII, but folds neither subaddress tags nor dots. Therefore
`victim+1@gmail.com` … `victim+N@gmail.com` (and `v.i.ctim@gmail.com`) are N distinct accounts, N
`PendingActivation` markers, N `system_sent_messages` rows and **N mails into one inbox**.

### Impact

- Mail-bomb any address on the internet from a customer's verified SES/SendGrid identity.
- Complaint-rate and domain-reputation damage, up to sending suspension — which takes down password
  reset and every other transactional mail with it.
- Unbounded row growth. `AuthRecord.PendingActivation` has `expiresAt = null` **by design** (that is
  what stops it failing open — see the record's KDoc), so the TTL indexes in
  `db/karango/KarangoAuthRecordsRepo.kt` and `db/monko/MonkoAuthRecordsRepo.kt` never cull it.
- Compounds with sign-up's enumeration asymmetry (below): the attacker can learn which addresses will
  generate mail before spending the attempt.

### Where the original scope-cut argument went wrong

`.claude/tasks/20260727-account-activation.md` argued that the missing `OnBeforeSend` hook blocks
activation RESEND, not activation, so resend could be cut and the hook deferred. That is true as far
as it goes, and it missed that **sign-up is the same primitive as resend** — an anonymous request that
causes a send to a caller-chosen address. Resend was cut; sign-up shipped.

## Options

Recorded with the trade-offs as assessed during the gate. Not decided.

### A. A suppression point in the send chain — `OnBeforeSend` (the principled fix)

`EmailHooks` has only `OnAfterSend`, so there is no point at which a send can be REFUSED. Add
`OnBeforeSend` returning a decision, and wire a per-address / per-IP cooldown through it.

- Serves every current and future consumer at once: sign-up, activation resend, password reset,
  suppression lists, send-idempotency.
- Needs a counter store. In-process is enough for a single JVM but wrong for a cluster; note that
  `CapturedEmails` is a global singleton precisely because it takes no constructor dependencies
  (`funktor/messaging/src/jvmMain/kotlin/senders/CapturedEmails.kt`) — the same trick applies, with
  the same caveat that per-JVM state is not a cluster-wide limit.
- Composition site is settled: `funktor/messaging/src/jvmMain/kotlin/index_jvm.kt`, the `Mailing`
  factory. Hooks must stay OUTSIDE the dev-config overrides so the throttle judges the ORIGINAL
  recipient, not a dev-redirected one.

### B. A per-flow cooldown in the provider

"Only send if the newest `EmailVerificationToken` for this user is older than N."

- Directly testable, no messaging change.
- **Does not fix this case**: each aliased sign-up creates a NEW user, so a per-user cooldown never
  triggers. It IS the right shape for resend, where the user already exists.

### C. Fold subaddresses in `EmailAddress`

Canonicalize `+tag` (and gmail dots) so the 2nd..Nth aliased sign-up hits "user already exists" and
mails nothing.

- Attacks the amplification rather than the rate, and is cheap.
- Changes ACCOUNT IDENTITY semantics globally — some users legitimately use `+tags` as separate
  accounts, and dot-folding is provider-specific (gmail yes, most others no). Belongs with the
  existing email-canonicalization work, not here.
- Leaves the one-mail-per-address vector untouched (see red-team §2, pre-registration phishing).

### D. Product-level gate

Captcha / proof-of-work on sign-up, or not granting `Capability.SignUp` on public realms.

## Also worth folding in

- **Sign-up's enumeration asymmetry.** A fresh address returns `200 {success=true,
  requiresActivation=true}`; an existing one returns `400` + `withInfo("User already exists")`. Both
  status and body differ. Sign-in, recover-account and activate are all correctly uniform — sign-up is
  the odd one out. Pre-existing. The fix that also helps here: answer with the SAME shape for an
  existing address and mail THAT address a "someone tried to sign up with your address" notice instead
  of an activation link.
- **The activation mail's copy** has no "if you did not sign up, ignore this" (red-team §2). A text
  change, and it belongs with whatever touches this flow next.

## Red-team scenarios

`.claude/tasks/20260727-redteam-account-activation.md` §1 (the amplifier), §3 (enumeration), §4
(pre-registration DoS). COLLECTED — do not execute during feature work.
