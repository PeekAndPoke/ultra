# UserRecord → sealed interface + Caller dispatcher + ApiKey auth

**Status:** SHIPPED 2026-04-25 in v0.107.0.

## Shipped 2026-04-25 — actual outcome (deltas from the original plan)

The plan below is preserved as written. These are the deviations during execution:

- **Back-compat shim removed.** The original plan added a companion `operator fun invoke`
  on `UserRecord` to keep ~34 test sites compiling. We dropped it on user request — there
  are no external consumers that need the soft landing (CTB is in active development and
  will fix immediately; aktor is dead; thebase is legacy). All call sites migrated to
  explicit variant constructors (`UserRecord.LoggedIn(...)`, `UserRecord.Anonymous(...)`).

- **`Caller` does NOT extend Ktor `Principal`.** Ktor 3.x deprecated the `Principal`
  marker interface ("can be safely removed"). `Caller` is a plain sealed interface;
  `principal<Caller>()` still works because Ktor 3.x's `principal<T>()` is a generic
  extension.

- **Auth-config helpers — three of them, lambda-based.** Final shape:
    - `jwtCaller(name, realm) { token -> ... }` — Ktor `bearer { }` provider; `validate`
      lambda has `ApplicationCall` as receiver and defaults to `tryJwtCaller(token)` so
      the simple case is one line.
    - `apiKeyCaller(name, realm) { token -> ... }` — Ktor `bearer { }` provider; `resolve`
      lambda is mandatory (no sensible framework default for key resolution),
      `ApplicationCall` receiver.
    - `anonymous(name)` — custom `AuthenticationProvider` that always succeeds with
      `Caller.AnonymousCaller`. KDoc warns it MUST be listed last in `authenticate(...)`.
    - The original plan had a kontainer-registered `ApiKeyResolver` interface; that was
      dropped — passing the lambda directly is more idiomatic Ktor.

- **`JwtAnonymous` + `JwtNullClaim` deleted.** The new design's terminal `anonymous()`
  provider replaces the old "challenge handler injects JwtAnonymous" pattern. The
  defensive `is JwtAnonymous` check in `currentUserProvider` was also removed — dead code.

- **`JwtGenerator.tryVerify(token): Payload?`** added to ultra/security; catches
  `JWTVerificationException` and returns null. Powers the default `tryJwtCaller`
  convenience in funktor/rest.

- **Demo wiring simplified.** `funktor-demo/server/api/ApiApp.kt` no longer takes
  `auth: JwtGenerator` as a constructor parameter — the resolver lambda fetches it from
  the per-call kontainer. End state:
  ```kotlin
  authentication {
      jwtCaller(AUTH_JWT, realm = "Funktor Demo")
      anonymous(AUTH_ANON)
  }
  ```

- **Tests added in `funktor/all/AuthApiSpec.kt`** for the security-critical fail-soft
  cases the plan didn't enumerate: garbage bearer token, structurally-valid JWT with bad
  signature, empty bearer — all must fall through to `AnonymousCaller` and 401 on
  authenticated routes.

