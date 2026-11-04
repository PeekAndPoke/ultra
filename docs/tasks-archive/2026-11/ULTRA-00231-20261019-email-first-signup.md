# ULTRA-00231: Email-first sign-up: the password is chosen when activating

> **Status (2026-11-04): DONE.** gate PASS in round 1 (2026-11-04); MINOR batch fixed and mutation-checked; docs follow-up ULTRA-00290, frontend follow-up ULTRA-00289; two decisions parked for the maintainer (1.12, 1.13)
>
> **Part of:** ULTRA-00248 (Auth and sessions epic)

**Plan:** ULTRA-00221 (insights auth parallel plan) (auth tracks); follows
ULTRA-00222 (activation hardening) (merged `ed8d3b38`, ships in 0.114.0; its parked row "Activate requires a password" is answered by THIS design instead)
**Security-critical:** yes — red-team: ULTRA-00127 (redteam account activation)

## Why

Activation hardening leaves one residual: a victim who clicks Activate on an attacker's pre-registration activates the
ATTACKER's password. An unverified account must never hold a credential (Sudhodanan & Paverd, USENIX Security 2022). The
known pattern (Slack, Notion, Atlassian): sign up with the email only; the mailed link opens "Choose your password";
setting it activates the account. The attacker never sees the link, so never holds a credential.

## Decisions (maintainer, 2026-11-04 — plan re-confirmed)

Checked against master `7adaaeb1` before building; the plan had six gaps, raised and settled:

1. **SSO on a pending account KEEPS deleting its Password records** before the marker goes. Pending accounts created
   under 0.114–0.117 still hold their sign-up password, possibly a pre-registering attacker's; dropping the step would
   let that password start working after a verified SSO activation. Also covers custom providers.
2. **Signing up again with a pending address answers exactly like a fresh sign-up.** The growing delay applies
   silently (no mail inside the window, no `nextResendAt`), so an anonymous caller cannot tell pending from new or read
   the schedule. An ACTIVE address still answers "User already exists" (the enumeration asymmetry stays with
   ULTRA-00128 (signup mail throttle)). The first sign-up's display name stays.
3. **The activation POST signs the user in** (maintainer chose this over the reset-like "go to sign-in"): the response
   carries an `AuthSignInResponse`, best-effort as after sign-up. A new session-minting path: the gate reviews it.
4. **Folded in:** the reset completion enforces the realm's password policy (red-team ULTRA-00127 §7, still live: only
   the frontend checked) — reset activates a pending account too, so it is a second way to set the first password; and
   the TypeScript SDK runtime (`ultra/codegen` `login.ts`, its verify script, the sdkgen demo) loses
   `activation-required` / `resendToken`.

Found in the check, no decision needed:
- **The activation link must be single-use for real.** `activateAccount` answers success on an already-active account
  and ignores the token's delete count (`EmailAndPasswordAuth.kt:536-559`). Once the POST sets a password, a leftover
  or double-submitted link would set a new password on an active account. So: consume the token by its delete count
  first, refuse when there is no pending marker, password before marker (the reset pattern).
- ULTRA-00130 (activation resend cooldown atomicity) loses its endpoint; its race moves to the sign-up re-send.
  ULTRA-00129 (activation resend and frontend) is mostly superseded. Both get updated at archive.

## Spec (re-confirm with the maintainer before implementing — this rewrites the activation flow)

- [x] `SignUp` (password provider) takes the email and profile fields, NO password; creates the pending account + marker.
      *(`AuthSignUpRequest.EmailAndPassword` lost `password`; `EmailAndPasswordAuth.signUp` writes the marker, then the
      link — no `Password` record.)*
- [x] The activation page asks for a new password (the realm's password policy); setting it activates (marker removed,
      Password record created) — one POST, single-use token. *(`AuthActivateAccountRequest.password`;
      `EmailAndPasswordAuth.activateAccount`: policy before the token, token by delete count FIRST, no marker → null,
      older passwords removed, chosen one written, marker removed only while unclaimed (else the written password is
      taken back), dead links cleaned. `AuthRealm.activate` signs in best-effort (decision 3). Page:
      `ActivateAccountPage` loads the realm for its policy; `AuthState.activateAccount` applies the session.)*
- [x] Verified SSO on a pending account: activates; unverified → refuse (unchanged). **Changed by decision 1:** it keeps
      deleting any password (accounts from before email first hold one).
- [x] Resend: signing up again with a pending address re-sends the link under the growing delays — the resend-token
      flow goes away. Sign-in of a pending account answers like a wrong password. *(`reSendIfPending`: silent inside
      the delay, claimed or expired → nothing sent; same `SignUpResult` as a fresh sign-up (decision 2).)*
