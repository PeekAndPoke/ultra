## Funktor — Modular Server Framework

### Overview {#funktor-overview}

# Funktor — Modular Server Framework on Ktor

A modular server framework built on Ktor for Kotlin/JVM. Provides auth, REST APIs, clustering, messaging, and
observability as composable modules wired through Kontainer (DI).

## REST Module

Type-safe API routes with compile-time parameter, body, and response types.

### Typed API endpoints

Define endpoints in shared code (Kotlin Multiplatform `commonMain`), mount on the server with handlers:

```kotlin
// Shared endpoint definition
companion object {
    val GetUsers = TypedApiEndpoint.Get(
        uri = "/api/users",
        response = UserModel.serializer().apiList(),
    )
}

// Server-side handler
val getUsers = GetUsers.mount {
    docs { name = "List users" }
    .codeGen { funcName = "getUsers" }
    .authorize { authenticated() }
    .handle { ApiResponse.ok(userService.listAll()) }
}
```

`TypedApiEndpoint` subclasses: `Get`, `Post`, `Put`, `Delete`, `Head`, `Sse`. Each carries an `httpMethod` property
and a `uri` pattern.

### Authentication pipeline

Three Ktor auth providers chain into a sealed `Caller` principal. Install them on
`AuthenticationConfig` in the same order you list them in `authenticate(...)`:

```kotlin
authentication {
    jwtCaller(AUTH_JWT, realm = "MyApp")             // Caller.JwtCaller
    apiKeyCaller(AUTH_API_KEY, realm = "MyApp") {    // Caller.ApiKeyCaller
        token -> kontainer.get<ApiKeyService>().tryResolve(token)
    }
    anonymous(AUTH_ANON)                             // Caller.AnonymousCaller (terminal)
}

routing {
    authenticate(AUTH_JWT, AUTH_API_KEY, AUTH_ANON) { /* routes */ }
}
```

Under Ktor's default `FirstSuccessful` strategy the first provider to return a principal wins.
`anonymous()` always returns one, so it MUST be listed last. Invalid credentials (expired JWT,
revoked key) silently fall through to `Caller.AnonymousCaller` rather than producing a 401 —
routes that require real auth check `User.isAnonymous()`.

`currentUserProvider()` (extension on `ApplicationCall`) dispatches the `Caller` into a sealed
`UserRecord`:

```kotlin
when (record) {
    is UserRecord.Anonymous -> // no credentials
    is UserRecord.System    -> // internal system actor
    is UserRecord.LoggedIn  -> // JWT-authenticated end-user
    is UserRecord.ApiKey    -> // API-key authenticated; carries keyId + keyName for audit
}

installKontainer { call ->
    app.kontainers.create { with { call.currentUserProvider() } }
}
```

Default `jwtCaller(name, realm)` uses the kontainer-bound `JwtGenerator`. For extra checks
(revocation list, audience switching) pass a custom `validate` lambda — the lambda has
`ApplicationCall` as receiver so the per-call kontainer is in scope.

**API-key resolver responsibilities (security-critical):** hashed storage (Argon2/HMAC, never
plaintext), constant-time comparison via `MessageDigest.isEqual`, fast-reject by prefix before
any DB lookup, never log the raw token. `Caller.ApiKeyCaller.permissions` defaults to
`UserPermissions.anonymous` — derive privileges from your authoritative store, not from caller
input.

### The authorization floor

Every `ApiRoutes` group MUST declare an `authFloor` — the parameter has no default, so a group cannot
be written without deciding who may reach it. The floor is prepended to every route in the group and
is append-only: a route may add restrictions, never remove them. Floors are materialized and validated
at group construction, so a malformed one aborts app start.

```kotlin
class OrgsApi : ApiRoutes("orgs", authFloor = { isSuperUser() }) {
    val list = ApiRoute.Get("", ...)
}
```

### Authorization DSL

Rules are STATEMENTS, not values. Each call appends to the chain and the chain is an AND. There is
**no `and`/`or` infix** — nest a block combinator for alternatives.

