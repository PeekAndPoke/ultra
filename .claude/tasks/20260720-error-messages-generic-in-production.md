# Error responses must not return `cause.message` in production

**Status:** TODO — tactical slice; see
`.claude/tasks/20260720-exception-disclosure-architecture.md` for the umbrella design
**Plan:** none — from `.claude/tasks/20260720-error-response-disclosure-audit.md` (findings 1 + 2)
**Security-critical:** yes

> This is the first implementable slice of the architecture task (its sequencing step 2 + 3). The
> umbrella design is **decided, not open**: default-deny at the render boundary via
> `HasClientMessage`, `Throwable.message` never rendered, option **C** (identity-gated disclosure,
> degrading to correlation-id-only when there is no authenticated caller). Do not re-litigate it
> here.

## Spec

The production branch of `ApiStatusPages` returns the raw exception message
(`funktor/rest/src/jvmMain/kotlin/ApiStatusPages.kt:47-54`):

```kotlin
fun <T> ApiResponse<T>.withCause(call: ApplicationCall, cause: Throwable): ApiResponse<T> {
    val config = call.kontainerOrNull?.getOrNull(AppConfig::class)

    return when (exposesStackTraces(config)) {
        true -> this.withError(cause.stackTraceToString())
        else -> this.withError(cause.message ?: "")   // <-- production
    }
}
```

That is not safe, because exception messages in this stack carry internals. The worst offender is
`karango/core/src/main/kotlin/vault/KarangoDriver.kt:130-138`, verified verbatim:

```kotlin
throw KarangoQueryException(
    query = query,
    message = "Error while querying '${e.message}':\n\n${query.query}\nwith params [${
        vars.keys.joinToString(", ")
    }]",
    cause = e,
)
```

So a correctly-configured **production** app returns the full AQL source, collection names and
bind-variable names to the client on any uncaught query error. Reachable from unauthenticated
endpoints (e.g. sign-in, which does a user lookup). Also leaks via `FileNotFoundException` (paths),
`ConnectException` / `UnknownHostException` (internal host:port), kontainer `ServiceDefinition`
messages (service FQCNs) and Slumber awaker errors (JSON field paths).

Acceptance criteria:

- [ ] `HasClientMessage` exists in `ultra/common` commonMain and is the **only** way an exception's
      own text reaches a client
- [ ] `withCause` renders: `clientMessage` if the cause implements `HasClientMessage`, otherwise a
      fixed generic string; plus a correlation id in **every** environment
- [ ] `Throwable.message` is never passed to `withError`/`withInfo` at a render boundary in any
      environment — including the non-production branch, which now goes through the superuser gate
      instead of the environment gate
- [ ] `exposesStackTraces(config: AppConfig?)` is replaced by an identity check; the `AppConfig`
      lookup disappears from this path
- [ ] Correlation id is minted once per failure, returned to the client, and present on the matching
      `logInternalError` line
- [ ] `KarangoQueryException` carries `query` / bind-var **names** in structured fields; its
      `message` contains no AQL
- [ ] `CouldNotConvertException` implements `HasClientMessage` (see below) so 400s stay useful
- [ ] Log-only warning phase ships first (see "Staged rollout")

## Implementation notes

### 1. `HasClientMessage` — placement

Must be visible to both `funktor/rest` and `karango/core`. Their only common ancestor is
`ultra:common` (`funktor/core/build.gradle.kts:58` `api(project(":ultra:common"))`,
`karango/core/build.gradle.kts:33` `implementation(project(":ultra:common"))`). Put it in
`ultra/common/src/commonMain/kotlin/` so client-side Kotlin can also see it.

> **Build change required:** `karango/core` depends on `ultra:common` with `implementation` scope
> (`karango/core/build.gradle.kts:33`). Once `KarangoQueryException` implements `HasClientMessage`,
> the interface appears in a public supertype and the dependency must become `api`.

```kotlin
package io.peekandpoke.ultra.common

/**
 * Marks an exception whose [clientMessage] is safe to render to any caller, including anonymous
 * ones. It must contain no internals (queries, paths, hosts, FQCNs) and no echo of caller input.
 */
interface HasClientMessage {
    val clientMessage: String
}
```

### 2. Target implementation of `withCause` / `exposesStackTraces`

