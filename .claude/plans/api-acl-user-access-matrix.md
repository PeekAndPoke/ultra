# User-Specific API Access Matrix (ApiAcl)

## 1. Problem Statement

The `authorize {}` DSL in Funktor is **server-side only** (`funktor/rest/src/jvmMain/`). Frontend applications (Kraft
SPAs) have no way to know which API endpoints the current logged-in user can access. This prevents common UI patterns:

- Showing a **read-only page** vs an **edit page**
- **Hiding buttons** (e.g., "Create Event") that lead to endpoints the user can't access
- **Hiding navigation links** to sections the user has no permission for
- Rendering **disabled states** instead of hiding elements entirely

### Current State

The existing `GetApiAccessMatrix` endpoint (`/_/funktor/introspection/api-access-matrix`):

- Returns access levels per **known role** (SuperUser, Anonymous, etc.) -- NOT per current user
- Requires **SuperUser** authorization -- regular users can't call it
- Returns a **hierarchical** model (Feature -> Group -> Endpoint -> [Role -> AccessLevel])
- Is designed for admin dashboards, not for frontend access control

### Desired Frontend Usage

```kotlin
// After login, fetch the current user's access matrix once
val apiAcl = ApiAcl(introspectionApi.getMyApiAccess().first().data!!)

// In UI components -- type-safe lookup using the same endpoint objects from ApiClient companions
if (apiAcl.hasAccessTo(FunktorConfApiClient.CreateEvent)) {
    button { +"Create Event" }
}

if (apiAcl.hasAccessTo(FunktorConfApiClient.ListEvents)) {
    navLink("/events") { +"Events" }
}

// Fine-grained: check for Partial access (e.g., some fields hidden)
if (apiAcl.hasAnyAccessTo(FunktorConfApiClient.GetEvent)) {
    // show event detail, but some fields may be redacted
}
```

---

## 2. Analysis of Existing Infrastructure

### 2.1 The Authorization Chain

```
TypedApiEndpoint (common)     -- endpoint definition with uri, serializers
        |
        | .mount { ... }      -- server-side, creates ApiRoute
        v
ApiRoute (jvmMain)            -- has method, pattern, authRules, estimateAccess()
        |
        | .authorize { ... }  -- attaches AuthRule instances
        v
AuthRule (jvmMain)            -- interface with check() and estimate()
        |
        +-- PermissionsCheck  -- evaluates against UserPermissions (roles, groups, perms)
        +-- CallCheck         -- custom logic via forCall(), has estimateFn
        +-- OrAuthRule        -- logical OR combination
        +-- AndAuthRule       -- logical AND combination
```

### 2.2 The Estimation Mechanism (Key Insight)

`ApiRoute.estimateAccess(UserPermissions)` already exists at `funktor/rest/src/jvmMain/kotlin/ApiRoute.kt:44-57`:

```kotlin
fun estimateAccess(permission: UserPermissions): ApiAccessLevel {
    return estimateAccess(
        user = AuthRule.EstimateCtx(permissions = permission)
    )
}

fun estimateAccess(user: AuthRule.EstimateCtx): ApiAccessLevel {
    return authRules.fold(ApiAccessLevel.Granted) { level, rule ->
        level and rule.estimate(user)
    }
}
```

This folds all auth rules using AND logic (most restrictive wins). Each rule type estimates differently:

| Rule Type             | Estimation Behavior                                                             |
|-----------------------|---------------------------------------------------------------------------------|
| `PermissionsCheck`    | Evaluates the check function against `UserPermissions` -> `Granted` or `Denied` |
| `CallCheck` (forCall) | Uses `estimateFn`, defaults to `Granted` (optimistic)                           |
| `OrAuthRule`          | Least restrictive of all sub-rules (logical OR)                                 |
| `AndAuthRule`         | Most restrictive of all sub-rules (logical AND)                                 |

### 2.3 `ApiAccessLevel` Enum

Located at `ultra/remote/src/commonMain/kotlin/ApiAccessLevel.kt` -- already in common module:

```kotlin
enum class ApiAccessLevel {
    Granted,   // Full access
    Partial,   // Partial access (some fields hidden)
    Denied;    // No access

    fun isGranted() = this == Granted
    fun isPartial() = this == Partial
    fun isDenied() = this == Denied
    infix fun and(other: ApiAccessLevel) = maxOf(this, other)  // most restrictive
    infix fun or(other: ApiAccessLevel) = minOf(this, other)   // least restrictive
}
```

