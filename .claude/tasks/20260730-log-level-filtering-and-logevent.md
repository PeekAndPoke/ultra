# ultra/log: level filtering, LogEvent, and per-class SLF4J loggers

**Status:** IMPLEMENTED 2026-07-30 — awaiting /feature-review
**Plan:** `.claude/tasks/20260729-log-scan-findings.md` → L4, L8, L9, L10, L11
**Security-critical:** no (L1 log forging is tracked separately in `20260729-redteam-log-forging.md`)

Decisions taken by the maintainer on 2026-07-30, after the scan's mechanical round landed.
**No backward compatibility is required** — implementors get a compile error, which is the point.

## What changes

### 1. Level filtering (L4) — both layers, manager decides

- `LogAppender.minLevel: LogLevel` (default `ALL`, i.e. accept everything).
- `UltraLogManager(appenders, minLevel = ALL)` — a global lower bound.
- The manager filters, so an appender is never called with an event it would drop.
- `OFF` and `ALL` are rejected as event levels: they are thresholds, not levels a message is at.
  This is what `LogLevel`'s KDoc always claimed and nothing enforced.

`isEnabled` answers truthfully because the manager knows every appender's bound, so when nothing
accepts a level `log()` returns **before** allocating the timestamp or launching the coroutine.

```kotlin
fun isEnabled(level: LogLevel): Boolean =
    level != LogLevel.OFF && level != LogLevel.ALL &&
    level.severity >= minLevel.severity &&
    appenders.any { level.severity >= it.minLevel.severity }
```

Severity is *higher = more critical* (`OFF` = `Int.MAX_VALUE`, `ERROR` = 500, `TRACE` = 100,
`ALL` = 0), so a bound reads as "accept this level and anything more critical".

### 2. `isEnabled` + lambda overloads on `Log` (L4)

`fun isEnabled(level): Boolean = true` plus `log(level) { }`, `debug { }`, … so an expensive message
is never built for a level nothing accepts, and the guard cannot be forgotten at a call site.

### 3. `LogEvent` (L8)

```kotlin
data class LogEvent(
    val ts: ZonedDateTime,
    val level: LogLevel,
    val message: String,
    val loggerName: String,
    val error: Throwable? = null,
)
```

`LogAppender.append(event: LogEvent)` replaces the 4-arg signature outright. The `Throwable` stops
being concatenated into the message, so structured backends receive a real exception object.

Rejected shape, recorded so it is not revisited: keeping the old 4-arg as a deprecated method with a
default body **alongside** a defaulted `append(LogEvent)` would mutually recurse into a
`StackOverflowError` for any implementor that overrode neither. A clean break cannot fail that way.

### 4. Per-class SLF4J loggers (L9)

`Slf4jAppender` resolves a logger per `event.loggerName` via `LoggerFactory` (which caches), instead
of routing everything through one injected logger. Restores per-package levels, appender routing and
IDE navigation. Two things become dead and go: the abbreviated-name prefix inside the message, and
the appender's private `OFF`/`ALL` special-case (the manager now filters sentinels centrally).

### 5. Dispatcher unchanged (L10)

`Dispatchers.Unconfined` stays. Suspending appenders already get async behaviour; going truly async
without a flush path would make L14 (writes lost at JVM exit) worse. The KDoc already describes the
inline-until-suspension behaviour accurately.

### 6. Kontainer scoping pinned, not fixed (L11)

Add the missing test asserting that injecting `Log` promotes a singleton to `SemiDynamic`, so the
behaviour is visible rather than surprising. The real fix is the `dynamicPrototype` injection type
that `index_jvm.kt:38-40` already asks for — a kontainer change, raised separately.

## Blast radius

| Kind | Sites |
|---|---|
| `LogAppender` impls | `ConsoleAppender`, `Slf4jAppender`, `funktor/insights` `LogCollector.Appender`, `funktor/logging` `KarangoLogAppender`, `MonkoLogAppender` |
| `Log` impls | `LogImpl`, `NullLog`, `ultra/vault` `FallbackLog` |
| Test doubles | `UltraLogManagerDispatchSpec` (3), `LogImplSpec`, `VaultHookScopeSpec`, `MonkoDriverConfigSpec` (3 anonymous `object : Log`) |

Consumers that only *inject* `Log` are unaffected — the convenience methods keep their signatures.

Also folded in while touching these files: **L20** — `LogImpl` re-resolved `caller.qualifiedName` on
every call although `caller` is immutable; hoisted to a constructor-time `val`.

## Spec

- [x] `LogEvent` added; `LogAppender.append(LogEvent)` is the sole abstract method
- [x] `LogAppender.minLevel` with default `ALL`
- [x] `UltraLogManager` global `minLevel`, manager-side per-appender filtering, no launch when nothing accepts
- [x] `Log.isEnabled` + lambda overloads; `Log.error(message, e)` passes the Throwable instead of concatenating
- [x] `Slf4jAppender` resolves per-class loggers and passes the Throwable through
- [x] `ConsoleAppender` renders the stack trace itself (console has nowhere else to put it)
- [x] All five production appenders and three `Log` impls updated
- [x] Kontainer `SemiDynamic` promotion pinned by a test
- [ ] Follow-up task raised for kontainer `dynamicPrototype`

### Two constraints discovered during implementation

**Kontainer resolves every primary-constructor parameter as a service and ignores Kotlin default
values.** Adding `minLevel: LogLevel = ALL` to a constructor therefore breaks registration by class
with an opaque `KontainerInconsistent`. It bit `UltraLogManager` and then `ConsoleAppender`.
Consequences, all deliberate:

- `UltraLogManager` is built by a factory lambda in `Ultra_Logging`, which closes over `minLevel`;
  `ultraLogging(minLevel = ...)` is now a parameterised module, matching the funktor modules.
- `ConsoleAppender` and `Slf4jAppender` take **no** constructor parameters and inherit
  `minLevel = ALL`, so `singleton(ConsoleAppender::class)` — the pattern the module KDoc documents —
  keeps working. They are `open`, so a bounded variant is a two-line subclass.
- `KarangoLogAppender`/`MonkoLogAppender` keep `minLevel` as a constructor parameter, because they
  were already registered through factory lambdas.

**Both DB appenders had already hand-rolled this exact filter** (`if (level.severity >=
minLevel.severity)` inside `append`). Hoisting it to the interface deleted that branch from both and
lets the manager skip the call entirely — the design was already latent in the codebase.

## Test evidence

- [x] `OFF`/`ALL` are never dispatched, from `log()` and from the convenience methods
- [x] A global bound drops levels below it; a per-appender bound drops for that appender only
- [x] An appender above the bound still receives, while one below it does not, for the same event
- [x] `isEnabled` is false when no appender accepts, and the lambda overload does not invoke its block
- [x] The `Throwable` arrives on `LogEvent.error`, not concatenated into `message`
- [x] `Slf4jAppender` calls the `(String, Throwable)` overload, on a logger named after the origin
- [x] Mutation-tested — 6/6 killed, each hitting exactly its own test:
      OFF/ALL rejection, global bound, per-appender bound, lambda guard, Throwable passing,
      per-origin logger routing. Two mutations killed a second test as well, correctly: dropping
      the sentinel check also breaks `isEnabled`, and dropping the global bound also breaks the
      kontainer wiring test — which is what proves `ultraLogging(minLevel = ...)` really reaches
      the manager.
- [x] Full sweep green after deleting every dependent module's compiled test classes:
      log 66, vault 285, reflection 56, slumber 1232, karango/core 1649, monko/core 249,
      funktor/core 772, funktor/logging 12 — **4321 tests, 0 failures**. Compile sweep: 0 errors.

New specs: `LogLevelFilteringSpec` (11), `Slf4jAppenderSpec` (5), plus 3 in
`LoggingKontainerModuleSpec`. Module 48 -> 66 tests.

**The `MonkoDriverConfigSpec` doubles were missed by the initial survey** — `grep ': Log'` finds
named implementors but not `object : Log { }` literals. They only surfaced once the stale test
classes were deleted and the module genuinely recompiled. Now a CLAUDE.md rule.

**A stale-build trap cost real time here and is now recorded in CLAUDE.md.** Gradle held
`:ultra:vault:compileTestKotlinJvm` UP-TO-DATE across the `Log` ABI change, so the compile sweep
reported zero errors while the compiled test double still implemented the old signature. It surfaced
only at runtime as an `AbstractMethodError` wrapped in an `AssertionFailedError` — i.e. it read as a
logic failure, not a build one. `touch` does not invalidate it; the dependent modules'
`build/classes/kotlin/**/test` had to be deleted before "compiles clean" meant anything.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

## Follow-ups

- [ ] Kontainer `dynamicPrototype` injection type (L11's real fix)
- [ ] L1 log forging — mitigations listed in `20260729-redteam-log-forging.md`, not yet decided
- [ ] L14 flush/shutdown path, if the dispatcher ever goes truly async
