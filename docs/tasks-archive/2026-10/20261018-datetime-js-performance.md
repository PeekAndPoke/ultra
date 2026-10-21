# ultra/datetime: performance in the browser (zone offsets, Long)

**Status:** DONE (archived 2026-10-19) — gate PASS after 2 rounds; spross re-profile requested (Spross-Main-Dev)
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

- [x] **Cache zone offsets on JS.** A zone's offset is constant between two transitions (twice a year for
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
- [x] **Cheaper fixed-offset check.** The prefixes became a constant (was a `listOf` per call). A per-id memo was NOT
      added: it would cost a string hash per call (review round 1).
- [x] **The `Long` cost** (measured and reported; see Implementation notes). `kotlin.time.Instant` and `Duration` are `Long`-backed, and Kotlin/JS emulates `Long` in
      software. What ultra can do, to be measured first:
      - keep hot paths of `MpInstant` (compare, minus, plus a `Duration`) from creating intermediate `Long`s or
        boxing them, where kotlinx's API allows (for example comparing epoch seconds and nanos once rather than
        going through a `Duration`);
      - check whether Kotlin 2.4's option to compile `Long` as a JS `BigInt` (an experimental compiler flag for
        ES2020 targets; the exact name and status are unverified, check before relying on it) is faster here or
        slower;
      - `hashCode` showed up at about 5 %: if `MpInstant` or `MpTimezone` hash their `Long`s or strings on every
        map lookup, a cached hash (a `MpTimezone` is immutable and few) may pay.