`exposesStackTraces(config: AppConfig?)` (`ApiStatusPages.kt:32-34`, currently
`config?.ktor?.isNotProduction == true`) is **deleted**, not fixed. Under option C the gate is
identity, not environment. Identity is available on the error path via the request kontainer:
`UserProvider` (`ultra/security/src/jvmMain/kotlin/user/UserProvider.kt:7-9`, `operator fun
invoke(): User`) and `User.permissions.isSuperUser`
(`ultra/security/src/commonMain/kotlin/user/UserPermissions.kt:9`). This is the same source
`AuthRule.CheckCtx` uses (`funktor/rest/src/jvmMain/kotlin/auth/AuthRule.kt:178-179`), so the
security property "you see internals iff you are a superuser" is checkable against one mechanism.

The identity lookup itself must not throw — `UserProvider` resolution goes through the kontainer and
`UserProvider.Lazy` (`UserProvider.kt:53-57`) evaluates a caller-supplied lambda on first access,
which can fail. Wrap it.

Replacement for `ApiStatusPages.kt:32-54`:

```kotlin
/** Rendered instead of any exception text that is not explicitly declared client-safe. */
const val GENERIC_ERROR_MESSAGE = "An internal error occurred. Please quote the error id when reporting this."

/**
 * Whether the current caller may see raw diagnostic detail inline.
 *
 * Identity-gated, not environment-gated: an unknown or unauthenticated caller must never see
 * internals, and a superuser may see them in every environment. Must never throw — the error path
 * is the one place where a secondary failure loses the original error.
 */
fun exposesDiagnostics(call: ApplicationCall): Boolean = try {
    call.kontainerOrNull?.getOrNull(UserProvider::class)?.invoke()?.permissions?.isSuperUser == true
} catch (_: Throwable) {
    false
}

fun <T> ApiResponse<T>.withCause(call: ApplicationCall, cause: Throwable, errorId: String): ApiResponse<T> {
    val clientText = when (cause) {
        is HasClientMessage -> cause.clientMessage
        else -> GENERIC_ERROR_MESSAGE
    }

    return this
        .withError(clientText)
        .withInfo("error-id: $errorId")
        .let {
            when (exposesDiagnostics(call)) {
                true -> it.withInfo(cause.stackTraceToString())
                else -> it
            }
        }
}
```

The `errorId` is minted once in the `on(CallFailed)` handler (`ApiStatusPages.kt:56`) and passed to
both `logInternalError` and `withCause`, so the log line and the response agree:

```kotlin
on(CallFailed) { call, cause ->
    val errorId = newErrorId()
    // ...
    call.logInternalError(message = "error-id: $errorId", cause = cause)
    call.apiRespond(
        ApiResponse.internalServerError<Any>()
            .withError("The request '${call.request.uri}' cause an internal server error")
            .withCause(call, cause, errorId)
    )
}
```

`logInternalError(message: String?, cause: Throwable?)` already takes a message parameter and
interpolates it into the log content (`funktor/core/src/jvmMain/kotlin/error_logging.kt:56-63`), so
no signature change is needed there — only the `CouldNotConvertException` and `NotFoundException`
branches (`ApiStatusPages.kt:65-78`) currently call no logger at all and need one if their error ids
are to be resolvable. Decide per branch; a 400 with a `HasClientMessage` cause arguably needs no id.

Note that `withError`/`withInfo` both append to `ApiResponse.messages`
(`ultra/remote/src/commonMain/kotlin/ApiResponse.kt:417-423`, `454-458`) — there is no separate
field for an error id. Using `withInfo` for the id is the zero-schema-change option; adding a typed
`errorId: String?` to `ApiResponse` would be cleaner but is a cross-platform serialisation change
and should be its own decision.

**Second boundary:** `funktor/rest/src/jvmMain/kotlin/respond.kt:48-69`
(`apiRespondUnauthorized`) still gates on `appConfig.ktor.isNotProduction` (line 57) to leak failed
auth-rule descriptions. It is not an exception boundary, so it is out of scope for `withCause`, but
it must switch to the same `exposesDiagnostics` predicate in this task or the environment gate
survives in the repo and the guard test below cannot assert its absence.

### 3. `KarangoQueryException` restructuring

Current definition — `karango/core/src/main/kotlin/exceptions.kt:6-17`:

```kotlin
open class KarangoException(message: String, cause: Throwable? = null) : Throwable(message, cause)

class KarangoQueryException(
    val query: AqlTypedQuery<*>,
    message: String,
    cause: ArangoDBException,
) : KarangoException(message, cause = cause)
```

