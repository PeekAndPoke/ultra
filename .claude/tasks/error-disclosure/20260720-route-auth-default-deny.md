# Routes without auth rules must not silently be public

**Status:** TODO
**Plan:** none — from `.claude/tasks/error-disclosure/20260720-error-response-disclosure-audit.md` (finding 6)
**Security-critical:** yes

## Spec

### 1. The mechanism — confirmed

`ApiRoute` is a sealed class with **five** subclasses, not three. Every one of them defaults
`authRules` to `emptyList()`:

| Subclass | default `authRules` | `checkAccess` |
|---|---|---|
| `ApiRoute.Plain` | `funktor/rest/src/jvmMain/kotlin/ApiRoute.kt:84` | `ApiRoute.kt:97-99` |
| `ApiRoute.Sse` | `ApiRoute.kt:144` | `ApiRoute.kt:159-161` |
| `ApiRoute.WithParams` | `ApiRoute.kt:205` | `ApiRoute.kt:218-220` |
| `ApiRoute.WithBody` | `ApiRoute.kt:272` | `ApiRoute.kt:285-287` |
| `ApiRoute.WithBodyAndParams` | `ApiRoute.kt:334` | `ApiRoute.kt:347-349` |

(The original spec text listed only `84,144,205` / `97-99,159-161,218-220`. `WithBody` and
`WithBodyAndParams` — i.e. every POST/PUT/DELETE-with-body route — have the same defect and must be
covered by the fix.)

**Where the allow decision is made.** Three hops, all verified:

1. `funktor/rest/src/jvmMain/kotlin/ApiRoute.kt:97-99` (and the four siblings):
   `AuthResult(failedRules = authRules.filter { !it.check(ctx) })`. With `authRules == emptyList()`,
   `filter` on an empty list returns an empty list. No rule is consulted because there is no rule.
2. `funktor/rest/src/jvmMain/kotlin/auth/AuthResult.kt:7`: `fun isSuccess() = failedRules.isEmpty()`.
   Empty ⇒ `true`. **This line is the actual allow decision** — it conflates "no rule objected" with
   "a rule approved".
3. `funktor/rest/src/jvmMain/kotlin/routing.kt:38-41` (`handlePlain`), and identically at
   `routing.kt:63-66`, `routing.kt:87-90`, `routing.kt:118-121`, `routing.kt:153-156`,
   `routing.kt:179-182` (SSE): `when (authResult.isSuccess()) { true -> route.handler(...) ... }`.
   The handler runs.

The routes are mounted inside `authenticate(AUTH_JWT, AUTH_ANON)`
(`funktor-demo/server/src/main/kotlin/api/ApiApp.kt:71`), so an anonymous caller is admitted to the
route by Ktor and then waved through by step 2. No exception, no log, no CI signal.

**Second-order effect (not in the original finding).** `ApiRoute.estimateAccess`
(`ApiRoute.kt:71-75`) folds over `authRules` starting from `ApiAccessLevel.Granted`. An empty rule
list therefore folds to `Granted`, so the introspection API-access matrix
(`funktor/inspect/src/jvmMain/kotlin/introspection/services/ApiAccessDescriptor.kt:26,75`) reports a
rule-less route as *granted to everyone* rather than flagging it. The one tool that could have
surfaced this agrees with the bug.

**`public()`** is `funktor/rest/src/jvmMain/kotlin/auth/AuthRuleBuilder.kt:33-38` — an `AuthRule`
whose check is `{ true }`, described `"Public to everyone"`, estimating `ApiAccessLevel.Granted`. It
is behaviourally identical to declaring nothing; its entire value is that it is *written down*. That
is exactly what makes enforcement cheap: intentionally-public routes already say so.

### 2. Audit of mounted routes — done, result is CLEAN

Method: every `ApiRoutes` subclass under `funktor/` and `funktor-demo/` (18 classes), every `mount`
call site, checked for an `.authorize { … }` in the same builder chain and the first rule inside it
read. **83 routes found, 83 declare rules, 0 rely on the empty default.**

Registration paths were checked for completeness first, so the `mount` scan is exhaustive:
`ApiRoutes` collects routes only via `addRoute` (`ApiRoutes.kt:159-161`), called from the `mount`
overloads (`ApiRoutes.kt:53-160`) and from `route { }` (`ApiRoutes.kt:47-49`). Grep found **zero**
uses of `route { }` and **zero** direct `addRoute(` calls outside `ApiRoutes.kt`, so `mount` is the
only path in use.

**Answer to the question that gated the design: no, the demo app has no route relying on the empty
default. A startup check would not break it today.** It would start green on first run.

| Route group / class                                                              | Route (file:line)                                                                                                                                                                                                                   | Rules declared                                                          |
|----------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `AuthLoginApi` (`funktor/auth/.../api/AuthApi.kt`)                               | `getRealm` :23                                                                                                                                                                                                                      | `public()`                                                              |
|                                                                                  | `signIn` :40                                                                                                                                                                                                                        | `public()`                                                              |
|                                                                                  | `selectOrg` :62                                                                                                                                                                                                                     | `public()` (comment at :68 notes the selection token is the credential) |
|                                                                                  | `setPassword` :84                                                                                                                                                                                                                   | `authenticated()`                                                       |
|                                                                                  | `signUp` :111                                                                                                                                                                                                                       | `public()`                                                              |
|                                                                                  | `activateAccount` :132                                                                                                                                                                                                              | `public()`                                                              |
|                                                                                  | `recoverAccountInitPasswordReset` :153                                                                                                                                                                                              | `public()`                                                              |
|                                                                                  | `RecoverAccountValidatePasswordResetToken` :176                                                                                                                                                                                     | `public()`                                                              |
|                                                                                  | `RecoverAccountSetPasswordWithToken` :199                                                                                                                                                                                           | `public()`                                                              |
|                                                                                  | `refreshToken` :221                                                                                                                                                                                                                 | `authenticated()`                                                       |
|                                                                                  | `getMyApiAccess` :240                                                                                                                                                                                                               | `authenticated()`                                                       |
| `OrgsApi` (`funktor/saas/.../api/OrgsApi.kt`)                                    | `list` :19, `get` :31, `create` :45, `update` :83                                                                                                                                                                                   | `isSuperUser()` (all 4)                                                 |
| `LoggingApi` (`funktor/logging/.../api/LoggingApi.kt`)                           | `list` :33, `get` :49, `execBulkAction` :63, `execAction` :80                                                                                                                                                                       | `isSuperUser()` (all 4)                                                 |
| `IntrospectionApi` (`funktor/inspect/.../introspection/api/IntrospectionApi.kt`) | `getLifecycleHooks` :22, `getConfigInfo` :60, `getCliCommands` :82, `getFixtures` :105, `getRepairs` :127, `getAllEndpoints` :141, `getAuthRealms` :169, `validatePassword` :196, `getAppLifecycle` :218, `getApiAccessMatrix` :240 | `isSuperUser()` (all 10)                                                |
| `BackgroundJobsApi` (`funktor/cluster/.../backgroundjobs/api/`)                  | `listQueued` :34, `getQueued` :55, `listArchived` :71, `getArchived` :93                                                                                                                                                            | `isSuperUser()` (all 4)                                                 |
| `DepotApi` (`funktor/cluster/.../depot/api/`)                                    | `listRepositories` :18, `browse` :34                                                                                                                                                                                                | `isSuperUser()`                                                         |
| `GlobalLocksApi` (`funktor/cluster/.../locks/api/`)                              | `listServerBeacons` :15, `listGlobalLocks` :39                                                                                                                                                                                      | `isSuperUser()`                                                         |
| `RandomCacheStorageApi` (`funktor/cluster/.../storage/api/`)                     | `list` :21, `get` :43                                                                                                                                                                                                               | `isSuperUser()`                                                         |
| `RandomDataStorageApi` (`funktor/cluster/.../storage/api/`)                      | `list` :20, `get` :43                                                                                                                                                                                                               | `isSuperUser()`                                                         |
| `VaultApi` (`funktor/cluster/.../vault/api/`)                                    | `listRepositories` :13                                                                                                                                                                                                              | `isSuperUser()`                                                         |
| `WorkersApi` (`funktor/cluster/.../workers/api/`)                                | `list` :16, `get` :30                                                                                                                                                                                                               | `isSuperUser()`                                                         |
| `FunktorConfApi` (`funktor-demo/server/.../api/funktorconf/`)                    | `listEvents` :28, `getEvent` :41, `listSpeakers` :127, `getSpeaker` :140, `listAttendees` :224, `getAttendee` :237                                                                                                                  | `public()`                                                              |
|                                                                                  | `createEvent` :56, `updateEvent` :80, `deleteEvent` :107, `createSpeaker` :155, `updateSpeaker` :178, `deleteSpeaker` :204, `createAttendee` :252, `updateAttendee` :274, `deleteAttendee` :299                                     | `isSuperUser()`                                                         |
| `AuthShowcaseApi` (`funktor-demo/server/.../api/showcase/`)                      | `getAuthRuleChecks` :13                                                                                                                                                                                                             | `public()`                                                              |
| `ClusterShowcaseApi` (same dir)                                                  | `getQueuedJobs` :55, `getArchivedJobs` :79, `getDepotRepos` :105, `getDepotFiles` :127, `getStorageEntries` :173, `getActiveLocks` :223, `getWorkers` :271                                                                          | `public()`                                                              |
|                                                                                  | `queueJob` :33, `uploadToDepot` :151, `saveStorageEntry` :198, `acquireLock` :245                                                                                                                                                   | `isSuperUser()`                                                         |
| `CoreShowcaseApi` (same dir)                                                     | `postRetryDemo` :14                                                                                                                                                                                                                 | `public()`                                                              |
| `MessagingShowcaseApi` (same dir)                                                | `getSentMessages` :49, `getEmailSenderInfo` :77                                                                                                                                                                                     | `public()`                                                              |
|                                                                                  | `sendTestEmail` :20                                                                                                                                                                                                                 | `isSuperUser()`                                                         |
| `RestShowcaseApi` (same dir)                                                     | `getPlain` :21, `getEcho` :40, `postTransform` :59, `putItem` :85                                                                                                                                                                   | `public()`                                                              |
| `SseShowcaseApi` (same dir)                                                      | `sseClock` :18, `sseMetrics` :38                                                                                                                                                                                                    | `public()`                                                              |

