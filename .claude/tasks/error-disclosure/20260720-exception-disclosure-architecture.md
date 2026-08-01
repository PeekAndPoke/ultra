# Exception architecture: separate client-safe message from diagnostic detail

**Status:** TODO — design decision first, then implement
**Plan:** umbrella for `.claude/tasks/error-disclosure/20260720-error-response-disclosure-audit.md`
**Security-critical:** yes

## Problem

An exception has one human-readable channel — `Throwable.message` — used for two incompatible
audiences: telling a client what went wrong, and telling an operator enough to debug.
`ApiStatusPages` then guesses which audience it is serving from the environment string.

That guess is the wrong shape: it fails open on unknown environments, it is all-or-nothing, and it
cannot distinguish "written for a user" (`"Invalid credentials"`) from "written for a developer"
(`"Error while querying '…': FOR u IN users FILTER …"`).

## The finding that shapes the design: there are TWO disclosure channels, not one

A full render-boundary audit found that exceptions reach clients through two structurally different
paths. **A fix aimed only at the first does not touch the second.** This is the main reason this
task exists as an umbrella rather than a one-line fix.

### Channel A — exception → error message

`ApiResponse.messages`, populated via `withError` / `withInfo`.

| Site | Gate | Status |
|---|---|---|
| `funktor/rest/src/jvmMain/kotlin/ApiStatusPages.kt:47-54` — `withCause` | environment (`exposesStackTraces`) | non-prod → `stackTraceToString()`; **prod → `cause.message`, which still leaks** (see `KarangoQueryException` below) |
| `funktor/rest/src/jvmMain/kotlin/respond.kt:48-69` — `apiRespondUnauthorized` | environment (`isNotProduction`) | leaks failed auth-rule descriptions |
| `funktor/auth/src/jvmMain/kotlin/api/AuthApi.kt:57,79,106,127,148,170,193,216,235` | **none** | `withInfo(e.message ?: "")` in *every* environment |

Worst payload on this channel: `karango/core/src/main/kotlin/vault/KarangoDriver.kt:130-138` builds
`KarangoQueryException.message` as `"Error while querying '${e.message}':\n\n${query.query}\nwith
params [...]"` — so the production branch returns the full AQL, collection names and bind-var names.

Confirmed sole exception→response plugin: no `install(StatusPages)`, no other `on(CallFailed)`, no
`exception<`, no GraphQL, no server-side WebSocket routes anywhere in the repo.

### Channel B — exception → persisted DTO field → API response

Stack traces are **stored as data** and served as ordinary payload via `ApiResponse.data`. No
environment gate applies to any of these, by design or otherwise:

| DTO field | Written at | Served by | Gate |
|---|---|---|---|
| `WorkerModel.Run.Result.Failure.stack` (`funktor/inspect/src/commonMain/kotlin/cluster/workers/api/WorkerModel.kt:30-40`) — `stackTraceToString()` | `WorkersFacade.kt:103-115` | `WorkersApi.kt:16-42` | `isSuperUser()` |
| `BackgroundJobs` result `data["stack"]` (`funktor/cluster/src/jvmMain/kotlin/backgroundjobs/BackgroundJobs.kt:500,578`) | job failure | `BackgroundJobsApi.kt:138-155` | `isSuperUser()` |
| `LogEntryModel.stackTrace` (`funktor/inspect/src/commonMain/kotlin/logging/api/LogEntryModel.kt:17,23`) | `LogbackKarangoLogAppender.kt:93-101`, attached to the **root logger** | `LoggingApi.kt:33-92` | `isSuperUser()` |
| `EmailResult.error["stackTrace"]` (`funktor/messaging/src/commonMain/kotlin/api/EmailResult.kt:20-26`) | `SendgridSender.kt:66-73` | latent — `SentMessageModel` embeds it; no core endpoint serialises it wholesale today | — |

Superuser gating is a defensible answer for channel B and should be kept. Two problems remain:

1. **A confirmed public leak on this channel.** `funktor-demo/server/src/main/kotlin/api/showcase/ClusterShowcaseApi.kt:271-292`
   — `getWorkers` is `authorize { public() }` and returns `"failure: ${r.message}"`. Verified by
   reading the file. Unauthenticated callers get worker exception messages. Demo app, but real.
2. **`ultra/log/src/jvmMain/kotlin/Log.kt:37-39`** folds the stack trace into the *message* string
   (`message + "\n" + e.stackTraceToString()`), so a stack trace lands in `LogEntryModel.message`
   even for appenders that never touch the `stackTrace` field. Any "strip the stackTrace field"
   fix that ignores this is incomplete.

Also on this channel: `karango/core/src/main/kotlin/utils/ArangoDbRequestUtils.kt:39-41` stores
`"ERROR: ${e.stackTraceToString()}"` into `queryExplained`, which the insights details page renders
(`funktor/insights/src/jvmMain/kotlin/collectors/VaultCollector.kt:133-141`) — and that page has no
auth at all (tracked separately in `.claude/tasks/error-disclosure/20260720-insights-gui-auth-gate.md`).

## Design

### 1. Channel A — default-deny at the render boundary

The renderer treats every `Throwable` as unsafe unless it explicitly declares a client-safe message.

```kotlin
interface HasClientMessage {
    /** Safe for any audience, including anonymous callers. No internals, no input echo. */
    val clientMessage: String
}
```

- Unknown exception → fixed generic string + correlation id. **This is the default**, so JDK,
  driver and third-party exceptions — which are the bulk of what leaks — are safe automatically.
- `HasClientMessage` → its `clientMessage` is rendered.
- `Throwable.message` is **never** rendered to a client, in any environment.

Opting *in* to safety is the only workable polarity here: a base class for our own exceptions cannot
help with `ConnectException`, `FileNotFoundException`, kontainer `ServiceDefinition` errors or
Slumber awaker errors, all of which reach this boundary today.

Our own hierarchy then carries both channels explicitly, with diagnostics as **structured fields**
rather than string-concatenated into `message`:

```kotlin
abstract class FunktorException(
    override val clientMessage: String,   // "The query could not be completed"
    message: String,                      // diagnostic: ids, query, params
    cause: Throwable? = null,
) : Exception(message, cause), HasClientMessage
```

`KarangoQueryException` already has a `query` property — the fix is to stop interpolating it into
`message`, not to add a new field.

### 2. Channel B — keep the identity gate, add serialisation discipline

Do not try to environment-gate channel B. Instead:

- Keep `isSuperUser()` on the admin APIs (already correct).
- Diagnostic fields (`stack`, `stackTrace`, `data["stack"]`) move into a distinct nested type so
  "contains a stack trace" is visible in the type system and greppable, rather than being an
  ordinary `String` field that any new endpoint can accidentally serialise.
- Fix the demo's public `getWorkers` to not include failure text (or require auth).
- Decide what to do about `Log.kt:37-39` folding stack traces into `message`.

### 3. The environment-vs-identity decision — RECOMMENDATION

This is the load-bearing choice and the answer to "how do we see errors in dev/qa but not prod".

| Option | What it means | Verdict |
|---|---|---|
| **A** — keep the environment gate | non-prod additionally inlines `message` + stack trace | **Reject.** Security posture depends forever on a config string being right. Every new environment name is a fresh chance to fail open — that is the bug we just fixed, twice |
| **B** — correlation id + server-side retrieval | every environment returns the same generic body + opaque `errorId`; detail lives in the log/error store | Sound. No gate to fail open. Costs a lookup step in dev |
| **C** — B, plus inline detail for callers who are already superusers | identity-gated | **Recommended** |

**Recommend C, degrading to B when there is no authenticated caller.** The security property becomes
"you see internals iff you are authorised to see internals" — checkable, testable, and it does not
drift with deployment config. Dev ergonomics are preserved because a local developer is a superuser
and the log is in the console anyway.

Honest cost of C: it needs an identity lookup on the error path (which must itself not throw), and
"just read the stack trace in the response" stops working for an anonymous local curl. That is a
real ergonomic regression for a small class of debugging, and it is the main argument for B-only.