Target:

```kotlin
open class KarangoException(message: String, cause: Throwable? = null) : Throwable(message, cause)

/** Thrown when an AQL query fails. Contains the [query] that caused the error. */
class KarangoQueryException(
    val query: AqlTypedQuery<*>,
    /** Names only — never values, which may contain user data or secrets. */
    val bindVarNames: List<String>,
    cause: ArangoDBException,
) : KarangoException(
    message = "AQL query failed: ${cause.message}",
    cause = cause,
), HasClientMessage {
    override val clientMessage: String = "The query could not be completed"

    /** Full diagnostic rendering — for logs and superuser-gated output only, never a response body. */
    fun describe(): String =
        "AQL query failed: ${cause?.message}\n\n${query.query}\nwith params [${bindVarNames.joinToString(", ")}]"
}
```

The `message` parameter is removed from the constructor entirely — that is what forces every call
site to stop concatenating. `cause.message` is an ArangoDB driver message; it stays in `message` for
the logs and is now unreachable by a client because `Throwable.message` is never rendered.

Call site — `karango/core/src/main/kotlin/vault/KarangoDriver.kt:129-139` becomes:

```kotlin
} catch (e: ArangoDBException) {
    throw KarangoQueryException(
        query = query,
        bindVarNames = vars.keys.toList(),
        cause = e,
    )
}
```

**Complete inventory** (`grep -rn "KarangoQueryException\|KarangoException" --include="*.kt"`,
excluding `build/`) — 4 non-import hits, all verified by reading:

| Site | Role | Action |
|---|---|---|
| `karango/core/src/main/kotlin/exceptions.kt:7,13-17` | definition | restructure as above |
| `karango/core/src/main/kotlin/vault/KarangoDriver.kt:130-138` | **only construction site** | drop the `message` argument, pass `bindVarNames` |
| `karango/core/src/main/kotlin/vault/EntityRepository.kt:332-333` | reads `e.query` (not `e.message`) into `RemoveResult(count = 0, query = e.query)` | unaffected by the change; see the `RemoveResult` note below |
| `funktor/cluster/src/jvmMain/kotlin/backgroundjobs/karango/KarangoBackgroundJobsQueueRepo.kt:142-151` | walks the `cause` chain for `ArangoDBException.errorNum == 1210` | unaffected — reads `errorNum`, not the message; the cause chain is preserved |

**No reader of `KarangoQueryException.message` exists outside the render boundary.** The change is
therefore behaviour-preserving for all in-repo consumers.

**`RemoveResult` open question resolved:** `ultra/vault/src/jvmMain/kotlin/results.kt:9` —
`data class RemoveResult(val count: Long, val query: TypedQuery<*>?)` is **not** `@Serializable`, and
neither is `TypedQuery`. kotlinx-serialization cannot emit it, so the success-path leak flagged in
the audit is not reachable through `ApiResponse` serialisation. It remains a footgun for any future
Jackson-based or reflective serialisation of the type; recommend leaving `RemoveResult` alone in
this task and noting it in the architecture doc as closed-for-now.

### 4. Which existing exceptions implement `HasClientMessage` immediately

| Type | Verdict | Justification |
|---|---|---|
| `CouldNotConvertException` (`funktor/core/src/jvmMain/kotlin/broker/exception.kt:14`) | **Yes — with a rewritten message** | It is the one type `ApiStatusPages` already special-cases into a 400 (`ApiStatusPages.kt:65-69`), so default-deny would silently turn every parse error into the generic string. But its five construction sites all echo caller input verbatim: `"Could not convert '$value'"` (`broker/vault/mpdatetime.kt:55`), `"Could not parse ZoneId '$value'"` / `"Could not parse LocalTime '$value'"` / `"Unknown date format '$value'"` / `"Could not parse '$value'"` (`broker/vault/javatime.kt:103,109,114,119`). The interface contract says no input echo. Implement `clientMessage` as a fixed `"A request parameter could not be parsed"`, or add a `field`/`format` property and build the message from those — not from `$value`. |
| `KarangoQueryException` | **Yes** | Covered above. Without it, DB errors become the generic string, which is the correct default anyway; declaring it explicitly makes the "no AQL" property greppable at the type. |
| `NoConverterFoundException`, `InvalidRouteParamsException` (`broker/exception.kt:9,19`) | **No** | Server-side wiring faults, not caller faults. Their messages name internal converter/route types. Generic string is correct. |
| `ConverterException` base (`broker/exception.kt:4`) | **No** | Marking the base would make the two above client-safe by inheritance. Mark leaves only — this is the reason for a marker interface rather than a base class. |
| `AuthError` (`funktor/auth/src/jvmMain/kotlin/AuthError.kt:3-31`) | **Not in this task** | Eight of its nine factories already read as user-facing (`"Invalid credentials"`, `"Weak password"`), but `userNotFound(user)` (line 12-13) and `providerNotFound(provider)` (line 6-7) interpolate identifiers and are exactly the account-enumeration vector owned by `.claude/tasks/20260720-auth-error-account-enumeration.md`. Blanket-marking `AuthError` here would bless the enumeration leak. Coordinate: that task decides the per-factory client messages, this task provides the interface. |
| `AppStartException` (`funktor/core/src/jvmMain/kotlin/lifecycle/AppStartException.kt:9`) | **No** | Never reaches a request boundary. |
| Validation errors | **Unresolved** — see Open questions |

