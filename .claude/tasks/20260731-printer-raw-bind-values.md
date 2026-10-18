# AqlPrinter / MongoPrinter render RAW bind values, not slumbered ones

**Status:** FIX LANDED, TESTS STILL MISSING (updated 2026-07-31)
**Plan:** `.claude/tasks-archive/2026-07/20260731-redacted-and-jackson-removal.md`
**Security-critical:** no — debug-only surface. But see "Why it is not zero-risk".

The code fix from "Fix direction" below shipped in commit `1bb495c8`, in BOTH printers —
`karango/core/src/main/kotlin/aql/printer.kt:34` and `monko/core/src/main/kotlin/lang/printer.kt:33`
now slumber before rendering, with the two-stage fallback so a debug helper cannot throw. The full
karango and monko suites stayed green, confirming the "scalars and lists slumber to themselves"
assumption that the fix rested on.

**What did NOT land: the three tests in "Tests to add".** `1bb495c8` touched no karango or monko test
file. So the new behaviour is real but unpinned — a regression would be silent, and the `Redacted`
row in particular is pinning a property that holds only by accident today. That is the remaining
work; it is small, and the blocker recorded in "Why it was not fixed when found" is gone (karango is
quiet again).

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

## Why it was not fixed when found — RESOLVED

Another agent had ~24 uncommitted files under `karango/core/src/main/kotlin/aql/` at the time. Touching
the module would have meant compiling their in-flight work, so a green or red result would have proved
nothing about this change. That work has since been committed and the module is quiet, which is how
`1bb495c8` was able to land the fix.

## Tests to add — THE REMAINING WORK

Still outstanding. Monko has `monko/core/src/test/kotlin/io/peekandpoke/monko/lang/MongoPrinterSpec.kt`
to extend; karango has no printer spec at all, so that one needs creating.

- A structured bind value renders as a JSON object, not a string.
- A `Map` with non-String keys does not throw.
- A `Redacted<String>` bound as a parameter renders as the placeholder — pinning the property that
  currently holds only because `Redacted.toString()` redacts.