- [x] **A benchmark** in `jsTest` (or a small tool) that a change is measured with: for example 100,000 sequential
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
- `OffsetIntervalCache` (`ultra/datetime/src/commonMain/kotlin/OffsetIntervalCache.kt`): up to 8 `[from, until)`
  intervals per zone with one offset each, most-recent first, joined when they touch. LAZY since review round 1: a miss
  far from anything known costs ONE lookup (remembered as a single second); a miss within a day of an interval extends
  it (binary search only when the offsets differ — then exactly one transition lies between, ≤ a day apart); a miss
  within an HOUR also probes a day ahead in the direction of travel (sequential walks). After review round 2: the
  transition search also runs only on such dense misses; bounds are `Double` (no emulated `Long` on hits); probes stay
  inside a `domain` (the JS Date range); the JS system zone id is read once. Used only by the JS actual (`TimezoneEngineJs.kt`, one
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
  ULTRA-00211 (mpzoneddatetime overlap offset) — do it there.

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

      After review round 1 (lazy cache, probe-ahead only within an hour), same engine, 2 runs each, vs the cache
      BYPASSED (temporary edit, restored + `cmp`):

      | Access pattern (`atZone(berlin).toLocalDateTime()`) | no cache | eager cache (round 1) | lazy, always probe | lazy, probe ≤ 1 h (final) |
      |---|---|---|---|---|
      | sequential minutes | 24 400 – 29 300 | 9 100 – 9 200 | 10 200 – 12 500 | 9 600 – 9 800 |
      | daily stride | 23 400 – 33 400 | (not measured) | 16 100 – 20 800 | 16 100 – 16 400 |
      | scattered over 50 years | 23 800 – 26 600 | (not measured) | 33 100 – 36 800 | 27 600 – 27 900 |

      Scattered access stays ~5–15 % behind no cache: the lookups are about equal, the rest is the cache's own
      emulated-`Long` bookkeeping, which cannot pay off when nothing repeats. Final `toInstant` 15 500 – 15 800,
      `MpLocalTime` 1 100 – 1 200.
- [x] Final tree after the round-2 batch (2026-10-19): `:ultra:datetime:allTests` jvmTest 2500/0, linuxX64Test 1856/0,
      jsBrowserTest 1868/0; `:ultra:slumber:jvmTest` 1244/0; `:kraft:core-tests:jsBrowserTest` 412/0;
      `:kraft:addons:datetime:jsBrowserTest` 2/0. Compile sweep incl. linuxX64: 0 errors, 0 warnings. docs-site builds.
- [ ] spross browser profile against a snapshot, before/after — REQUESTED from Spross-Main-Dev; append the figures here.

**Final benchmark** (ns/op, ChromeHeadless 155, machine load average 4–5 during these runs, so compare rows within a
run): sequential `atZone` 10 000 – 12 600 vs no cache 23 900; `toInstant` 16 300 – 17 700 vs 26 700; daily stride
14 100 – 15 900 (one outlier 34 900) vs 24 800; scattered 21 700 – 26 200 vs 24 300; `MpLocalTime` 1 200.

**Consumer notes for the 0.112.0 release:** `MpLocalTime` API is `Int` (`ofMilliSeconds`, `ofSecondOfDay`,
`inWholeMilliSeconds`, `MpLocalTimeSlot.ofSecondsOfDay`); stored values unchanged. Docs: `mp-local-time.astro` updated.

## Review record (the ledger, filled by /feature-review)

Change set: `842bdc9e` (base `08e3c8c3`). Round 1 BLIND, three `reviewer-high` (I = implementation & style,
D = domain, S = security).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR (fixed, see the access-pattern table) | `OffsetIntervalCache.kt:34-48` | Every miss costs 3 Intl calls and intervals only cover ±1 day around probed points, so cold / random-access callers pay 2–3x MORE than without the cache (cold `toInstant` 6 vs 3; daily stride misses every call); the benchmark only measures the sequential best case (D) | fix | Verified by tracing `remember` + `resolve`: true. Fix: lazy intervals — a cold miss costs ONE lookup and stores the point; only a miss within a day of a known interval extends it (plus one probe ahead in the direction of travel), so sequential stays ~2 lookups per simulated day and cold/daily-stride drop to ~1 per call; benchmark + lookup-count spec gain random-access and daily-stride rows |
| 1 | MINOR | `MpLocalTime.kt:83` | `of()` wraps only the hour; minute/second/milli overflow Int silently (`of(0, 35_792)`, `of(0, 0, 3_000_000)`) (I, D, S) | fix | Verified REGRESSION: at `08e3c8c3` `of()` computed in Long (`* 1_000L`). Each component wrapped by its own day multiple |
| 1 | MINOR | `OffsetIntervalCacheJvmSpec.kt:17` | The "no two transitions within a day" assumption is checked only indirectly and only 1900–2040; pre-1900 LMT and Morocco/Palestine predictions to the 2080s are unchecked (D) | fix | Probe run 2026-10-19: 0 pairs ≤ 1 day apart in ANY JDK zone from its first transition to 2100. Pinned as a direct JVM test |
| 1 | MINOR | `OffsetIntervalCache.kt:8` | KDoc "an interval grows by at most a day per probe" is false (joins) (I) | fix | KDoc rewritten with the lazy design |
| 1 | MINOR | `TimezoneEngine.kt:28,160` | Spec item "cheaper fixed-offset check" neither done nor declined; spec boxes unticked (I) | fix (record) | Recorded: prefixes made constant; a per-id memo would cost a string hash per call — not added; boxes ticked |
| 1 | MINOR | `jvmMain/kotlin/conversion.kt:117` | KDoc says `java.time.LocalDate`, returns `LocalTime` (I, pre-existing on a touched line) | fix | Factually wrong |
| 1 | MINOR | `TimezoneEngineJs.kt:93-104` | Case variants of Intl-known ids MISSING from `supportedIds` (`Europe/Kyiv`, `America/Ciudad_Juarez`, `Pacific/Kanton`) still pass → per-spelling formatter + cache growth (S) | maintainer list | Real, pre-existing key set (same as `formatterCache`); fix would compare `resolvedOptions().timeZone` ignoring case |
| 1 | MINOR | `OffsetIntervalCache.kt:37-88` | Worst case per miss rose from 1 to ~20 Intl calls; adversarial instants near many transitions defeat the 4-entry LRU (S) | ~~fix (by the MAJOR's fix)~~ REOPENED in round 2 | Round-2 S and I named the factual error: `resolve` itself creates two looked-up points < a day apart (`wall − DAY`, `wall − before`), so a cold `toInstant` within ~a day after a transition still runs the ~17-step search (~20 calls vs 4). Also "less than a day" → the code searches spans up to exactly a day. See round-2 row |
| 1 | MINOR | `MpLocalTimeSlot.kt:173-180` | `splitWithGaps` loops forever (memory DoS) when `duration (+ gap)` wraps past midnight, e.g. `24.hours` (S) | out-of-scope → follow-up task | Verified by reading; PRE-EXISTING (old `%` wrapped the same). `docs/tasks/20261019-mplocaltimeslot-splitwithgaps-loop.md` |
| 1 | MINOR | `MpLocalTimeSerializer.kt:13` | Descriptor says `PrimitiveKind.STRING` but encodes a Long (D, I; pre-existing) | maintainer list | Pre-existing; matters for descriptor-driven formats only |
| 1 | MINOR | `jvmMain/kotlin/conversion.kt:112` | JVM `LocalTime.mp` drops milliseconds, the kotlinx one keeps them (I, pre-existing) | maintainer list | Pre-existing |
| 1 | MINOR | surplus | `MpLocalTime` Int API was not asked for by the profile (I) | reject | Settled maintainer decision 2026-10-18 ("Switch to Int"), recorded under Decisions |
| 1 | MINOR | surplus | Benchmark runs on every `jsBrowserTest` (~1–2 s) (I, D) | maintainer list | Real cost; could be gated by a flag |

**Probed and CLEAN** (reviewers): interval bounds inclusive/exclusive; join soundness; no overflow / infinite loop in
the binary searches; no half-built interval after a throwing lookup; no prototype keys (`Object.create(null)`); cached
answers never outside the JS Date range; `shift()` clamping and nanos; MpLocalTime decode wrap for every Long incl.
`Long.MIN_VALUE`, NaN/∞ via slumber; benchmark method adequate for the SEQUENTIAL figures; Intl stable per process.

Fix-batch mutations (each RED, restored + `cmp`): near-threshold 30 days → RED only after adding "points with the
same offset more than a day apart are not joined across a short period of another offset" (the sweep cannot see it:
no real zone has transitions that close — that is the assumption); backward and forward branch without the binary
search → RED only after adding "a point learned on the other side of a transition is not stretched over it" (after a
probe ahead the bounds are already exact, so the sweep never reaches those branches with a coarse bound); probe ahead
on every miss → RED ("costs one lookup (no probe ahead)"); never probe ahead → RED (sequential lookup count); cold
miss probing → RED ("exactly one lookup"); `MpLocalTime.of` regression pinned by "of() wraps every component".

Fix-batch final tree (2026-10-19): `:ultra:datetime:allTests` jvmTest 2498/0, linuxX64Test 1854/0, jsBrowserTest
1866/0; `:ultra:slumber:jvmTest` 1244/0; `:kraft:addons:datetime:jsBrowserTest` 2/0.

**Verdict:** round 1 NOT clean (1 MAJOR fixed) → round 2 (`reviewer-xhigh`, two-phase) after the fix batch.

**Round 2** (`reviewer-xhigh`, two-phase, change set `842bdc9e`+`f3ee6961`). Process note: all three reviewers
DISCLOSED that their phase-1 Read of this file returned the Review record too, so phase 1 was not strictly blind; their
findings were traced from the code and they reconciled in phase 2. Zero CRITICAL/MAJOR in phase 1; phase 2 reopened
one round-1 MINOR (row above) and withdrew two duplicates. All FIXED entries confirmed holding at `f3ee6961`; no
rejected CRITICAL/MAJOR exists. **Round 2 CLEAN → loop ends; open MINORs go to the maintainer as a list.**

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 | MINOR | `OffsetIntervalCache.kt:50-57`, `TimezoneEngine.kt:118-123` | Cold `toInstant` within ~a day after a transition: `resolve`'s probes form a pair < a day apart → ~17-step bisect, ~20 Intl calls vs 4 (S1, I) | fixed (maintainer-chosen MINOR batch) | `learn` bisects only on a DENSE miss; mutation → "two points less than a day apart … cost no search" RED |
| 2 | MINOR | `OffsetIntervalCache.kt:77-79` + `TimezoneEngineJs.kt:47` | A dense probe ±DAY can fall outside the JS Date range → `offsetSecondsAt` throws for an in-range instant (years ±275 760) (S2, I3) | fixed (batch) | `domain` parameter (JS passes ±8.64e12 s); mutation → "a probe ahead outside the lookup's domain is skipped" RED |
| 2 | MINOR | `OffsetIntervalCacheSpec.kt:86-94` | Threshold test pins `DAY` only within ~7 days (`DAY = 2*86_400` survives) (I1) | fixed (batch) | 1.5-day period, points ~1.9 days apart; mutation `DAY = 2 * 86_400` → RED |
| 2 | MINOR | `OffsetIntervalCacheSpec.kt:75` | Daily-stride test says "across both transitions" but crosses one (I2) | fixed (batch) | `repeat(110)` |
| 2 | MINOR | `OffsetIntervalCache.kt:61-66` | The wide-span `learn` branches are never checked against real zone data: walks never re-ask the seconds between two points (D2, corrected in phase 2) | fixed (batch) | `recheckTransitions` after the daily and hourly walks; mutation "stretch `near` over the transition" → the daily walk RED |
| 2 | MINOR | `OffsetIntervalCache.kt:7` | KDoc "what the IANA data guarantees" — IANA guarantees no spacing; it is a pinned property of the JDK's copy (D4) | fixed (batch) | "a property of the current tz data, not a guarantee of it" |
| 2 | MINOR | `TimezoneEngineJs.kt:71-75`, `MpTimezone.kt:25-29` | `systemDefault` builds an `Intl.DateTimeFormat` on EVERY access (~25 per-row call sites in funktor/inspect) (D1) | fixed (batch) | `TimezoneEngineJs.kt` reads the id once (`lazy`); a tab sees an OS zone change on reload. Not separately measured |
| 2 | MINOR | `OffsetIntervalCache.kt:20` etc. | `Double` bounds instead of `Long` could remove the scattered-access penalty (D3) | fixed (batch, measured) | Interval bounds are `Double`. Same-load runs: scattered 21 700 – 26 200 ns vs Long 30 400 – 34 200 vs no cache 24 300; daily stride 14 100 – 15 900 (one outlier 34 900) vs Long 18 600. Mutation: midpoint without `floor` → 5 specs RED |
| 2 | note | `TimezoneEngine.kt:115-119` | `resolve`'s ±1-day window needs transitions > ~2 days apart; the pin checks 1 day (I) | fixed (batch) | Pin widened to `<= 2 * 86_400` |
| 2 | note | `docs-site/src/pages/ultra/datetime/mp-local-time.astro:37-38` | Docs still show `ofMilliSeconds(52_215_000L)` / `ofSecondOfDay(52_215L)` — no longer compiles (I, D, S) | fixed | `mp-local-time.astro` uses Int literals |
| 2 | note | task Spec "Cheaper fixed-offset check" | Stale proposal sentence kept after the "NOT added" note (I) | fixed | Spec item rewritten |

**Verdict: gate PASS** (round 2 clean; no open CRITICAL/MAJOR). The maintainer chose to apply every round-2 MINOR
(2026-10-19); per review-loop a MINOR batch takes no further review round. Each fix was mutation-checked (above).
Still on the maintainer list from round 1: case variants of ids missing from `supportedIds`; `MpLocalTimeSerializer`
descriptor kind; JVM `LocalTime.mp` dropping milliseconds; the benchmark running on every `jsBrowserTest`.
