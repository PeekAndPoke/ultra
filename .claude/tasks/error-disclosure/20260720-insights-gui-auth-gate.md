# Insights GUI must be authenticated and environment-gated

**Status:** TODO
**Plan:** none — from `.claude/tasks/error-disclosure/20260720-error-response-disclosure-audit.md` (finding 2)
**Security-critical:** yes

## Spec

`funktor/insights/src/jvmMain/kotlin/gui/InsightsGui.kt` (`mount()`, lines 19-55) registers, as
plain Ktor routes with no auth rule and no environment check:

- `GET /_/insights/bar/{bucket}/{file}` (line 21)
- `GET /_/insights/details/{bucket}/{file}` (line 41)

Mounted unconditionally under the `admin.*` host in
`funktor-demo/server/src/main/kotlin/server.kt:93-102` (via `init.use(InsightsGui::class) { mount() }`,
line 100-102) — that host block has **no** `authentication { }` / `authenticate(...)` wrapper at all
(confirmed by grep across `funktor/` — only the `api.*` host, `funktor-demo/server/src/main/kotlin/api/ApiApp.kt:22-30,71`,
configures `jwtCaller`/`anonymous` and wraps its routes in `authenticate(AUTH_JWT, AUTH_ANON)`).

The default is fail-closed: `funktor/insights/src/jvmMain/kotlin/insights_module.kt:37` binds
`InsightsConfig.Disabled` (`enabled = false`,
`funktor/core/src/commonMain/kotlin/model/InsightsConfig.kt:8`), and when disabled the `Insights`
service resolves to `InsightsSlim` (`insights_module.kt:41`), whose `getRequestDetailsUri()`/
`getRequestDetailsUrl()` both return `null` (`funktor/insights/src/jvmMain/kotlin/impl/InsightsSlim.kt:8-10`).
So this is only live once someone sets `enabled=true` — exactly what one does to debug a deployed
incident.

- [ ] `InsightsGui.mount()` requires the caller to be a super-user before serving either route (see
      "How to gate" below — there is no `.authorize {}` DSL available here, and the credential-transport
      question in that section must be resolved first, not just the check itself)
- [ ] Both routes additionally refuse to serve unless the environment is recognised-development —
      written as an allow-list so unknown environments are denied (depends on
      `.claude/tasks/error-disclosure/20260720-env-classification-allowlist.md`; `AppConfig.ktor.isDevelopment` at
      `funktor/core/src/jvmMain/kotlin/config/ktor/KtorConfig.kt:40` is already exactly this
      allow-list and is already reachable from `funktor:insights` — see below)
- [ ] `detailsUri` / `detailsUrl` are omitted from API responses (`funktor/rest/src/jvmMain/kotlin/respond.kt:84-85`)
      for callers who would not be allowed to fetch them
- [ ] Denials return `404`, not `403`, so the endpoints' existence is not confirmed
- [ ] `InsightsConfig` default stays `Disabled` (`insights_module.kt:37`)
- [ ] Decide and implement (or explicitly reject with reasoning) a startup refusal when
      `enabled=true` in production (see "Startup refusal" below)

## Analysis

All line numbers below were read directly from the cited files. Items marked **OPEN** could not be
confirmed from source alone and need a decision during implementation.

### 1. Exactly what each collector captures (the "what's exposed" question)

Registered as the default collector set in `funktor/insights/src/jvmMain/kotlin/insights_module.kt:67-77`.
Real class names, verified against `funktor/insights/src/jvmMain/kotlin/collectors/*.kt`:

- **`RequestCollector`** (`RequestCollector.kt:15-23,40-55`) — method, scheme, host, port, full URI,
  **all request headers**, and **all query params**. Header redaction is partial and easy to miss:
  only the `Authorization` header is truncated (`RequestCollector.kt:47-53`, first 20 chars + `...`).
  **`Cookie` is not redacted at all**, nor is any custom auth header (`X-Api-Key`, etc.) — those are
  captured verbatim. Query params are captured verbatim too, so a token-in-URL pattern would leak in
  full.