### 5. Correlation id — investigation result

Three candidates were checked. **Recommendation: mint a new one.**

| Candidate | Finding | Verdict |
|---|---|---|
| `TimingInterceptor` (`funktor/core/src/jvmMain/kotlin/coroutines/timing.kt:41-96`) | Read in full. Carries `start`, `activeNs`, `children`, `capturedContext` — **no identifier field of any kind**. It is a CPU-profiling context element. | Reject — nothing to reuse |
| Insights per-request file (`funktor/insights/src/jvmMain/kotlin/impl/InsightsFull.kt:39`) | `private val filename = "records-$date/$dateTime.json"`, derived from `LocalDateTime.now()` at construction (line 30). Already surfaced to clients as `ApiResponse.insights.detailsUri` via `RequestMetricsProvider.getRequestDetailsUri()` (`funktor/rest/src/jvmMain/kotlin/respond.kt:84`). **But:** the default binding is `InsightsConfig.Disabled` (`funktor/insights/src/jvmMain/kotlin/insights_module.kt:37`), which yields `InsightsSlim` whose `getRequestDetailsUri()` returns `null` (`impl/InsightsSlim.kt:8`); it also degrades to `InsightsSlim` when the repository or mapper is missing (`insights_module.kt:46-50`). So it is absent exactly in the production configuration where the correlation id matters most. It is also timestamp-derived, hence collision-prone under concurrency and not opaque. | Reject |
| MDC / Ktor `CallId` | `grep -rn "MDC\|callId\|CallId\|X-Request-Id\|correlationId"` over `*.kt`/`*.xml` outside `build/` returns **zero hits**. Nothing exists. | Nothing to reuse |

Mint a new one at the boundary: a random opaque token (e.g. 12 hex chars from
`kotlin.random.Random` — already imported at `ApiStatusPages.kt:19`, or `UUID.randomUUID()`). Keep
it short enough for a human to read over the phone and long enough not to collide. Do **not** derive
it from a timestamp, request URI, or user id — it must be opaque and unguessable so it cannot be
used to probe whether another request failed.

Scope: mint per **failure**, not per request. A request that fails once produces one id. This is
simpler than a request-scoped id and needs no new coroutine context element or plugin. If a
request-wide id is wanted later, it belongs in a `CallId`-style plugin, which is a separate change.

The id must appear in the response and in `logInternalError`'s output — both driven from the same
local in `on(CallFailed)`, so they cannot drift.

### 6. Staged rollout

Ship in two releases so downstream reliance is discovered before anything breaks.

**Phase 1 — log-only (no behaviour change for clients).** `withCause` keeps its current output, but
additionally logs a WARN whenever `cause !is HasClientMessage` at the boundary:

```
WARN client-message-audit: <cause::class.qualifiedName> reached the render boundary without
HasClientMessage; from <method> <uri>; error-id: <errorId>
```

This produces the real inventory of what downstream apps rely on. Two ways to be honest about it:
log the exception **class name** and the route, never the message, so the audit log does not become
a second copy of the leak. Run for at least one full release cycle in a real deployment.

**Phase 2 — enforce.** Flip `withCause` to the implementation in section 2 and delete
`exposesStackTraces`. The `KarangoQueryException` restructuring (section 3) can land in phase 1
independently — it is a pure improvement that removes the worst payload before the boundary change,
and it is the item worth back-porting if phase 2 slips.