- **Docs landed** in `docs-site/src/pages/ultra/funktor/rest.astro` ("Authentication
  pipeline" section) and the LLM mirror `docs-site/src/data/llms/funktor.md`. Covers the
  full chain, ordering rules, security-critical resolver checklist.

- **Versions bumped** to 0.107.0 across `gradle.properties` (already there),
  `docs-site/src/data/site.ts`, and `README.MD`'s dependency snippet.

- **Pre-existing breakage flagged, not fixed.** `funktor/core/.../lifecycle/lifecycle.kt`
    + `AppLifeCycle.kt` had in-flight changes calling `log.info(...)` inside
      `onAppStarting { }` lambdas where `log` doesn't resolve as a free identifier. Cached
      funktor:core jar masked it during this session's verification. Trivial fix
      (`it.log.info(...)` or rename param to `app`), but it's user's in-flight code so left
      alone.

---

## Context

The funktor framework currently models authenticated identity as a single
`@Serializable data class UserRecord(userId, clientIp, email, desc, type)` and a
single auth path: Ktor `JWTPrincipal` → `JwtGenerator.extractUser` → `User`.
A consumer project added a second auth method (API keys) and built a local
`Caller` sealed hierarchy + `Caller → UserRecord` dispatcher inside the
project to make it work. We want to promote that design into the framework so
every funktor consumer gets API-key auth and a typed identity model out of the
box.

The refactor has three pieces:

1. **`UserRecord` becomes a sealed interface** in ultra (`Anonymous` / `System`
   / `LoggedIn` / `ApiKey`) with `@SerialName` per variant and `email`/`desc`/`type`
   lifted onto the interface (default `null`) so existing field reads keep
   compiling.
2. **A `Caller` sealed interface lives in funktor/rest** (`JwtCaller` /
   `ApiKeyCaller` / `AnonymousCaller`) as the internal dispatch type that
   `currentUserProvider()` produces from the Ktor principal.
3. **funktor ships an API-key Ktor auth provider** symmetric to the existing
   `jwt()` setup, backed by a kontainer-registered `ApiKeyResolver` strategy
   so consumers only have to provide the lookup function.

Serialization compatibility is intentionally broken (the new JSON gains a
`type` discriminator) — flagged for v1 release notes, no custom back-compat
serializer.

## Confirmed design decisions

| Fork                    | Choice                                                                          |
|-------------------------|---------------------------------------------------------------------------------|
| Common fields placement | Lift `email`/`desc`/`type` to the sealed interface with `get() = null` defaults |
| API-key auth provider   | Funktor ships `apiKey(...)` Ktor provider + `ApiKeyResolver` interface          |
| Caller location         | `funktor/rest/src/jvmMain/kotlin/auth/` (JVM only)                              |
| Serialization compat    | Accept the break, document in release notes                                     |

## ApiKey variant — recommended fields

Required to make the variant useful at the framework level without leaking
project-specific concerns:

```kotlin
@SerialName("api-key")
data class ApiKey(
    override val userId: String,
    override val clientIp: String? = null,
    val keyId: String,                 // stable, non-secret identifier of the key
    val keyName: String? = null,       // human-readable label, useful in logs/audit
    override val email: String? = null,
    override val desc: String? = null,
    override val type: String? = null,
) : UserRecord
```

Deliberately **out of scope** at the framework level (project concerns):

- key scopes/permissions (use `UserPermissions` — the existing structure is
  already richer than anything an API key would model)
- `lastUsedAt` / `expiresAt` (lifecycle metadata belongs in the consumer's key
  store, not in the per-request identity record)
- raw key value (must never be in the record)

## Implementation plan

### Phase A — `ultra/security`: sealed `UserRecord`

**File: `ultra/security/src/commonMain/kotlin/user/UserRecord.kt`** — rewrite:

```kotlin
@Serializable
sealed interface UserRecord {
    val userId: String
    val clientIp: String?
    val email: String? get() = null
    val desc: String? get() = null
    val type: String? get() = null

    fun isAnonymous(): Boolean = userId == ANONYMOUS_ID
    fun isSystem(): Boolean = userId == SYSTEM_ID

    @Serializable
    @SerialName("anonymous")
    data class Anonymous(
        override val clientIp: String? = null,
    ) : UserRecord {
        override val userId: String get() = ANONYMOUS_ID
    }

    @Serializable
    @SerialName("system")
    data class System(
        override val clientIp: String? = null,
    ) : UserRecord {
        override val userId: String get() = SYSTEM_ID
    }

    @Serializable
    @SerialName("logged-in")
    data class LoggedIn(
        override val userId: String,
        override val clientIp: String? = null,
        override val email: String? = null,
        override val desc: String? = null,
        override val type: String? = null,
    ) : UserRecord

    @Serializable
    @SerialName("api-key")
    data class ApiKey(
        override val userId: String,
        override val clientIp: String? = null,
        val keyId: String,
        val keyName: String? = null,
        override val email: String? = null,
        override val desc: String? = null,
        override val type: String? = null,
    ) : UserRecord

    companion object {
        const val ANONYMOUS_ID = "anonymous"
        const val SYSTEM_ID = "system"

        val anonymous: UserRecord = Anonymous()
        fun system(ip: String?): UserRecord = System(clientIp = ip)
    }
}
```

Notes:

- `userId` on `Anonymous`/`System` is a constant `get()` so it stays out of
  the JSON for those variants.
- `@SerialName` strings: `anonymous`, `system`, `logged-in`, `api-key` —
  matches the kebab-case convention seen in
  `ultra/maths/src/commonMain/kotlin/RandomRange.kt`.
- `isAnonymous`/`isSystem` stay as default methods on the interface to keep
  existing call sites (`record.isAnonymous()`) compiling.

**Migration helper to keep ~34 test constructor calls working**

Add an `operator fun invoke` on the companion that mimics the old data-class
constructor signature:

```kotlin
operator fun invoke(
    userId: String = ANONYMOUS_ID,
    clientIp: String? = null,
    email: String? = null,
    desc: String? = null,
    type: String? = null,
): UserRecord = when (userId) {
    ANONYMOUS_ID -> Anonymous(clientIp)
    SYSTEM_ID -> System(clientIp)
    else -> LoggedIn(userId, clientIp, email, desc, type)
}
```

This makes `UserRecord(userId="alice", email="a@b.com")` continue to compile
and produce a `LoggedIn`. It avoids touching all 34+ test sites in this
refactor — those can migrate to explicit variant constructors over time.

**File: `ultra/security/src/commonMain/kotlin/jwt/JwtUserData.kt`** — change
`toUserRecord` to construct the `LoggedIn` variant explicitly:

```kotlin
fun toUserRecord(clientIp: String?): UserRecord = UserRecord.LoggedIn(
    userId = id,
    clientIp = clientIp,
    email = email,
    desc = desc,
    type = type,
)
```

### Phase B — `funktor/rest`: `Caller` + dispatcher + API-key provider

**New file: `funktor/rest/src/jvmMain/kotlin/auth/Caller.kt`**

```kotlin
package io.peekandpoke.funktor.rest.auth

import com.auth0.jwt.interfaces.Payload

sealed interface Caller {
    val clientIp: String?

    data class Anonymous(override val clientIp: String?) : Caller
    data class JwtCaller(override val clientIp: String?, val payload: Payload) : Caller
    data class ApiKeyCaller(override val clientIp: String?, val key: ApiKeyPrincipal) : Caller
}
```

**New file: `funktor/rest/src/jvmMain/kotlin/auth/ApiKeyPrincipal.kt`**

```kotlin
package io.peekandpoke.funktor.rest.auth

import io.ktor.server.auth.Principal

data class ApiKeyPrincipal(
    val keyId: String,
    val keyName: String?,
    val userId: String,
    val email: String? = null,
    val desc: String? = null,
    val type: String? = null,
    val permissions: io.peekandpoke.ultra.security.user.UserPermissions =
        io.peekandpoke.ultra.security.user.UserPermissions.anonymous,
) : Principal
```

(import this properly — no FQCN per the project style guide; shown inline
here only for plan readability)

**New file: `funktor/rest/src/jvmMain/kotlin/auth/ApiKeyResolver.kt`**

```kotlin
package io.peekandpoke.funktor.rest.auth

fun interface ApiKeyResolver {
    /** Returns a principal if [token] is a valid key, or null to reject. */
    suspend fun resolve(token: String): ApiKeyPrincipal?
}
```

Registered into the kontainer alongside `JwtGenerator`.

**New file: `funktor/rest/src/jvmMain/kotlin/auth/apiKey.kt`** — Ktor auth
provider (uses Ktor's built-in `bearer { ... }` so we don't reimplement
header parsing):

```kotlin
fun AuthenticationConfig.apiKey(name: String, resolver: () -> ApiKeyResolver) {
    bearer(name) {
        authenticate { credential ->
            resolver().resolve(credential.token)
        }
    }
}
```

Plus the matching anonymous fallback the user already does for JWT — emit a
sentinel `ApiKeyPrincipal` (or simply leave the principal null and let
`currentUserProvider()` produce `Caller.Anonymous`).

**File: `funktor/rest/src/jvmMain/kotlin/auth/call.kt`** — replace
`jwtUserProvider()` with a unified dispatcher:

```kotlin
fun ApplicationCall.currentUserProvider(): UserProvider = UserProvider.lazy {
    val caller = resolveCaller()
    val clientIp = caller.clientIp

    when (caller) {
        is Caller.Anonymous -> User.anonymous.copy(
            record = UserRecord.Anonymous(clientIp = clientIp),
        )
        is Caller.JwtCaller -> kontainer.jwtGenerator.extractUser(clientIp.orEmpty(), caller.payload)
        is Caller.ApiKeyCaller -> User(
            record = UserRecord.ApiKey(
                userId = caller.key.userId,
                clientIp = clientIp,
                keyId = caller.key.keyId,
                keyName = caller.key.keyName,
                email = caller.key.email,
                desc = caller.key.desc,
                type = caller.key.type,
            ),
            permissions = caller.key.permissions,
        )
    }
}

private fun ApplicationCall.resolveCaller(): Caller {
    val clientIp = request.origin.remoteHost
    val jwt = principal<JWTPrincipal>()
    val apiKey = principal<ApiKeyPrincipal>()

    return when {
        apiKey != null -> Caller.ApiKeyCaller(clientIp, apiKey)
        jwt != null && jwt.payload !is JwtAnonymous -> Caller.JwtCaller(clientIp, jwt.payload)
        else -> Caller.Anonymous(clientIp)
    }
}

@Deprecated("Use currentUserProvider()", ReplaceWith("currentUserProvider()"))
fun ApplicationCall.jwtUserProvider() = currentUserProvider()
```

Keep the `jwtUserProvider()` name as a `@Deprecated` alias for one release so
external consumers (funktor-demo, aktor, thebase, the project that prompted
this) get a soft-landing migration.

**File: `funktor/rest/src/jvmMain/kotlin/index_jvm.kt`** — extend
`FunktorRestBuilder` so consumers can register the resolver:

```kotlin
fun apiKey(resolver: ApiKeyResolver) {
    with(kontainer) {
        singleton(ApiKeyResolver::class) { resolver }
    }
}
```

### Phase C — Internal consumers (mechanical)

These compile-but-need-touching because of the `.copy()` removal and the
shape change:

| File                                                                       | Change                                                                                                                                                  |
|----------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `funktor/rest/src/jvmMain/kotlin/auth/call.kt:18`                          | replaced wholesale (Phase B)                                                                                                                            |
| `funktor/rest/src/jvmMain/kotlin/ApiRoute.kt:52`                           | `UserRecord(userId="role-eval")` → still compiles via the companion `invoke` shim. No change required.                                                  |
| `funktor/auth/src/jvmMain/kotlin/api/AuthApi.kt:74,209`                    | `.userId`/`.type` reads — both lifted to interface, no change required.                                                                                 |
| `funktor/insights/src/jvmMain/kotlin/collectors/UserCollector.kt:20,30,53` | `UserRecord` typed field + `.userId` read — both fine via the sealed interface.                                                                         |
| `ultra/security/src/jvmMain/kotlin/csrf/StatelessCsrfProtection.kt:24,25`  | `.userId`/`.clientIp` — fine via interface.                                                                                                             |
| `ultra/security/src/commonMain/kotlin/jwt/JwtUserData.kt`                  | rewritten in Phase A to produce `LoggedIn`.                                                                                                             |
| `ultra/security/src/jvmMain/kotlin/user/UserProvider.kt`                   | `static(record, permissions)` overload: change default `record = UserRecord.anonymous` keeps working since `anonymous` is now `Anonymous()`. No change. |

The companion `invoke` shim is what keeps the test surface (~34 sites) and
`ApiRoute.kt` compiling without edits. Tests can migrate to explicit variant
constructors lazily.

### Phase D — Sample wiring & docs

**File: `funktor-demo/server/src/main/kotlin/api/ApiApp.kt`** — add a sample
`apiKey(...)` block alongside `jwt(...)` to demonstrate the new path. Use a
dummy in-memory `ApiKeyResolver` so the demo continues to boot without
external config. Update the route block to
`authenticate(AUTH_REALM_ID, AUTH_API_KEY_REALM_ID) { ... }`.

**File: `funktor-demo/server/src/main/kotlin/kontainer.kt:39`** — change
`call.jwtUserProvider()` → `call.currentUserProvider()`.

**Docs**: per the project's CLAUDE.md, library docs live in
`docs-site/src/pages/ultra/*` and the LLM mirror in
`docs-site/src/data/llms/*.md`. Add a short page covering:

- The new sealed `UserRecord` + variants
- How to register an `ApiKeyResolver`
- The deprecation of `jwtUserProvider()`

Defer to the docs-site skill's workflow when actually writing this.

### Phase E — Tests

- `ultra/security/src/jvmTest/kotlin/user/UserRecordSpec.kt` — add cases for
  each variant (serialize/deserialize round-trip, `@SerialName` discriminator
  presence, default `email`/`desc`/`type` are null on Anonymous/System).
- `ultra/security/src/jvmTest/kotlin/user/UserSpec.kt` — verify
  `User.anonymous.record is UserRecord.Anonymous`, etc.
- New test in `funktor/rest/src/jvmTest/kotlin/auth/` covering
  `currentUserProvider()` dispatch across all three principal states
  (Anonymous, JWT, ApiKey).
- Existing tests in `funktor/rest/src/jvmTest/kotlin/auth/*Spec.kt` should
  pass unchanged thanks to the `invoke` shim.

## Critical files (cheat sheet)

- `ultra/security/src/commonMain/kotlin/user/UserRecord.kt` — sealed rewrite
- `ultra/security/src/commonMain/kotlin/jwt/JwtUserData.kt` — variant
  construction
- `funktor/rest/src/jvmMain/kotlin/auth/call.kt` — dispatcher rewrite
- `funktor/rest/src/jvmMain/kotlin/auth/Caller.kt` — NEW
- `funktor/rest/src/jvmMain/kotlin/auth/ApiKeyPrincipal.kt` — NEW
- `funktor/rest/src/jvmMain/kotlin/auth/ApiKeyResolver.kt` — NEW
- `funktor/rest/src/jvmMain/kotlin/auth/apiKey.kt` — NEW (Ktor provider)
- `funktor/rest/src/jvmMain/kotlin/index_jvm.kt` — `FunktorRestBuilder.apiKey(...)`
- `funktor-demo/server/src/main/kotlin/api/ApiApp.kt` — sample wiring
- `funktor-demo/server/src/main/kotlin/kontainer.kt` — rename call site

## Verification

End-to-end checks before declaring done:

1. **Compile**: `./gradlew :ultra:security:build :funktor:rest:build :funktor:auth:build :funktor:insights:build` — all
   green.
2. **Unit tests**: `./gradlew :ultra:security:jvmTest :funktor:rest:jvmTest` — existing suite passes via the `invoke`
   shim; new dispatcher tests pass.
3. **Serialization**: round-trip each `UserRecord` variant through
   kotlinx-serialization JSON; assert the `type` discriminator and that
   `Anonymous`/`System` JSON does not contain `userId`.
4. **Demo boot**: start funktor-demo server, hit an authenticated endpoint
   three ways:
    - no auth header → server treats as anonymous, `record is Anonymous`
    - valid JWT → `record is LoggedIn`
    - valid `Authorization: Bearer <api-key>` → `record is ApiKey`,
      `keyId`/`keyName` populated.
5. **Insights regression note**: confirm in release notes that any
   pre-refactor serialized `UserRecord` JSON in insights stores will no
   longer deserialize.

## Out of scope / follow-ups

- Migrating the ~34 test sites off the `invoke` shim to explicit variant
  constructors. Leave for a later cleanup PR (or open a one-time scheduled
  agent for it after this lands).
- Project-side migration in CTB / aktor / thebase (the codebases that
  prompted this). They consume `UserRecord` and the deprecated
  `jwtUserProvider()`; both keep working until the deprecation grace
  period ends.
- API-key lifecycle / storage / rotation — explicitly punted to consumers
  via `ApiKeyResolver`.