- **`ResponseCollector`** (`ResponseCollector.kt:13-16,31-34`) — status code and **all response
  headers verbatim**, including `Set-Cookie` (session tokens the server just issued) — no redaction
  at all here.
- **`UserCollector`** (`UserCollector.kt:19-22,52-55`) — the full `UserRecord` (sealed type,
  `ultra/security/src/commonMain/kotlin/user/UserRecord.kt:9-68`: `userId`, `clientIp`, `email`,
  `desc`, `type`, plus `keyId`/`keyName` for API-key callers — no password/secret material, but real
  PII) and the full `UserPermissions` (`ultra/security/src/commonMain/kotlin/user/UserPermissions.kt:8-17`:
  `isSuperUser`, `org`, `accessibleOrgs`, `branches`, `groups`, `roles`, `permissions`) of the
  **authenticated caller who made the captured request** — i.e. reading another user's record here
  discloses that user's identity and full authorization profile.
- **`VaultCollector`** (`VaultCollector.kt:27-29,343-347`) — every DB query profiled during the
  request: AQL/query text, **bind variables**, result counts, per-phase timings, and (when triggered)
  the query's `explain` plan and a full `DatabaseGraphModel` of the repo/reference graph.
- **`KontainerCollector`** (`KontainerCollector.kt:34-38,444-465`) — the entire DI graph for that
  request's kontainer: every registered service class (`DebugInfo`), instance counts, injection
  edges, and definition/overwrite chain with source locations. Architecture/internals disclosure,
  not user data.
- **`AppConfigCollector`** (`AppConfigCollector.kt:14-29`) — `AppInfo` and the **entire `AppConfig`**
  serialized to a `Map` via Jackson `convertValue`. **OPEN**: whether `AppConfig` contains
  credentials/connection strings was not audited as part of this task — `funktor/core/src/jvmMain/kotlin/config/ktor/KtorConfig.kt:25-32`
  shows at least `KtorConfig.Security.keyStorePassword`/`privateKeyPassword` are `@JsonIgnore`'d for
  *logging*, but `AppConfigCollector` uses a *different* serialization path (Jackson `convertValue`,
  not the logging serializer) — needs an explicit check of whether that annotation is honoured here
  too, or whether DB/AWS/JWT-signing-key config leaks through this collector. Flag as a blocking
  open question for implementation, not something to assume is safe.
- **`RuntimeCollector`** (`RuntimeCollector.kt:19-28,121-149`) — JVM version, heap stats, CPU count,
  open/max file descriptors, and **the full JVM `System.getProperties()` map** — this can include
  `-D` flags passed at process launch, which not infrequently carry secrets or internal paths.
- **`LogCollector`** (`LogCollector.kt:19-44,102-109`) — every log line emitted during the request,
  captured via a request-scoped `LogAppender` — content is whatever the app logs, so any accidental
  secret-logging elsewhere in the app is re-exposed here per-request.
- **`RoutingCollector`** (`RoutingCollector.kt:13-15,34-40`) — Ktor's route-resolution trace text.
  Internals disclosure, no user data.
- **`TemplateInsightsCollector`** (`TemplateInsightsCollector.kt:14-16`) — view render time only, no
  sensitive data.

Net: enabling insights turns every captured request into a bundle containing that request's full
headers/cookies/query string, its response headers (incl. `Set-Cookie`), the authenticated caller's
identity and permissions, every DB query and bind variable it ran, and (if logged) any secret that
leaked into application logs — all reachable by whoever can hit these two routes.

### 2. Are `{bucket}/{file}` guessable/enumerable?