Do **not** offer a config flag to re-enable the old behaviour. That reintroduces the fail-open
environment gate the architecture doc rejected as option A.

## Test evidence

Unit — `funktor/rest/src/jvmTest/kotlin/` (module already has a `jvmTest` source set):

- [ ] `withCause` + arbitrary `RuntimeException("secret internal detail")` + anonymous caller →
      body contains `GENERIC_ERROR_MESSAGE`, does not contain `"secret internal detail"`
- [ ] Same, but caller is a superuser (`UserProvider.static(permissions = UserPermissions.system)`)
      → body additionally contains the stack trace
- [ ] `withCause` + a `HasClientMessage` exception → body contains `clientMessage` and **not**
      `message`
- [ ] `withCause` when the kontainer has no `UserProvider` → generic string, no throw
- [ ] `exposesDiagnostics` when `UserProvider.Lazy` throws → returns `false`, does not propagate
- [ ] The same error id appears in the response and in the captured log output
- [ ] Parameterised over environment: identical output for production and non-production configs
      (this is the regression guard against the environment gate returning)

Karango — `karango/core/src/test/`:

- [ ] `KarangoQueryException.message` contains no `"FOR "`, no `"FILTER "`, no collection name
- [ ] `describe()` does contain the AQL (so the diagnostic path is not accidentally gutted)
- [ ] `bindVarNames` holds names only; no bind **value** appears in any field or in `message`
- [ ] `KarangoBackgroundJobsQueueRepo.tryInsertWithDedupe` still detects errorNum 1210 through the
      restructured exception (cause chain preserved) — `KarangoBackgroundJobsQueueRepo.kt:142-151`

End-to-end — `AppSpec`/`AppUnderTest` (`funktor/testing/src/jvmMain/kotlin/`):

- [ ] Force a failing repo call on a real route; assert the production response body contains
      neither the AQL nor `"Error while querying"` nor `"AQL query failed"`
- [ ] Same route, superuser caller → body does contain diagnostics
- [ ] Storage-touching cases run under `MatrixTest2d` against both DB backends
- [ ] Guard test over rendered bodies: no `java.`, no `io.peekandpoke.`, no `/` path separators
      outside the request URI, no AQL keywords — for the anonymous case in every environment

Commands:

- [ ] `./gradlew :funktor:rest:jvmTest :funktor:core:jvmTest :funktor:cluster:jvmTest :karango:core:test`

## Open questions

- **Validation errors.** The architecture doc names them as `HasClientMessage` candidates, but no
  validation exception type was located in this pass. `grep -rn "class .*ValidationException\|
  ValidationError"` over `*.kt` outside `build/` returned no server-side hit. Kraft's form
  validation (`kraft/core/src/jsMain/kotlin/forms/validation/`) is client-side and does not throw
  across this boundary. **Someone must confirm whether a server-side validation exception exists at
  all** before this bullet can be closed; if none exists, drop it from the spec.
- **Error id in `ApiResponse`.** Section 2 smuggles the id through `withInfo`. Whether to add a
  typed `errorId: String?` to `ApiResponse` (`ultra/remote/src/commonMain/kotlin/ApiResponse.kt:10-19`)
  is undecided — it is a client-visible schema change affecting every consumer and deserves its own
  call. `withInfo` is the shippable option; the typed field is the right one long-term.
- **`CouldNotConvertException` message shape.** Recommending "no input echo" is a contract call, but
  the five sites (`broker/vault/javatime.kt:103,109,114,119`, `broker/vault/mpdatetime.kt:55`) echo
  the value to help the caller fix their own request, and the value is the caller's own input.
  Whether the loss of that hint is acceptable has not been checked with a consumer of these 400s.
- **`NotFoundException` / `CouldNotConvertException` branches log nothing.** `ApiStatusPages.kt:65-78`
  — the 400 branch calls no logger (`logBadRequest` exists at `error_logging.kt:30` but is unused
  here). If those branches return an error id, it resolves to nothing. Not verified whether
  `logBadRequest` has any caller at all.
- **SSE boundary.** The architecture doc flags `funktor/rest/src/jvmMain/kotlin/routing.kt:164-181`
  as unproven. Not investigated in this pass — still open there, not here.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/20260720-redteam-error-disclosure.md`
