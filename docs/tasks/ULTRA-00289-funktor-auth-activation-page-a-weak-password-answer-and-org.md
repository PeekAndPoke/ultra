# ULTRA-00289: Funktor auth activation, round 2: overlapping links, the name at activation, a weak-password answer, org realms

> **Status (2026-11-05): DESIGNED.** Decisions taken by the maintainer on 2026-11-05. Plan to be re-confirmed before
> building.
>
> **Part of:** ULTRA-00248 (Epic: Auth and sessions)

**Security-critical:** yes (the activation link sets the password and signs in; ULTRA-00231).

## Why this exists

Created from the ULTRA-00231 gate, round 1 (2026-11-04). Its two MINORs, 1.9 and 1.10, are parts C and D below. On
2026-11-05 the maintainer folded in the gate's two parked decisions, 1.12 and 1.13 (parts A and B).

- **1.12:** after the growing delay, anyone who signs the address up again rotates the activation link. That kills
  the link a real user may be typing a password into.
- **1.13:** a pre-registering attacker chose the display name, and it survived into the victim's account.
- **1.9:** a weak password is shown as a dead link. `AuthLoginApi.activateAccount` answers every `AuthError` the same
  way: 400, `{success: false}`, no info.
- **1.10:** org realms. `OrgSelectionRequired` is wasted, and with 0 orgs the page claims "You can sign in now".

## Decisions (maintainer, 2026-11-05)

1. Older activation links stay alive for a while after a re-send. That while is a per-realm setting with a sensible
   default; 30 minutes was named.
2. No profile fields before activation. The display name is a mandatory field on the activation page, "for now".
3. A new operation on `AuthUserAdapter` writes the name at activation, and it is required: no default.
4. Pending accounts from 0.114–0.117 that carry an attacker's name need no special handling, as there are no live
   apps.
5. ULTRA-00289 (1.9, 1.10) is folded in here.
6. A dedicated answer for a weak password.
7. Org realms: n orgs go to the login page's org picker; 0 orgs say "active" without "sign in now".
8. Breaking API changes are fine; nothing is online.

## The plan

### A. Older links overlap

- `RealmTokenConfig.activationLinkOverlap: Duration = 30.minutes`. It must not be negative. 0 means today's
  behaviour: only the newest link lives.
- On a re-send, the new token is created first, as today. Then each older `EmailVerificationToken` of the owner
  changes:
  - an expired one is removed;
  - a live one gets `expiresAt = min(its own, now + overlap)`.
  A link is never extended. This replaces `removeEmailVerificationTokens(exceptId)` on that path.
- The rest stays as it is:
  - Using any link removes all the others (`activateAccount`).
  - The cancel link is untouched.
  - Rows stay bounded: the growing delays allow about three live links, all inside the first 30 minutes.

### B. The name at activation

- `AuthSignUpRequest.EmailAndPassword.displayName` is removed. The pending account gets the default derived from
  the address (`CreateUserForSignupParams.of(email)`), which tells nothing the mail does not.
- `AuthActivateAccountRequest.displayName: String` is new and required.
- The server checks it before the token is touched, next to the password policy:
  - trimmed;
  - 1 to 100 characters;
  - no control characters.
- `AuthUserAdapter.activate(user, params: ActivateUserParams): Stored<USER>` is new and required.
  - `ActivateUserParams(displayName)` is a params class, so a later profile field does not break the interface
    again.
  - A realm without sign-up throws `AuthError.notSupported()`, as `createForSignup` does.
- **The order in the activation POST:**
  1. Check the policy and the name.
  2. Consume the token.
  3. Write the password.
  4. Win the marker.
  5. Remove the other passwords and the other links.
  6. `users.activate(...)`.
  7. Sign in.

  The name is written only after the activation has won. If the adapter throws, the account is active and keeps the
  default name; the POST answers 500 and logs it. A pending account with a dead link would be worse.
- An activation through a reset or SSO asks for no name. Those accounts keep the default derived from the address.
- **The frontend:**
  - `ActivateAccountPage` gets a mandatory name field.
  - The sign-up form in `LoginController` drops its name field.
  - The TS SDK runtime (`login.ts`, `verifyRuntime.ts`) and the Vue demo's `LoginView.vue` follow.
- **Implementations to update:**
  - `TestUserRealm`;
  - `ActivationHardeningE2eApps`;
  - the test realm in `funktor/auth/src/jvmTest/kotlin/index_jvmTest.kt`;
  - the four funktor-demo realms (`AdminUserRealm`, `B2bRealm`, `B2b2cRealm`, `OperatorRealm`).

### C. A dedicated answer for refused input

- `AuthActivateAccountResponse.failure: ActivationFailure?`, with `WeakPassword` and `InvalidDisplayName`. Every other
  failure stays `400 {success: false}` with no reason, so a used link, an unknown link and an expired link remain
  indistinguishable.
- Both checks run before the token is looked at, so a reason tells nothing about the token.
- `ActivateAccountPage` shows the policy message or a field error and keeps the form. The link stays usable, as the
  token was not touched.

### D. Org realms

- **n orgs:** `AuthState.activateAccount` hands `OrgSelectionRequired` to `pendingOrgSelection`, and the page routes
  to the login page's picker.
- **0 orgs, or sign-in switched off** (`signIn == null`): the page says the account is active, without "sign in now".

### Not here

- Storing tokens hashed: ULTRA-00291.
- Session revocation: ULTRA-00136.

## Acceptance

- [ ] **A:** e2e on both backends.
  - [ ] Inside the overlap, an older link still activates, and then the newer one is dead.
  - [ ] After the overlap, the older link is dead.
  - [ ] A link is never extended.
  - [ ] With overlap 0, only the newest link lives.
- [ ] **B:** e2e.
  - [ ] Activation writes the given name.
  - [ ] Sign-up ignores a `displayName` sent by an old client.
  - [ ] A blank, overlong or control-character name is refused before the token: the link still works afterwards.
  - [ ] A throwing adapter leaves the account active.
- [ ] **C:** e2e for both reasons, plus a page spec with a 400 weak-password answer, which the fake server cannot do
      today.
- [ ] **D:** e2e on an org realm with sign-up, with 0, 1 and n orgs, plus a page spec for the picker handoff.
- [ ] Mutation checks on A, B and C, the security-relevant paths.
- [ ] CHANGELOG `## Unreleased` names the breaking API. ULTRA-00290 (docs) is updated with the settled API.
- [ ] ULTRA-00127 (red team) has the new scenarios: a stale link inside the overlap, and the name field as a phishing
      carrier.

## History

- 2026-11-04: created from the ULTRA-00231 gate (1.9, 1.10).
- 2026-11-05: 1.12 and 1.13 folded in on the maintainer's decisions; rewritten as the plan above.