**Routes with NONE: none.** The finding stands as a framework-default defect and a downstream-app
risk, not as a live vulnerability in this repo — which upgrades the fix from "urgent remediation"
to "cheap, zero-migration hardening we should land while it is still free". The window closes the
moment someone adds route 84.

Out of scope of any registry-based check: the demo mounts raw Ktor handlers outside `ApiRoutes` —
`/_/ping` (`ApiApp.kt:~30-57`) and `/_/appinfo` (`ApiApp.kt:59-63`) — which are unauthenticated by
construction and invisible to `ApiRoute` machinery. Not a regression, but note it so the check is
not mistaken for total coverage.

### 3. Route registry at startup — confirmed walkable

A startup validator **is** implementable as described. The registry is the same object graph the
mounting code uses:

- `ApiFeature.getRouteGroups(): List<ApiRoutes>` — `funktor/rest/src/jvmMain/kotlin/ApiFeature.kt:17`
- `ApiRoutes.all: List<ApiRoute<*>>` — `funktor/rest/src/jvmMain/kotlin/ApiRoutes.kt:43`
- `ValidateRoutesOnAppStarting` already walks exactly this —
  `funktor/rest/src/jvmMain/kotlin/ValidateRoutesOnAppStarting.kt:22-33` — via injected
  `features: Lazy<List<ApiFeature>>`, throwing `AppStartException` at :35-38.
