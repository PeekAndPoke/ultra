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

## Review record

Gate per `/feature-review`, change set `b265aed4` (the file set in its message). The verdict line given to every
reviewer: "a round is clean when it has zero CRITICAL and zero MAJOR findings; MINORs and NITs do not make it unclean."

**Round 1 (2026-11-04), level 1 (`reviewer-high`), blind. NOT CLEAN: 1 MAJOR.** Implementation: CLEAN. Domain: NOT
CLEAN. Security: CLEAN. All three case tables (provider × operation × account state) agree that no row gives an
unverified address a session or an account.

| # | Charter | Sev | Finding | Disposition | Reason |
|---|---------|-----|---------|-------------|--------|
| 1.1 | Domain | MAJOR | GitHub requires the PRIMARY address (`GithubSsoAuth.kt:228`). A user whose `/user` (public) address is verified but not their primary is now refused on an ACTIVE account, with no compile signal | FIXED | Verified against ULTRA-00222's archived record: the primary rule was accepted "fail-closed, and only for pending accounts". This change extended it to active accounts without revisiting that reason. `verified` alone proves the mailbox: GitHub cannot verify an address for someone who does not hold it. The maintainer's decision says "verified", not "primary". The pending rule loosens the same way |
| 1.2 | Domain | MINOR | CHANGELOG lacks the no-compile-signal traps: a custom authorize URL without `user:email`, a GitHub App without the "Email addresses" permission, users with an unverified Google address | FIXED | |
| 1.3 | Domain | MINOR | `GithubSignInButton.kt:54-55` says the scope matters only for pending accounts | FIXED | |
| 1.4 | Domain | MINOR | Linking by address alone: Google's `email_verified` on a non-authoritative domain; no tie to the provider's stable subject (`sub`, GitHub id) | → ULTRA-00117 | Follows from the A/C decision; recorded as red-team scenarios |
| 1.5 | Impl | MINOR | `FakeGithub.emailsAsked` incremented, never read; nothing pins one `/user/emails` call | FIXED | Asserted |
| 1.6 | Impl, Sec | MINOR | No Google e2e over HTTP | REJECTED | Google's client library is not on `funktor/all`'s test classpath (ULTRA-00222's recorded deviation). Google runs against the real store on both backends in `SsoPendingAccountBaseSpec`; the wiring after the provider is shared with GitHub's e2e |
| 1.7 | Sec | MINOR | Test gaps: sign-up on an EXPIRED pending account (retire, then the creation gate) never runs; no ACTIVE "verified but not primary" row | FIXED | Rows added |
| 1.8 | Sec | MINOR, PRE-EXISTING | A stale user gets one JWT on a row retired between the load and the marker read | → ULTRA-00288 | |
| 1.9 | Sec | MINOR, PRE-EXISTING | GitHub OAuth without `state` (login CSRF) | → ULTRA-00288 | |
| 1.10 | Sec | MINOR, PRE-EXISTING | SSO `createForSignup` and Google's verification can escape as 500 | → ULTRA-00288 | |

Probed and CLEAN (round 1): Google ID token audience/issuer/signature; GitHub code exchange with the secret; canonical
matching; retired keys unreachable; claim-first ordering; enumeration (identical refusals); SSO-created users are not
superusers; docs and mirror agree; no other provider resolves a third-party address. A repo-wide grep for the old
statements ("SSO sign-in does NOT", "verified or not", "never asked") found no other copy.

Fix batch (2026-11-04): GitHub accepts any verified entry of `/user/emails` matching the address (`isVerifiedAddress`).
The rest of the batch: the CHANGELOG traps, the button comment, `emailsAsked` asserted, and new rows for "primary or
not" (pending and active) and for a sign-up on an expired pending account. Green on the final tree: auth 273 tests and
funktor-all 238 tests, both backends. Mutation-checked: putting the primary check back reddens the three GitHub
"primary or not" / creation rows; removing Google's creation gate reddens both Google creation rows, the new
expired-account row included; nothing else turns red.

Next round: level 2 (`reviewer-xhigh`), two-phase, because round 1 had a MAJOR.

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
