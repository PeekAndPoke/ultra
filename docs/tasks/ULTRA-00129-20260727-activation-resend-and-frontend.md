# ULTRA-00129: Activation resend + the activation frontend

**Status:** IMPLEMENTED 2026-07-27 (`84fa3b47`, reshaped by the gate in `0a5ca8bf`); every Spec box ticked 2026-10-19 against
the code, with the evidence under each. **Not archived, deliberately:** this doc records ONE gate round — 16 confirmed
findings (13 fixed, 3 recorded), and the HIGH reshaped the feature (a single-use `ActivationResendToken` replaced the
email-keyed resend) — but neither a PASS verdict nor a second review round over the reshaped design. Its sibling
ULTRA-00126 (account activation) got a "gate passed" commit (`b96c2c1c`); this doc never did, and the Status was never
touched after the fix commit. Archive on the maintainer's word that the gate stands, or after a clean round. Open elsewhere,
not here: ULTRA-00130 (activation resend cooldown atomicity) (TODO) and
ULTRA-00127 (redteam account activation) (collected, not executed).
**Plan:** ULTRA-00030 (v1 email auth and sessions) → Phase 4 (verification) + Phase 6 (demo pages)
**Follows:** ULTRA-00126 (account activation) — the backend guard. This is its open scope
item 2, plus the resend it deliberately deferred.
**Security-critical:** yes → red-team scenarios fold into
ULTRA-00127 (redteam account activation)

## Why

The guard works server-side and nothing exposes it. Concretely, today:

- `LoginController.kt:526` collapses every sign-in failure into `Message.error("Login failed")`, so an
  unactivated account is indistinguishable from a wrong password.
- There is no resend endpoint anywhere — `grep -i resend` returns only comments saying so.
- `AuthFrontendRoutes.activateAccount` exists but `AuthFrontendDefault.mount` mounts only `login` and
  `resetPassword`, and `AuthState` has no `activateAccount`. The mailed link lands on a 404.

So a demo sign-up produces an account with no self-service way in.

## The throttle question, resolved

ULTRA-00128 (signup mail throttle) is deferred, and resend does NOT need it. The two
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
- [x] `AuthError.AccountNotActivated` typed subclass *(`AuthError.kt:16`)*
- [x] `AuthSignInResponse.ActivationRequired`; `AuthRealm.signIn` returns it *(`AuthSignInResponse.kt:81`; the catch,
      the `ActivationResendToken` write and the return are `AuthRealm.kt:269-289`)*
- [x] `AuthResendActivationRequest` / `AuthResendActivationResponse` (neutral — the same answer for an
      unknown address, an already-activated account, and a cooldown hit)
      *(as REDESIGNED by the gate, not as written here: the request carries the single-use resend `token` instead of an
      address and the response reports `sent` — see the Review record. Models: `AuthResendActivationRequest.kt`,
      `AuthResendActivationResponse.kt`)*
- [x] `EmailAndPasswordAuth.resendActivation`: only for accounts with a pending marker, cooldown on the
      newest token, rotate the token, share the send path with `signUp`
      *(`EmailAndPasswordAuth.kt:364`; cooldown `:394`; shared `issueAndSendActivationToken` `:416`, called from `:353`
      and `:401`)*
- [x] `RealmTokenConfig.activationResendCooldown` *(`RealmTokenConfig.kt:33`; the gate added
      `activationResendTokenLifetime`, `:39`)*
- [x] Realm + system + API route + client *(`AuthRealm.kt:349`, `AuthSystem.kt:110`, `AuthLoginApi.kt:117`,
      `AuthApiClient.kt:60` endpoint and `:121` call)*

Frontend:
- [x] `AuthFrontendRoutes.resendActivation` (the no-token variant of the deep link) *(`AuthFrontendRoutes.kt:29`)*
- [x] `AuthState.activateAccount` / `resendActivation` / `pendingActivation` carrier *(`AuthState.kt:263,271`; carrier
      `PendingActivation` `:208`, `pendingActivation` `:219`)*
- [x] `ActivateAccountPage` — serves the deep link, shows activated / expired, and carries the resend
      form for the expired case *(`ActivateAccountPage.kt`: states `Activated` / `LinkNotUsable` / `Offer`; as redesigned
      by the gate there is no email field — the resend button uses the carried token)*
