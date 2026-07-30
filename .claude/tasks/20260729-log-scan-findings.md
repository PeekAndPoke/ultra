# ultra/log scan — findings backlog

**Status:** IN PROGRESS — 9 files documented, 20 findings; mechanical fixes DONE, 5 design questions open

## DONE — mechanical round, 2026-07-30

| Finding | Fix | Mutation check |
|---|---|---|
| L2 concurrent `add()` loses events | `CopyOnWriteArrayList` | kills "an appender registered during an in-flight dispatch does not lose the event" |
| L3 throwing appender suppresses later ones | per-appender `try/catch`, rethrowing `CancellationException`, failure to stderr | kills "a throwing appender does not suppress the appenders after it" |
| L5 `formatLoggerName` crash on empty segment | `it.take(1)` instead of `it[0]` | kills "formatLoggerName survives empty package segments" |
| L6 variable-width timestamp | `DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")` | kills both timestamp tests |
| L7 unsynchronized name cache | `ConcurrentHashMap` | none — see note |

New spec `UltraLogManagerDispatchSpec` (3 tests) plus 2 in `LogAppenderFormatEdgeCasesSpec`.
Module 43 → 48 tests. Dependent suites unchanged and green: vault 285, karango 1649, monko 249,
funktor/core 772. Compile sweep clean.

`LogAppenderFormatEdgeCasesSpec.kt:100` asserted the old seconds-less timestamp — updated. Its own
comment (`toLocalTime() = "08:30"`) shows the author saw the missing seconds and recorded it as
expected, which is what a captured-behaviour test looks like.

**L7 has no mutation check on purpose.** The race was never reproducible (16 threads × 4000 names
stayed clean), so no honest test can fail when it is reverted. It is a JMM-correctness change, not a
bug fix, and is recorded as such rather than dressed up with a test that would pass either way.

### Two vacuous tests written and discarded on the way to L2 — worth remembering

The first concurrency test passed **with the bug reinstated**. Two independent reasons, both of which
generalise:

1. `java.util.ArrayList`'s iterator checks `modCount` in `next()`, not `hasNext()`, so an appender at
   index 0 always receives the event before the throw. A recorder placed first can never observe the
   loss — it has to come **after** the mutation point.
2. The `ConcurrentModificationException` surfaces inside the dispatch coroutine and never reaches the
   caller. Asserting that `log.info(...)` does not throw therefore proves nothing at all; only the
   delivery count is evidence. That is the finding itself, restated as a test-design trap.

The second attempt (two racing threads, recorder last) also failed to kill the mutation: with
`Dispatchers.Unconfined` the whole `forEach` runs synchronously inside `log()`, so two plain threads
almost never interleave inside it. The working version is deterministic instead of racy — a
**suspending** appender holds the iteration open across its IO, `log` returns at that suspension
point, and the test then mutates the list into the parked iterator.
**Plan:** none — output of a 4-agent scan of `ultra/log`, 2026-07-29
**Security-critical:** YES — see L1 (log forging, CWE-117). A red-team task is collected separately.

All main-source files were documented (comment-only; the diff contains no logic changes). Where an
agent found behaviour it was also reporting as wrong, it left a `// TODO(scan):` marker instead of
KDoc blessing it — a rule added after the reflection scan, where three agents documented defects as
if they were the contract.

Module is JVM-only in practice (`LogLevel` is the sole `commonMain` file). Consumers: **funktor/core,
karango/core, monko/core, ultra/vault**.

Baseline: 43 tests across 7 specs, 0 failures.

## The two cross-cutting themes

### T1 — There is no level filtering anywhere. `LogLevel.OFF` is not honoured.

Found independently by three agents. `LogLevel.severity` is referenced **only in doc comments** across
the entire module — no appender and no manager ever compares against it. `UltraLogManager.log()`
forwards every event unconditionally; `ConsoleAppender` prints whatever it receives. So:

```
UltraLogManager(listOf(ConsoleAppender())).getLogger(X::class).log(LogLevel.OFF, "secret")
  -> stdout: 2026-07-29 23:33:22 OFF - X - secret
```

`LogLevel`'s own KDoc says OFF "suppresses all log output". `Slf4jAppender` **does** drop OFF and ALL
(`Slf4jAppender.kt:36`), so the two built-in appenders disagree with each other. There is also no
`Log.isEnabled(level)` and no lambda-message overload, so callers always pay full message
construction — including `Throwable.stackTraceToString()` — even with zero appenders registered
(probed: the stack trace was rendered with an empty appender list).

`LogLevelOrderingSpec.kt:73-91` *simulates* a gate inside the test, which is itself evidence that no
gate exists in the code.

### T2 — Dispatch is neither thread-safe nor asynchronous, and failures are silent.

`UltraLogManager.log()` is `scope.launch { appenders.forEach { it.append(...) } }` on
`Dispatchers.Unconfined`, over a plain `ArrayList`. Three consequences, all probed:

- **Unconfined runs inline.** The body executes on the caller's thread up to the first suspension
  point. `ConsoleAppender` and `Slf4jAppender` never suspend, so **logging is synchronous** — an
  appender doing `Thread.sleep(200)` held the caller for 200 ms on its own thread. The class KDoc
  claimed the opposite ("so that logging does not block the caller"); it has been corrected.
- **The appender list is unsynchronized.** `add()` mutates the same `ArrayList` that `log()`
  iterates. 8 threads × 5000 calls with periodic `add()` produced **490 `ConcurrentModificationException`s
  on stderr while `runCatching` around every single call caught ZERO**. Each one is a dropped event.
  Because `append` is `suspend`, an appender that genuinely suspends holds the iteration open across
  its IO, widening the race from nanoseconds to the duration of the write.
- **A throwing appender aborts the loop.** `forEach` stops at the first exception, so every appender
  registered after it never sees the event, and `launch` does not rethrow — the failure reaches only
  the default handler. Silent event loss in a logging framework.

Mitigating, and worth keeping in mind before rating this HIGH: no in-repo appender currently throws
(`KarangoLogAppender`/`MonkoLogAppender` go through `Repository.tryInsert`, which swallows every
Throwable at `ultra/vault/src/jvmMain/kotlin/Repository.kt:267`), and `SupervisorJob` means a failure
does not take the shared scope down — later events still dispatch.

## Findings

### Security

| # | Sev | Finding |
|---|---|---|
| L1 | **MED-HIGH** `[verified]` | **Log forging (CWE-117).** `LogAppender.format:61` interpolates `$message` raw — no newline sanitisation. A caller-controlled message containing `\n2026-01-01 00:00:00 ERROR - x - y` produces what reads as a complete additional log entry. Made materially worse by `Log.error(message, e)` (`Log.kt:42`) folding the stack trace into the message with `"\n"`, so **multi-line records are normal here** and a parser cannot distinguish a forged line from a real one. Any code logging a user-supplied email, path or header is a live injection point. |

### Correctness

| # | Sev | Finding |
|---|---|---|
| L2 | HIGH `[verified]` | Concurrent `add()` + `log()` drops events silently — see T2. Found independently by 3 agents. |
| L3 | MED `[verified]` | A throwing appender suppresses every later appender for that event, silently — see T2. |
| L4 | MED `[verified]` | `LogLevel.OFF` is not honoured and the two built-in appenders disagree — see T1. |
| L5 | MED `[verified]` | `formatLoggerName` throws `StringIndexOutOfBoundsException` on a name with an empty non-final segment (`"a..b.C"`, `".b.C"`), because `parts.take(n-1).map { it[0] }` indexes char 0 unconditionally (`LogAppender.kt:85`). Not reachable through `LogImpl` (names come from `KClass.qualifiedName`), but reachable through the public `UltraLogManager.log(level, message, loggerName)` and the public `format`/`formatLoggerName`. `Slf4jAppender` calls it unconditionally, so a 4-char name is enough there; `ConsoleAppender` only reaches it above 30 chars. |
| L6 | MED `[verified]` | Timestamps vary in width and silently lose the seconds field. `format` uses `ts.toLocalTime()`, i.e. `LocalTime.toString()`, which omits seconds when they are zero and appends nanoseconds when they are not: `08:30` vs `23:33:22.265603439`. Roughly one line in sixty has no seconds. Output is not uniformly parseable. **`LogAppenderFormatEdgeCasesSpec.kt:100` asserts the seconds-less form** — that test pins captured behaviour, it does not establish it as intended. |
| L7 | MED `[reported]` | `LogAppender.loggerNameLookUp` (`:38`) is a plain `mutableMapOf` in a companion field, mutated by `getOrPut` from arbitrary logging threads. Same class of defect as the `ultra/reflection` caches. **Probed and stayed clean** — 16 threads × 4000 distinct names produced no exception and no wrong value — so this rests on the JMM argument, not an observed failure. Values are a pure function of the key, so a lost entry is self-healing; the hazard is torn resize. |

### Design / API

| # | Sev | Finding |
|---|---|---|
| L8 | MED `[verified]` | `Log.error(message, e)` discards the `Throwable`. `LogAppender.append` has no throwable parameter, so `Slf4jAppender` can only ever call `slf4j.error(String)` — verified with a `java.lang.reflect.Proxy`: the `error(String, Throwable)` overload is never invoked. Logback/Sentry/ELK therefore receive no exception object: no `%ex`, no `stack_trace` field, no grouping or dedup. Not fixable inside the appender; the `append` signature has to carry it. |
| L9 | MED `[verified]` | `Slf4jAppender` routes every event to one fixed SLF4J logger (`:14,28`); the originating class survives only as text inside the message. Per-package levels, appender routing and IDE log navigation cannot discriminate by origin — which is the usual reason to use SLF4J at all. Proxy-verified: no per-class logger lookup ever happens. |
| L10 | MED `[verified]` | Dispatch is synchronous despite the name — see T2. KDoc corrected; whether to change the dispatcher is a scoping decision. |
| L11 | MED `[verified]` | `index_jvm.kt:36` registers the manager `dynamic`, which silently promotes **every service injecting `Log`** to `SemiDynamic` — one instance per kontainer. A singleton holding shared state loses it every request. Not new: the same mechanism caused the bug in `.claude/tasks-archive/2026-07/20260720-vault-hook-scope.md`, and an in-file TODO at `:38-40` already acknowledges the missing `dynamicPrototype`. It is however **untested** — `LoggingKontainerModuleSpec` only asserts the manager can be fetched. |
| L12 | LOW `[verified]` | `add()` on a kontainer-provided manager is per-request, so boot-time `kontainer.get(UltraLogManager::class).add(x)` is silently discarded for every later kontainer. Contradicts the method's "future log events" wording. |
| L13 | LOW `[verified]` | `kontainer.get(Log::class)` yields a logger named `io.peekandpoke.ultra.kontainer.Kontainer`, so manually-fetched loggers are unattributable. Consistent with `InjectionContext` semantics — a usability trap, not a kontainer bug. Injected `Log` resolves correctly (probed). |
| L14 | LOW `[verified]` | No flush or shutdown path. `scopeJob`/`scope` are private companion state with no join/cancel API and no shutdown hook, so an appender still suspended at JVM exit loses its write. Matters only for genuinely suspending appenders (the Karango/Monko DB ones). |
| L15 | LOW `[verified]` | Continuation lines of a multi-line message carry no prefix, so line-oriented collectors read each stack-trace line as a separate event at unknown level. Same root cause as L1/L8. |
| L16 | LOW `[verified]` | The same event renders a different logger name in console vs SLF4J: `formatLoggerName` is unconditional in `Slf4jAppender` but only applies above 30 chars in `format`. Grepping one output for a name seen in the other fails. |
| L17 | LOW `[verified]` | `Slf4jAppender` discards `ts` and lets SLF4J stamp its own. Drift is ~0 while dispatch is inline, but console and SLF4J can disagree on the time of the same event. No measurable skew could be forced. |
| L18 | LOW `[verified]` | Natural/ordinal `LogLevel` comparison is inverted relative to `severity`: `ERROR < WARNING` is `true` while `ERROR.severity < WARNING.severity` is `false`, because declaration order runs OFF(0)…ALL(6). Nothing uses natural ordering today. Note `entries.sorted()` happens to equal `sortedByDescending { severity }`, so only pairwise comparison disagrees. |
| L19 | LOW `[reported]` | `loggerNameLookUp` never evicts. Bounded when names come from `LogImpl` (one per class), unbounded via the public API. |
| L20 | LOW `[verified]` | `LogImpl` re-resolves `caller.qualifiedName` on every call although `caller` is immutable. Cheap while the kotlin-reflect soft reference is warm; re-derived from `@Metadata` after memory pressure. Cost only. |

## Checked and CLEAN — do not re-tread

- **Concurrent `ConsoleAppender` writes do not interleave.** 8 threads × 2000 entries with a 120-char
  payload → 16000 lines, 0 torn. `PrintStream.println(String)` is atomic for the whole call, including
  a multi-line message.
- **SLF4J `{}` placeholders are a non-issue.** Ultra's `Log` has no parameterised form and the appender
  resolves to the single-`String` overloads only (proxy-verified; `javap` confirms `error(String)` is a
  distinct overload from `error(String, Object...)` in slf4j-api 2.0.17), so `{}` passes through
  literally and no arity mismatch is possible.
- **Slf4j level mapping is correct and exhaustive** for all 7 enum constants; OFF/ALL produce zero calls.
- **An appender exception does not kill the caller or the manager** — `SupervisorJob` keeps the shared
  scope alive, so only that one event's remaining appenders are lost.
- **Single-thread ordering survives a suspending appender** — 3 calls through a `delay(10)` appender
  arrived in order. An earlier reorder hypothesis did not survive its probe.
- **`Log.error(String)` shadowing stdlib `kotlin.error(...)`**: a real hazard in principle, but every
  implementor and every `with(log)`/`log.apply` scope was grepped — no bare `error(...)` call site
  exists. Not reported.
- `formatLoggerName` is correct for normal and boundary inputs (single segment, trailing dot, empty
  string, deep packages); the 30-char threshold is exact (30 intact, 31 abbreviates).
- Anonymous/local classes yield `qualifiedName == null` → `"n/a"`, the author's explicit fallback; it
  does not throw.
- `List<LogAppender>` injection resolves all-by-supertype and an empty list builds and logs fine.
- The event timestamp is captured on the caller before `launch`, so all appenders see the same time.
- `NullLog` is a trivial no-op with no branch or state — nothing to get wrong.

## Open decisions

None taken yet. The obvious mechanical fixes (L5 guard, L6 fixed-width formatter, L7 `ConcurrentHashMap`,
L2/L3 thread-safe list + per-appender `try/catch`) are separable from the design questions
(L4 where filtering belongs, L8 changing the `append` signature, L9 per-class SLF4J loggers,
L10 dispatcher choice, L11 Kontainer scoping) — the latter change a published API or its behaviour.