```kotlin
.authorize {
    // chain is an AND — both must hold
    authenticated()
    forPermission("orders.read")

    // alternatives: forAny {} is OR, forAll {} is AND
    forAny {
        forRole("admin")
        forGroup("staff")
    }

    forCall("owns resource") { params -> user.id == params.ownerId }
}
```

| Rule | Passes for |
|---|---|
| `authenticated()` | any non-anonymous caller |
| `isSuperUser()` | super users |
| `forUserType(t)` | callers of that user type |
| `forRole(r)` / `forAnyRole(…)` | role holders |
| `forGroup(g)` / `forAnyGroup(…)` | group members |
| `forPermission(p)` / `forAnyPermission(…)` | permission holders |
| `forCall(desc) { }` | a custom predicate over params/body |
| `public()` / `forbidden()` | everyone / nobody — SOLE rule only |

**`public()` and `forbidden()` must be the SOLE rule of a chain.** Combined with (or nested inside)
anything else they silently become a no-op or an always-allow, so the builder throws at BOOT. To serve
both a public and a protected audience, split into separate `ApiRoutes` groups each with its own floor.
An empty `forAll {}`/`forAny {}` is rejected too — an empty AND allows everyone, an empty OR denies
everyone.

The statements-only shape is deliberate: an expression DSL let the last expression silently win, which
reads as a tightening and evaluates as a replacement.

Rules also support **access estimation** — `ApiRoute.estimateAccess(user)` evaluates them without making
a request, powering the API access matrix and admin dashboards.

### API features

Group related routes into `ApiFeature` implementations. Registered in Kontainer, auto-discovered and mounted.

## Auth Module

Multi-realm authentication with pluggable providers, JWT tokens, and frontend session management.

### Realms

An `AuthRealm<USER>` represents a user population with its own providers, password policy, and user model.
An app can have multiple realms (admin users, customers, etc.).

Key methods: `generateJwt()`, `signIn()`, `signUp()`, `activate()`, `refreshToken()`. User operations live on
`AuthRealm.users: AuthUserAdapter<USER>` — `loadById(UserId)`, `loadByEmail(EmailAddress)`, `createForSignup`,
`activate(user, ActivateUserParams)` (writes the name chosen at activation), `retire(user, AccountRetirement)`
(see Retirement) and `serialize`. `activate` and `retire` have NO default; a realm without sign-up throws
`AuthError.notSupported()` from both. Ids and emails are `@JvmInline value class`es — `UserId`, `OrgId`, `RealmId`,
`EmailAddress` — so an `EmailAddress` lookup is case-insensitive by type.

### Providers

