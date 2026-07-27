# Activation resend + the activation frontend

**Status:** IN PROGRESS 2026-07-27
**Plan:** `.claude/tasks/v1-email-auth-and-sessions.md` → Phase 4 (verification) + Phase 6 (demo pages)
**Follows:** `.claude/tasks/20260727-account-activation.md` — the backend guard. This is its open scope
item 2, plus the resend it deliberately deferred.
**Security-critical:** yes → red-team scenarios fold into
`.claude/tasks/20260727-redteam-account-activation.md`

## Why

The guard works server-side and nothing exposes it. Concretely, today:

- `LoginController.kt:526` collapses every sign-in failure into `Message.error("Login failed")`, so an
  unactivated account is indistinguishable from a wrong password.
- There is no resend endpoint anywhere — `grep -i resend` returns only comments saying so.
- `AuthFrontendRoutes.activateAccount` exists but `AuthFrontendDefault.mount` mounts only `login` and
  `resetPassword`, and `AuthState` has no `activateAccount`. The mailed link lands on a 404.

So a demo sign-up produces an account with no self-service way in.

## The throttle question, resolved

`.claude/tasks/20260727-signup-mail-throttle.md` is deferred, and resend does NOT need it. The two
cases differ in exactly the way that matters:

- **Sign-up** creates a NEW user per aliased address, so a per-user cooldown never triggers. That is
  why it needs `OnBeforeSend` and per-address/per-IP counters — the deferred work.
- **Resend** targets an EXISTING user, so a cooldown keyed on that user's newest
  `EmailVerificationToken` is self-throttling, lives in the provider, and needs no messaging change.

Resend also **rotates** the token (removes the user's previous ones), which bounds row growth and
means only the newest link is live.

## The contract change this needs

The client cannot detect "not activated" today: `AuthRealm.signIn` throws, `AuthApi` maps it to a 403
with `withInfo("Account not activated")`, and matching on a message STRING is not a contract —
it breaks on rewording or translation.

**`AuthSignInResponse` gains `ActivationRequired`.** That is precisely the shape of the existing
`OrgSelectionRequired`: credentials were valid, sign-in is not complete, here is what to do next.
`AuthState.login` already branches on this sealed type. `AuthError.AccountNotActivated` becomes a
typed subclass so `AuthRealm.signIn` can catch it by TYPE rather than by message.

Sign-in for an unactivated account therefore answers `200 ActivationRequired` instead of `403`. Not a
disclosure change: reaching it already required the correct password, and the 403 said as much in its
message.

## Spec

Backend:
- [ ] `AuthError.AccountNotActivated` typed subclass
- [ ] `AuthSignInResponse.ActivationRequired`; `AuthRealm.signIn` returns it
- [ ] `AuthResendActivationRequest` / `AuthResendActivationResponse` (neutral — the same answer for an
      unknown address, an already-activated account, and a cooldown hit)
- [ ] `EmailAndPasswordAuth.resendActivation`: only for accounts with a pending marker, cooldown on the
      newest token, rotate the token, share the send path with `signUp`
- [ ] `RealmTokenConfig.activationResendCooldown`
- [ ] Realm + system + API route + client

Frontend:
- [ ] `AuthFrontendRoutes.resendActivation` (the no-token variant of the deep link)
- [ ] `AuthState.activateAccount` / `resendActivation` / `pendingActivation` carrier
- [ ] `ActivateAccountPage` — serves the deep link, shows activated / expired, and carries the resend
      form for the expired case
- [ ] `AuthFrontendDefault.mount` mounts both
- [ ] `LoginController` routes an unactivated sign-in to the page with the email carried over

## Implementation notes

- `issueAndSendActivationToken` is shared by `signUp` and `resendActivation`, so the token lifetime,
  the link shape and the failure logging cannot drift apart. A resend that built a different URL
  would be a dead link that every server-side test still passes — the e2e asserts the exact URL from
  both paths.
- Resend **rotates**: it removes the user's previous verification tokens before issuing a new one.
  Only the newest link stays live, which also bounds the row growth flagged in red-team §1.
- The cooldown reads `createdAt` on the newest NON-EXPIRED token. Expired ones are filtered out by
  the lookup, which is the behaviour we want: once a link has lapsed the user may ask again at once.
- `TestUserRealm` overrides ONLY `activationResendCooldown` (2s), so both sides of the window are
  testable against a real clock. Exposed as `TestUserRealm.TOKEN_CONFIG` so the spec's wait cannot
  drift out of step with it.
- The email is carried to the activation page on `AuthState.pendingActivation`, NOT as a route
  param — a param would put the address in the address bar, the browser history and any `Referer`.
- `AuthState.login` clears `pendingActivation` at the start of every attempt, for the same reason it
  already cleared `pendingOrgSelection`: a stale one would send the next person who fails to sign in
  to a page prefilled with the previous user's address.

## Test evidence

- [x] Unit (`EmailAndPasswordAuthSpec`, 22 → 26): resend sends nothing for an unknown address, sends
      nothing for an already-activated account, sends nothing inside the cooldown, and past the
      boundary rotates the token and mails the exact link. The first three lean on the spec's
      exploding service defaults, so a stray storage or mail call fails the test rather than being
      asserted after the fact.
- [x] End-to-end (`AccountActivationEmailE2eSpec`, 2 → 5): a resend delivers a WORKING new link and
      the superseded one is dead (and the account stays blocked, so the dead link really did nothing);
      a second resend inside the window sends nothing while answering identically; unknown and
      already-activated addresses send nothing.
- [x] `funktor:auth` green, `funktor:all` 132 → 135, `funktor-demo:server` green, `assemble` green
      (JS included).

### Mutation evidence

| Mutation | Result |
|---|---|
| Drop the cooldown check | ✅ reddens exactly the cooldown test |
| Drop the token rotation | ✅ reddens exactly "kill the old one" |
| Drop resend's pending-marker check | ✅ reddens exactly the already-activated test |
| `signIn` rethrows instead of answering `ActivationRequired` | ✅ reddens the e2e walk, the resend walk and `AuthApiSpec` |

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |
