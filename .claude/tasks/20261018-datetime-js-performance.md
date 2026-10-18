# ultra/datetime: performance in the browser (zone offsets, Long)

**Status:** IN REVIEW (Ultra-Main-Dev) — implemented 2026-10-19, `/feature-review` next
**Plan:** none. Reported by spross (2026-10-18) from a CPU profile of its browser dashboard; written by the spross
agent at the maintainer's request.
**Security-critical:** no (it touches time-zone resolution, which persistence relies on: mutation-check bar)

## Why

spross runs its greenhouse simulation and its automation interpreter in the browser, one simulated minute per step, up
to 1440 steps per real second. A CPU profile of that run (Chrome DevTools protocol, headless Chrome without a GPU,
4 s while the run was going, ultra 0.111.0, Kotlin 2.4.21) showed ultra/datetime and Kotlin's `Long` emulation among
the largest costs. Shares of all CPU time:

| What | Self | Inclusive |
|---|---|---|
| `offsetSecondsAt` (the JS `platformOffsetSecondsAt`, `Intl.DateTimeFormat.formatToParts`) | 6.0 % | 7.7 % |
| `atZone` (instant to zoned) | | 8.8 % |
| Kotlin `Long` emulation: `add` 5.3, `compare` 2.2, `isNegative` 1.4, `equalsLong` 1.2, `divide` 1.2, `invert` 1.1, `multiply` 0.6, `new Long` 0.8, `isZero` 0.7, `boxedLong` init 0.5 | about 15 % | |
| `hashCode` (two entries, 3.0 and 2.3; `InternalHashMap` around them) | about 5 % | |
| garbage collector | 3.5 % | |

For scale: spross's whole interpreter tick was 33 % inclusive and its simulation step 14 %; the `Long` work sits
mostly under them, in `kotlin.time.Instant` and `Duration` arithmetic (comparisons, `at - last >= interval`, `plus`).
The callers of `offsetSecondsAt`, by share: `atZone` through `ofLocal` / `localInfo` 3.2 %, `toLocalDateTime` through
`atZone` from the interpreter's local minute 1.7 %, `toInstant` 1.0 %, the simulation's step and state 0.7 % each.

## What the code does today (read 2026-10-18 in the ultra working tree)

The profile above ran on the released 0.111.0, which still resolved zones through the older Intl to js-joda provider
(the `ofLocal` / `localInfo` frames). The code below is the new zone engine in the working tree (`829f6a59`,
`fed9393d`, unpushed on 2026-10-18), which the next release ships; the profile is to be retaken against a snapshot of
it before and after the cache (Ultra-Main-Dev, 2026-10-18). Separately, `MpLocalTime` is the only datetime class that
stores a `Long`; moving it to `Int` is on Ultra-Main-Dev's list.

- `TimezoneEngine.offsetSecondsAt` (`ultra/datetime/src/commonMain/kotlin/TimezoneEngine.kt:27`) tries
  `fixedOffsetSecondsOrNull(zoneId)` (a `startsWith` chain and a regex for offset ids) and otherwise calls
  `platformOffsetSecondsAt`. Nothing is cached.
- The JS `platformOffsetSecondsAt` (`ultra/datetime/src/jsMain/kotlin/TimezoneEngineJs.kt:24`) formats the instant
  with `Intl.DateTimeFormat.formatToParts` (the formatter is cached per zone by `formatterFor`), builds an object from
  the parts, rebuilds a `Date` with `setUTCFullYear` and `setUTCHours`, and takes the difference: a few microseconds
  per call, every call.
- `toInstant` (local to instant) goes through `resolve` (`TimezoneEngine.kt:109`): 3 to 4 platform lookups (wall
  minus and plus `probeWindow`, then `isValid`). `toLocalDateTime` and `startOfDay` add one or more each.
- spross calls these several times per simulated minute: `SimEngine` takes the local date in three places
  (`sim/src/commonMain/kotlin/SimEngine.kt:176, 281, 576`); the interpreter takes the local minute for every
  schedule trigger and every time-of-day expression (`logic/src/commonMain/kotlin/interpreter/Interpreter.kt:266,
  289, 424`). At 1440 steps per second that is tens of thousands of `Intl` calls per second.

## Spec

