# ULTRA-00118: SSO signup links to an existing account without checking `email_verified` (account takeover)

> **Status (2026-11-04): IN PROGRESS.** Decided by the maintainer 2026-11-04: A/C, link only if verified. Rechecked
> 2026-11-04: STILL LIVE for an ACTIVE account. Activation hardening (ULTRA-00222) closed the pending half: there,
> `AuthRealm.resolveSsoAccount` refuses an unverified address. For an account that is not pending it returns the user
> "and [isEmailVerified] is never asked" (its KDoc). Google and GitHub, sign-in and sign-up, hand an unverified
> provider address that resolves to an active account a session. `GoogleSsoAuthSpec` "should return the existing user
> when the user already exists" pins that behaviour: its payload has no `email_verified`.
>
> **Part of:** ULTRA-00248 (Auth and sessions epic)

**Found via:** the security review of ULTRA-00119 (value class emailaddress) (Step 4).
**Severity:** HIGH. **PRE-EXISTING** — not introduced by the email work, but that work makes it more
reliably reachable, which is why it was raised there.
**Security-critical:** yes → red-team scenarios already recorded (see bottom).

## The gap

`funktor/auth/src/jvmMain/kotlin/provider/GoogleSsoAuth.kt` and `GithubSsoAuth.kt`, signup path:

```kotlin
val email = EmailAddress.parseOrNull(payload.email) ?: throw AuthError.invalidCredentials()
val existing = realm.users.loadByEmail(email)
val user = existing ?: realm.users.createForSignup(...)
return AuthProvider.SignUpResult(user = user, requiresActivation = false)
```

`payload.emailVerified` is never read, although the Google library exposes it and Google's own
integration guidance requires checking it. GitHub's `email` from `/user` is likewise taken on faith.

**Attack:** an attacker controls a provider account whose email claim is `victim@corp.com` with
`email_verified = false` (a Workspace on a domain they control, or an unverified alternate address).
They hit SSO signup → `loadByEmail` resolves the VICTIM's existing user → the `existing` branch →
`requiresActivation = false` → a full session JWT is minted. Takeover with no password.

Live in the demo: `funktor-demo/server/src/main/kotlin/admin/AdminUserRealm.kt` enables
`Capability.SignUp` on all three providers.

**Why Step 4 raised it:** canonicalizing the provider email is what makes `loadByEmail` HIT where it
previously missed on case variants. The canonicalization is correct and wanted — it just converts a
latent linking gap into a reliably reachable one.

## Composes with a second pre-existing gap

Email+password signup returns `requiresActivation = true`
(`provider/EmailAndPasswordAuth.kt`), but `AuthRealm.signUp` has
`// TODO: send account activation email` and calls `issueSignIn(result.user)` unconditionally — so an
unverified address gets a live session immediately.

**Pre-hijack chain:** attacker registers `victim@corp.com` with their own password and is signed in.
The victim later uses "Sign up with Google" and, via the gap above, is handed the ATTACKER's account.
The attacker keeps the password, so they retain co-access indefinitely.

## The decision needed (why this is not just "add a few lines")

What should happen when a provider reports an email that resolves to an existing local account?

- **A — reject unverified.** If `email_verified == false`, refuse (`invalidCredentials`). Minimal,
  closes the takeover. Does not address a VERIFIED provider email auto-linking to a
  password-created account, which is still an implicit trust decision.
- **B — never auto-link.** SSO signup creates a NEW account, or fails when the address exists;
  linking requires an explicit, authenticated "connect this provider" action. Strongest, most work,
  changes the product flow.
- **C — verified-only auto-link.** Auto-link only when the provider says verified AND the local
  account's own email is verified. Requires the activation flow below to exist first.

Any of these depends on whether the account-activation TODO gets implemented, since "is the local
address verified?" has no answer today.

## Decision (maintainer, 2026-11-04): A/C — link only if verified

The maintainer chose "A/C: link only if verified" and "00118 first, then 00231". Since activation exists, an active
account's address is verified locally (activated, or created by verified SSO), so A and C coincide. An account created
before activation existed has no marker either. A covers it, because the rule depends on the provider's verification
alone.

- `AuthRealm.resolveSsoAccount`: an account that is not pending is returned only when `isEmailVerified()`, else
  `invalidCredentials`. The pending branches are unchanged; the provider is still asked after the marker read, which
  the claim-first race specs depend on.
- `GoogleSsoAuth.signUp` and `GithubSsoAuth.signUp`: a new account is created only for a verified address (else whoever
  claims an address first owns the account its owner signs up for later).
- Specs:
  - `SsoPendingAccountBaseSpec` (real store, both backends): the two rows that pinned "an ACTIVE account keeps today's
    behaviour, verified or not" are flipped, and new rows cover sign-up creating an account.
  - `GoogleSsoAuthSpec` and `GithubSsoAuthSpec`: the success rows now carry a verified address.
  - `ActivationHardeningE2eSpec` (HTTP, both backends): two new rows, an active account and a new address.
- Docs: `auth.astro` and `llms/funktor.md` said SSO does not activate a pending account, which has been stale since the
  activation hardening; both now state the rule. A CHANGELOG entry under `## Unreleased`.

## Also required

- Implement (or explicitly drop) the activation email — `AuthRealm.signUp`'s
  `// TODO: send account activation email`, and honour `requiresActivation` instead of always
  calling `issueSignIn`.

## Red-team scenarios

Recorded in ULTRA-00117 (redteam value class emailaddress) §1 and §2 — do NOT execute
during feature work.

## History

- 2026-10-21: numbered ULTRA-00118 (was `docs/tasks/20260726-sso-email-verification.md`).
- 2026-11-04: rechecked against the code. The takeover remains for active accounts, and the activation TODO is done
  (sign-up honours `requiresActivation`), so option C's "is the local address verified?" now has an answer.
