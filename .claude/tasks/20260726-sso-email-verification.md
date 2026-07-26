# SSO signup links to an existing account without checking `email_verified` (account takeover)

**Status:** TODO — needs a POLICY decision from the user before implementing.
**Found via:** the security review of `.claude/tasks/20260726-value-class-emailaddress.md` (Step 4).
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

## Also required

- Implement (or explicitly drop) the activation email — `AuthRealm.signUp`'s
  `// TODO: send account activation email`, and honour `requiresActivation` instead of always
  calling `issueSignIn`.

## Red-team scenarios

Recorded in `.claude/tasks/20260726-redteam-value-class-emailaddress.md` §1 and §2 — do NOT execute
during feature work.