`InsightsFull` (`funktor/insights/src/jvmMain/kotlin/impl/InsightsFull.kt:23-43`) is bound as
`dynamic(Insights::class)` (`insights_module.kt:39-60`). `InjectionType.Dynamic` means "a singleton
that only lives within a single kontainer instance" (`ultra/kontainer/src/main/kotlin/InjectionType.kt:13-14`),
and each HTTP request gets its own kontainer instance — so `InsightsFull` is constructed once per
request, and its `date`/`dateTime` fields (`InsightsFull.kt:29-30`, `LocalDate.now()` /
`LocalDateTime.now()`) are captured at that moment. The stored path is:

```kotlin
private val filename: String = "records-$date/$dateTime.json"   // InsightsFull.kt:39
```

So `bucket = "records-$date"` (today's date, in the clear — trivially guessable, same for every
request that day) and `file = "$dateTime.json"` (a wall-clock timestamp with **no random
component**). This is not a capability token: it is a plain timestamp. An attacker who has any way
to narrow a victim's request to within a second or so (e.g. from `ApiResponse.Insights.ts`,
`respond.kt:78`, which is epoch-seconds and — per finding 3 below — is present on every enriched API
response; or from the HTTP `Date` response header) is then brute-forcing only the sub-second part of
`LocalDateTime.toString()`, a small, cheap-to-enumerate search space, against an endpoint that has no
visible rate limiting (`InsightsGui.kt` is a bare Ktor route; no rate-limit plugin was found applied
to it). **OPEN**: exact JVM clock resolution (and therefore exact brute-force cost) was not measured;
treat "small, brute-forceable" as the working assumption regardless of the precise number.

`InsightsFileRepository` (`InsightsFileRepository.kt:6`) extends `FileSystemRepository`
(`funktor/cluster/src/jvmMain/kotlin/depot/repos/fs/FileSystemRepository.kt`), whose `getFile`/
`getContent` (lines 111-121, 135-145) do validate against path traversal (`validateName()` rejects
`..`, lines 15-21) but do **not** check any caller identity — anyone who can produce a valid
`{bucket}/{file}` string is served the file.

`InsightsDataLoader.loadGuiData` (`InsightsDataLoader.kt:17-60`) additionally computes `nextFile`/
`previousFile` siblings (lines 27-28, by listing the whole day's bucket and sorting by
`lastModifiedAt` — i.e. sibling files belong to **every** request that day, not just the caller's
own) and puts them on `InsightsGuiData` (`gui/InsightsGuiData.kt:19-20`). **As of today this is dead
data** — grepping the whole `funktor/insights` module for `nextFile`/`previousFile` shows they are
only ever assigned, never read by `InsightsGuiTemplate.render()` or anywhere else — so there is
currently no in-app "click next/previous" pagination that would let a caller walk from their own
record into another user's. Flag this explicitly so a future GUI enhancement doesn't wire it up
without also gating it: it is the one place enumeration-via-listing already has plumbing.

Conclusion: identifiers are not guessable at random, but they are **not secrets** either — they're
timestamps with a public prefix (the date) and a narrow, un-rate-limited suffix. Do not rely on
unguessability as the access control; that's what the auth gate below is for.

### 3. Verifying the `detailsUri`/`detailsUrl` exposure claim

`funktor/rest/src/jvmMain/kotlin/respond.kt:72-88`:

```kotlin
fun <T : Any?> ApplicationCall.enrichApiResponseWithInsights(response: ApiResponse<T>): ApiResponse<T> {
    val metrics = kontainerOrNull?.getOrNull(RequestMetricsProvider::class)
    return response.withInsights(
        ApiResponse.Insights(
            ...
            detailsUri = metrics?.getRequestDetailsUri()?.encodeUriComponent(),   // line 84
            detailsUrl = metrics?.getRequestDetailsUrl(),                         // line 85
        )
    )
}
```

Called from `apiRespond()` (`respond.kt:25-43`) for **every** `ApiResponse<*>` returned through that
function — unconditionally, with no environment check and no caller-identity check anywhere in this
path. The only actual gate is whether `metrics?.getRequestDetailsUri()` is non-null:
`InsightsSlim` (insights disabled) returns `null` for both (`InsightsSlim.kt:8-10`); `InsightsFull`
(insights enabled) **always** returns a non-null value (`InsightsFull.kt:41-43`), regardless of who
is calling or what environment the app is running in. Claim confirmed as written in the original
task: once `enabled=true`, every API response — to every caller, anonymous or not, dev or prod —
carries a live link to that response's own insights record.

