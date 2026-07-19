# Exception architecture: separate client-safe message from diagnostic detail

**Status:** TODO — design first, then implement
**Plan:** none — umbrella for `.claude/tasks/20260720-error-response-disclosure-audit.md`
**Security-critical:** yes

## Problem

Today an exception has exactly one human-readable channel — `Throwable.message` — and it is used for
two incompatible purposes: telling a client what went wrong, and telling an operator enough to
debug. `ApiStatusPages` then has to guess which audience it is serving, based on the environment.

That guess is the wrong shape:

- It fails open when the environment is unknown (see
  `.claude/tasks/20260720-env-classification-allowlist.md`).
- It is all-or-nothing: either the full raw message or nothing.
- It cannot distinguish "this message was written for a user" (`"Invalid credentials"`) from "this
  message was written for a developer" (`"Error while querying '…': FOR u IN users FILTER …"`).

**Key constraint that shapes the whole design:** most of what leaks is *not* our exception. The
audit's worst finding was `KarangoQueryException`, but the same channel carries `ConnectException`
(host:port), `FileNotFoundException` (paths), kontainer `ServiceDefinition` errors (service FQCNs)
and Slumber awaker errors (JSON field paths) — plus every JDK and third-party driver exception we
will ever encounter. So a new base class for *our* exceptions cannot be the mechanism: anything that
relies on exceptions opting **in** to being unsafe will be wrong by default.

## Design direction

Two independent decisions, and the second is the one that answers "how do we see errors in dev/qa
but not prod".

### 1. Default-deny at the render boundary

The renderer treats every `Throwable` as unsafe unless it explicitly declares a client-safe message.

```kotlin
interface HasClientMessage {
    /** Safe for any audience, including anonymous callers. No internals, no input echo. */
    val clientMessage: String
}
```

- Unknown exception → fixed generic string + correlation id. This is the default, so new code and
  third-party exceptions are safe automatically.
- `HasClientMessage` → its `clientMessage` is rendered.
- `Throwable.message` is **never** rendered to a client, in any environment.

Our own exception hierarchy then carries both channels explicitly:

```kotlin
abstract class FunktorException(
    override val clientMessage: String,   // safe: "The query could not be completed"
    message: String,                      // diagnostic: includes AQL, params, ids
    cause: Throwable? = null,
) : Exception(message, cause), HasClientMessage
```

Diagnostic context should be structured fields rather than string-concatenated into `message` — e.g.
`KarangoQueryException` keeps `query` and `varNames` as properties (it already has `query`), so the
logger can render them and nothing can accidentally interpolate them into a client-facing string.

### 2. Make disclosure identity-dependent, not environment-dependent

This is the part worth deciding deliberately. Options:

**Option A — keep the environment gate.** Non-production additionally inlines
`message` + stack trace into the response.
- Familiar, zero extra infrastructure.
- Security posture depends on a config string being right, forever. Every new environment name is a
  new chance to fail open. This is exactly the bug we just fixed.

**Option B — correlation id + server-side retrieval (recommended).** *Every* environment returns the
same generic body plus an opaque `errorId`. The full detail lives server-side (log + optionally a
short-TTL error store) and is retrievable through an existing superuser-gated API.
- The response body no longer depends on the environment at all, so there is no gate to fail open
  and no "staging is internet-facing" problem.
- Dev convenience is preserved by a different route: locally the log is right there in the console,
  and in qa/staging a superuser can fetch the detail by id.
- Costs: an error store or a log-search path, and slightly worse ergonomics than "stack trace right
  in the response".

**Option C — B, plus inline detail only for callers who are already superusers.** Identity-gated
rather than environment-gated. Keeps the inline convenience for the people who could read the logs
anyway, with no environment dependency.

Recommendation: **C**, falling back to B where there is no authenticated caller. The security
property becomes "you see internals iff you are authorised to see internals", which is checkable and
does not drift with deployment config.

- [ ] Decide between A / B / C before writing code — this is the load-bearing decision
- [ ] `HasClientMessage` (or equivalent) exists and the render boundary default-denies
- [ ] `Throwable.message` is never rendered to a client in any environment
- [ ] Correlation id is generated once per failure, returned to the client, and present on the log
      line for the same failure
- [ ] Our exception types carry diagnostic context as structured fields, not concatenated strings
- [ ] `KarangoQueryException` no longer builds the AQL into `message`
      (`karango/core/src/main/kotlin/vault/KarangoDriver.kt:130-138`)
- [ ] A lint/test guard prevents reintroducing `withError(cause.message)` at the render boundary
- [ ] Migration guide for downstream apps that currently rely on exception messages reaching clients

## Relationship to the other tasks

- `.claude/tasks/20260720-error-messages-generic-in-production.md` is the **tactical first slice** of
  this: stop returning `cause.message`, add a correlation id. Doing it in the shape described here
  means it will not need redoing. If this architecture task is picked up first, fold that one in.
- `.claude/tasks/20260720-auth-error-account-enumeration.md` becomes an application of
  `HasClientMessage`: `AuthError` declares client-safe codes, everything else defaults to generic.
- `.claude/tasks/20260720-env-classification-allowlist.md` is still needed regardless — it also
  gates fixtures and the insights GUI — but under option B/C it stops being load-bearing for error
  disclosure, which is the point.

## Implementation notes

- Inventory first: the audit lists ~20 `stackTraceToString()` sites (log-only) and the exception
  types that currently reach responses. Re-derive that list as step one, since the fix is only as
  good as its coverage.
- The single render boundary today is `funktor/rest/src/jvmMain/kotlin/ApiStatusPages.kt` — the
  audit confirmed there is no second status page. That makes this tractable: one choke point.
  `funktor/rest/src/jvmMain/kotlin/respond.kt:57-66` (failed auth rule descriptions) is a second,
  smaller one.
- Watch `EntityRepository.kt:332-333`, which puts `e.query` into a returned `RemoveResult` — a
  *success*-path disclosure that no environment gate would ever catch. Structured-fields discipline
  should cover it, but verify.
- Consider whether `clientMessage` should be a translation key rather than a literal string, if
  client-facing errors are ever localised.

## Test evidence

- [ ] Test that an arbitrary/unknown exception renders as the generic string in **every**
      environment, including dev
- [ ] Test that a `HasClientMessage` exception renders its `clientMessage` and nothing else
- [ ] Test that no rendered body ever contains `Throwable.message` of a non-client-safe exception
- [ ] Property/fuzz-style test over the repo's exception types asserting the render boundary output
      contains none of: AQL keywords, file path separators, `java.`/`io.peekandpoke.` FQCNs
- [ ] End-to-end via `AppSpec`/`AppUnderTest`: force failures on real routes, assert bodies
- [ ] Full test command(s) run + green: `./gradlew :funktor:rest:jvmTest :funktor:core:jvmTest
      :karango:core:test`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/20260720-redteam-error-disclosure.md`