- It is registered as `singleton(ValidateRoutesOnAppStarting::class)` in
  `funktor/rest/src/jvmMain/kotlin/index_jvm.kt:41`.
- `AppStartException` (`funktor/core/src/jvmMain/kotlin/lifecycle/AppStartException.kt`) is
  documented as *not* caught by the lifecycle builder — it propagates and prevents startup.
- Mounting walks the identical traversal: `funktor-demo/server/src/main/kotlin/api/ApiApp.kt:72`
  (`features.flatMap { it.getRouteGroups() }.flatMap { it.all }`) and
  `funktor/all/src/jvmTest/kotlin/server.kt:37-40`. **Registry set == mounted set**, so the check
  has no false negatives against what is actually served.

One correctness detail that makes this safe: `ApiRoute` subclasses are `data class`es and
`authorize` returns a `copy` (`ApiRoute.kt:105-109` etc.). Registration happens *after* the builder
chain — `.block().apply { addRoute(this) }` (`ApiRoutes.kt:58-62`) — so the registered instance is
the final copy carrying the rules, not a pre-`authorize` original. A validator reading
`route.authRules` sees the real value.

### 4. Enforcement options — recommendation

**(a) Startup validation hook — RECOMMENDED.**
- *What breaks:* nothing in this repo (see audit: 0 offenders). Downstream apps with a rule-less
  route fail to boot on upgrade — loudly, at deploy time, with the offending routes named.
- *Migration cost:* one line per offending route (`.authorize { public() }` or a real rule). No
  signature change, no recompile of callers, source- and binary-compatible.
- *Loudness:* maximum useful. Fails at boot, before traffic, in every environment including the
  developer's first run — not at 3am on an unauthenticated request nobody logs.
- *Weakness (state it honestly):* runs at startup, not compile time, so a rule-less route can be
  merged and reach CI before it fails — it fails at the first app boot, which for this repo means
  the `AppSpec`/`AppUnderTest` suite, i.e. still in CI.

**(b) Non-defaulted `authRules`.**
- *What breaks:* every direct `ApiRoute.*(...)` construction, including the five factory helpers in
  `ApiRoutes.RouteBuilder` (`routePlain` :330, `routeSse` :340, `routeParams` :349, `routeBody` :359,
  `routeParamsBody` :370) — none of which can know the rules, since `authorize` is applied later in
  the chain. So (b) does not actually work as stated without restructuring the whole builder DSL:
  the constructor genuinely cannot have the rules yet. Making the parameter required would just
  force every factory to pass `emptyList()` explicitly and buy nothing.
- *Verdict:* rejected on mechanism, not merely on cost. Compile-time enforcement would need a
  different shape (e.g. an unauthorized-route type that only `authorize` can convert into a
  mountable route) — a large DSL redesign, out of proportion to the risk. Worth recording as a
  possible future direction, not as this task.

**(c) Default-deny inside `checkAccess`.**
- *What breaks:* silently and remotely. A downstream app that today serves a rule-less route keeps
  booting fine and starts returning 401 to real users at runtime. The failure surfaces as a
  production outage with no startup signal, and the diagnostic (`failedRules` is empty, because
  there are no rules to report) is actively misleading —
  `apiRespondUnauthorized(..., authResult.failedRules)` at `routing.kt:40` would render an empty
  rule list.
- *Migration cost:* same one-line fix, but discovered by users rather than by the deploy.
- *Loudness:* the worst of the three — quiet at deploy, loud in production, unhelpful message.
- *Verdict:* rejected. Same security outcome as (a), strictly worse failure mode. It is the option
  that trades a boot failure for an outage.