(Separately, `apiRespondUnauthorized`, `respond.kt:48-69`, already gates its extra `withInfo(...)`
detail on `appConfig.ktor.isNotProduction` — the same shape of gate this task needs, just applied to
a different field. That gate is exactly the one flagged as unsound in
`.claude/tasks/error-disclosure/20260720-env-classification-allowlist.md`, so don't copy its `isNotProduction` form —
use the allow-list `isDevelopment` instead, see below.)

### 4. How to actually gate a non-`ApiRoute` (the crux)

`InsightsGui.mount()` routes are registered via `funktor:core`'s typed-route `get()` helper
(`funktor/core/src/jvmMain/kotlin/broker/route.kt:32-34`), which is a thin wrapper over plain Ktor
`route(pattern, HttpMethod.Get) { handle(...) }` — it does typed-parameter conversion and nothing
else. There is no `.authorize {}` DSL, no `AuthRule`, at this layer.

**Gradle dependency check** (`funktor/insights/build.gradle.kts:65-77`): `funktor:insights` depends
on `funktor:core`, `funktor:cluster`, `funktor:staticweb` — **not** `funktor:rest`. So
`AuthRule`/`AuthRuleBuilder`/`Caller`/`currentUserProvider()` (all in
`funktor/rest/src/jvmMain/kotlin/auth/*.kt`) are not on `funktor:insights`'s compile classpath at
all. `funktor:cluster` depends on `funktor:rest` too, but only via `implementation(...)`
(`funktor/cluster/build.gradle.kts:40`), which is not transitively exposed — so that path doesn't
help either. The `.authorize {}` DSL genuinely cannot be reached from inside `funktor:insights` code
without adding a new Gradle dependency (see options below).

**What *is* already reachable from `funktor:insights` today:**

- `UserPermissions`/`UserProvider` — `ultra:security`, pulled in transitively because `funktor:core`
  depends on it as `api` (`funktor/core/build.gradle.kts:64`). Already used by `UserCollector`
  (`UserCollector.kt:9-16`), so this is proven to compile and resolve from this module.
- `JwtGenerator` (`ultra/security/src/jvmMain/kotlin/jwt/JwtGenerator.kt:1,13`) — also lives in
  `ultra:security`, not `funktor:rest`, so it's reachable the same way even though nothing in
  `funktor:insights` uses it yet. It exposes `tryVerify(token): Payload?` (line 36),
  `extractPermissions(payload): UserPermissions` (line 65), and `extractUser(clientIp, payload): User`
  (line 70) — everything needed to check `isSuperUser` from a raw bearer token, without any
  `funktor:rest` dependency. The *instance* is registered as a kontainer singleton by
  `FunktorRestBuilder.jwt()` (`funktor/rest/src/jvmMain/kotlin/index_jvm.kt:81-95`), which
  `funktor-demo`'s single shared blueprint installs via `rest = { jwt() }`
  (`funktor-demo/server/src/main/kotlin/kontainer.kt:53-55`) — that blueprint backs **both** the
  `api.*` and `admin.*` hosts (`installApiKontainer`/`installWwwKontainer` both call
  `app.kontainers.create { ... }` off the same `app`), so `kontainer.getOrNull(JwtGenerator::class)`
  resolves at runtime inside `InsightsGui` even though `funktor:insights` never declared a
  compile-time dependency on `funktor:rest` — it only needed the *type* (`JwtGenerator`, from
  `ultra:security`), and the *instance* comes from whatever the app wired up.
- `AppConfig` (already used, `InsightsGui.kt:9,32`) → `AppConfig.ktor.isDevelopment`
  (`KtorConfig.kt:40`) is the allow-list environment check this task needs, already reachable.

