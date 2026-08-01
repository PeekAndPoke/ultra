# Auth API must not echo internal error messages on public routes

**Status:** TODO
**Plan:** none — from `.claude/tasks/error-disclosure/20260720-error-response-disclosure-audit.md` (finding 7).
Applies the design in `.claude/tasks/error-disclosure/20260720-exception-disclosure-architecture.md` (default-deny at
the render boundary, `HasClientMessage` opt-in) to `AuthError` specifically. That design is settled —
this task does not re-open it.
**Security-critical:** yes

## Problem

`funktor/auth/src/jvmMain/kotlin/api/AuthApi.kt:57,79,106,127,148,170,193,216,235`:

```kotlin
} catch (e: AuthError) {
    ApiResponse.forbidden<AuthSignInResponse>().withInfo(e.message ?: "")
}
```

`AuthError.message` reaches the client unconditionally — no environment gate, no filtering. Seven of
the nine catch sites are on `public()` routes, so the audience is anonymous.

Route gating, read from `AuthApi.kt`:

| Handler | Line of `withInfo` | `authorize` | Line |
|---|---|---|---|
| `signIn` | 57 | `public()` | `AuthApi.kt:46` |
| `selectOrg` | 79 | `public()` | `AuthApi.kt:69` |
| `setPassword` | 106 | `authenticated()` | `AuthApi.kt:90` |
| `signUp` | 127 | `public()` | `AuthApi.kt:116` |
| `activateAccount` | 148 | `public()` | `AuthApi.kt:138` |
| `recoverAccountInitPasswordReset` | 170 | `public()` | `AuthApi.kt:159` |
| `recoverAccountValidatePasswordResetToken` | 193 | `public()` | `AuthApi.kt:182` |
| `recoverAccountSetPasswordWithToken` | 216 | `public()` | `AuthApi.kt:205` |
| `refreshToken` | 235 | `authenticated()` | `AuthApi.kt:227` |

## 1. Complete inventory of `AuthError` construction sites

`AuthError` is `open class AuthError(message: String, cause: Throwable?) : Throwable`
(`funktor/auth/src/jvmMain/kotlin/AuthError.kt:3`). Eight factory functions plus six ad-hoc
`AuthError("literal")` constructions. Full set from a repo-wide grep of non-`build/` `.kt` files:

### Factories — `AuthError.kt`

| Factory | Message | Interpolates | Line |
|---|---|---|---|
| `providerNotFound` | `Provider '$provider' not found` | **caller-controlled** — `provider` comes off the request body | `AuthError.kt:6-7` |
| `providerDoesNotSupportAction` | `Provider '$provider' does not support action '$action'` | **caller-controlled** provider + internal capability name | `AuthError.kt:9-10` |
| `userNotFound` | `User '$user' not found` | **internal** — a user id | `AuthError.kt:12-13` |
| `notSupported` | `Not supported` | none | `AuthError.kt:15-16` |
| `invalidCredentials` | `Invalid credentials` | none | `AuthError.kt:18-19` |
| `noOrganisationAccess` | `No organisation access` | none | `AuthError.kt:22-23` |
| `invalidRequest` | `Invalid request` | none | `AuthError.kt:25-26` |
| `weakPassword` | `Weak password` | none | `AuthError.kt:28-29` |

### Call sites

Reachability column: which of the nine `AuthLoginApi` catch sites can surface it. "public" means at least
one reaching route is `public()`.

