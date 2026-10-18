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

The client cannot detect "not activated" today: `AuthRealm.signIn` throws, `AuthLoginApi` maps it to a 403
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

Gate run 2026-07-27 over `b96c2c1c..84fa3b47`.

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | findings | 5 — 4 fixed, 1 recorded |
| 2. Domain expert | findings | 5 — 4 fixed, 1 recorded |
| 3. Security | findings | 6 — 5 fixed, 1 recorded |

All three reviewers independently found the same HIGH, and they were right.

### The design was wrong, and the fix reshaped the feature

**The throttle argument in this task's own "resolved" section was incomplete.** It claimed a per-user
cooldown makes resend independent of the deferred sign-up throttle. That bounds mail PER ACCOUNT while
the attacker chooses HOW MANY ACCOUNTS EXIST — and `PendingActivation` never expires, so every pending
account is a permanent tap. As first built, `resendActivation` took a bare email on an anonymous
route: 12 mails/hour to any pending address, forever, and ×N via the sign-up aliasing already recorded
in `.claude/tasks/20260727-signup-mail-throttle.md`. Before this feature, sign-up was a ONE-SHOT (one
mail per address, ever). It would have become a sustained faucet.

The domain reviewer identified the root cause exactly: `ActivationRequired` was modelled on
`OrgSelectionRequired` **but dropped the field that makes it safe** — the token. `OrgSelectionRequired`
carries a `selectionToken` precisely because `selectOrg` needs an authorization.

Fixed by mirroring it properly:

- `AuthRecord.ActivationResendToken` — single-use, short-lived, minted in `AuthRealm.signIn` on the
  only path that has proven the password.
- `ActivationRequired.resendToken` carries it; `AuthResendActivationRequest` takes a **token, not an
  email**. An anonymous caller who merely knows an address cannot reach the send path at all.
- The token is consumed BEFORE anything is sent, so one refused sign-in buys exactly one resend.
- The cooldown stays as a second throttle behind the authorization.

That also dissolved four other findings: the timing-oracle (the send path is unreachable without a
token), the capability-gate concern, and "an anonymous caller can delete the victim's pending link".

### Also fixed

- **Rotation deleted the token it had just mailed.** Reversed to create-then-remove-`exceptId`, so two
  interleaved resends can no longer leave a user holding a link that was dead before it arrived.
- **The response lied.** It was a neutral `data object`, so the UI said "a new link is on its way" even
  when the cooldown had suppressed it — the single most likely path, since users try to log in
  minutes after signing up. Now `AuthResendActivationResponse(sent)`, which is safe *because* the
  request is authorized, and the page says either "on its way" or "we already sent one recently".
- **A reload of a consumed activation link said "invalid or expired" and offered a resend that could
  never fire** — for an account that was in fact already activated. Now it names both possibilities
  and points at sign-in.
- **The login → activation hand-off explained nothing.** It now leads with "your account is not
  activated, we sent a link to X, check your inbox" and demotes resend to a secondary action.
- **The resend form had no validation**, so `formCtrl.isValid` and its `disabled` modifier were
  decoration and an empty address reached the server. The field is gone entirely — the token
  identifies the account.
- **`clearPendingActivation()` had no callers.** The page now reads-and-clears the carrier, so a
  shared browser cannot show the previous user's address.
- **A "single-use" e2e test passed for the wrong reason** — the cooldown blocked the replay too, so
  dropping `removeAuthRecord` left the suite green. Caught by mutation, fixed by waiting the cooldown
  out first so single-use is the only thing that can stop the second send.
- **The cooldown e2e raced a 2-second window against ~1s of injected server delay, and could silently
  change meaning**: a slow box let the first resend through, `emails.clear()` swallowed it, and the
  assertion then tested a different path. Restructured so a slow box fails LOUDLY instead.

### Recorded, not fixed

- **The cooldown is still a check-then-act.** K concurrent requests can pass the window check together.
  Now bounded by the single-use token (K requests must share one token, and the burst is one send per
  token in practice), so the impact is small — but a unique index on `(realm, ownerId, _type)` or a
  conditional write is the real fix. Added to the red-team task.
- `recoverAccountSetPasswordWithToken` still does not enforce the password policy (pre-existing;
  already red-team §7).

### Mutation evidence (second round)

| Mutation | Result |
|---|---|
| The resend token is never consumed | ✅ reddens single-use — **only after that test was fixed**; it was green before |
| Rotation without `exceptId` | ✅ reddens the rotation unit test |
| The resend token is never persisted | ✅ reddens both resend walks |
| Drop the cooldown check | ✅ reddens the cooldown test |
| `signIn` rethrows instead of `ActivationRequired` | ✅ reddens three specs |