### 2.4 `TypedApiEndpoint` (Common Module)

Located at `ultra/remote/src/commonMain/kotlin/TypedApiEndpoint.kt` -- sealed class:

```kotlin
sealed class TypedApiEndpoint {
    abstract val uri: String
    abstract val attributes: TypedAttributes
    // NOTE: no httpMethod property -- method is implicit in subclass type

    data class Get<out RESPONSE>(...) : TypedApiEndpoint()     // line 121
    data class Post<out BODY, out RESPONSE>(...) : TypedApiEndpoint()  // line 220
    data class Put<out BODY, out RESPONSE>(...) : TypedApiEndpoint()   // line 277
    data class Delete<RESPONSE>(...) : TypedApiEndpoint()      // line 92
    data class Head<RESPONSE>(...) : TypedApiEndpoint()        // line 191
    data class Sse(...) : TypedApiEndpoint()                   // line 159
}
```

Endpoint definitions in ApiClient companions reference these directly:

```kotlin
// FunktorConfApiClient.kt companion object
val ListEvents = TypedApiEndpoint.Get(uri = "$BASE/events", response = ...)
val CreateEvent = TypedApiEndpoint.Post(uri = "$BASE/events", body = ..., response = ...)
```

### 2.5 `ApiRoute` (Server-side)

Located at `funktor/rest/src/jvmMain/kotlin/ApiRoute.kt`:

```kotlin
sealed class ApiRoute<RESPONSE> {
    abstract val method: HttpMethod          // line 24 -- has the HTTP method!
    abstract val pattern: UriPattern         // line 27 -- URI pattern string
    abstract val authRules: List<AuthRule<*, *>>  // line 36

    fun estimateAccess(permission: UserPermissions): ApiAccessLevel  // line 44
}
```

The `method.value` gives the uppercase string ("GET", "POST", etc.).
The `pattern.pattern` gives the URI string (e.g., "/funktor-conf/events/{id}").

### 2.6 `ApiAccessDescriptor` (Server-side)

Located at `funktor/inspect/src/jvmMain/kotlin/introspection/services/ApiAccessDescriptor.kt`:

```kotlin
class ApiAccessDescriptor(
    features: Lazy<List<ApiFeature>>,   // all registered API feature modules
    realms: Lazy<List<AuthRealm<*>>>,   // auth realms (for known roles)
) {
    fun describe(): ApiAccessMatrixModel {  // existing: per-role matrix
        val roles = collectKnownRoles()
        return ApiAccessMatrixModel(
            features = features.map { describeFeature(roles, it) }
        )
    }
    // We will add: describeForUser(permissions: UserPermissions): UserApiAccessMatrix
}
```

### 2.7 Existing IntrospectionApi Handler Pattern

Located at `funktor/inspect/src/jvmMain/kotlin/introspection/api/IntrospectionApi.kt`:

```kotlin
val getApiAccessMatrix = IntrospectionApiClient.GetApiAccessMatrix.mount {
    docs {
        name = "API Access Matrix"
    }.codeGen {
        funcName = "getApiAccessMatrix"
    }.authorize {
        isSuperUser()      // <-- SuperUser only
    }.handle {
        val descriptor = call.kontainer.get(ApiAccessDescriptor::class)
        ApiResponse.ok(descriptor.describe())
    }
}
```

### 2.8 URI Pattern Matching Verification

The URI in `TypedApiEndpoint` and `ApiRoute.pattern.pattern` must match for lookups to work.

**Proof**: In `ApiRoutes.kt` (the mount functions), the `TypedApiEndpoint.uri` is passed to `routeBuilder.get(uri)`which
creates the route. The `ApiRoutes` class uses `mountPoint = ""` by default. Looking at`FunktorConfApi("funktor-conf")`,
the name is "funktor-conf" but the mount point is still empty. The URIs in`FunktorConfApiClient` already contain the
full path (`/funktor-conf/events`). So `TypedApiEndpoint.uri` ==`ApiRoute.pattern.pattern`.

---

## 3. Alternatives Considered

### Alternative A: Declare Auth Rules in Common Module

Move authorization declarations to the `TypedApiEndpoint` definitions in common code:

```kotlin
val CreateEvent = TypedApiEndpoint.Post(
    uri = "$BASE/events",
    body = ..., response = ...,
).withAccess { isSuperUser() }  // hypothetical
```

**Rejected because:**

- Would require making `AuthRule`, `AuthRuleBuilder`, `PermissionsCheck`, etc. multiplatform (currently JVM-only)
- `forCall()` custom checks reference `ApplicationCall`, `Kontainer`, and other JVM-only types
- Massive refactoring scope; the entire auth module would need to become multiplatform
- Creates duplication risk: auth rules declared in common but still need server-side enforcement

### Alternative B: Frontend Evaluates Auth Rules Locally

Ship `UserPermissions` to the frontend and evaluate rules client-side.

**Rejected because:**

- Would require the full `AuthRule` hierarchy in common code (same problem as A)
- `forCall()` custom checks can reference request params, body, kontainer services -- impossible to replicate
- Exposes the full permission structure to the frontend (security concern)

### Alternative C: User-Specific Access Matrix Endpoint (CHOSEN)

Add a new endpoint that returns the access matrix evaluated for the **current user's permissions**.

**Chosen because:**

- Leverages the existing `estimateAccess(UserPermissions)` infrastructure entirely
- No changes to the auth module architecture
- Minimal new code (2 new files, 4 modified files)
- The server remains the single source of truth for access decisions
- Custom `forCall()` checks work via their `estimateFn` (optimistic default is acceptable)
- Clean, type-safe frontend API using existing `TypedApiEndpoint` objects as lookup keys

---

## 4. Detailed Implementation Plan

### Step 1: Add `httpMethod` to `TypedApiEndpoint`

**File**: `ultra/remote/src/commonMain/kotlin/TypedApiEndpoint.kt`

**Why**: The `TypedApiEndpoint` sealed class has no explicit HTTP method property. The method is implicit in the
subclass type (Get, Post, etc.). For the `ApiAcl` lookup, we need to derive the method string from any`TypedApiEndpoint`
instance. Adding an abstract property is cleaner than a `when` expression in the `ApiAcl` class.

**Change**: Add abstract property at line ~85 (after `attributes`), override in each subclass as a computed `get()`
property (not a constructor param, so no effect on data class equals/hashCode/copy/serialization).

```kotlin
// In sealed class TypedApiEndpoint (after line 86):
/** The HTTP method as an uppercase string (e.g., "GET", "POST"). */
abstract val httpMethod: String

// In each subclass:
data class Delete<RESPONSE>(...) : TypedApiEndpoint() {
    override val httpMethod: String get() = "DELETE"
    // ... rest unchanged
}

data class Get<out RESPONSE>(...) : TypedApiEndpoint() {
    override val httpMethod: String get() = "GET"
    // ... rest unchanged
}

data class Sse(...) : TypedApiEndpoint() {
    override val httpMethod: String get() = "GET"
    // ... rest unchanged
}

data class Head<RESPONSE>(...) : TypedApiEndpoint() {
    override val httpMethod: String get() = "HEAD"
    // ... rest unchanged
}

data class Post<out BODY, out RESPONSE>(...) : TypedApiEndpoint() {
    override val httpMethod: String get() = "POST"
    // ... rest unchanged
}

data class Put<out BODY, out RESPONSE>(...) : TypedApiEndpoint() {
    override val httpMethod: String get() = "PUT"
    // ... rest unchanged
}
```

### Step 2: Create `UserApiAccessMatrix` Model

**New file**: `funktor/inspect/src/commonMain/kotlin/introspection/api/UserApiAccessMatrix.kt`

**Why**: We need a serializable model to transport the user's access levels from server to frontend. This is
intentionally flat (not hierarchical like `ApiAccessMatrixModel`) because the frontend only needs to look up access by
endpoint, not browse by feature/group.

```kotlin
package io.peekandpoke.funktor.inspect.introspection.api

import io.peekandpoke.ultra.remote.ApiAccessLevel
import kotlinx.serialization.Serializable

/**
 * A flat access matrix describing the estimated [ApiAccessLevel] for each API endpoint,
 * evaluated for a specific user's permissions.
 *
 * Use [ApiAcl] to perform lookups against this matrix.
 */
@Serializable
data class UserApiAccessMatrix(
    val entries: List<Entry>,
) {
    @Serializable
    data class Entry(
        /** HTTP method: "GET", "POST", "PUT", "DELETE", "HEAD" */
        val method: String,
        /** URI pattern, e.g. "/funktor-conf/events/{id}" */
        val uri: String,
        /** The estimated access level for the current user */
        val level: ApiAccessLevel,
    )
}
```

