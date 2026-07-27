# Account activation (email verification on sign-up)

**Status:** IN PROGRESS 2026-07-27
**Plan:** `.claude/tasks/v1-email-auth-and-sessions.md` → Phase 4, verification half (§7 "signUp →
if `requiresActivation`, create `EmailVerificationToken`, send verification email. Don't auto-issue
session/JWT yet"; §7 "signIn → on unverified user").
**Security-critical:** yes → red-team follow-up required.

## Why now

`EmailAndPasswordAuth.signUp` has returned `requiresActivation = true` since it was written, and
`AuthRealm.signUp:229` answers it with an empty `if (result.requiresActivation) { // TODO }` and then
calls `issueSignIn` regardless. So today an address nobody has proved they own gets a live session
immediately. `AuthRecord.EmailVerificationToken` exists with zero producers and zero consumers, and
`AuthSystem.activate` is a stub returning `success = false`.

It also composes into a pre-hijack chain recorded in `.claude/tasks/20260726-sso-email-verification.md`:
an attacker registers `victim@corp.com`, is signed in, and keeps co-access when the victim later
arrives via SSO and is auto-linked to that account. Activation is the half of that fix that does not
need a policy decision.

This is the feature the email test seam
(`.claude/tasks-archive/2026-07/20260727-framework-composed-email-sender.md`) was built for: the flow
is only real if a test can read the mail and follow the link in it.

## The design

### Where "not activated yet" lives

**A marker row, `AuthRecord.PendingActivation`, whose PRESENCE means "not activated".** Created at
sign-up, removed on activation. `expiresAt = null`, `token = null` — it is state, not a secret.

Rejected alternatives, both for concrete reasons:

- **A field on the user** (`AuthUser.emailVerifiedAt`). `AuthUser` is deliberately tiny and its
  additions must be non-breaking via default getters — but the only safe default here (`null` =
  unverified) would lock out every existing account in every realm the moment sign-in checks it.
- **"A non-expired `EmailVerificationToken` exists" as the state.** Fails OPEN. `findLatestRecordBy`
  filters expired records out, so 24 hours after sign-up the user would silently become activated —
  the check would erase itself exactly when it starts to matter.

The marker keeps the token free to expire on its own schedule, and is back-compat-safe by
construction: no existing account has one, so nothing already in a database is affected. SSO users
never get one either (`requiresActivation = false`), so SSO sign-in is untouched.

### Flow

1. `EmailAndPasswordAuth.signUp` — after the password record: write the `PendingActivation` marker,
   write an `EmailVerificationToken` (lifetime from `RealmTokenConfig.emailVerificationTokenLifetime`),
   and send the activation mail with a deep link. **In the provider, not the realm** — `FrontendUrls`
   lives on the provider, which is why `recoverAccountInitPasswordReset` already sends its own mail.
   The layering mirrors it exactly.
2. `AuthRealm.signUp` — when `requiresActivation`, return `AuthSignUpResponse(signIn = null,
   requiresActivation = true)` and do NOT call `issueSignIn`. That is the realm's whole share.
3. `EmailAndPasswordAuth.signIn` — after the password check, a pending marker means
   `AuthError.accountNotActivated()`. Without this the feature is decorative.
4. `AuthSystem.activate` → `AuthRealm.activate` → `EmailAndPasswordAuth.activateAccount`: consume the
   token (single-use), drop the marker, `success = true`. Unknown or expired token → `success = false`,
   never an error — the endpoint is anonymous.
5. `recoverAccountSetPasswordWithToken` **also drops the marker.** Following a link mailed to the
   address proves precisely what the activation link proves. This is the recovery path, see below.

### Scope cut: no resend endpoint in this increment

`.claude/tasks/20260727-messaging-followups.md` §5 records "no `OnBeforeSend` hook, so activation
resend has nowhere to put a throttle" as a blocker. It blocks RESEND, not activation — so resend is
out of scope here and the hook is not needed yet.

The reason that is acceptable rather than a hole is step 5: an account whose activation link lapsed
recovers through "forgot password", which mails the same address and, on completion, activates. There
is always a way back without a resend endpoint.

**Note for whoever builds resend:** the throttle does not have to be an `OnBeforeSend` hook. A
per-flow cooldown ("only send if the newest verification token for this user is older than N") lives
naturally in the provider, is directly testable, and needs no messaging change. The hook is for
cross-cutting infrastructure suppression (global suppression lists), which is a different problem.

### Smaller decisions

- **`AuthActivateAccountRequest` gains `provider`.** Every other auth request model carries it, the
  realm needs it to pick a provider, and the deep link already has `{provider}` in its path, exactly
  as `resetPassword` does.
- **`AuthSignUpResponse.success` becomes `signIn != null || requiresActivation`.** It previously meant
  "you are signed in". With activation, a successful sign-up legitimately issues no session — and
  `LoginController.doSignup` renders "Sign-up failed" on `success == false`, so leaving it would make
  every successful activation-required sign-up look broken to the user. (That frontend already
  ignores `data.signIn` entirely, so withholding the session changes nothing else there.)
- **`AuthActivateActivateResponse` → `AuthActivateAccountResponse`.** The duplicated word is a typo in
  a type that had no implementation; renaming it now costs 5 files and never gets cheaper.
- **Blocked sign-in throws `AuthError`, it does not add an `AuthSignInResponse` case.** Precedent:
  `noOrganisationAccess` is the same shape of failure (credentials fine, sign-in refused). A dedicated
  response case is what the frontend resend flow will want — that is a parked frontend decision, so it
  is not made here.
- **No per-realm opt-out knob.** `EmailAndPasswordAuth` already hardcodes `requiresActivation = true`;
  a realm that does not want activation does not grant `Capability.SignUp`. Add the knob on the second
  real case, not the first imagined one.
- Two `// TODO: make configurable` comments on the password-recovery path are resolved in passing:
  `RealmTokenConfig.passwordRecoveryTokenLifetime` and `randomTokenByteLength` already exist for
  exactly this, and the new code reads them, so leaving the old code hardcoded next to it would be
  strictly worse.

## Spec

- [x] `AuthRecord.PendingActivation` marker record (never expires, no token)
- [x] `AuthFrontendRoutes.activateAccount` deep-link route
- [x] `EmailAndPasswordAuth.signUp` writes marker + token and mails the activation link
- [x] `AuthRealm.signUp` withholds the session when activation is required
- [x] `EmailAndPasswordAuth.signIn` refuses while the marker is present
- [x] `activateAccount` consumes the token once, drops the marker
- [x] Password reset completion drops the marker (the recovery path)
- [x] `AuthRealm.Messaging.sendAccountActivationEmail` + `DefaultMessaging` template, stored
      anonymized
- [x] Model changes: request `provider`, response rename, `AuthSignUpResponse.success`

## Implementation notes

- The marker is written BEFORE the token and the mail (`provider/EmailAndPasswordAuth.kt`), so a crash
  mid-signup leaves an account that cannot be used rather than one that is silently activated.
- `AuthRealm.activate` gates on `Capability.SignUp` — a provider that cannot sign users up cannot have
  issued an activation token.
- `AuthApi` answers `ok(success = false)` for an unknown token and `badRequest` only for an unknown
  realm/provider. The endpoint is anonymous, so a different answer for a token that exists would let
  an attacker probe for live tokens.
- **`AuthRecordStorage.removeAllByOwner` had no test in either backend** and activation is its first
  real consumer. Covered now in `AuthRecordStorageBaseSpec`, i.e. on Karango *and* Monko.
- `EmailStoring` tags: `account-activation`. `PasswordResetEmailE2eSpec` used to pick the persisted
  mail with `last()`; that address now has an activation mail under the same refs, so both e2e specs
  select by TAG instead — otherwise the assertion silently drifts onto a different mail.

## Test evidence

- [x] Unit: `EmailAndPasswordAuthSpec` (16 → 22) — sign-up writes all three records and the mailed
      link, sign-in refused while pending, activation consumes the token and drops the marker,
      unknown token touches nothing, reset also activates
- [x] Storage, BOTH backends: `AuthRecordStorageBaseSpec` (5 → 7 each for Karango and Monko) — the
      marker's null-token/null-expiry round-trip, and `removeAllByOwner` scoped to realm + owner + type
- [x] End-to-end through the mail: `AccountActivationEmailE2eSpec` (new, 2) — sign up → read the mail
      → assert the link survives URL encoding → sign-in refused → activate → sign-in works → token is
      single-use; plus the persisted copy carries no token
- [x] API surface: `AuthApiSpec` — sign-up returns no session, sign-in before activation is 403,
      activation via the mailed token then returns a real token
- [x] `PasswordResetEmailE2eSpec`'s "signup must not mail anything yet" tripwire went red exactly as
      designed and now asserts the activation mail; its closing sign-in documents that a completed
      reset is what activates that account
- [x] `./gradlew :funktor:auth:jvmTest :funktor:all:jvmTest :funktor-demo:server:test` green
      (`funktor:all` 128 → 132), `./gradlew assemble` green

### Mutation evidence

| Mutation | Result |
|---|---|
| Drop the pending-marker check from `signIn` | ✅ reddens the provider unit test |
| `AuthRealm.signUp` issues the session anyway | ✅ reddens the e2e + `AuthApiSpec` sign-up contract |
| Password reset no longer drops the marker | ✅ reddens `PasswordResetEmailE2eSpec` + the unit test |
| `activateAccount` does not consume the token | ✅ reddens the single-use leg (unit + e2e) |
| **`PendingActivation` gets a finite `expiresAt`** | ✅ reddens the e2e + `AuthApiSpec` — an unverified account signs straight in, which is the exact fail-open the marker design exists to prevent |

## Review record (filled by /feature-review)

Gate run 2026-07-27 over `e510725e..31667449`.

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | findings | 3 — 2 fixed, 1 escalated |
| 2. Domain expert | findings | 5 — 2 fixed, 2 rejected with reason, 1 escalated |
| 3. Security | findings | 6 — 1 escalated (blocking), 5 recorded as red-team |

Every finding was re-verified against the code before being accepted. Two reviewers independently
found the missing frontend; two independently found that the activation gate sits in one provider
rather than at the session choke point.

### Fixed here

- **`activateAccount` consumed the token BEFORE dropping the marker.** The two writes are not atomic,
  and that order fails to a permanent lockout (link dead, account still blocked, no resend). Reversed,
  with the reasoning written down: the other order fails to a live token on an already-activated
  account, which is a no-op.
- **`AuthRealm.activate` gated on `Capability.SignUp`.** Closing public registration would have
  retroactively voided every outstanding activation link — while leaving the password-reset workaround
  open, i.e. blocking the intended path and not the fallback. Gate dropped; a provider that does not
  issue these tokens already refuses via `AuthProvider.activateAccount`'s `notSupported()` default.
- **`AuthApiSpec`'s "sign in before activation must be refused" asserted only 403**, which is also
  what an unknown user and a wrong password return — so it would have stayed green, proving nothing,
  if the sign-up test above it were renamed or reordered. Now asserts the discriminating message.
- `AuthSignUpResponse.success`'s kdoc overclaimed; narrowed, and the org-realm case it does not cover
  is named.

### Rejected, with the reason recorded in code

Two reviewers wanted the marker cleared on **SSO sign-in** and on authenticated **`setPassword`**, for
consistency with the password-reset rule. Both would be wrong, and the rule they generalise from is
narrower than it looks: clearing requires proving the mailbox **AND** invalidating every earlier
password. Reset does both (`findLatestPasswordRecord` only validates the newest record, so the
sign-up password dies). SSO proves the mailbox but leaves the attacker's password intact — clearing
there would REOPEN the exact pre-hijack chain this feature closes. `setPassword` proves the password,
not the mailbox. Written up on `AuthRecord.PendingActivation`, including the accepted cost: an
unactivated account that signs in via SSO still cannot use its password until it goes through a reset.

### Escalated to the user — see the two scope items below

1. **Sign-up is now an unthrottled outbound-mail primitive** (security, HIGH).
2. **The activation link has no frontend page** (impl + domain, HIGH).

### Recorded, not fixed

`.claude/tasks/20260727-redteam-account-activation.md` — 7 scenarios, led by the mail amplifier, the
pre-registration phishing angle and sign-up's enumeration asymmetry.

**Red-team follow-up** (security-critical): `.claude/tasks/20260727-redteam-account-activation.md`

## Open scope items (user decision)

### 1. No throttle on sign-up mail — the scope-cut argument was incomplete

This task argued that the missing `OnBeforeSend` hook blocks *resend*, not activation. The security
review showed that reasoning misses a case: **sign-up itself is the same primitive.** Before this
commit sign-up mailed nothing; now `POST /login/{realm}/signup` — anonymous, `public()`, with no rate
limiting anywhere in the repo — sends mail to an attacker-chosen address from the app's verified
sending identity.

A given address string can only be signed up once, so the amplification comes from aliasing:
`EmailAddress.of` folds neither subaddress tags nor dots, so `victim+N@gmail.com` are N accounts and N
mails into one inbox.

Not fixed here because a real throttle needs infrastructure this increment does not have (per-address
or per-IP counters, and a suppression point in the send chain).

**Deferred by the user 2026-07-27** → full write-up, options and trade-offs in
`.claude/tasks/20260727-signup-mail-throttle.md`, which supersedes
`.claude/tasks/20260727-messaging-followups.md` §5 (same mechanism, second consumer).

### 2. The frontend is a separate increment — the mailed link currently lands on nothing

`AuthFrontendRoutes.activateAccount` is new, but `AuthFrontendDefault.mount` mounts only `login` and
`resetPassword`, there is no `ActivateAccountPage`, and `AuthState` has no `activateAccount` call.
`funktor-demo/adminapp` — the ONLY demo realm granting `Capability.SignUp` — mounts neither the
activation page nor the reset page, so a demo sign-up today produces an account with no self-service
way in at all.

**This is deliberate**: the frontend design questions (separate activation page vs. a login-page mode;
out-of-the-box page vs. embeddable widget; whether activation returns a token or forwards to login;
the unactivated-login → resend flow) are parked by the user and were not to be decided here. It is
recorded as a scope cut rather than a finding — but the backend is not USABLE until it lands, and
`LoginController` currently renders the 403 as a generic "Login failed" rather than "check your mail".