- [x] `AuthFrontendDefault.mount` mounts both *(`AuthFrontend.kt:41` and `:50`)*
- [x] `LoginController` routes an unactivated sign-in to the page with the email carried over
      *(`LoginController.kt:535`; the address travels on `AuthState.pendingActivation`, in memory, not in the URL)*

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
      *Re-checked 2026-10-19 from the last recorded run (`build/test-results` XML, not re-run): `EmailAndPasswordAuthSpec`
      26, `AccountActivationEmailE2eSpec` 8 (the doc's counts above are the 2026-07-27 ones), `AuthApiSpec` 23 — all 0
      failures.*

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
in ULTRA-00128 (signup mail throttle). Before this feature, sign-up was a ONE-SHOT (one
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

## Review record — round 2 (/feature-review, 2026-10-19, maintainer: "review round")

`reviewer-xhigh` ×3, two-phase, over the CURRENT code on master (the feature's files after the `AuthApi` split and the
`EmailAddress` value class). Round 1's mechanics hold (token minted after the password check, consumed before sending,
realm- and owner-scoped, rotation, reload copy, carrier clearing). **Round 2 is NOT clean: 5 MAJOR.** Three need a
maintainer decision. The July ledger above lists 2 of its 3 "recorded" findings and 3 of the "four dissolved" ones; the
missing entries cannot be recovered from this repo.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 | MAJOR | `EmailAndPasswordAuth.kt:364-404`, `AuthRealm.kt:269-292`, `GoogleSsoAuth.kt:169` (domain) | Resend lifts the 24-hour limit on the pre-registration takeover. An attacker signs up `victim@corp.com` with password P; the victim later uses Google sign-in on that same pending row; the attacker, who knows P, triggers genuine activation mails whenever they like; one click activates and P works | maintainer-decision | Verified: SSO loads by email (`GoogleSsoAuth.kt:169`) and does not clear the marker by design (`AuthRecord.kt:66-78`, "a stuck credential, not a shared account"). Before resend the stuck state was permanent; resend lets the password-holder re-mail the victim. Round 1 never weighed it: its fix assumes "knows the password" means "owns the mailbox". Root cause: ULTRA-00118 (sso email verification) (options B/C close the chain, A does not) |
| 2 | MAJOR | `ActivateAccountPage.kt:95-115`, `AuthFrontend.kt:41-47`, `DataLoader.kt:99-101` (security; domain MINOR) | The activation link redeems on PAGE LOAD, so a mail gateway that detonates links in a JS sandbox (Safe Links, Mimecast, Proofpoint) activates an attacker's pre-registered account with no click; activation also only proves the mailbox, against the marker-clearing rule "prove the mailbox AND invalidate every earlier password" (`AuthRecord.kt:61-64`, `auth.astro:132-134`) | maintainer-decision | New with this feature (before it the link went to a 404); the KDoc at `:97-100` weighs UX only |
| 2 | MAJOR | `AuthRealm.kt:267-293`, `RealmTokenConfig.kt:28-33`, `AuthRecord.kt:90-91,105-107` (security) | The resend token does not stop the mail faucet it was introduced for: the attacker in round 1's own HIGH creates the accounts and chose every password, so sign-in mints a token at will — 12 mails/hour per address, forever (`PendingActivation` never expires, nothing counts resends); disabling `SignUp` does not stop existing accounts (resend is ungated, `:349-351`) | maintainer-decision | The round-1 HIGH's fix reason ("an anonymous caller cannot reach the send path") does not describe that attacker. Options: a per-account resend cap or growing cooldown on the marker; a "this wasn't me" link that deletes the unactivated account |
| 2 | MAJOR | `funktor/all/src/jvmTest/kotlin/index_testJvm.kt:43-45`, `AuthRecordStorageBaseSpec.kt` (implementation) | `ActivationResendToken` is never written or read on Mongo by any test (the e2e is Karango-only, the both-backend storage spec has no case for it) | fix | CLAUDE.md: storage-touching features run on both backends; a type serialized in production is serialized by a test. Add `findByToken` round-trip, expiry and type-scoping cases (precedent: `PendingActivation` at `:214`) |
| 2 | MAJOR | `AuthRealm.kt:277-287`, `EmailAndPasswordAuth.kt:240-242,377` (domain; implementation + security duty (c)) | The recorded July entry "cooldown is check-then-act" is LOW only on a false premise: K concurrent resends need NOT share a token — every refused sign-in mints a fresh one and none is revoked, so K parallel sign-ins give K tokens and K resends race the non-atomic cooldown; token consumption also ignores the delete count | fix (cheap half here) + correct the tracked task | In scope: revoke earlier resend tokens when minting, and consume only when `removeById(..).count == 1` (atomic single-document delete). The cooldown race itself stays with ULTRA-00130 (activation resend cooldown atomicity), whose "Why not urgent", severity and red-team §8 premise must be corrected |
| 2 | MINOR | `EmailAndPasswordAuth.kt:401-403,433,443-449` (implementation, security) | `sent = true` even when the mail failed, after rotation already revoked the working link | fix | Return the `EmailResult` and answer `sent = false` on failure |
| 2 | MINOR | `AuthRealm.kt:284-285` (implementation) | The resend token's lifetime is pinned by no test (a far-future expiry survives) | fix | Short lifetime in `TestUserRealm` + e2e past it |
| 2 | MINOR | `EmailAndPasswordAuth.kt:297-306`, `EmailAndPasswordAuthSpec.kt:234-276` (implementation) | "password check before marker check" — the whole authorization for minting — is pinned only by an exploding stub | fix | e2e (pending + wrong password → 403, no `ActivationRequired`) and a unit test |
| 2 | MINOR | `ActivateAccountPage.kt:147-159`, `AuthState.kt:263-277` (implementation, domain) | The error/Retry branch is unreachable (failures become `null`), so a transient error on a valid link shows "not valid any more"; the `NotSent` copy asserts a cooldown for every `sent = false` | fix | Surface the failure; keep the `NotSent` copy only where it is true (implementation withdrew the copy part; domain kept it) |
| 2 | MINOR | `AuthProvider.kt:79-84`; `EmailAndPasswordAuth.kt:468-469,589-592`; `EmailAndPasswordAuthSpec.kt:853-855`; `AuthRecord.kt:102`; `AuthSignInResponse.kt:85`; `AuthRealm.kt:98`; `MonkoAuthRecordsRepo.kt:53-56` (all three) | KDoc/comments factually wrong: the interface still documents the email-keyed neutral resend; "no resend endpoint"; the endpoint is `/activate/resend`; `AuthApi` no longer exists; Mongo TTL "culls" | fix | Factually wrong text |
| 2 | MINOR | `funktor/auth/src/jsTest` (implementation) | The frontend half is untested (the `ActivationRequired` branch, the carrier clears, the routing) | fix | A `TestBed.preact` spec |
| 2 | MINOR | `AccountActivationEmailE2eSpec.kt:12` (implementation) | Wildcard import | fix | Code style |
| 2 | MINOR | `AuthRecordStorageBaseSpec.kt:235-275` (domain) | `removeAllByOwner(exceptId)` — what rotation relies on — is unpinned at storage level on both backends | fix | Storage case + mutation check |
| 2 | MINOR | `AuthRealm.kt:277-287` vs `EmailAndPasswordAuth.kt:371-377` (domain) | The token is minted in `AuthRealm` but consumed in the provider through `Services` (OrgSelectionToken does both in the realm) | maintainer-decision | Design fit; a provider built with other `Services` storage gets `sent = false` forever |
| 2 | MINOR | `AuthLoginApi.kt:129-131` (security) | A tenth public site echoing `AuthError.message`, missing from the error-disclosure inventory | fix | Add to ULTRA-00051 (auth error account enumeration) |
| 2 | MINOR | `docs-site/.../funktor/auth.astro:123-134`, `llms/funktor.md:185-190` (security) | The docs over-claim the token as the fix for the mail faucet, and state a marker-clearing rule `activateAccount` does not meet | after the decisions | Rewrite against the decided design |
| 2 | MINOR | `MonkoAuthRecordsRepo.kt:57-60` (all three) | Pre-existing: Mongo's TTL index on a `Long` never expires anything | out-of-scope → follow-up | In the parallel plan's backlog (`e4076e46`) |
| 2 | — | `EmailAndPasswordAuth.kt:573-603` | (July, recorded) the reset path skips the password policy | accepted | Still true; tracked at `ULTRA-00127 (redteam account activation):97-99` |

**Red-team, to add to ULTRA-00127 (redteam account activation)** (collected, not executed): scanner detonation of a
pre-registered address; a sustained resend stream from an attacker-owned pending account; persistence after the victim's
reset via `refresh-token`; resend-token row growth; type confusion between resend and verification tokens across
providers; a custom provider that throws `AccountNotActivated(userId)` before its credential check. §2's "a copy change
to close" is wrong once activation is zero-click.

**Verdict:** gate FAIL — 3 MAJOR await the maintainer (pre-registration takeover, zero-click activation, mail faucet);
2 MAJOR are fixable now (Mongo coverage; revoke-on-mint and conditional consumption). Not archived.

## History

- 2026-10-21: numbered ULTRA-00129 (was `docs/tasks/20260727-activation-resend-and-frontend.md`).