| Site | Message produced | Data in message | Reachable from |
|---|---|---|---|
| `AuthSystem.kt:80` | `Found duplicated authentication realms: $duplicatedRealms` | internal realm ids | startup validation, not a request path — see Open questions |
| `AuthSystem.kt:91` (`getRealm`) | `Realm not found: $realm` | **caller-controlled** — `params.realm` echoed back | **public** — every route calls `getRealm` |
| `AuthProvider.kt:41,50,59,68,77,86` | `Not supported` | none | public (default impls for all six capabilities) |
| `AuthRealm.kt:235` | `Provider '…' does not support action 'SignIn'` | caller-controlled | **public** — `signIn` |
| `AuthRealm.kt:320` | `User not found: $userId` | internal user id (from the caller's own JWT) | `refreshToken`, `authenticated()` |
| `AuthRealm.kt:326,362,367,402,408,412,414` | `No organisation access` | none | **public** — `signIn`, `selectOrg` |
| `AuthRealm.kt:339` | `Token refresh denied` | none | `refreshToken`, `authenticated()` |
| `AuthRealm.kt:447` (`getProvider`) | `Provider '$id' not found` | **caller-controlled** | **public** — all provider-dispatching routes |
| `AuthRealm.kt:455` | `Provider '…' does not support action '…'` | caller-controlled | **public** |
| `EmailAndPasswordAuth.kt:211` | `Invalid request` | none | **public** — `signIn` |
| `EmailAndPasswordAuth.kt:214,217,220,223` | `Invalid credentials` | none | **public** — `signIn` |
| `EmailAndPasswordAuth.kt:236,241` | `Invalid request` | none | **public** — `signUp` |
| `EmailAndPasswordAuth.kt:243` | `Weak password` | none | **public** — `signUp` |
| `EmailAndPasswordAuth.kt:245` | **`User already exists`** | none, but the *fact* is the leak | **public** — `signUp` |
| `EmailAndPasswordAuth.kt:253` | **`User already exists`** (concurrent-race branch) | none | **public** — `signUp` |
| `EmailAndPasswordAuth.kt:275` | `Weak password` | none | `setPassword`, `authenticated()` |
| `EmailAndPasswordAuth.kt:278` | **`User '$userId' not found`** | internal user id | `setPassword`, `authenticated()` |
| `EmailAndPasswordAuth.kt:282` | `Invalid credentials` | none | `setPassword` |
| `GoogleSsoAuth.kt:151` | `Invalid request` | none | **public** |
| `GoogleSsoAuth.kt:154,156,162,176,178,182` | `Invalid credentials` | none | **public** |
| `GoogleSsoAuth.kt:173` | `Invalid sign-up request for Google` | none | **public** — `signUp` |
| `GithubSsoAuth.kt:166,169,172,175,178,192,195,198` | `Invalid credentials` | none | **public** |
| `GithubSsoAuth.kt:189` | `Invalid sign-up request for GitHub` | none | **public** — `signUp` |

Three classes of leak, in descending severity:

1. **Account enumeration** — `EmailAndPasswordAuth.kt:245,253` (`"User already exists"`), reachable
   anonymously on `signUp`.
2. **Caller input echoed back** — `AuthSystem.kt:91` and `AuthRealm.kt:447,455` interpolate the
   `realm` / `provider` strings the caller supplied into a response body. Reflection of unvalidated
   input, and it doubles as a discovery oracle for valid realm and provider ids.
3. **Internal identifier disclosure** — `AuthRealm.kt:320` and `EmailAndPasswordAuth.kt:278` put a
   user id in the body. Both sit behind `authenticated()` and both echo the caller's *own* id, so
   the practical severity is low; they still violate the "`message` is never rendered" rule.

## 2. Account-enumeration analysis

### Sign-up — confirmed oracle

`EmailAndPasswordAuth.signUp` (`EmailAndPasswordAuth.kt:231-264`) runs in this order:

1. request type check → `invalidRequest` (`:236`)
2. email syntax check → `invalidRequest` (`:241`)
3. password policy → `weakPassword` (`:243`)
4. **`realm.loadUserByEmail(...) != null` → `AuthError("User already exists")` (`:245`)**
5. `createUserForSignup`, with the duplicate-key catch also throwing `"User already exists"` (`:249-254`)

At the API layer (`AuthApi.kt:126-127`) both the exists and the not-exists failure produce
`ApiResponse.badRequest(AuthSignUpResponse.failed)`, so **status and body already match** — the
message is the only differentiator on the success/failure boundary. But the exists case is compared
against a *successful* sign-up, and those differ completely (200 + `AuthSignUpResponse` carrying a
sign-in). See the sign-up design problem in §4.

### Sign-in — message is already uniform

`EmailAndPasswordAuth.signIn` (`EmailAndPasswordAuth.kt:208-226`) throws `invalidCredentials` for
all four of: bad email syntax (`:214`), blank password (`:217`), **user not found (`:220`)**, and
wrong password (`:223`). Message-wise there is no oracle here. This is correct and should be
preserved as-is.

Caveat resolved: `signIn` normalises with `email.trim().lowercase()` (`:219`) and `signUp` looks up
with `createParams.email`, which `AuthRealm.CreateUserForSignupParams.of` normalises the same way
(`AuthRealm.kt:148-151`). No case-variation bypass.

### Other enumeration surfaces checked

- `recoverAccountInitPasswordReset` (`EmailAndPasswordAuth.kt:303-339`) **returns the same
  `AuthRecoverAccountResponse.InitPasswordReset` for an unknown email** (`:307-308`) as for a known
  one (`:338`). Message-wise correct. It is, however, a strong *timing* oracle — see §3.
- `recoverAccountValidatePasswordResetToken` (`:344-354`) returns `success = tokenRecord != null`.
  That is a token-validity oracle by design, not an account oracle.
- `noOrganisationAccess` on `signIn` (`AuthRealm.kt:362`) is thrown only **after** credentials
  verify, so it is not an unauthenticated oracle. It does tell an authenticated caller that the
  account exists but has no org — acceptable.

## 3. Timing oracles

The configured default hasher is `CompoundPasswordHasher.default`
(`ultra/security/src/jvmMain/kotlin/index_jvm.kt:23`), whose primary is
`Argon2PasswordHasher.id_m65536i2p4o32` (`CompoundPasswordHasher.kt:18-26`) — 64 MiB, 2 iterations,
4 lanes. That is a deliberately expensive, and therefore highly measurable, unit of work.

### Sign-in: confirmed timing oracle

`EmailAndPasswordAuth.kt:219-220` — on unknown email, `signIn` throws immediately. It never reaches
`validateCurrentPassword` (`:222`), which is the only path to `services.checkPassword` (`:395`) and
hence to Argon2. So:

- unknown email → one repository lookup, no KDF
- known email + wrong password → repository lookup + `findLatestPasswordRecord` (`:391-393`) + a full
  Argon2 verification

`letTheBotsWait()` (`AuthApi.kt:254-256`) adds `delay(Random.nextLong(250, 500))` before the try
block. This does **not** close the channel: it adds uniform noise of fixed width to both branches
while the KDF adds a consistent positive offset to only one, so the branch means separate cleanly
under averaging over a modest number of samples. Random jitter raises the sample count an attacker
needs; it does not remove the signal.

**Fix:** in the not-found branch, verify the submitted password against a fixed dummy hash produced
by the same primary hasher, then throw `invalidCredentials`. The dummy hash must be generated with
the *configured* hasher (not a hardcoded constant), or the `hash.id` mismatch guard at
`CompoundPasswordHasher.kt:52` / `BcryptPasswordHasher.kt:35-37` will return `false` early and skip
the KDF entirely — reintroducing the very oracle the fix is meant to close. This is the trap worth
calling out explicitly in review.

### Sign-up: timing oracle in the opposite direction

`signUp` throws at `:245` before `createUserForSignup` (`:250`) and before `createPasswordRecord`,
which is the only caller of `services.hashPassword` (`:405`). So an **existing** email returns
*faster* than a fresh one — the reverse polarity of sign-in, but the same class of leak. Equalising
sign-up timing needs a dummy hash on the already-exists branch too.

### Password recovery: timing oracle

`recoverAccountInitPasswordReset` returns early at `:307-308` for an unknown email, skipping token
generation (`:311`), a record write (`:314-322`) and an **email send** (`:324-330`). The email send
is a network round-trip — the largest timing difference of the three flows, and it makes the
constant-response guarantee at `:308` largely cosmetic. Equalising this properly means moving the
send off the request path (queue it), not padding the response.

Whether the third flow is in scope for this task or split out is a decision for implementation; it
is the same bug class and the same file, so folding it in is reasonable.

## 4. Design

### `AuthError` gets a closed code set and implements `HasClientMessage`

```kotlin
package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.core.exceptions.HasClientMessage

open class AuthError(
    val code: Code,
    message: String,
    cause: Throwable? = null,
) : Throwable(message = message, cause = cause), HasClientMessage {

    /** Closed set of client-safe outcomes. No internals, no input echo. */
    enum class Code(val clientMessage: String) {
        INVALID_REQUEST("Invalid request"),
        INVALID_CREDENTIALS("Invalid credentials"),
        WEAK_PASSWORD("Weak password"),
        NO_ORGANISATION_ACCESS("No organisation access"),
        NOT_SUPPORTED("Not supported"),
    }

    override val clientMessage: String get() = code.clientMessage

    companion object {
        fun providerNotFound(provider: String, cause: Throwable? = null) =
            AuthError(Code.INVALID_REQUEST, "Provider '$provider' not found", cause)

        fun providerDoesNotSupportAction(provider: String, action: String, cause: Throwable? = null) =
            AuthError(Code.NOT_SUPPORTED, "Provider '$provider' does not support action '$action'", cause)

        fun userNotFound(user: String, cause: Throwable? = null) =
            AuthError(Code.INVALID_CREDENTIALS, "User '$user' not found", cause)

        fun notSupported(cause: Throwable? = null) =
            AuthError(Code.NOT_SUPPORTED, "Not supported", cause)

        fun invalidCredentials(cause: Throwable? = null) =
            AuthError(Code.INVALID_CREDENTIALS, "Invalid credentials", cause)

        fun noOrganisationAccess(cause: Throwable? = null) =
            AuthError(Code.NO_ORGANISATION_ACCESS, "No organisation access", cause)

        fun invalidRequest(cause: Throwable? = null) =
            AuthError(Code.INVALID_REQUEST, "Invalid request", cause)

        fun weakPassword(cause: Throwable? = null) =
            AuthError(Code.WEAK_PASSWORD, "Weak password", cause)

        fun userAlreadyExists(cause: Throwable? = null) =
            AuthError(Code.INVALID_REQUEST, "User already exists", cause)
    }
}
```

`message` keeps the diagnostic detail — ids, provider names, realm names — for the log. The
constructor is deliberately code-first so the six ad-hoc `AuthError("literal")` sites
(`AuthSystem.kt:80,91`, `AuthRealm.kt:320,339`, `GoogleSsoAuth.kt:173`, `GithubSsoAuth.kt:189`, plus
`EmailAndPasswordAuth.kt:245,253`) cannot compile without picking a code. Making the old
`(message, cause)` constructor unavailable is the point; a deprecated overload would let sites drift
back.

Note `userAlreadyExists` maps to `INVALID_REQUEST`, not to a code of its own — an
`ACCOUNT_EXISTS` code would just relocate the oracle from the message into the code field.

### Call-site → code mapping

| Sites | Code |
|---|---|
| `AuthSystem.kt:91`, `AuthRealm.kt:447` (realm/provider not found) | `INVALID_REQUEST` |
| `AuthRealm.kt:235,455`, `AuthProvider.kt:41,50,59,68,77,86` | `NOT_SUPPORTED` |
| `AuthRealm.kt:320,339` | `INVALID_CREDENTIALS` |
| `AuthRealm.kt:326,362,367,402,408,412,414` | `NO_ORGANISATION_ACCESS` |
| `EmailAndPasswordAuth.kt:211,236,241` | `INVALID_REQUEST` |
| `EmailAndPasswordAuth.kt:214,217,220,223,282` | `INVALID_CREDENTIALS` |
| `EmailAndPasswordAuth.kt:243,275` | `WEAK_PASSWORD` |
| `EmailAndPasswordAuth.kt:245,253` | `INVALID_REQUEST` (via `userAlreadyExists`) |
| `EmailAndPasswordAuth.kt:278` | `INVALID_CREDENTIALS` |
| `GoogleSsoAuth.kt:151,173`, `GithubSsoAuth.kt:189` | `INVALID_REQUEST` |
| `GoogleSsoAuth.kt:154,156,162,176,178,182`, `GithubSsoAuth.kt:166,169,172,175,178,192,195,198` | `INVALID_CREDENTIALS` |
| `AuthSystem.kt:80` (startup) | `INVALID_REQUEST` — never rendered, see Open questions |

### API layer

The nine catch blocks render `e.clientMessage`, never `e.message`, and log the full detail with the
correlation id from the umbrella task:

```kotlin
} catch (e: AuthError) {
    log.warning("Sign-in failed [$correlationId]", e)
    ApiResponse.forbidden<AuthSignInResponse>().withInfo(e.clientMessage)
}
```

Once the umbrella's default-deny boundary lands, these blocks can go away entirely — an uncaught
`AuthError` would render `clientMessage` automatically. Keeping the explicit catches until then is
fine; they must not be the long-term mechanism. **Sequencing note:** this task can ship before the
umbrella (the codes and the timing fix stand alone) but the `HasClientMessage` import needs the
interface to exist first, so land the umbrella's interface definition ahead of this.

### Making sign-up responses identical — the unresolved trade-off

The spec item "sign-up returns an identical response whether or not the account exists" **cannot be
satisfied by the error-message fix alone**, and this is the one genuinely open design decision here.

`AuthRealm.signUp` (`AuthRealm.kt:249-271`) auto-signs-in the new user (`:260-265`) and returns
`AuthSignUpResponse(signIn = signInResponse, requiresActivation = …)`. A successful sign-up therefore
returns 200 with a token; an already-exists sign-up returns 400. Identical responses require
changing what *success* looks like, not what failure looks like.

Two options:

- **A — neutral sign-up response.** Sign-up always returns the same 200
  `AuthSignUpResponse(signIn = null, requiresActivation = true)`. A fresh email gets an activation
  mail; an existing email gets a "someone tried to sign up with your address" mail. This is the
  standard resolution and it fits the existing intent: `EmailAndPasswordAuth.kt:262` already sets
  `requiresActivation = true`. **Cost:** drops auto-sign-in, a breaking client-visible change, and it
  depends on activation actually working — which today it does not (`AuthSystem.activate` is a stub
  returning `success = false`, `AuthSystem.kt:100-104`, with `// TODO: send account activation email`
  at `AuthRealm.kt:255`). Option A is blocked on implementing activation.
- **B — equalise failure responses only.** Same code, body and timing across all sign-up *failure*
  modes; accept that success and already-exists still differ. Reduces the oracle to "a request that
  fails" vs "a request that succeeds", which an attacker can still read.

**Recommendation: B now, A once activation is implemented.** B is shippable in this task; A is the
correct end state and should be tracked as a follow-up rather than silently dropped. Recording this
explicitly matters — the spec checkbox as originally written implies A is achievable now, and it is
not.

## Spec

- [ ] `AuthError` carries a closed `Code` set and implements `HasClientMessage`; the constructor
      forces every site to pick a code
- [ ] All 14 construction sites in the §1 table are mapped per the §4 table
- [ ] The nine `AuthLoginApi` catch sites render `clientMessage`, never `message`
- [ ] No caller-supplied `realm` / `provider` string is echoed into any response body
- [ ] Sign-in: unknown-user path performs a dummy verification with the **configured** hasher
      before throwing `invalidCredentials`
- [ ] Sign-up: already-exists path performs comparable work; all sign-up failure modes return an
      identical status, body and approximate timing (option B)
- [ ] Decision on option A recorded; follow-up task opened if deferred
- [ ] Full detail still logged server-side with a correlation id
- [ ] `recoverAccountInitPasswordReset` timing addressed or explicitly split into its own task

## Implementation notes

- `letTheBotsWait()` (`AuthApi.kt:254-256`) is jitter, not a timing defence. Keep it, do not count
  on it, and do not let it be the reason a dummy verification is skipped.
- The dummy hash must be derived from the injected `PasswordHasher`, not a literal — see the
  `hash.id` guard at `CompoundPasswordHasher.kt:52`.
- `EmailAndPasswordAuth.kt:247-248` already documents that the exists check is best-effort and needs
  a unique index for TOCTOU safety. Unchanged by this task; both race branches map to the same code.
- Timing tests are inherently flaky on shared CI. Prefer asserting *call counts* on a
  `PasswordHasher` test double (both branches invoke `check` exactly once) over wall-clock
  measurement. Keep a wall-clock test if desired, but do not gate CI on it.

## 5. Tests asserting exact `AuthError` message text

All must move from message assertions to code assertions (`error.code shouldBe
AuthError.Code.X`). 29 assertions across three specs.

| Spec | Lines | Current assertion | Becomes |
|---|---|---|---|
| `funktor/auth/src/jvmTest/kotlin/provider/EmailAndPasswordAuthSpec.kt` | 117, 296, 320, 344 | `"Invalid request"` | `INVALID_REQUEST` |
| | 138, 159, 185, 229, 567 | `"Invalid credentials"` | `INVALID_CREDENTIALS` |
| | 370, 491 | `"Weak password"` | `WEAK_PASSWORD` |
| | **400** | **`"User already exists"`** | `INVALID_REQUEST` — plus a new assertion that the message is *not* in the response body |
| | **521** | **`"User 'user-id' not found"`** | `INVALID_CREDENTIALS`; the id must not appear in any rendered output |
| `funktor/auth/src/jvmTest/kotlin/provider/GoogleSsoAuthSpec.kt` | 119 | `"Invalid request"` | `INVALID_REQUEST` |
| | 143, 182, 263, 287, 317 | `"Invalid credentials"` | `INVALID_CREDENTIALS` |
| | 239 | `"Invalid sign-up request for Google"` | `INVALID_REQUEST` |
| `funktor/auth/src/jvmTest/kotlin/provider/GithubSsoAuthSpec.kt` | 145, 175, 208, 240, 280, 371, 403, 435 | `"Invalid credentials"` | `INVALID_CREDENTIALS` |
| | 341 | `"Invalid sign-up request for GitHub"` | `INVALID_REQUEST` |

`AuthRealmSpec.kt` contains no `AuthError` assertions (grepped — no matches), so it needs no changes
for the message rename. There is currently **no API-level auth spec at all**: `funktor/auth/src/jvmTest`
holds nine files, none exercising `AuthLoginApi` through HTTP (no `AppSpec`, `AppUnderTest`, `withInfo` or
`MatrixTest2d` reference anywhere under that directory). The response-body assertions below are all
new tests in a new spec, not edits to existing ones. That is the larger share of the test work.

## 6. Rate limiting — absent

A case-insensitive grep for `ratelimit|rate_limit|rate limit|throttl|bucket4j` across `funktor/` and
`ultra/` (excluding `build/`) returned **zero matches**. There is no rate-limiting mechanism in the
repo, so these public auth routes have no request-rate control beyond `letTheBotsWait()`'s 250–500 ms
delay, which caps a single connection's throughput but does nothing against parallel requests.

This materially raises the severity of every oracle above — enumeration and timing attacks both need
volume, and nothing limits volume. It is nonetheless a **separate gap**, deliberately out of scope
here: it belongs at the routing layer, not in `AuthError`. Worth its own task.

## Test evidence

- [ ] Unit: every `AuthError.Code` maps to its expected `clientMessage`; the three provider specs
      pass with code assertions
- [ ] Unit: sign-in against a `PasswordHasher` double — unknown user and wrong password each invoke
      `check` exactly once
- [ ] Unit: sign-up — already-exists and fresh-email paths perform the same number of hash operations
- [ ] API: sign-in for unknown user vs wrong password → byte-identical response (status, body,
      message list)
- [ ] API: sign-up failure modes → identical status and body; option-A gap documented if deferred
- [ ] API: no response body from any auth route contains a user id, an email, a realm id, a provider
      id, or the literal `"User already exists"`
- [ ] API: a caller-supplied `realm` / `provider` string is never reflected in the body
- [ ] End-to-end via `AppSpec`/`AppUnderTest` against **both DB backends via `MatrixTest2d`**
      (`funktor/testing/src/jvmMain/kotlin/MatrixTest2d.kt:118`) — auth touches storage, and the
      existing storage specs are already split Karango/Monko
      (`AuthRecordStorageKarangoSpec.kt` / `AuthRecordStorageMonkoSpec.kt`)
- [ ] Full test command(s) run + green: `./gradlew :funktor:auth:jvmTest`

## Open questions

Not verified — do not treat as established.

- **Does `withInfo` actually reach the client body?** Inherited from the umbrella task's channel-A
  analysis; `ApiResponse.messages` serialisation was not independently read here.
- ~~**Email normalisation mismatch.**~~ **RESOLVED — no bug.** Verified by the coordinator:
  `AuthRealm.CreateUserForSignupParams.of` normalises with `email.trim().lowercase()`
  (`funktor/auth/src/jvmMain/kotlin/AuthRealm.kt:148-151`), identical to `signIn`
  (`EmailAndPasswordAuth.kt:219`). The exists check at `:245` therefore cannot be bypassed with case
  variation. Recorded so nobody re-investigates.
- **`AuthSystem.kt:80` reachability.** Classified as startup-time realm validation from its context,
  but the calling path was not traced. If it can fire during a request it leaks realm ids.
- **`HasClientMessage` package and module.** The umbrella task defines the interface but not where it
  lives. The import in §4 is a placeholder; `funktor/auth` must be able to depend on it.
- **Correlation id.** The umbrella task flags this as unresolved (`OPEN QUESTION`, its "Correlation
  id — reuse, don't invent" section). This task assumes one exists but does not settle it.
- **Downstream `AuthError` subclasses.** `AuthError` is `open` (`AuthError.kt:3`). No subclasses exist
  in this repo, but downstream apps may have some; the constructor change is source-breaking for them
  and needs a migration note.
- **`AuthApi.getRealm` (`AuthApi.kt:23-38`)** is `public()` and returns `okOrNotFound` — a realm
  existence oracle by construction. Presumably intentional (the frontend needs it pre-auth), but not
  confirmed.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/error-disclosure/20260720-redteam-error-disclosure.md`