- [x] Cancel, expiry, retire (hashed key) unchanged.
- [x] API break (pre-1.0): listed in `CHANGELOG.md` `## Unreleased`, with the traps that give no compile signal.
- [x] **Owns the auth docs follow-up for BOTH changes** — ULTRA-00290 (docs funktor auth sign-up and activation). The
      activation section of `auth.astro` / `llms/funktor.md` was rewritten here already, because it described the
      removed resend flow; the rest (cancel, expiry, retire, the breaking-API lists, the upgrade traps of 0.114.0 and
      of this change) is the docs task's.
- [x] E2e on both backends; frontend via `TestBed.preact`; mutation-checked — see Test evidence.

## Review record (the ledger)

### Round 1 — level 1 (`reviewer-high` ×3), blind, at `11f49ed3`: CLEAN (0 CRITICAL, 0 MAJOR)

Implementation & code style 7 MINOR · domain 5 MINOR · security 6 MINOR + 2 NIT. Overlaps merged; every finding was
checked against the code before acting.

| # | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1.1 | MINOR | `EmailAndPasswordAuth.kt:538-549` | Activation removed every password BEFORE winning the marker: a concurrent reset that wins loses its password, leaving an active account with none (all three charters) | fix | Verified: the order was remove-all → write → conditional marker delete. Now write → win the marker → remove the others (`exceptId`). A legacy password is already dead before the cleanup: sign-in validates only the newest record |
| 1.2 | MINOR | `EmailAndPasswordAuth.kt:727-739`, `AuthRecord.kt:59` | A reset that activates leaves a pre-upgrade sign-up password stored; the marker KDoc says no such password is left (impl, security) | fix | Verified. The reset now removes the other passwords after winning the marker; KDoc corrected |
| 1.3 | MINOR | `AuthRealm.kt:330-342` | The activation POST hands out a session through a provider without the `SignIn` capability (security) | fix | Verified: `activate` never checked it. Now `success = true, signIn = null` |
| 1.4 | MINOR | `ActivateAccountPage.kt:115` | The page redirected whenever ANYONE was signed in, hiding a refused link or a failure (impl) | fix | Verified. Redirects only when this answer carried a `Success`; js spec added |
| 1.5 | MINOR | docs `auth.astro:120`, `llms/funktor.md:180` | "null when an org choice is due" — it is `OrgSelectionRequired` then (domain) | fix | Verified against `issueSignIn` |
| 1.6 | MINOR | `AccountActivationEmailE2eSpec.kt:49-51`, `AuthRealm.kt:775`, `AuthState.kt` KDoc, `ActivateAccountPage` failure copy | Stale texts: "the activation half generalizes", "sign-up commits a password record", "an org choice is left to the login page", "Nothing has changed" after a possible commit (impl, domain) | fix | Verified, each reworded |
| 1.7 | MINOR | `LoginController.kt:4`, `EmailAndPasswordAuth.kt:138` | Unused `asFormRule` import; a double blank line (impl, security NIT B) | fix | Verified |
| 1.8 | MINOR | ticket `:66` | "mutation-checked" ticked before the evidence existed (impl) | fix | The Test evidence section was written while the round ran |
| 1.9 | MINOR + NIT | `AuthLoginApi.kt:110`, `ActivateAccountPage.kt:109` | A weak password at activation reads as a dead link (domain F1, impl F5, security NIT A) | out-of-scope → ULTRA-00289 | Reachable only when the page's policy check and the server's disagree; needs an API choice (a response field or a fixed info) |
| 1.10 | MINOR | `AuthRealm.kt:701-719`, `AuthState.kt:236` | Org realms: 0 orgs says "you can sign in now", n orgs mints an `OrgSelectionToken` the page drops; untested (domain F4, impl SURPLUS) | out-of-scope → ULTRA-00289 | No realm in this repo has both sign-up and orgs; the server answer is right, the frontend handling is the gap |
| 1.11 | MINOR | `EmailAndPasswordAuth.kt:396-409` | Signing up again has a timing tell (mail sent inline on one branch) (security F2) | out-of-scope → ULTRA-00128 | Status and body are identical (decision 2); the fix is sending mail off the request path, which is ULTRA-00128's mechanism |
| 1.12 | MINOR | `EmailAndPasswordAuth.kt:418,466` | An anonymous sign-up again rotates the link and kills a genuine user's live one (security F3) | maintainer decision | A trade-off: keep older live links (more live credentials, now session-granting) or keep rotation (a nuisance, the newest mail still arrives). Recorded in ULTRA-00130 and the red team |
| 1.13 | MINOR | `EmailAndPasswordAuth.kt:360-366` | A pre-registering attacker's display name survives into the victim's account (domain F5, a consequence of decision 2) | maintainer decision | Pre-existing; options: confirm the name on the activation page, or take no profile fields before activation |
| 1.14 | MINOR | ULTRA-00127, `RealmTokenConfig.kt:24` | Red team not updated; the 24 h link is now a session credential, unnamed (security F6) | fix | ULTRA-00127 extended, retired items marked; CHANGELOG names the credential and the capability rule |

