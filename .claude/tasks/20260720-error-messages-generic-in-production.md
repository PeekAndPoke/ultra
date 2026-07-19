# Error responses must not return `cause.message` in production

**Status:** TODO — tactical slice; see
`.claude/tasks/20260720-exception-disclosure-architecture.md` for the umbrella design
**Plan:** none — from `.claude/tasks/20260720-error-response-disclosure-audit.md` (findings 1 + 2)
**Security-critical:** yes

> If the architecture task is picked up first, fold this one into it rather than doing both. This
> task fixes the specific leak; that task fixes the shape that allows leaks.

## Spec

The production branch of `ApiStatusPages` returns the raw exception message:

```kotlin
return when (exposesStackTraces(config)) {
    true -> this.withError(cause.stackTraceToString())
    else -> this.withError(cause.message ?: "")   // <-- production
}
```

`funktor/rest/src/jvmMain/kotlin/ApiStatusPages.kt`

That is not safe, because exception messages in this stack carry internals. The worst offender is
`karango/core/src/main/kotlin/vault/KarangoDriver.kt:130-138`:

```kotlin
throw KarangoQueryException(
    message = "Error while querying '${e.message}':\n\n${query.query}\nwith params [${vars.keys.joinToString(", ")}]",
    ...
)
```

So a correctly-configured **production** app returns the full AQL source, collection names and
bind-variable names to the client on any uncaught query error. Reachable from unauthenticated
endpoints (e.g. sign-in, which does a user lookup). Also leaks via `FileNotFoundException` (paths),
`ConnectException` / `UnknownHostException` (internal host:port), kontainer `ServiceDefinition`
messages (service FQCNs) and Slumber awaker errors (JSON field paths).

This is the remaining instance of the bug class; the earlier null-config fix did not cover it.

- [ ] In production, error responses carry a fixed generic string, never `cause.message`
- [ ] A correlation id is generated per failure, returned to the client, and logged alongside the
      full cause so the error stays diagnosable
- [ ] Exceptions whose messages ARE intended for clients (e.g. `CouldNotConvertException`,
      validation errors) are surfaced via an explicit whitelist/marker interface, not by passing
      `Throwable.message` through blanket
- [ ] `KarangoQueryException.message` no longer embeds the query — move the AQL and bind-var names
      to dedicated fields that only the logger reads
- [ ] Non-production behaviour (stack traces) is unchanged

## Implementation notes

- A marker interface (e.g. `ClientSafeMessage`) is probably cleaner than a type whitelist, since
  downstream apps define their own exceptions and cannot extend a list in the framework.
- Check `funktor/rest/src/jvmMain/kotlin/ApiStatusPages.kt` `logInternalError` — the correlation id
  should thread through there so the log line and the response agree.
- `karango/core/src/main/kotlin/vault/EntityRepository.kt:332-333` swallows a
  `KarangoQueryException` into `RemoveResult(count = 0, query = e.query)`. If any endpoint returns a
  `RemoveResult` and that type serialises `query`, the AQL leaks on the **success** path too, in any
  environment. Unconfirmed — no such endpoint found in this repo, but verify before closing.

## Test evidence

- [ ] Unit tests on the message-selection logic: production + arbitrary exception → generic string;
      production + client-safe exception → its message; non-production → stack trace
- [ ] A test asserting a `KarangoQueryException` message never reaches a production response
- [ ] End-to-end: boot via `AppSpec`/`AppUnderTest`, force a failing repo call on a route, assert the
      production response body contains neither the AQL nor `"Error while querying"`
- [ ] Full test command(s) run + green: `./gradlew :funktor:rest:jvmTest :karango:core:test`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/20260720-redteam-error-disclosure.md`
