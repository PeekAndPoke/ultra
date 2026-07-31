# AqlPrinter / MongoPrinter render RAW bind values, not slumbered ones

**Status:** TODO — found by the review gate on the Jackson removal, 2026-07-31. Comment corrected in
place; the behaviour is unchanged and untested.
**Plan:** `.claude/tasks/20260731-redacted-and-jackson-removal.md`
**Security-critical:** no — debug-only surface. But see "Why it is not zero-risk".

## What is wrong

`AqlPrinter.queryVars` holds the value the caller bound — `karango/core/src/main/kotlin/aql/base_expr.kt`
(`p.value(name, value as Any?)`). Slumbering happens later and elsewhere, in
`karango/core/src/main/kotlin/vault/KarangoDriver.kt`. `MongoPrinter` has the same shape.

When the Jackson mapper was replaced with `JsonUtil.toJsonElement()`, both printers acquired a comment
claiming *"Slumber already produced a plain tree; this only turns it into text"*. That is true for
`JsonPrinter` and `InsightsFull`, which both call `codec.slumber` first. It is **false at these two call
sites**. The comment is corrected; the behaviour is not.

## Consequences

`JsonUtil.toJsonElement` ends with `else -> JsonPrimitive(toString())`
(`ultra/slumber/src/commonMain/kotlin/JsonUtil.kt`), so:

| Bound value | Jackson rendered | Now renders |
|---|---|---|
| a data class (`INSERT(entity.aql()) INTO repo`) | `{ "name": "x", "age": 3 }` | the JSON **string** `"Person(name=x, age=3)"` |
| a `Map` with non-String keys | stringified keys | raw `ClassCastException` from the unchecked cast in `JsonUtil` |
| scalars, lists of scalars | unchanged | unchanged |

The last row is why 1898 karango tests and the monko suite stayed green: every printer spec binds
scalars and lists.

## Why it is not zero-risk despite being debug-only

- `printRawQuery` is **public API**, not an internal helper.
- A `ClassCastException` out of a debug printer is a crash where the previous behaviour degraded.
- The raw value bypasses `Redacted<T>`: a `Redacted` bound directly as a query parameter renders via
  `toString()`, which redacts — correct, but **by accident**, not because the printer knows the type.
  Worth a pinning test either way.

## Fix direction

Slumber before rendering, with a safe fallback so a debug helper never throws:

```kotlin
runCatching { codec.slumber(value).toJsonElement() }
    .recoverCatching { value.toJsonElement() }
    .getOrElse { JsonPrimitive(value.toString()) }
```

Scalars and lists slumber to themselves, so existing expectations should not move — but that must be
confirmed against the full karango and monko suites, not assumed.

## Why it was not fixed when found

Another agent had ~24 uncommitted files under `karango/core/src/main/kotlin/aql/` at the time. Touching
the module would have meant compiling their in-flight work, so a green or red result would have proved
nothing about this change. Do this when karango is quiet.

## Tests to add

- A structured bind value renders as a JSON object, not a string.
- A `Map` with non-String keys does not throw.
- A `Redacted<String>` bound as a parameter renders as the placeholder — pinning the property that
  currently holds only because `Redacted.toString()` redacts.
