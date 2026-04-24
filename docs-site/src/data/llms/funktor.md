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

### Authorization DSL

Every route declares its authorization rules in the `.authorize { }` block:

```kotlin
.authorize {
    public()              // allow everyone (including anonymous)
    authenticated()       // any logged-in user (non-anonymous)
    isSuperUser()         // super user only
    forRole("admin")      // role-based
    forGroup("staff")     // group-based
    forPermission("edit") // permission-based
    forOrganisation("x")  // org membership
    forbidden()           // deny all

    // Combine rules
    forRole("admin") or forGroup("staff")

    // Custom rule with request context
    forCall("owns resource") { user.id == params.ownerId }
}
```

Rules support **access estimation** — `ApiRoute.estimateAccess(user)` evaluates rules without making an actual
request. This powers the API access matrix and admin dashboards.

### API features

Group related routes into `ApiFeature` implementations. Registered in Kontainer, auto-discovered and mounted.

## Auth Module

Multi-realm authentication with pluggable providers, JWT tokens, and frontend session management.

### Realms

An `AuthRealm<USER>` represents a user population with its own providers, password policy, and user model.
An app can have multiple realms (admin users, customers, etc.).

Key methods: `loadUserById()`, `loadUserByEmail()`, `generateJwt()`, `signIn()`, `signUp()`, `refreshToken()`.

### Providers

- `EmailAndPasswordAuth` — sign up, sign in, password recovery, password change
- `GoogleSsoAuth` — Google OAuth flow
- `GithubSsoAuth` — GitHub OAuth flow

### Auth API endpoints

All endpoints use the `{realm}` path parameter:

| Method | URI                                                 | Auth          | Description                                   |
|--------|-----------------------------------------------------|---------------|-----------------------------------------------|
| GET    | `/auth/{realm}/realm`                               | public        | Get realm config (providers, password policy) |
| POST   | `/auth/{realm}/signin`                              | public        | Sign in                                       |
| POST   | `/auth/{realm}/signup`                              | public        | Sign up                                       |
| POST   | `/auth/{realm}/activate`                            | public        | Activate account                              |
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

```kotlin
// Fetch once after login
val matrix = api.auth.getMyApiAccess().first().data!!
val acl = ApiAcl(matrix)

// Type-safe lookup using endpoint objects
if (acl.hasAccessTo(MyApiClient.CreateEvent)) { /* show button */ }
if (acl.hasAnyAccessTo(MyApiClient.GetEvent)) { /* show with partial access */ }
```

Key classes:

- `UserApiAccessMatrix` — serializable model (`List<Entry>` with method, uri, level)
- `ApiAcl` — client-side lookup. `hasAccessTo()` (Granted only), `hasAnyAccessTo()` (not Denied)
- `ApiAcl.empty` — denies everything (safe default)
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