Did not survive or needed no action: the SURPLUS answers found nothing removable; the `isExpired` branch of
`activateAccount` stays (an operator can shorten `pendingAccountLifetime` after links went out). The leftover
`activation-resend-token` rows: probed by the domain charter, nothing deserializes them.

**Probed and CLEAN (round 1):** single use under concurrency, no session for a non-pending / claimed / expired /
retired account, the write table of every path that touches a pending account's passwords or marker (security), the
upgrade of markers, legacy passwords and old clients (Slumber ignores the stale `password` key — verified in
`DataClassAwaker`), re-sign-up status and body, reset policy order, SSO decision 1, the TS runtime's fallback, imports
and KDoc links.

**Verdict: gate PASS** — round 1 clean at level 1 (0 CRITICAL, 0 MAJOR), e2e present and green on both backends,
every MINOR fixed, routed (ULTRA-00289, ULTRA-00128) or parked for the maintainer (1.12, 1.13), and the fix batch
mutation-checked. The fix batch is MINOR-only, so by the loop's rules it needs no further round.

## Test evidence (at `11f49ed3`; the round-1 fix batch on top: auth 276 / 0, js 14 / 0, funktor-all 237 / 0)

Counts from the XMLs under `.build-claude/<module>/test-results/` with fresh timestamps; failing cases read from the XML.

- `:funktor:auth:jvmTest` 274 / 0 (both backends). `:funktor:auth:jsBrowserTest` 12 / 0 (`ActivateAccountPageSpec` 9).
- `:funktor:all:jvmTest` 235 / 0 (`ActivationHardening{Karango,Monko}E2eSpec` 24 + 24, `AccountActivationEmailE2eSpec` 7,
  `AuthApiSpec` 23, `PasswordResetEmailE2eSpec` 3).
- `:ultra:codegen:tsVerify` and `:funktor:codegen:vueVerify` green. Compile sweep (`compileKotlin*`/`compileTestKotlin*`,
  JVM and JS) green, the demo apps included. Docs site builds.
- `kotlinStoreYarnLock` was skipped (`-x`): it copies an identical lock onto the maintainer's file and fails on the mode
  change (MC-00031).

### Mutation evidence

Backed up with `copyfile`, restored, verified with `cmp`; one mutant per run. "unit" = `:funktor:auth:jvmTest`, "e2e" =
`:funktor:all:jvmTest`, "js" = `:funktor:auth:jsBrowserTest`; both backends where a spec is per backend.

