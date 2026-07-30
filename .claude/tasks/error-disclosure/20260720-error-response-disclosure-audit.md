# Error-response information disclosure — audit findings

Status: SPLIT INTO TASKS — this file is now the findings index; no work happens here
Date: 2026-07-20
Origin: follow-up to the `ApiStatusPages` fail-open fix in
`.claude/tasks-archive/2026-07/20260720-vault-hook-scope.md` — "are there other status pages with the same
issue?"

## Where the work lives

| Finding | Task |
|---|---|
| (umbrella) exception design | `20260720-exception-disclosure-architecture.md` |
| 1 + 2 — production returns `cause.message` (full AQL) | `20260720-error-messages-generic-in-production.md` |
| 3 — `isProduction` allow-list fails open on unknown env | `20260720-env-classification-allowlist.md` |
| 4 — fixtures install on unknown env | `20260720-fixtures-env-gate.md` |
| 5 — auth-rule descriptions leak | folded into `20260720-env-classification-allowlist.md` |
| 6 — routes default to public | `20260720-route-auth-default-deny.md` |
| 7 — auth error account enumeration | `20260720-auth-error-account-enumeration.md` |
| attack scenarios | `20260720-redteam-error-disclosure.md` |

## Short answer to the original question

**There is exactly one error-handling installation in the repo**: `ApiStatusPages`
(`funktor/rest/src/jvmMain/kotlin/ApiStatusPages.kt`). No `install(StatusPages`, no `exception<`,
no other `on(CallFailed)` anywhere. The other two `createRouteScopedPlugin` sites
(`funktor/core/.../ktor_call.kt:24`, `funktor/insights/.../routing.kt:31`) only hook `CallSetup` /
`ResponseSent` and never write an error response. No GraphQL. No server-side WebSocket routes.

So: no second status page with the same bug. But the audit surfaced the same *class* of bug
elsewhere, plus one worse issue.

## Findings

Ordered by priority. All verified against the code.

### 1. HIGH — production still returns `cause.message`, and Karango puts the whole AQL query in it

`funktor/rest/src/jvmMain/kotlin/ApiStatusPages.kt` (production branch → `cause.message ?: ""`)
combined with `karango/core/src/main/kotlin/vault/KarangoDriver.kt:130-138`:

```kotlin
throw KarangoQueryException(
    message = "Error while querying '${e.message}':\n\n${query.query}\nwith params [${vars.keys.joinToString(", ")}]",
    ...
)
```

The null-config fix does **not** cover this: even in correctly-configured production, an uncaught
`KarangoQueryException` returns the full AQL source, collection names and bind-variable names to the
client. Reachable from any endpoint that touches Arango, including unauthenticated ones.

Fix direction: in production return a fixed generic string plus a correlation id, and log the cause
against that id. If specific messages are wanted, whitelist exception types rather than passing
`Throwable.message` through blanket.

Same channel also leaks `FileNotFoundException` paths, `ConnectException` host:port, kontainer
service FQCNs and Slumber field paths.

### 2. HIGH — Insights GUI has no auth gate at all

`funktor/insights/src/jvmMain/kotlin/gui/InsightsGui.kt:20-56` registers
`GET /_/insights/bar/{bucket}/{file}` and `GET /_/insights/details/{bucket}/{file}` as plain routes
— no auth rule, no environment check. Mounted in `funktor-demo/server/src/main/kotlin/server.kt:100`.

Default is fail-closed (`insights_module.kt:37` binds `InsightsConfig.Disabled`), so this is only
live once someone sets `enabled=true` — which is precisely what one does to debug a deployed
incident. When enabled, the payload includes the whole `AppConfig`, every registered kontainer
service, per-request logs, the captured request's authenticated user, executed AQL with bind vars,
and request/response headers.

Worse: `funktor/rest/src/jvmMain/kotlin/respond.kt:84-85` puts `detailsUri` / `detailsUrl` into
**every** API response, so an attacker does not even have to guess the bucket/file path.

Fix direction: require `isSuperUser()` inside `InsightsGui.mount()`; additionally refuse to serve
unless `isDevelopment` (allow-list, see finding 3); suppress `detailsUri`/`detailsUrl` for
unauthenticated callers.

### 3. MEDIUM — `isProduction` is an allow-list, so unknown environments fail open

`funktor/core/src/jvmMain/kotlin/config/ktor/KtorConfig.kt:42-44`:

```kotlin
val isProduction: Boolean get() = deployment.environment.lowercase() in listOf("live", "prod", "production")
val isNotProduction: Boolean get() = !isProduction
```

Anything outside the allow-list — `staging`, `prd`, `production-eu`, `live-de`, a trailing space, or
an unresolved `${ENV}` from a templating bug — is classified non-production and therefore gets the
permissive branch everywhere `isNotProduction` is read.

This is the root cause behind findings 4 and 5 as well; fixing it fixes all three.

Fix direction: define `isNotProduction` from its own allow-list (`dev`/`test`/`local`/`qa*`) instead
of negating `isProduction`, so an unrecognised environment is treated as production. Better: parse
`environment` into a sealed type at config load and fail startup on an unrecognised value.

NOTE: the current tests in `funktor/core/.../KtorConfigSpec.kt` and
`funktor/rest/.../ApiStatusPagesSpec.kt` deliberately assert today's behaviour — that `staging` is
non-production and so may expose stack traces. If finding 3 is fixed, **those assertions must be
inverted**; they encode the current semantics, not a desired invariant.

### 4. MEDIUM — fixtures are installed on unknown environments

`funktor/core/src/jvmMain/kotlin/fixtures/fixtures_module.kt:16-26` — `if (config.ktor.isProduction)
{ Null } else { SimpleFixtureInstaller + InstallFixturesCliCommand + ListFixturesCliCommand }`.

Unknown environment → else branch → destructive seed fixtures become loadable. Consequence is worse
than disclosure. Fix: invert to `if (config.ktor.isDevelopment) { install } else { Null }`.

### 5. LOW — auth-rule descriptions leak on unknown environments

`funktor/rest/src/jvmMain/kotlin/respond.kt:57-66` returns `"Failed auth rules: …"` with rule
descriptions when `isNotProduction`. Polarity is correct and it fails closed on a null boolean, but
it inherits finding 3: on `environment=staging` every 401 maps the permission model to an
anonymous caller. Secondary: `appConfig` here throws when no kontainer is on the call, turning a
clean 401 into a 500. Consider `kontainerOrNull?.getOrNull(AppConfig::class)?.ktor?.isNotProduction
== true` for symmetry with the `ApiStatusPages` fix.

### 6. MEDIUM (framework default) — routes with no `.authorize {}` are public

`funktor/rest/src/jvmMain/kotlin/ApiRoute.kt:84,144,205` — `authRules` defaults to `emptyList()`,
and `checkAccess` treats "no failed rules" as success, so omitting `.authorize { … }` silently
publishes an endpoint. `ValidateRoutesOnAppStarting` validates parameters only, not auth coverage.

Fix direction: `public()` already exists as an explicit opt-in marker, so a startup check that fails
on `authRules.isEmpty()` would be cheap and non-breaking for intentionally public routes.

### 7. LOW — auth API echoes `AuthError.message` to unauthenticated callers

`funktor/auth/src/jvmMain/kotlin/api/AuthApi.kt:57,79,106,127,148,170,193,216,235` —
`.withInfo(e.message ?: "")`, not environment-gated. Observed messages are mostly generic, but
`"User already exists"` on the public sign-up route is an account-enumeration oracle. Fix: map
`AuthError` to a closed set of client-safe codes on `public()` routes.

## Cleared (explicit negatives)

- No second StatusPages / `on(CallFailed)` / `exception<` handler anywhere.
- No GraphQL; no server-side WebSocket routes (only the test harness).
- SSE (`funktor/rest/.../routing.kt:167-183`) checks auth before the handler and cannot leak a body
  after headers flush.
- All introspection/admin APIs are `isSuperUser()`-gated, not environment-gated — `IntrospectionApi`,
  `LoggingApi`, `GlobalLocksApi`, `DepotApi`, `WorkersApi`, `VaultApi`, `BackgroundJobsApi`,
  `RandomCacheStorageApi`, `RandomDataStorageApi`. This is the right design.
- ~20 `stackTraceToString()` sites verified as log-only (listed in the audit; none reach a response).
- `ApiStatusPages` no longer has a nullable-chain fail-open, and it was the only such pattern in the
  repo. No `!isProduction` negations on nullable chains anywhere.
- Background-job / worker / log records do persist stack traces, but are served only through
  superuser-gated APIs.

## Suggested sequencing

Finding 3 first — it is a two-line change that closes 3, 4 and 5 at once (and requires inverting
the two test assertions noted above). Then 1, then 2. Findings 6 and 7 are independent hardening.