- [ ] **Cache zone offsets on JS.** A zone's offset is constant between two transitions (twice a year for
      `Europe/Berlin`, never for many zones). Proposal: per zone, keep the last interval known to have one offset,
      `[from, until)`, with exact bounds from the binary search `TimezoneEngine` already has (`transitionAfter`,
      `TimezoneEngine.kt:132`), and answer an instant inside it with a range check, no `Intl` call. A sequential
      caller (a simulation, a chart's axis, a list of dates) then hits the cache almost always; a small number of
      intervals per zone (a handful, least recently used) covers callers that jump around. `resolve` benefits too,
      since its probes at plus and minus one day land in the same interval except near a transition.
      - Where: the JS actual (single-threaded) is the simplest home. In `commonMain` the cache would need to be
        safe under JVM threads, and java.time's `ZoneRules` is already fast there, so the JVM likely needs none:
        measure before adding one.
      - Correctness: the interval's bounds must be exact (to the second, as `transitionAfter` gives); a row per
        zone kind (a region with DST, a region without, a fixed offset id, a zone with a historical offset change),
        instants just before, at and after a transition, cache hits and misses giving the same answers as the
        uncached path over a sweep (every hour of a year in `Europe/Berlin`, `America/New_York`,
        `Australia/Lord_Howe` with its half-hour shift, `Pacific/Apia` with its skipped day).
- [ ] **Cheaper fixed-offset check.** `fixedOffsetSecondsOrNull` runs on every call; caching its answer per zone id
      (null included) removes the string work from the hot path.
- [ ] **The `Long` cost.** `kotlin.time.Instant` and `Duration` are `Long`-backed, and Kotlin/JS emulates `Long` in
      software. What ultra can do, to be measured first:
      - keep hot paths of `MpInstant` (compare, minus, plus a `Duration`) from creating intermediate `Long`s or
        boxing them, where kotlinx's API allows (for example comparing epoch seconds and nanos once rather than
        going through a `Duration`);
      - check whether Kotlin 2.4's option to compile `Long` as a JS `BigInt` (an experimental compiler flag for
        ES2020 targets; the exact name and status are unverified, check before relying on it) is faster here or
        slower;
      - `hashCode` showed up at about 5 %: if `MpInstant` or `MpTimezone` hash their `Long`s or strings on every
        map lookup, a cached hash (a `MpTimezone` is immutable and few) may pay.
- [ ] **A benchmark** in `jsTest` (or a small tool) that a change is measured with: for example 100,000 sequential
      minute steps of `atZone(...).toLocalDateTime()` and `toInstant(zone)` in `Europe/Berlin`, timed, before and
      after. The figures go into this file.

## How spross measured it (to reproduce)

`./gradlew :app-web:jsBrowserDevelopmentExecutableDistribution` in `/opt/dev/peekandpoke/spross`, serve
`app-web/build/dist/js/developmentExecutable` statically, open it in headless Chrome with remote debugging, click
"Simulation", "1440x", "Automation off" (to turn it on) and "Play", then `Profiler.start` and `Profiler.stop` over
4 s; self and inclusive times per function from the profile's nodes. The spross agent can rerun it on request
(`Spross-Main-Dev`).

## Implementation notes

**Decisions (maintainer, 2026-10-18, plan re-confirmed before implementing):**
- The interval cache is a COMMON class wrapping any "offset at instant" function; only the JS actual uses it at runtime
  (no JVM threading), and a JVM test runs it against java.time over every zone 1900–2040 — the same sweep as the engine.
- `MpLocalTime` stores an `Int` and its API switches to `Int` (`ofMilliSeconds`, `ofSecondOfDay`,
  `inWholeMilliSeconds`, `MpLocalTimeSlot.ofSecondsOfDay`) — breaking, accepted; the wire value stays a number.
- Order: benchmark first (baseline), then each change measured against it. `Long`-as-`BigInt` and `hashCode`: measure
  and report before changing anything.

**What was done (2026-10-18/19):**
- Benchmark `ultra/datetime/src/jsTest/kotlin/DatetimeJsBenchmarkSpec.kt` (20 000 sequential minute steps, ChromeHeadless
  in the Karma suite). Browser console output is not captured, so the figures are measured while the spec registers its
  tests and land in the TEST NAMES (`BENCH …: N ns/op` in the jsBrowserTest XML). Run with
  `./gradlew :ultra:datetime:jsBrowserTest --rerun` — without `--rerun` Gradle serves the previous XML. Noise between
  runs is ~20 %, more while other builds run on the machine.