### Step 3: Create `ApiAcl` Lookup Class

**New file**: `funktor/inspect/src/commonMain/kotlin/introspection/api/ApiAcl.kt`

**Why**: This is the developer-facing utility that makes the access check ergonomic. It builds an internal lookup map
from the matrix and provides type-safe methods that accept `TypedApiEndpoint` instances.

```kotlin
package io.peekandpoke.funktor.inspect.introspection.api

import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.remote.TypedApiEndpoint

/**
 * Client-side API access control lookup.
 *
 * Built from a [UserApiAccessMatrix] (fetched from the server for the current user),
 * this class provides type-safe access checks using [TypedApiEndpoint] instances.
 *
 * Usage:
 * ```

* val acl = ApiAcl(matrix)
* if (acl.hasAccessTo(MyApiClient.CreateItem)) { /* show button */ }
* ```

*/
class ApiAcl(matrix: UserApiAccessMatrix) {

    companion object {
        /** An empty ACL that denies access to all endpoints. */
        val empty = ApiAcl(UserApiAccessMatrix(entries = emptyList()))

        /** Builds the internal lookup key from method and uri. */
        private fun key(method: String, uri: String): String = "$method|$uri"
    }

    private val lookup: Map<String, ApiAccessLevel> = matrix.entries.associate { entry ->
        key(entry.method, entry.uri) to entry.level
    }

    /**
     * Returns the estimated [ApiAccessLevel] for the given [endpoint].
     *
     * Returns [ApiAccessLevel.Denied] if the endpoint is not present in the matrix
     * (secure-by-default).
     */
    fun getAccessLevel(endpoint: TypedApiEndpoint): ApiAccessLevel {
        return lookup[key(endpoint.httpMethod, endpoint.uri)] ?: ApiAccessLevel.Denied
    }

    /**
     * Returns `true` if the given [endpoint] has [ApiAccessLevel.Granted] access.
     *
     * Use this for strict access checks where partial access is not sufficient.
     */
    fun hasAccessTo(endpoint: TypedApiEndpoint): Boolean {
        return getAccessLevel(endpoint).isGranted()
    }

    /**
     * Returns `true` if the given [endpoint] has any access (not [ApiAccessLevel.Denied]).
     *
     * Use this when partial access is acceptable (e.g., showing a page with some
     * fields redacted).
     */
    fun hasAnyAccessTo(endpoint: TypedApiEndpoint): Boolean {
        return !getAccessLevel(endpoint).isDenied()
    }

}

```

**Design decisions**:
- **Default to `Denied`**: If an endpoint is not in the matrix (e.g., new endpoint deployed after the matrix was fetched), the ACL conservatively denies access. The UI hides the feature until refresh.
- **`hasAccessTo` vs `hasAnyAccessTo`**: Two levels of strictness. `hasAccessTo` requires `Granted`; `hasAnyAccessTo` allows `Partial`. This lets developers choose per use case.
- **`empty` companion val**: Safe default for initialization before the matrix is loaded. Prevents NPEs and null checks in UI code.
- **Immutable**: The matrix and lookup are set at construction time. Thread-safe for coroutine use.

### Step 4: Add `describeForUser()` to `ApiAccessDescriptor`

**File**: `funktor/inspect/src/jvmMain/kotlin/introspection/services/ApiAccessDescriptor.kt`

**Why**: This service already has access to all `ApiFeature` instances and the pattern of iterating features -> route groups -> routes. Adding a method that evaluates for a single user's permissions (instead of all known roles) is straightforward.

**Add after the existing `describe()` method (after line 30)**:

```kotlin
/**
 * Builds a [UserApiAccessMatrix] by evaluating all registered API routes
 * against the given user [permissions].
 */
fun describeForUser(permissions: UserPermissions): UserApiAccessMatrix {
    val entries = features.flatMap { feature ->
        feature.getRouteGroups().flatMap { group ->
            group.all.map { route ->
                UserApiAccessMatrix.Entry(
                    method = route.method.value,
                    uri = route.pattern.pattern,
                    level = route.estimateAccess(permissions),
                )
            }
        }
    }
    return UserApiAccessMatrix(entries = entries)
}
```