| # | Mutation | Result |
|---|---|---|
| M1 | Sign-up stores a password again | RED unit "sign up user successfully"; e2e "Email first: a sign-up stores NO password" ×2. (The marker-first unit test stays green under it: the fake's exploding `hashPassword` throws first.) |
| M2 | Activation ignores the link's delete count | RED unit "consumed first goes no further"; realm spec "the delete COUNT decides" ×2 |
| M3 | A leftover link sets the password of an ACTIVE account | RED unit "leftover link of an ACTIVE account"; realm spec "leftover activation token sets NO password" ×2 and "retired row" ×2. e2e stays green: there a replay finds no link (consumed and cleaned), so the marker check is reached only through a failed delete |
| M4 | Activation keeps older passwords | RED unit event order; e2e "pending account from BEFORE email first … the link replaces it" ×2 |
| M5 | Activation without the password policy | RED unit "WEAK password is refused"; e2e "A WEAK password at activation is refused" |
| M6 | Reset completion without the password policy | RED unit reset "WEAK password"; e2e reset walk (the weak step) |
| M7 | Signing up again ignores the delay | RED unit "inside the first delay", "the delay GROWS"; e2e "GROWING delays" ×2, "inside the delay sends nothing" |
| M9 | SSO keeps passwords (decision 1 undone) | RED `SsoPendingAccount*` legacy and GitHub rows and the order row ×2, `SignInRace*` ×2; e2e SSO legacy ×2 |
| M10 | Sign-in ignores the pending marker | RED unit legacy pending and claimed rows, `SignInRace*` ×4; e2e legacy pending ×2 |
| M11 | A lost claim keeps the written password | RED unit "exactly the password written here is taken back"; realm spec stale read ×2 |
| M12a | The page POSTs on load | RED js, all 9 page cases (the load-time POST shows in every call list) |
| M12b | The page sends no password | RED js first case and the Retry case (body asserts) |
| M13 | Activation issues no session | RED e2e walkthrough (`signIn` is `Success`) |
| M14 | Signing up again answers "already exists" | RED unit 6 sign-up-again rows; e2e 9 rows |
| M15 | `AuthState` ignores the activation's session | RED js "a session in the answer signs the user in" |

Round-1 fix batch (same protocol, on the fixed tree):

| # | Mutation | Result |
|---|---|---|
| N1 | Activation removes the other passwords BEFORE winning the marker (the 1.1 order) | RED unit event order and "a retire, SSO or reset that won … taken back" (its `removePasswordRecords` explodes) |
| N2 | Activation never removes the other passwords | RED unit event order; e2e "BEFORE email first … the link replaces it" ×2 |
| N3 | A reset that activates keeps the other passwords | RED unit "should ALSO activate"; e2e "A reset of a pending account from BEFORE email first" ×2 |
| N4 | Activation ignores the `SignIn` capability | RED realm spec "through a provider WITHOUT SignIn" ×2 |
| N5 | The page redirects whenever anyone is signed in | Survived the first version of its js case (it relied on the router's exploding default, whose throw does not fail the spec); the case now asserts the URL hash → RED |
| N6 | M3 again (a leftover link sets an active account's password) | RED e2e "Cancel never retires an ACTIVATED account" ×2 — the leftover link after a reset-activation; M3's e2e gap is closed |

## What this REMOVES — the simplification (recorded 2026-10-19 at the maintainer's request)

File references are to the activation-hardening branch (`wt/auth-activation`) as of its round-1 fix batch; re-grep
before relying on a line — rounds 2–4 moved them (claim-first retire, the sign-in re-read, cancel ordering).

- **The whole resend-token flow.** Today a pending user can only get a new link by signing in with the sign-up password:
  sign-in answers `ActivationRequired` with a short-lived `ActivationResendToken`, which the page then trades for a
  resend. With no password at sign-up there is nothing to sign in with, so it all goes:
  - the `ActivationResendToken` auth-record type (`funktor/auth/src/jvmMain/kotlin/domain/AuthRecord.kt`) and its
    storage cases (`AuthRecordStorageBaseSpec`, both backends);
  - minting it on sign-in, revoking the owner's earlier ones, consuming it only on `count == 1`
    (`AuthRealm.kt`, `EmailAndPasswordAuth.kt`);
  - the `ActivationRequired` sign-in answer (`AuthSignInResponse.kt`, `AuthError.kt`) and the "password check before
    marker check" rule that exists only because of it;
  - the resend endpoint and its request/response (`AuthResendActivationRequest.kt`, `AuthResendActivationResponse`,
    `AuthApiClient.kt`, `AuthLoginApi.kt`, `AuthProvider.resendActivation`);
  - the frontend's in-memory resend carrier and its Retry/"we already sent one" copy (`AuthState.kt`,
    `LoginController.kt`, `ActivateAccountPage.kt`);
  - the tests that pin all of it (`AccountActivationEmailE2eSpec`, `ActivationHardeningE2eSpec` resend cases,
    `AuthRealmActivationBaseSpec`, `EmailAndPasswordAuthSpec`, `ActivateAccountPageSpec`).

  **Resend becomes:** signing up again with a still-pending address re-sends the link, under the same growing delays on
  the marker. One path instead of three.
- **SSO on a pending account** loses its most delicate step: "delete the sign-up password FIRST, then the marker" and the
  fault-injection test pinning that order (`AuthRealm.resolveSsoAccount`, `SsoPendingAccountBaseSpec`). A pending
  account has no password, so a verified SSO sign-in just activates it.
- **The attacker's credential never exists**, so the scenarios built around it stop applying: the one-click Activate
  pre-hijack (the residual decision 1 accepted), "reset supersedes the attacker's password" (`findLatest` ordering), and
  the red-team items about the attacker polling sign-in or rotating the token through resends. Re-read
  ULTRA-00127 (redteam account activation) and retire what no longer applies.
- **Retire gets simpler:** there is no Password record on a pending account to remove.

What it ADDS is small: a password field on the activation page (with the realm's policy) and a `Password` record created
by the activation POST.

## History

- 2026-10-21: numbered ULTRA-00231 (was `docs/tasks/20261019-email-first-signup.md`).
- 2026-11-04: IN PROGRESS. plan re-confirmed with the maintainer 2026-11-04, decisions recorded
- 2026-11-04: DONE. gate PASS in round 1 (2026-11-04); MINOR batch fixed and mutation-checked; docs follow-up ULTRA-00290, frontend follow-up ULTRA-00289; two decisions parked for the maintainer (1.12, 1.13)
- 2026-11-04: implemented (`11f49ed3`), gate PASS in round 1, MINOR batch fixed; archived. Follow-ups: ULTRA-00290 (docs),
  ULTRA-00289 (activation page: weak password, org realms); red team ULTRA-00127 extended.