- `OffsetIntervalCache` (`ultra/datetime/src/commonMain/kotlin/OffsetIntervalCache.kt`): up to 4 `[from, until)`
  intervals per zone with one offset each, most-recent first, joined when they touch; a miss costs 3 lookups (±1 day)
  plus a ~17-step binary search per side with a transition. Used only by the JS actual (`TimezoneEngineJs.kt`, one
  cache per zone id that resolved). Checked against java.time by `OffsetIntervalCacheJvmSpec`: every JDK zone, probes
  around every transition 1900–2040 in four orders (1 015 008 checks, 0 mismatches) + hourly walks 1970–2040 for
  Berlin, New York, Lord Howe, Apia, Casablanca; `OffsetIntervalCacheSpec` (common) counts lookups.
- `TimezoneEngine` internals on plain epoch seconds instead of `Instant ± Duration`; the platform primitive takes
  `epochSeconds: Long`. Fixed-offset prefixes are a constant (was a `listOf` per call).
- `MpLocalTime` stores and exposes `Int` (wire value still written as a `Long`; decode wraps any `Long` into the day).
- `-Xes-long-as-bigint` (exists since Kotlin 2.2.20, experimental): with `target = es2015` it compiles but the generated
  code still uses the emulated `Long` (`add_…`/`compare_…` calls in the module `.mjs`), and `target = "es2020"` is
  rejected by Kotlin 2.4.21 ("Unsupported ECMA version"). No gain measurable here; not pursued.
- `hashCode`: no Kotlin `HashMap` is on ultra's hot path any more (JS caches are prototype-free JS objects), so the
  profile's ~5 % is most likely on the caller's side (map keys) — re-profile spross to confirm.
- **Biggest remaining ultra-side cost:** `MpZonedDateTime`'s init recomputes its instant from local time + zone
  (`MpZonedDateTime.kt:151`), so `instant.atZone(zone)` resolves the zone twice, and `kotlin.time.Instant`/`Duration`
  arithmetic is emulated-`Long` work. Passing the known instant in is the same redesign as
  `.claude/tasks/20261018-mpzoneddatetime-overlap-offset.md` — do it there.

## Test evidence

- [x] Unit/behavior tests, mutation-checked (each restored + `cmp`): cache skips the transition search → RED
      (`OffsetIntervalCacheSpec`, both JVM sweeps); cache joins intervals of different offsets → RED; `MpLocalTime.plus`
      without the day reduction (Int overflow past ~24.8 days) → RED; slumber `MpLocalTime` slumberer returning `Int`
      → RED (type pin, `MpLocalTimeCodecSpec`; with the declared `Long?` the mutant does not even compile); slumber
      awake and kotlinx deserialize truncating instead of wrapping → RED.
- [x] Final tree (2026-10-19): `:ultra:datetime:allTests` jvmTest 2490/0, linuxX64Test 1848/0, jsBrowserTest 1858/0;
      `:ultra:slumber:jvmTest` 1244/0; `:kraft:core-tests:jsBrowserTest` 412/0; `:kraft:addons:datetime:jsBrowserTest`
      2/0. Compile sweep incl. linuxX64: 0 errors, 0 warnings.
- [x] Benchmark (`DatetimeJsBenchmarkSpec`, ns/op, ChromeHeadless 155; baseline = 3 runs, final = 2 runs):

      | Operation | Baseline (new engine, no cache) | Final |
      |---|---|---|
      | `instant.atZone(berlin).toLocalDateTime()` | 33 000 – 39 000 | 9 100 – 9 200 |
      | `local.toInstant(berlin)` (incl. the loop's own UTC stepping) | 34 000 – 43 000 | 15 200 – 15 400 |
      | `berlin.offsetSecondsAt(instant)` (incl. the loop's `+ 1.minutes`) | 5 800 – 7 000 | 1 900 – 2 000 |
      | instant compare / minus / plus (untouched; `kotlin.time` Long) | 3 700 – 4 600 | 3 600 |
      | `MpLocalTime` plus / hour / minute | 2 500 – 3 100 | 1 000 – 1 100 |

      Intermediate: cache only → `atZone` ~21 600, `toInstant` ~24 000; + epoch-second engine → ~11 000 / ~18 700.
- [ ] spross browser profile against a snapshot, before/after (Spross-Main-Dev)

## Review record (the ledger, filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**