**Additional import needed**:

```kotlin
import io.peekandpoke.funktor.inspect.introspection.api.UserApiAccessMatrix
```

Note: `UserPermissions` is already imported.

### Step 5: Add Endpoint to `IntrospectionApiClient`

**File**: `funktor/inspect/src/commonMain/kotlin/introspection/api/IntrospectionApiClient.kt`

**Why**: The client-side endpoint definition must be in the common module so both frontend and server can reference it.

**In the companion object (after `GetApiAccessMatrix` at line 65)**:

```kotlin
val GetMyApiAccess = Get(
    uri = "$base/my-api-access",
    response = UserApiAccessMatrix.serializer().api(),
)
```

**In the class body (after `getApiAccessMatrix()` at line 107)**:

```kotlin
fun getMyApiAccess(): Flow<ApiResponse<UserApiAccessMatrix>> = call(
    GetMyApiAccess()
)
```

**No new imports needed** -- `Get`, `api`, `call`, `Flow`, `ApiResponse` are already imported.

### Step 6: Add Server Handler to `IntrospectionApi`

**File**: `funktor/inspect/src/jvmMain/kotlin/introspection/api/IntrospectionApi.kt`

**Why**: The server-side handler mounts the endpoint, applies authorization, and delegates to
`ApiAccessDescriptor.describeForUser()`.

**Add after the `getApiAccessMatrix` handler (after line 252)**:

```kotlin
val getMyApiAccess = IntrospectionApiClient.GetMyApiAccess.mount {
    docs {
        name = "My API Access"
    }.codeGen {
        funcName = "getMyApiAccess"
    }.authorize {
        public()
    }.handle {
        val descriptor = call.kontainer.get(ApiAccessDescriptor::class)
        val userProvider = call.kontainer.get(UserProvider::class)

        ApiResponse.ok(descriptor.describeForUser(userProvider().permissions))
    }
}
```

**Additional import needed**:

```kotlin
import io.peekandpoke.ultra.security.user.UserProvider
```

**Authorization choice**: `public()` means any authenticated user. The Ktor authentication plugin still requires a valid
token before reaching this handler. We use `public()` (not `isSuperUser()`) because every user should be able to query
their own access -- this is the whole point.

### Step 7: Add Tests

**File**: `funktor/all/src/jvmTest/kotlin/IntrospectionApiSpec.kt`

**Why**: Follow the existing test patterns in this file. All other introspection endpoints have tests for
anonymous/user/superuser access.

**Add a new test section following the existing pattern** (after the `getApiAccessMatrix` test block):

```kotlin
api.introspection.getMyApiAccess { route ->
    "Anonymous request must be unauthorized" {
        apiApp {
            anonymous {
                request(route) {
                    status shouldBe HttpStatusCode.Unauthorized
                }
            }
        }
    }

    "Authenticated user request must succeed" {
        apiApp {
            authenticate(/* regular user token */) {
                request(route) {
                    status shouldBe HttpStatusCode.OK
                    val matrix = apiResponseData<UserApiAccessMatrix>()
                    matrix.shouldNotBeNull()
                    matrix.entries.shouldNotBeEmpty()
                }
            }
        }
    }

    "Super user request must succeed" {
        apiApp {
            authenticate(/* super user token */) {
                request(route) {
                    status shouldBe HttpStatusCode.OK
                    val matrix = apiResponseData<UserApiAccessMatrix>()
                    matrix.shouldNotBeNull()
                    matrix.entries.shouldNotBeEmpty()
                    // Super user should have Granted on all endpoints
                    matrix.entries.forEach { entry ->
                        entry.level shouldBe ApiAccessLevel.Granted
                    }
                }
            }
        }
    }
}
```

**Note**: The exact test DSL syntax needs to match the patterns in the existing spec file. The test structure above is
illustrative -- actual syntax will be adapted to match the existing `IntrospectionApiSpec.kt` patterns during
implementation.

---

## 5. Design Notes

### 5.1 `forCall()` Custom Checks and Estimation Accuracy

The `forCall()` DSL creates a `CallCheck` with an `estimateFn` that defaults to `ApiAccessLevel.Granted`:

```kotlin
// AuthRule.kt:49-54
fun <P, B> forCall(
    description: String,
    estimateFn: EstimateCtx.() -> ApiAccessLevel = { ApiAccessLevel.Granted },
    checkFn: CheckCtx<P, B>.() -> Boolean,
): AuthRule<P, B>
```