### Correlation id — reuse, don't invent

**OPEN QUESTION — not verified.** The audit did not confirm whether a per-request id already exists.
Before inventing one, check: `funktor/core/src/jvmMain/kotlin/coroutines/timing.kt` (`TimingInterceptor`
is the only known `CoroutineContext` element in the repo), the insights request capture (which
already mints a `bucket`/`file` identifier per request — see
`funktor/insights/src/jvmMain/kotlin/`), and whether any MDC/logging context exists. The insights
identifier looks like the strongest reuse candidate.

## Spec

- [ ] Decide A / B / C explicitly and record the decision here before writing code
- [ ] `HasClientMessage` exists; the channel-A render boundary default-denies
- [ ] `Throwable.message` is never rendered to a client in any environment
- [ ] Correlation id generated once per failure, returned to the client, present on the matching log
      line; reuses an existing request id if one exists
- [ ] `KarangoQueryException` no longer builds the AQL into `message` (`KarangoDriver.kt:130-138`)
- [ ] `AuthLoginApi`'s nine `withInfo(e.message)` sites go through the same mechanism
      (overlaps `.claude/tasks/error-disclosure/20260720-auth-error-account-enumeration.md`)
- [ ] Channel-B diagnostic fields are typed distinctly and cannot be serialised by accident
- [ ] `ClusterShowcaseApi.getWorkers` no longer exposes failure text publicly
- [ ] A test guard prevents reintroducing `withError(cause.message)` at a render boundary
- [ ] Migration guide for downstream apps declaring their own client-safe exceptions

## Implementation notes

- **Single choke point for channel A** makes this tractable: `ApiStatusPages.withCause` is the only
  exception→response plugin in the repo (verified — no `install(StatusPages)`, no other
  `on(CallFailed)`). `respond.kt:48-69` is a second, smaller one.
- **Staged rollout:** ship the default-deny first with a log-only warning whenever an exception
  without `HasClientMessage` reaches the boundary. That produces the real inventory of what
  downstream apps rely on before anything breaks for them.
- `karango/core/src/main/kotlin/vault/EntityRepository.kt:332-333` puts `e.query` into a returned
  `RemoveResult` — a **success-path** disclosure no environment gate would ever catch. Verify
  whether `RemoveResult.query` is serialised by any endpoint.
- SSE (`funktor/rest/src/jvmMain/kotlin/routing.kt:164-181`) has no local exception handling; a
  throw propagates to `CallFailed`, which is wrapped in `try { … } catch (_: Throwable) {}`. Almost
  certainly swallowed once streaming has begun, but **unproven** — worth a defensive try/catch and a
  test rather than an assumption.

### Sequencing

1. `.claude/tasks/error-disclosure/20260720-env-classification-allowlist.md` — two lines, closes several fail-opens,
   independent of this design.
2. Channel A default-deny + correlation id (the tactical slice is
   `.claude/tasks/error-disclosure/20260720-error-messages-generic-in-production.md` — fold it in if this lands first).
3. `KarangoQueryException` restructuring.
4. Channel B typing discipline + the public `getWorkers` fix.

## Test evidence

- [ ] An arbitrary/unknown exception renders as the generic string in **every** environment
- [ ] A `HasClientMessage` exception renders its `clientMessage` and nothing else
- [ ] A `KarangoQueryException` never reaches a response body containing AQL, in any environment
- [ ] Guard test: rendered bodies contain none of — AQL keywords, path separators, `java.` /
      `io.peekandpoke.` FQCNs
- [ ] Channel B: a non-superuser cannot retrieve worker/job/log diagnostics; the public showcase
      endpoint exposes no failure text
- [ ] End-to-end via `AppSpec`/`AppUnderTest`: force failures on real routes, assert bodies
- [ ] Full: `./gradlew :funktor:rest:jvmTest :funktor:core:jvmTest :funktor:cluster:jvmTest
      :karango:core:test`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/error-disclosure/20260720-redteam-error-disclosure.md`