**Recommendation: (a).** It is the only option that is both implementable against the current DSL
and fails at a time and place where the fix is obvious. Optionally revisit (b)-as-redesign later;
(c) should not be done even as a belt-and-braces addition, because it converts a would-be startup
failure into a runtime 401 with an empty explanation.

#### Sketch

```kotlin
package io.peekandpoke.funktor.rest

import io.ktor.server.application.Application
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks.ExecutionOrder
import io.peekandpoke.funktor.core.lifecycle.AppStartException

/**
 * Fails app startup when a mounted route declares no authorization rules.
 *
 * A route with an empty rule list passes [io.peekandpoke.funktor.rest.auth.AuthResult.isSuccess]
 * vacuously and is therefore reachable by anonymous callers. Intentionally public routes must say
 * so with `authorize { public() }`.
 */
class ValidateRouteAuthOnAppStarting(
    private val features: Lazy<List<ApiFeature>>,
) : AppLifeCycleHooks.OnAppStarting {

    override val executionOrder: ExecutionOrder = ExecutionOrder.VeryEarly

    override suspend fun onAppStarting(application: Application) {
        val offenders = features.value.flatMap { feature ->
            feature.getRouteGroups().flatMap { group ->
                group.all
                    .filter { it.authRules.isEmpty() }
                    .map { "${feature.name} / ${group.name}: ${it.method.value} ${it.pattern.pattern}" }
            }
        }

        if (offenders.isNotEmpty()) {
            throw AppStartException(
                buildString {
                    append("Route authorization validation failed: ")
                    append("${offenders.size} mounted route(s) declare no authorization rules.\n")
                    append("A route without rules is reachable by ANONYMOUS callers.\n")
                    append("Fix each route below by adding either a real rule or an explicit\n")
                    append("`authorize { public() }` if it is intentionally public:\n")
                    offenders.forEach { append("  - $it\n") }
                }
            )
        }
    }
}
```

Registered next to the existing hook in `funktor/rest/src/jvmMain/kotlin/index_jvm.kt:41`:

```kotlin
singleton(ValidateRoutesOnAppStarting::class)
singleton(ValidateRouteAuthOnAppStarting::class)
```

Exact failure-message format (every offending route named, one per line):

```
Route authorization validation failed: 2 mounted route(s) declare no authorization rules.
A route without rules is reachable by ANONYMOUS callers.
Fix each route below by adding either a real rule or an explicit
`authorize { public() }` if it is intentionally public:
  - Showcase / showcase-rest: GET /api/showcase/plain
  - FunktorConf / funktor-conf: POST /api/funktor-conf/events
```

Identifier choice, and its limit: at runtime the only stable identifiers available are
`ApiFeature.name` (`ApiFeature.kt:8`), `ApiRoutes.name` (`ApiRoutes.kt:35`), `ApiRoute.method`
(`ApiRoute.kt:26`) and `ApiRoute.pattern` (`ApiRoute.kt:29`). **The message cannot print a
`file:line`** — routes carry no source location. `feature / group: METHOD uri` is the most specific
locator obtainable and is enough to grep for. For SSE routes `method` is hardcoded `Get`
(`ApiRoute.kt:152`), so an SSE offender prints as `GET`; if that proves confusing, use
`(route as? ApiRoute.Sse)?.let { "SSE" } ?: route.method.value`.

### Acceptance criteria

- [ ] Startup validation fails the app if any mounted route has `authRules.isEmpty()`, covering all
      **five** `ApiRoute` subclasses (incl. `WithBody` / `WithBodyAndParams`, missing from the
      original finding)
- [ ] Intentionally public routes keep working by declaring `public()` explicitly
- [ ] The failure message names every offending route as `feature / group: METHOD uri`, and states
      the two ways to fix it
- [x] Audit existing mounted routes across `funktor/*` and `funktor-demo` — **DONE, see §2. 83/83
      routes declare rules; 0 offenders; no remediation commits needed and the demo app will start
      green under the new check**