- `EmailAndPasswordAuth` — sign up, sign in, password recovery, password change
- `GoogleSsoAuth` — Google OAuth flow
- `GithubSsoAuth` — GitHub OAuth flow; the identity is the public `/user` address, which GitHub must list as
  verified in `/user/emails` (the `user:email` scope, or a GitHub App's "Email addresses" permission)

### Sign-up and activation

Email first: a new account proves its address before it holds anything. Sign-up takes the address ONLY (no
password, no name), mails a link and issues NO session. The link opens "Choose your password", which asks for the
password and the display name; submitting it activates the account and signs the user in.

```kotlin
// Sign-up: the address, nothing else (`api` is the realm's AuthApiClient)
api.signUp(AuthSignUpRequest.EmailAndPassword(provider = "email-password", email = email))

// The mailed link's page: the first password and the name activate the account
val response = authState.activateAccount(
    AuthActivateAccountRequest(provider, token, password = password, displayName = name),
)

val problem = when {
    response == null -> "The server could not be reached."
    response.success -> null // signed in: response.signIn
    response.failure == Failure.WeakPassword -> "Please choose a stronger password."
    response.failure == Failure.InvalidDisplayName -> AuthDisplayName.refusalOf(name)?.message
    else -> "This link is used, unknown or expired."
}
```

- `AuthActivateAccountResponse(success, signIn, failure)`. Password policy and name are checked BEFORE the token:
  a refused input is a 200, `success = false`, `failure` = `WeakPassword` | `InvalidDisplayName`, link still
  usable. A dead link: `success = false`, no reason. `signIn` is `Success`, `OrgSelectionRequired`, or null (no
  org yet; provider without sign-in).
- Org realm: `AuthState` hands an `OrgSelectionRequired` from activation to the login page, which opens on the
  picker ONCE (`AuthState.takeActivationOrgSelection()`).

The life of a pending account:

| When | What happens |
|---|---|
| link opened | nothing; only the page's POST activates (mail scanners follow links) |
| address signs up again | re-sent once the account's next delay passed; answered exactly like a fresh sign-up; older links live `activationLinkOverlap` more, then die |
| "Didn't sign up? Cancel" | every mail has its OWN cancel link, valid as long as the account; retires it. The activation page's cancel button does the same with the activation token |
| password reset completes | activates too (proves the mailbox) |
| SSO with the address | provider-verified (Google `email_verified`, GitHub's verified public address): activates and deletes any earlier password; else `invalidCredentials`. No password until a reset |
| sign-in attempt | plain "invalid credentials", no hint to activate |
| `pendingAccountLifetime` passed | retired at next use (sign-up, activation, reset, SSO) or by `funktor/all`'s `PendingAccountsExpiryWorker` (every 15 min, registered by the `Funktor` module); address free again |

- Activation, cancel, retire, SSO and an activating reset remove ALL the sign-up's links.
- Activated by reset or SSO: never asked for a name, keeps the default (the address's local part).
- A pending address and a new one answer alike; an ACTIVE address answers "User already exists" (400).
- "Not activated" is the PRESENCE of a marker record — not a boolean on the user (the only safe default would
  lock out every existing account) and not "an unexpired token exists" (that test erases itself on expiry).
- SSO reaches an account (active, pending or new) only through an address the provider verified.

`RealmTokenConfig` (per realm):

| Setting | Default | Meaning |
|---|---|---|
| `pendingAccountLifetime` | 7 days | life of a never-activated account; no link outlives it |
| `emailVerificationTokenLifetime` | 24 h | life of an activation link — a credential (sets the password, signs in) |
| `activationResendDelays` | 5m, 15m, 1h, 4h, 12h, 24h | wait before each re-send, per account; last entry repeats; not empty |
| `activationLinkOverlap` | 30 min | older links stay usable this long after a re-send; 0 = only the newest |
| `maxSignUpCancelTokens` | 12 | cancel links kept per account, oldest dropped; default delays = sign-up mail + 11 re-sends in 7 days, so every mailed link lives as long as the account; >= 2 |
| `randomTokenByteLength` | 256 | bytes per one-shot token; >= 32 |

#### Retirement

Expiry and cancel RETIRE the account: auth records deleted, then `AuthUserAdapter.retire(user, AccountRetirement)`,
then a check that the address no longer resolves to the account. The row stays (other collections reference it);
its email becomes `retired-<64 hex>@<32 hex>#retired.invalid` (SHA-256 of the address, no plaintext; the `#` makes
it unparseable as a sign-up). `retire` must rename EVERY unique key (`retirement.rename(email)`) and clear fields
derived from the address. `AccountRetirement.retiredKeyPrefix(email)` finds an address's retired rows,
`isRetired(email)` detects them. A `retire()` that throws or renames nothing leaves the account pending, logs an
error; the next sweep retries. `DefaultMessaging` refuses to mail a retired address.

#### Display name (`AuthDisplayName`, common code)

Shared by page and server: 1 to 100 UTF-16 chars after trimming, one line, only characters that show. Refused:
control chars, invisible ones (format controls, tag block, default-ignorable, blank-rendering letters), undisplayable
ones (private use, noncharacters, broken UTF-16), a leading combining mark, > 30 marks in a row. The England,
Scotland and Wales flag emoji are refused (tag characters).

- `refusalOf(raw): Refusal?` — `problem`, `codePoint`, `position`, and `message` for the user, e.g.
  "Your name contains an invisible character (U+200B) at the start. ..."
- `normalizeOrNull(raw): String?` — trimmed, or null when refused. `MAX_LENGTH = 100`, `DESCRIPTION`.
- SSO sign-up (`CreateUserForSignupParams.of(email, displayName)`): the profile name if the rule accepts it,
  else the canonical (lower-case) local part, cut to 100 if needed.

#### One-shot tokens are stored hashed

Activation, cancel, reset, email-change and org-selection tokens are stored as `tokenHash: TokenHash`
(base64url SHA-256), never the raw token, which exists only in the mail or the answer. A read of the auth records
yields no working link. Unsalted and fast on purpose — safe only because tokens are long and random (hence the
32-byte floor). Lookups go through `AuthRecordStorage.findByRawToken`, which hashes inside.

### Organisations (OrgPolicy)

`AuthRealm.orgPolicy` declares the relationship. Default `OrgPolicy.None` (org-less realm).

Under `OrgPolicy.Required`, sign-in resolves accessible orgs and runs the 0/1/n flow: zero refuses,
one auto-selects, many return `OrgSelectionRequired` backed by a single-use selection token. Token
refresh keeps the SAME org and never silently changes it.

```kotlin
class B2bRealm(...) : AuthRealm<B2bUser> {
    override val orgPolicy = OrgPolicy.Required()

    override suspend fun getMemberships(user: Stored<B2bUser>): Set<OrgMembership> =
        orgMembers.sessionMembershipsOf(UserId(user._id))

    // How "accessible" is defined is the APP's call — the framework only asks.
    override suspend fun getAccessibleOrgs(memberships: Set<OrgMembership>): List<AuthOrgRef> =
        orgs.accessibleActiveOrgs(memberships)
}
```

`sessionMembershipsOf` takes a `UserId`; `Stored<T>._id` is a `String`, so it must be wrapped.
`accessibleActiveOrgs` is an APP-level helper (see the demo's `saas_org_hooks.kt`), not framework API —
the framework defines the hook, the app decides what "accessible" means.

### Typed identifiers

`RealmId`, `UserId`, `OrgId` and `EmailAddress` are value classes threaded end to end — JWT claims,
URI params, storage, API surface. `EmailAddress` canonicalizes on construction (trim + lower-case),
making lookups case-insensitive by type rather than by convention at each call site.

### Localized auth emails

Locale precedence: the user's `language.messaging` -> the realm's `defaultLanguage` -> the framework
fallback. A CHAIN, not an either/or — otherwise a German-only deployment mails English to a user
whose stored language it has no rendering for.

```kotlin
class MyRealm(...) : AuthRealm<MyUser> {
    override val defaultLanguage = Locale("de")

    override val messaging = AuthRealm.DefaultMessaging(
        senderEmail = "noreply@example.com",
        senderName = "Example",
        applicationName = "Example",
        realm = this,
        templates = AuthEmailTemplates.default(myBrandedLayout),
    )
}
```

See the Messaging module for the template and layout surface.

### Auth API endpoints

All endpoints use the `{realm}` path parameter:

| Method | URI                                                 | Auth          | Description                                   |
|--------|-----------------------------------------------------|---------------|-----------------------------------------------|
| GET    | `/auth/{realm}/realm`                               | public        | Get realm config (providers, password policy) |
| POST   | `/auth/{realm}/signin`                              | public        | Sign in                                       |
| POST   | `/auth/{realm}/select-org`                          | public        | Pick the org after `OrgSelectionRequired`     |
| POST   | `/auth/{realm}/signup`                              | public        | Sign up (address only)                        |
| POST   | `/auth/{realm}/activate`                            | public        | Choose password and name, activate, sign in   |
| POST   | `/auth/{realm}/activate/cancel`                     | public        | "Didn't sign up?" — retire a pending account  |
| PUT    | `/auth/{realm}/update`                              | authenticated | Set password                                  |
| PUT    | `/auth/{realm}/recover/reset-password/init`         | public        | Start password reset                          |
| PUT    | `/auth/{realm}/recover/password-reset/validate`     | public        | Validate reset token                          |
| PUT    | `/auth/{realm}/recover/password-reset/set-password` | public        | Complete password reset                       |
| GET    | `/auth/{realm}/refresh-token`                       | authenticated | Refresh JWT token                             |
| GET    | `/auth/my-api-access`                               | authenticated | Get user's API access matrix                  |

### Token refresh

`GET /auth/{realm}/refresh-token` issues a fresh JWT for authenticated users. The server re-loads the user and
regenerates the token, so permission changes are picked up automatically. Cross-realm validation prevents
privilege escalation.

### API access matrix (ApiAcl)

`GET /auth/my-api-access` returns a flat list of API endpoints the current user can access, with their estimated
access level (`Granted` or `Partial`). Denied endpoints are filtered out.

**ADVISORY ONLY** — decides what to RENDER. The server is the authority; this enforces nothing.

```kotlin
// Fetch once after login
val matrix = api.auth.getMyApiAccess().first().data!!
val acl = ApiAcl(matrix)

if (acl.canAccess(MyApiClient.GetEvent)) { /* show it */ }
if (acl.canAccess(MyApiClient.DeleteEvent)) { /* destructive too — Partial IS access */ }
```

Predicates (same names in the generated TypeScript SDK's `runtime/acl.ts`):

| | true when |
|---|---|
| `canAccess` | not `Denied` — the everyday predicate. **Includes `Partial`, and that is access** |
| `canFullyAccess` | `Granted` — narrow; the route works whichever resource it is pointed at |
| `canPartiallyAccess` | `Partial` |
| `isDenied` | the negation of `canAccess` |

`canAccess` ≡ `!isDenied`; the three exact predicates are mutually exclusive and exhaustive.

**`Partial` means "callable, and the server checks the arguments"** — typically "do you own this
resource". The user may use the route and its UI; the per-resource rule is the backend's job. So
`canAccess` is the predicate for visibility, destructive actions included — gating a delete on
`canFullyAccess` hides it from the user on their OWN resource.

**`canFullyAccess` means only that no rule REPORTED an argument check** — not that the route works
for every resource. `forCall` estimates `Granted` unless given an `estimateFn`, so an ownership check
written with it reads as full access; handler checks and `RouteParamsGuard`s such as the saas
`OrgIsolationGuard` are not `AuthRule`s, so `estimateAccess` cannot see them either.

**`Partial` comes only from a rule that estimates it** — `forCall(..., estimateFn = { Partial })` or
an `AccessLevelCheck`. The built-in rules yield `Granted` or `Denied` and `and`/`or` fold with
`maxOf`/`minOf`. Do not gate on `canPartiallyAccess` expecting the branch to run.

Key classes:

- `UserApiAccessMatrix` — serializable model (`List<Entry>` with method, uri, level)
- `ApiAcl` — client-side lookup, keyed on `method|uri` (the uri PATTERN, placeholders included)
- `ApiAcl.empty` — denies everything, **including `public()` routes**. A logged-out visitor gets this,
  so do not gate a sign-in control on it
- `UserApiAccessProvider` — server-side interface, implemented by `ApiAccessDescriptor`

### Frontend session lifecycle

`AuthState<USER>` manages the full auth lifecycle in Kraft SPAs. Enabled by default:

- **Periodic refresh** — checks token expiry every 60s, refreshes 2 min before expiry
- **Focus check** — verifies token when browser tab regains focus
- **Graceful expiry** — saves current page, logs out, redirects to login; after re-login, returns to previous page
- **Page reload** — clears expired tokens from localStorage on app init

```kotlin
// Zero config — auto-refresh enabled by default
val auth = authState<MyUser>(
    frontend = AuthFrontend.default(config = AuthFrontendConfig(redirectAfterLogin = Nav.dashboard())),
    api = Apis.auth,
    router = { kraft.router },
)

// Custom config
val auth = authState<MyUser>(
    // ...
    sessionConfig = AuthSessionConfig(
        checkIntervalMs = 30_000,
        refreshBeforeExpiryMs = 120_000L,
        onTokenRefreshed = { refreshApiAcl() },
        onSessionExpired = { showLoginDialog() },
    ),
)

// Disable
sessionConfig = AuthSessionConfig.disabled
```

Key classes:

- `AuthState<USER>` — reactive stream of auth data (token, user, permissions, expiry)
- `AuthSessionConfig` — controls refresh interval, focus check, expiry behavior
- `AuthFrontend` — mounts login/reset-password routes
- `AuthFrontendConfig` — redirect-after-login, background image
- `AuthApiClient` — typed API client for all auth endpoints

### Route protection

```kotlin
fun RootRouterBuilder.mountNav(authState: AuthState<MyUser>) {
    authState.mount(this)  // login + reset-password routes (public)

    val authMiddleware = authState.routerMiddleWare(Nav.auth.login())

    middleware(authMiddleware) {
        // Protected routes — unauthenticated users redirected to login
        mount(Nav.dashboard) { DashboardPage() }
    }
}
```

The middleware saves the current URI before redirecting. After login, `authState.redirectAfterLogin(defaultUri)`
navigates back to the original page.

### Upgrading to email-first sign-up: what gives NO compile signal

The compiler and `CHANGELOG.md` cover the API. These do not:

App's own pages, mails, routes:
- sign-up form still sending a password or name: ignored by the server;
- activation page must POST `password` AND `displayName` (no name: 400, link stays usable), never on load;
- activation mail template must render `cancelUrl`; its copy should say the link chooses the password;
- own router must mount `AuthFrontendRoutes.cancelSignUp` (`AuthState.mount` does).

App's own providers and hooks:
- SSO provider: route every existing account through `AuthRealm.resolveSsoAccount` (asks `isEmailVerified` for
  ACTIVE accounts too); check verification before `createForSignup`;
- a `displayName` passed to `CreateUserForSignupParams.of` that the rule refuses becomes the local part;
- own `AuthRealm.Messaging` lacks `DefaultMessaging`'s retired-recipient guard;
- `AuthRecordStorage.OnAfterSave` listeners do not see the conditional writes (claims, re-send schedules,
  activations).

Every app:
- links mailed before token hashing stop working; old one-shot rows (`token`, no `tokenHash`) cannot be read
  back — delete them from dev databases. `activation-resend-token` rows are dead weight too;
- accounts pending from before email first still hold their sign-up password: it no longer signs in; the link
  replaces it; SSO deletes it;
- the first sweep retires every pending account older than `pendingAccountLifetime`; retired rows stay in the
  user collection (lists and admin screens show them unless they filter `AccountRetirement.isRetired`);
- without `funktor/all`'s `Funktor` module there is no sweep: expiry happens only at use;
- GitHub needs the `user:email` scope (GitHub App: "Email addresses" permission). `GithubSignInButton` asks for
  it, sends a one-time `state` and returns to the page's route, also under hash routing; an authorize URL the app
  builds itself is no longer acted on;
- a Google account with an unverified address can no longer sign in, also to accounts SSO created earlier;
  sessions taken that way before the upgrade survive — rotate the JWT signing key to end them;
- `RealmTokenConfig` throws at construction for `randomTokenByteLength < 32` or `maxSignUpCancelTokens < 2`.

## Messaging Module

Email sending, dev overrides, storage of sent messages, and the auth email templates.

### The sender chain is composed by the framework

An app supplies only its provider; the framework composes the rest at ONE site. In test mode it
substitutes a capturing sender and never touches the app's provider — so a test can never send real
mail by forgetting to override something.

```kotlin
funktorMessaging {
    useSender(devConfig) { awsSes(sesConfig) }
}
```

Tests read what was "sent" from the capture rather than from storage, because auth mails are
persisted with anonymized links (see below).

### Auth email templates

The three transactional auth mails — account activation, password changed, password recovery — are
rendered IN KOTLIN via the kotlinx.html DSL, one template class each, selected per locale.

```kotlin
abstract class AccountActivationEmailTemplate :
    LocalizedEmailTemplate<AccountActivationEmailTemplate.Params>() {

    data class Params(
        val recipient: EmailAddress,
        val senderEmail: String,
        val senderName: String,
        val applicationName: String,
        val activationUrl: String,  // "Choose your password"; a live single-use token
        val cancelUrl: String,      // "Didn't sign up? Cancel"; its own token per mail
    )
}
```

- Each template declares its own nested `Params` — a renamed or removed value is a COMPILE ERROR,
  not a placeholder shipped to a recipient. A value the renderer IGNORES is not: an activation
  template that never renders `cancelUrl` silently mails no cancel link.
- Renderings live in `Map<Locale, (Params, Locale) -> Email>`; a shared walker resolves
  `de-CH -> de -> en`.
- The renderer is HANDED the locale it matched, so the map key and the rendering cannot drift into a
  mixed-language mail.
- `AuthEmailTemplates` holds the three by NAME (`.accountActivation`, `.passwordChanged`,
  `.passwordRecovery`) — no lookup by string key. It validates at wiring time that each template can
  render its own guaranteed locale.

### Branding via EmailLayout

```kotlin
val branded = EmailLayout { locale, content ->
    EmailBody.Html {
        body {
            div { +"…header…" }
            content()
            div { +footerFor(locale) }
        }
    }
}

AuthEmailTemplates.default(branded)   // all three mails share it
```

The layout takes the LOCALE because a footer carries the imprint and unsubscribe line, which have to
be translated. A layout blind to the locale can only put an English footer under a German body.

Override one mail by subclassing its template and passing it alongside the layout:

```kotlin
AuthEmailTemplates(layout = branded, accountActivation = MyActivationMail(branded))
```

### What a template does NOT control

A template builds the `Email` and nothing downstream of it. The caller applies the storing policy and
re-asserts the envelope, so a template cannot:

- choose whether its own live token is persisted, or
- add a cc/bcc that would deliver an activation link to a second mailbox.

### Sent-message storage and anonymization

`EmailStoring.withAnonymizedContent(refs, tags)` strips links from the PERSISTED copy, so the
sent-messages inspector — which support staff can read — never holds a live activation or reset
token. Both the body and the subject go through the policy. Use `withoutContent` to store metadata
only, `withContent` to store verbatim.

## SaaS Module

Organisations, memberships, and a boot-enforced cross-tenant isolation boundary.

```kotlin
funktor(
    config = config,
    auth = { useKarango() },
    saas = { useKarango() },   // or useMonko()
)
```

Registers the org/membership repositories, the `orgs` API, AND the isolation boot check + request
guard. The marker interfaces below are inert without this registration.

### Storage

```kotlin
orgs.findBySlug("acme")
orgs.ensureBySlug(slug = "acme", name = "Acme Inc")
orgs.create(organisation); orgs.save(organisation)

orgMembers.sessionMembershipsOf(UserId(user._id))   // -> Set<OrgMembership>
```

An `Organisation` has a `slug`, `name` and `OrgStatus`. Only `Active` orgs should be sign-in-able —
see the Auth module for how a realm turns memberships into a session (`OrgPolicy.Required`, 0/1/n).

`Slugs` validates for subdomain use: 2–63 chars; `normalize()` trims and lower-cases;
`validationError(slug)` returns a reason string rather than a bare boolean.

### Org isolation

```kotlin
// 1. mark the entity as org-owned
data class Invoice(override val org: Ref<Organisation>, val total: Money) : OrgAware

// 2. the route params must carry the addressed org
data class InvoiceParams(
    override val org: Stored<Organisation>,
    val invoice: Stored<Invoice>,
) : OrgAwareParam
```

- **At boot:** any route resolving an `OrgAware` entity is FORCED to be `OrgAwareParam`. A route that
  loads org-owned data without knowing which org the request addresses fails app start.
- **At request time:** the guard compares each loaded entity's `org` ref against the request's org and
  refuses a mismatch.

**Contract:** `OrgAware.org` must carry the org's CANONICAL `_id` (collection-qualified, e.g.
`organisation/acme`), because the guard compares on `_id`. A persisted `Ref<Organisation>` satisfies
this automatically; a hand-built one must use `_id`, never a bare `_key`. Getting it wrong fails
CLOSED — every same-org request 404s — so it is an availability landmine, not an IDOR.