**Two problems that mean "just check `isSuperUser` in the handler" is not sufficient by itself:**

1. **The `UserProvider` bound in the admin/www kontainer is always anonymous today.**
   `ultra/security/src/jvmMain/kotlin/index_jvm.kt:16` binds the default:
   `dynamic(UserProvider::class) { UserProvider.anonymous }`. `installApiKontainer`
   (`funktor-demo/server/src/main/kotlin/kontainer.kt:36-47`) overrides this with
   `with { call.currentUserProvider() }` (line 40) so API requests see the real caller.
   `installWwwKontainer` (`kontainer.kt:25-34`) — the one used for the `admin.*` host, where
   `InsightsGui` is mounted — **does not** do this override. So today, anything in the admin/www
   kontainer that asks "who is the current user" (including `UserCollector`, and including any naive
   `kontainer.get(UserProvider::class)` gate added to `InsightsGui`) always gets
   `UserPermissions.anonymous` (`isSuperUser = false`), regardless of who's actually browsing. A
   naive fix is safe (fails closed) but non-functional — nobody would ever pass the gate.
2. **There is no credential transport to these routes at all, even if the kontainer wiring above is
   fixed.** The `admin.*` host has no `authentication { }` block and no `authenticate(...)` route
   wrapper (only `api.*` has this, `ApiApp.kt:22-30,71`), so Ktor's `call.principal<Caller>()` is
   never populated for these routes even in principle. And even if it were, the admin SPA does not
   use cookies for auth anywhere (`grep` across `funktor/auth/src/jsMain/kotlin/AuthState.kt` and the
   rest of the admin frontend found zero cookie usage) — the JWT is attached to `fetch`/`XHR` calls
   by a request interceptor (the deleted `SetBearerRequestInterceptor.kt`, currently being replaced
   per the in-flight `ultra/remote` refactor on this branch — see `.claude/tasks/20260719-ktor-client-unification.md`),
   **not** to plain browser navigations. And that's exactly how these two routes are loaded:
   - The bar is fetched by a same-origin `$.ajax` call with no custom headers
     (`funktor/insights/src/jvmMain/resources/assets/funktor/insights/bar.js:15`), triggered by a
     `.insights-bar-placeholder` div that `InsightsRenderer.render()` embeds into *server-rendered*
     HTML pages (`funktor/insights/src/jvmMain/kotlin/gui/InsightsRenderer.kt:30-35`) whenever
     `insights.config.enabled` — unconditional on the viewer's identity, and it always points at the
     *viewer's own* `getRequestDetailsUri()`.
   - The "details" link is a plain `<a target="_blank">` (`InsightsBarTemplate.kt:46`) — a full page
     navigation, which also carries no bearer header.
   So a bearer-token check (`Authorization` header, `JwtGenerator.tryVerify`) would **always fail**
   for a real developer's browser tab, not just for an attacker — there is currently no way for these
   requests to prove who is making them.

**Recommended design** (needs sign-off before implementing, this is the open design decision):

- Short term / minimum viable, no new transport needed: gate on **environment only**
  (`AppConfig.ktor.isDevelopment`, see item 5) — acceptable because these routes are meant for local
  debugging, and this alone removes the anonymous-prod-exposure the finding is about. Ship this
  first.