This means:

- **Standard checks** (`isSuperUser()`, `forRole()`, `forGroup()`, `forPermission()`) evaluate accurately against
  `UserPermissions`
- **Custom `forCall()` checks** default to `Granted` for estimation -- the frontend may optimistically show access where
  the server would deny at runtime

This is **acceptable** because:

1. The server **always** enforces the real `check()` at request time -- the frontend matrix is advisory only
2. The worst case is: button is shown, user clicks it, server returns 403, frontend handles the error
3. Developers can provide an explicit `estimateFn` for custom checks to make estimation accurate:
   ```kotlin
   .authorize {
       forCall(
           "Custom check",
           estimateFn = { if (permissions.hasRole("editor")) ApiAccessLevel.Granted else ApiAccessLevel.Denied },
       ) { /* full custom check with request context */ }
   }
   ```

### 5.2 Performance

The server iterates all registered routes (typically dozens to a few hundred in a real app) and calls `estimateAccess()`
for each. This is:

- **CPU**: Pure in-memory logic, no I/O, no DB queries. Trivial even for hundreds of routes.
- **Response size**: Each entry is ~50 bytes of JSON. 100 routes = ~5KB.
- **Frequency**: Called once after login, not on every request.
- **No server-side caching needed**.

### 5.3 Frontend Caching and Invalidation

The `ApiAcl` is an **immutable snapshot**. Recommended pattern:

```kotlin
// In app state / session management
var apiAcl: ApiAcl = ApiAcl.empty

// After login or token refresh
suspend fun refreshAcl(introspectionApi: IntrospectionApiClient) {
    introspectionApi.getMyApiAccess().collect { response ->
        response.data?.let { matrix ->
            apiAcl = ApiAcl(matrix)
        }
    }
}
```

Invalidation triggers:

- User logs in
- Token refresh (if roles/permissions might have changed)
- Explicit "refresh permissions" action

No automatic invalidation is built in -- this is the app developer's responsibility.

### 5.4 Secure-by-Default Behavior

- **Unknown endpoints**: `ApiAcl` returns `Denied` for any endpoint not in the matrix
- **Empty matrix**: `ApiAcl.empty` denies everything -- safe before data is loaded
- **No permission leakage**: The matrix only reveals access levels (Granted/Partial/Denied), not the actual permission
  rules or the user's roles/groups

---

## 6. Files Summary

| # | File                                                                                | Action  | Description                                                 |
|---|-------------------------------------------------------------------------------------|---------|-------------------------------------------------------------|
| 1 | `ultra/remote/src/commonMain/kotlin/TypedApiEndpoint.kt`                            | Modify  | Add abstract `httpMethod: String`, override in 6 subclasses |
| 2 | `funktor/inspect/src/commonMain/kotlin/introspection/api/UserApiAccessMatrix.kt`    | **New** | Serializable flat access matrix model                       |
| 3 | `funktor/inspect/src/commonMain/kotlin/introspection/api/ApiAcl.kt`                 | **New** | Client-side lookup utility class                            |
| 4 | `funktor/inspect/src/commonMain/kotlin/introspection/api/IntrospectionApiClient.kt` | Modify  | Add `GetMyApiAccess` endpoint + `getMyApiAccess()` method   |
| 5 | `funktor/inspect/src/jvmMain/kotlin/introspection/services/ApiAccessDescriptor.kt`  | Modify  | Add `describeForUser(UserPermissions)` method               |
| 6 | `funktor/inspect/src/jvmMain/kotlin/introspection/api/IntrospectionApi.kt`          | Modify  | Add `getMyApiAccess` route handler                          |
| 7 | `funktor/all/src/jvmTest/kotlin/IntrospectionApiSpec.kt`                            | Modify  | Add test cases for the new endpoint                         |

---

## 7. Verification

1. **Build**: Compile the full project to verify no breakage from `httpMethod` addition
2. **Run tests**: Execute `IntrospectionApiSpec` to verify the new endpoint works
3. **Manual verification**: The endpoint should return a flat list of entries with method, uri, and access level for the
   authenticated user
4. **Frontend smoke test**: Instantiate `ApiAcl` from the response and verify `hasAccessTo()` returns expected results
   for known endpoints