- [ ] Decide whether `estimateAccess` (`ApiRoute.kt:71-75`) should also stop folding an empty rule
      list to `Granted` — currently the access matrix would not flag such a route either

## Implementation notes

- Home: a second `AppLifeCycleHooks.OnAppStarting` participant beside `ValidateRoutesOnAppStarting`
  (`funktor/rest/src/jvmMain/kotlin/ValidateRoutesOnAppStarting.kt`), registered in
  `funktor/rest/src/jvmMain/kotlin/index_jvm.kt:41`. Separate class rather than folding into the
  existing one: different concern, and a distinct error message is the whole point.
- `ExecutionOrder.VeryEarly` matches the existing validator
  (`ValidateRoutesOnAppStarting.kt:19`), so both fail before anything expensive boots.
- Keep it a hard `AppStartException` with no config switch. An opt-out flag is how this class of
  check dies — someone flips it during an incident and it never comes back. If a downstream app
  needs an escape hatch, the escape hatch is `authorize { public() }`, which is auditable.
- Superseded note from the original draft: "SUSPECTED in the audit rather than confirmed" — now
  resolved. Confirmed as a real framework defect (§1) with zero current offenders (§2).

## Test evidence

- [ ] Unit: a fake `ApiFeature` with a rule-less route ⇒ `AppStartException`, message contains the
      route's `METHOD uri`
- [ ] Unit: same route with `authorize { public() }` ⇒ no throw
- [ ] Unit: same route with `authorize { isSuperUser() }` ⇒ no throw
- [ ] Unit: two offenders ⇒ both named, count in the header is `2`
- [ ] Unit: one rule-less route per `ApiRoute` subclass (`Plain`, `Sse`, `WithParams`, `WithBody`,
      `WithBodyAndParams`) is detected — guards the two subclasses the original finding missed
- [ ] End-to-end via `AppSpec`/`AppUnderTest` (`funktor/testing/src/jvmMain/kotlin/AppSpec.kt`,
      `AppUnderTest.kt`): the real demo app still starts, proving the audit result
- [ ] No `MatrixTest2d` run needed — no storage involvement
- [ ] Full test command(s) run + green: `./gradlew :funktor:rest:jvmTest :funktor-demo:server:test`

## Open questions

- Downstream apps outside this repo cannot be audited from here. Unknown how many rule-less routes
  exist there, i.e. the real blast radius of the boot failure on upgrade. Needs a CHANGELOG entry
  with the one-line fix, and argues for shipping this in a minor-version bump, not a patch.
- Not read: `funktor-demo/adminapp`, `ops-app`, `console`, `common`. Assumed to contain no JVM
  `ApiRoutes` because the `: ApiRoutes(` grep over `funktor-demo` returned hits only under
  `server/`, but their build configs were not inspected to confirm they have no JVM server target.
- Whether `ValidateRoutesOnAppStarting` is actually reached in every app's boot path was inferred
  from its `singleton(...)` registration at `index_jvm.kt:41` plus the `AppStartException` contract.
  The lifecycle runner that invokes `onAppStarting` and its exception handling were not read.
- `AuthRuleBuilder` was read only to line 60 — `isSuperUser()` is referenced throughout the audit
  but its definition was not read; it is assumed to be a genuine restriction and not a no-op. Worth
  one confirming read before relying on the audit's "all 83 are protected" framing.
- The audit classifies rules by the *first* rule inside each `authorize { }` block. A block
  combining rules (e.g. `forAny(...)`, `ApiRoutes.kt` DSL) would be summarised by its first line
  only. This affects the descriptive accuracy of the table's right-hand column, **not** the
  emptiness check, which is what this task turns on.
- Whether `estimateAccess`'s `Granted`-seeded fold should change is left open above; changing it
  affects the admin access-matrix UI and may need its own task.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/error-disclosure/20260720-redteam-error-disclosure.md`