- To add real per-caller authorization on top (recommended before this is usable in any shared
  dev/staging environment with multiple developers), fix the transport gap, not just the check.
  Two candidate designs, pick one during implementation — do not silently default to whichever is
  easiest:
  1. **Signed capability token embedded at render time.** Wherever `getRequestDetailsUri()`/
     `getRequestDetailsUrl()` are consumed to build a link (`InsightsRenderer.kt:32`,
     `respond.kt:84-85`), the code doing so *does* know the current caller (it's inside
     `funktor:rest`/app code, which has `Caller`/`currentUserProvider()`). Append a short-lived HMAC-
     or JWT-signed token scoped to that specific `bucket/file`, generated only when the caller is
     already a superuser, and verify it in `InsightsGui` using `JwtGenerator` (see above — reachable
     without a new Gradle dependency). This also naturally solves "suppress `detailsUri`/`detailsUrl`
     for callers who can't use them" (next section) — for non-superusers, don't mint the token, and
     `InsightsGui` treats a request with no/invalid token as unauthenticated.
  2. **Cookie-based admin session.** Bigger scope, but matches the direction hinted at by this
     branch's own git log ("select-org, sign in") — if the admin app moves to a real session cookie
     for its own login state, `InsightsGui` could read that cookie directly and check permissions the
     same way `UserCollector` already does for `UserProvider`. Larger change; treat as **OPEN** and
     out of scope unless a reviewer decides the org/session work already covers it.
  Either way, `installWwwKontainer` (`kontainer.kt:25-34`) needs the same `with { call.currentUserProvider() }`
  override `installApiKontainer` has (`kontainer.kt:40`) if any part of the fix relies on
  `UserProvider` inside the www kontainer being accurate — currently it silently isn't.

Concrete sketch for the environment-only v1 gate (no new dependency, no transport change):

```kotlin
// InsightsGui.kt
class InsightsGui(
    private val routes: InsightsGuiRoutes,
    private val insights: InsightsDataLoader,
) {
    fun Route.mount() {
        get(routes.bar) { file -> serveIfAllowed(file) { guiData -> renderBar(file, guiData) } }
        get(routes.details) { file -> serveIfAllowed(file) { guiData -> renderDetails(guiData) } }
    }

    private suspend fun RoutingContext.serveIfAllowed(
        file: InsightsGuiRoutes.PathParam,
        respondWith: suspend (InsightsGuiData) -> Unit,
    ) {
        val appConfig = kontainer.get(AppConfig::class)

        // Environment gate — allow-list, not negation (see env-classification-allowlist task)
        if (!appConfig.ktor.isDevelopment) {
            call.respond(HttpStatusCode.NotFound)
            return
        }

        // TODO(auth): per-caller check goes here once the transport question above is resolved.
        // Until then this endpoint is dev-environment-only, which is the primary control.

        val guiData = insights.loadGuiData(file.path)

        if (guiData != null) respondWith(guiData) else call.respond(HttpStatusCode.NotFound)
    }
}
```

### 5. Full fix design

- **Auth gate**: see item 4 — ship the environment gate now; per-caller `isSuperUser` gate is
  blocked on resolving the credential-transport design.
- **Environment gate**: `AppConfig.ktor.isDevelopment` (`KtorConfig.kt:40`), already an allow-list
  (`isLocalDev || isTest || isQa`), fails closed on unrecognised environment strings. Do **not** use
  `isNotProduction` (negation, unsound per `.claude/tasks/error-disclosure/20260720-env-classification-allowlist.md`).
- **Suppress `detailsUri`/`detailsUrl`**: in `enrichApiResponseWithInsights`
  (`respond.kt:72-88`), only populate them when the environment is development (mirrors the pattern
  already used for `apiRespondUnauthorized`'s extra detail at `respond.kt:56-66`, but on
  `isDevelopment` not `isNotProduction`). If/when the per-caller design lands (item 4, option 1),
  additionally require the caller to be a superuser before minting a token-bearing URI.
- **404 not 403**: the sketch above already does this — every denial path returns
  `HttpStatusCode.NotFound`, same as the existing "file not found" path (`InsightsGui.kt:37,52`), so
  a prober can't distinguish "wrong file" from "not allowed to look."
- **`InsightsConfig` default stays `Disabled`**: no change needed — already true
  (`insights_module.kt:37`), just don't regress it.
- **Same treatment for other non-`ApiRoute` GUI surfaces**: re-checked
  `funktor/insights/src/jvmMain/kotlin/routing.kt` — it only installs the timing/collector plugin
  (`instrumentWithInsights`, lines 18-61), no additional HTTP routes. The two routes in
  `InsightsGui.kt` are the only ones. Nothing else to re-scope.

### 6. Startup refusal in production — recommendation

Recommend **yes**, as defence in depth, using the existing lifecycle-hook mechanism rather than a new
mechanism: `AppLifeCycleHooks.OnAppStarting` (`funktor/core/src/jvmMain/kotlin/lifecycle/AppLifeCycleHooks.kt`)
+ `AppStartException` (`funktor/core/src/jvmMain/kotlin/lifecycle/AppStartException.kt`) is exactly
the pattern `ValidateRoutesOnAppStarting` already uses in `funktor:rest`
(`funktor/rest/src/jvmMain/kotlin/ValidateRoutesOnAppStarting.kt:13-40`, throws `AppStartException`
when validation fails, executes `ExecutionOrder.VeryEarly`). Both
`AppLifeCycleHooks`/`AppStartException` live in `funktor:core`, so a
`RefuseInsightsInProduction : AppLifeCycleHooks.OnAppStarting` can be added inside
`funktor:insights` itself with no new dependency:

```kotlin
class RefuseInsightsInProduction(
    private val config: InsightsConfig,
    private val appConfig: AppConfig,
) : AppLifeCycleHooks.OnAppStarting {
    override val executionOrder = AppLifeCycleHooks.ExecutionOrder.VeryEarly

    override suspend fun onAppStarting(application: Application) {
        if (config.enabled && !appConfig.ktor.isDevelopment) {
            throw AppStartException(
                "InsightsConfig.enabled=true is not allowed outside development environments " +
                    "(current environment: '${appConfig.ktor.deployment.environment}'). " +
                    "Insights exposes request/response headers, DB queries, and caller identity."
            )
        }
    }
}
```

Register it in `Funktor_Insights` (`insights_module.kt:36-88`) alongside the other collectors. This
makes the misconfiguration loud (app refuses to boot) instead of silently serving the routes to
whoever asks. **OPEN**: confirm whether any existing deployment intentionally runs `enabled=true` in
a non-dev environment today (e.g. a monitored staging) — if so this needs a documented escape hatch,
not a hard refusal.

## Implementation notes

- Ship in two steps if useful: (1) environment gate + 404s + suppressed `detailsUri`/`detailsUrl` +
  startup refusal — all buildable today with existing primitives, no new dependency, no transport
  design needed; (2) per-caller superuser gate, blocked on picking one of the two transport designs
  in item 4 and getting sign-off, since it's a bigger change (new signed-token scheme or a new
  cookie-based session).
- Do not add `funktor:rest` as a dependency of `funktor:insights` to reach `AuthRule`/`Caller` — the
  primitives needed (`UserPermissions`, `JwtGenerator`) are already reachable via `ultra:security`
  without it, and pulling in `funktor:rest` would invert the current layering (`funktor:rest`
  already depends on nothing insights-related, keep it that way).
- `funktor/rest/src/jvmMain/kotlin/ValidateRoutesOnAppStarting.kt` is the existing reference
  implementation for an `AppLifeCycleHooks.OnAppStarting` + `AppStartException` check.

## Test evidence

- [ ] Route tests: anonymous in dev → 200 (v1) / 404 once per-caller gate lands and non-superuser;
      any caller in a non-dev/unrecognised environment string → 404
- [ ] Test that `detailsUri` / `detailsUrl` are absent from responses outside development
      environments (and, once the per-caller design lands, for non-superuser callers)
- [ ] Test that the routes refuse to serve in production and on an unknown environment string
      (e.g. `"staging"`, `""`, `"prd"` — see env-classification-allowlist task for the exact list)
- [ ] Startup refusal test: `InsightsConfig.enabled = true` + non-development environment →
      `AppStartException` at boot
- [ ] End-to-end via `AppSpec`/`AppUnderTest` with insights enabled
- [ ] Full test command(s) run + green: `./gradlew :funktor:insights:jvmTest :funktor:rest:jvmTest`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/error-disclosure/20260720-redteam-error-disclosure.md`
