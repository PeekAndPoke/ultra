# ultra/datetime: own epoch ↔ local date-time conversion (no emulated Long on JS)

**Status:** DONE (archived 2026-10-19) — gate PASS after 3 rounds (round 3 with the maintainer in the loop)
**Plan:** follow-up of `docs/tasks-archive/2026-10/20261018-datetime-js-performance.md`; maintainer: before the 0.112.0 release (2026-10-19)
**Security-critical:** no — but every zoned value goes through it, persistence included (mutation-check bar)

## Why

spross profiled the 0.112.0-SNAPSHOT (`60bdd5d3`) against 0.111.0 in the browser (headless Chrome 155, its simulation at
1440 sim-min/s, 3 alternating runs each): the Intl zone lookups are gone (`offsetSecondsAt` 6.6 % → 0.2 % of wall time),
but datetime as a whole costs ~40 % MORE per simulated minute (all datetime stacks 8.1 % → 11.4 %). The new cost is
kotlinx-datetime 0.8's pure-Kotlin epoch ↔ `LocalDateTime` conversion, all emulated-`Long` work:
`toEpochDays < toEpochSecond < toInstant_0 < localDateTimeToInstant` (3.6 %) and
`fromEpochDays < toLocalDateTimeImpl < toLocalDateTime_0 < instantToLocalDateTime` (4.7 %). On 0.111 (kotlinx 0.6.2)
that work went through js-joda and cost nothing in `Long` terms.

## Spec

- [x] `TimezoneEngine` splits epoch seconds + offset into date and time itself, and back, with `Int`/`Double`
      arithmetic (Hinnant's days-from-civil / civil-from-days) instead of kotlinx's `toLocalDateTime(UTC)` /
      `toInstant(UTC)` / `atStartOfDayIn(UTC)`; kotlinx stays the holder of the values. Outside the range where
      `Int` days and `Double` seconds are exact, fall back to kotlinx.
- [x] Parity: a common test comparing the own conversion with kotlinx's over a wide spread of instants, offsets and
      dates (incl. negative years, leap days, centuries, the fallback edges), on all three targets; the existing
      ZoneParity table + JVM sweeps stay green.
- [x] Measured: the benchmark rows before/after; then spross re-profiles against a snapshot.
- [x] Mutation-checked (persistence bar).

## Implementation notes

- `ultra/datetime/src/commonMain/kotlin/EpochMath.kt`: civil-from-days / days-from-civil (Hinnant) on `Int` days and
  `Double` seconds; kotlinx takes over beyond ±1 million years (`MAX_EXACT_YEAR`), where `Int` days would overflow.
- `TimezoneEngine` runs on `Double` epoch seconds (`resolve` probes, the platform primitive, `OffsetIntervalCache`)
  wherever a Double holds every whole second (|s| < 2^52); from 2^52 on it takes an exact Long path
  (`platformOffsetSecondsAtExact`; JS never gets there). `transitionAfter` bisects on Long. Results are always built
  by `EpochMath` from exact values. A JS conversion creates no emulated `Long` except reading `Instant.epochSeconds`
  and building the result `Instant`. Date-based `plus` still adds the
  date through kotlinx (`LocalDate.plus`) — not on spross's hot path.
- Tests: `EpochMathSpec` (common, all targets) compares with kotlinx on 20 000 pseudo-random instants over ±12 000
  years, the exact-range edges and the fallback range near `Instant`'s own limits, plus calendar edge dates.

## Test evidence

- Final tree (2026-10-19): `:ultra:datetime:allTests` jvmTest 2503/0, linuxX64Test 1859/0, jsBrowserTest 1871/0;
  `:ultra:slumber:jvmTest` 1244/0; `:kraft:core-tests:jsBrowserTest` 412/0; `:kraft:addons:datetime:jsBrowserTest`
  2/0. Compile sweep incl. linuxX64: 0 errors, 0 warnings.
- Mutations (each RED, restored + `cmp`): no fallback; year carry for Jan/Feb dropped; days-from-civil March shift
  dropped; truncating instead of flooring division (negative instants); century rule dropped — each fails
  `EpochMathSpec` and, for most, `ZoneParitySpec` / `Kronos` / Genesis–Doomsday specs too.
- Benchmark (ns/op, ChromeHeadless 155, load average ~5; before = the `60bdd5d3` figures, after = `fd8eb172`; the
  final tree `79284123` measured `atZone` 2 740, `toInstant` 3 650, daily stride 6 550, scattered 13 800 — see the
  ledger):

  | Row | before | after |
  |---|---|---|
  | `instant.atZone(berlin).toLocalDateTime()` sequential | 10 000 – 12 600 | 3 100 – 3 700 |
  | `local.toInstant(berlin)` | 16 300 – 17 700 | 3 900 – 4 200 |
  | `atZone`, daily stride | 14 100 – 15 900 | 7 600 – 9 100 |
  | `atZone`, scattered over 50 years | 21 700 – 26 200 | 18 600 |

- **spross browser profile** (Spross-Main-Dev, 2026-10-19; spross 7e5d09b, headless Chrome 155, 1440x, 3 alternating
  runs, development builds), inclusive % of wall time, 0.111.0 → snapshot `fd8eb172` [the `60bdd5d3` snapshot]:
  `atZone` 8.5 → 1.5 [9.7]; `toInstant` 2.8 → 1.0 [6.1]; `MpZonedDateTime` init 2.8 → 0.7 [4.6]; `offsetSecondsAt` 7.2
  → 0.2 [0.2]; kotlinx `localDateTimeToInstant`/`instantToLocalDateTime` 0 → 0.0 [3.8/4.4]; **all datetime stacks 8.7 →
  1.7 [11.4]**. Remaining `Long` work under datetime ~0.7 points. Seen on the caller's side only:
  `kotlin.time.Instant.plus(Duration)` (spross calls it per stored sample per tick; spross fixes its loop).

## Review record — the LEDGER (filled by /feature-review)

Change set `fd8eb172` (base `60bdd5d3`). Round 1 BLIND, three `reviewer-high` (I, D, S).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | CRITICAL | `TimezoneEngine.kt:136-147` | `transitionAfter` bisects on Double; beyond 2^53 s the midpoint rounds onto `lo`/`hi` and the loop NEVER ends — a start of day with midnight in a gap (Asia/Beirut, Havana, Santiago, Cairo) at year ~285M+ hangs a JVM/native worker thread; reachable from stored `ts`, ISO strings, `MpLocalDate.atStartOfDay`/`atTime`/`toRange`, `DateTimeRangeConverter` (S) | fix | CONFIRMED by experiment: `FarFutureZoneJvmSpec` (thread join 10 s) — worker still alive. Regression of this commit (base bisected on Long). Fix: Long bisection in the engine; progress guard in the cache's Double bisection |
| 1 | MAJOR | `EpochMath.kt:29-30, 60-61, 70-71, 78-79` + `TimezoneEngine.kt` callers | Beyond 2^53 s the "kotlinx fallback" gets an already-rounded Double: dates up to ±2 s off, and a stored `ts` ≥ ~9.007e18 ms CHANGES on decode → re-encode (S; I and D raised it as MINOR) | fix | Verified by reading; the MAJOR (persistence) severity is the reviewer's and stands. Fix: Long across every inexact boundary — `localDateTime(epochSeconds: Long, offset, nanos)`, `instant(local, offset)` / `instant(date, offset)` exact via kotlinx beyond the exact range; tests with odd seconds above 2^53 against Long oracles + a `ts` re-save test |
| 1 | MINOR | `EpochMathSpec.kt:27-28` | "far beyond" values are round (exact in Double) so the fallback test passes for the wrong reason; the comment "days no longer fit an Int" is false for ±4e13 (I, D, S) | fix | Odd values above 2^53 added; comment corrected |
| 1 | MINOR | `EpochMathSpec.kt:45` | Expected value in the fallback range is the same lossy `.toDouble()` as the code (I, S) | fix | Oracle becomes kotlinx's Long result, compared as Long |
| 1 | MINOR | `EpochMath.kt:17-19` | KDoc says kotlinx "does the conversion" beyond the exact range — implies exactness that was not there (S) | fix | Rewritten with the fix |
| 1 | MINOR | surplus | `EpochMath.epochSeconds` fallbacks unreachable/useless as written (I) | fix (superseded) | The Long-boundary rewrite replaces them |

**Probed and CLEAN** (reviewers): Hinnant formulas line by line (era floor, March shift, Jan/Feb carry, year 0, negative
years, century/400 rules); Int division truncates on every target; exactness of the ±1 M-year path (Int days to
±5.88 M years, Double seconds ≤ 3.2e13, `floor(s/86400)` cannot misround); nanoseconds preserved; every cache/platform
caller passes whole seconds; no new exception types; `toLong` saturation unreachable; JS Date-range behaviour unchanged;
benchmark credible as a relative gain. Remaining cost: `MpZonedDateTime` init re-resolving + `Instant` construction —
the overlap follow-up task.

Fix batch (2026-10-19): `transitionAfter` bisects on Long again (`TimezoneEngine.kt`); `EpochMath` keeps Long across
every inexact boundary (`localDateTime(epochSeconds: Long, offset, nanos)`, `instant(local, offset)`,
`instant(date, offset)`, `epochSecondsExact(date)`); progress guard in `OffsetIntervalCache.firstChangeAfter`.
New tests: `FarFutureZoneJvmSpec` (the hang, 10 s join), odd seconds above 2^53 with kotlinx Long oracles, a stored
`ts` of 2^53+1 s surviving decode → re-encode. Mutations RED (restored + `cmp`): Double midpoint in `transitionAfter`
→ the hang test; fallback fed the rounded Double (both directions) → `EpochMathSpec` + the `ts` test. The cache guard
has no test of its own (the JS domain keeps it far below 2^53). Final tree: `:ultra:datetime:allTests` jvmTest
2505/0, linuxX64Test 1860/0, jsBrowserTest 1872/0; slumber 1244/0; kraft core-tests 412/0; kraft datetime addon 2/0;
compile sweep 0 errors / 0 warnings. Benchmark held: `atZone` 3 400 – 3 500, `toInstant` 4 400, daily stride
8 100 – 8 300, scattered 16 700 – 17 300 ns/op.

**Verdict:** round 1 NOT clean (1 CRITICAL + 1 MAJOR) → fix batch → round 2.

**Round 2** (`reviewer-xhigh`, two-phase; the reviewers read the task only up to the ledger this time). Phase 1 is
the basis below; the SAFETY VALVE fired (two rounds without a clean one), so phase 2 was not run and the open
findings went to the maintainer, who chose "exact Long path, then round 3" (2026-10-19).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 | MAJOR | `TimezoneEngine.kt:25,38,94,118-126` + `TimezoneEngine{Jvm,Native}.kt` | Beyond 2^53 s the zone LOOKUP still takes a rounded Double: one or two seconds before each DST transition (transitions ≡ 0 mod 4) the offset AFTER the transition comes back. A stored `MpZonedDateTime` `ts` there decodes an hour off and re-encodes 3 600 000 ms different (JVM/native; years 285M–292M); regression (base looked up with Long); the "offset is not affected" comment is false (D, S MAJOR; I MINOR) | fix | Verified by reasoning, then by test: `FarFutureZoneJvmSpec` (300 java.time comparisons around transitions in years 290M and 600M, Berlin `ts` re-save) — RED with the routing mutated. Fix: `platformOffsetSecondsAtExact(zoneId, Long)` for \|s\| ≥ 2^52; `resolve` is one inline function fed by a fast Double or an exact Long probe; `transitionAfter` probes through the same router; comments corrected |
| 2 | MINOR | `EpochMath.kt:104-106` | The `instant(date, offset)` fallback cannot fail any test (midnights are exact Doubles; only offset 0 tested) (I) | fix | Calendar rows over odd offsets (1, −1521, 3600, −9001); deleting the branch → RED |
| 2 | MINOR | `TimezoneEngine.kt:50,69` | `wall` computed before the fixed-offset early return (wasted on every UTC encode/decode) (D, S, I) | fix | Fixed-offset check first |
| 2 | MINOR | `EpochMath.kt:84-91` | `epochSecondsExact` redundant (S, I) | fix | Removed; `instant(date, 0).epochSeconds` |
| 2 | MINOR | task file line 33 | Says `transitionAfter` runs on Double (D) | fix | Restated |
| 2 | MINOR | `FarFutureZoneJvmSpec.kt:11-12` | KDoc "within a couple of seconds" false for the local time (I, D, S) | fix | Spec rewritten: exact equality with java.time |

**Probed and CLEAN** (round 2): Hinnant formulas and Int ranges again; both fast-path thresholds exact; fallbacks
get exact Longs; `transitionAfter` (Long) and the cache's guarded bisection terminate for any input; UTC/fixed-offset
round trips stable for every Long `ts`; no new exception types; JS unaffected beyond the Date range. Pre-existing,
not from this change: JVM `DateTimeException` within a day of year ±999 999 999 in DST zones (`resolve` probes ±1
day); JS `offsetSecondsAt` NaN → 0 within |offset| of the ±8.64e15 ms limit; the overlap round trip (own task).

Round-2 fix batch, final tree (2026-10-19): `:ultra:datetime:allTests` jvmTest 2507/0, linuxX64Test 1860/0,
jsBrowserTest 1872/0; slumber 1244/0; kraft core-tests 412/0; kraft datetime addon 2/0, pixijs addon 14/0; compile
sweep 0/0. Mutations RED (restored + `cmp`): lookup always via Double → all 3 `FarFutureZoneJvmSpec` rows;
`toInstant` always on the fast path → 2 rows; `instant(date, offset)` fallback deleted → `EpochMathSpec` calendar
rows. Benchmark: `atZone` 2 740, `toInstant` 3 650, daily stride 6 550, scattered 13 800 ns/op.

**Round 3** (`reviewer-top`, two-phase; maintainer in the loop). Phase 1: **zero CRITICAL/MAJOR in the change set**
from all three charters (reviewer 1 disclosed that a Grep returned nine ledger lines; analysis from the code).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 3 | MAJOR (pre-existing) | `nativeMain/kotlin/TimezoneEngineNative.kt:10,24-33` | Native zone cache keyed by the raw id; kotlinx native accepts path variants (`Europe/./Berlin`, `Europe//Berlin`) → unbounded growth + O(n) copy per insert from hostile ids (S) | out-of-scope → follow-up task | Introduced by the previous task's MINOR batch, not this change set; JVM/JS unaffected. `docs/tasks/20261019-datetime-native-zone-cache-growth.md` |
| 3 | MAJOR (pre-existing) | `MpZonedDateTime.kt:151` | A stored `ts` in the second half of an overlap re-encodes an hour early (I, D, S) | out-of-scope → existing follow-up | Already ULTRA-00211 (mpzoneddatetime overlap offset) |
| 3 | MINOR (pre-existing) | `TimezoneEngineJs.kt:70-76` | JS: wall time past the Date limit → NaN → offset 0, silently (D) | maintainer list | Year ±275 760 only; noted in the native-cache follow-up task |
| 3 | MINOR (pre-existing) | `TimezoneEngine.kt:52-53,76-77` + `TimezoneEngineJvm.kt:12` | JVM/native: 999999999-12-31 in a rules zone → +1-day probe → raw `DateTimeException` (D, S) | maintainer list | One day out of ±10^9 years; noted in the follow-up task |
| 3 | MINOR | `TimezoneEngineJs.kt:53` | `flooredMs` no longer floors (I) | maintainer list | Cosmetic rename |
| 3 | MINOR | `TimezoneEngine.kt:139-142` | `validBefore` evaluated before the `before == after` short-circuit; `offsetAt(0)` arm unreachable under the pinned assumption — one lookup per unique resolution could go (I) | maintainer list | Simplification; the defensive arm is a legitimate choice |
| 3 | note | benchmark | The "after" figures should name their tree (D) | fix | Recorded: final-tree run on `79284123` — `atZone` 2 740, `toInstant` 3 650, daily stride 6 550, scattered 13 800 ns/op |

Phase 2 (reconcile): all three — nothing reopened; every FIXED entry holds at `79284123` and reached every place it
needed (security noted one record-only gap: the Spec benchmark table did not name its tree → fixed); no rejected
CRITICAL/MAJOR; both out-of-scope MAJORs have their follow-up tasks. Security additionally confirmed the JS NaN row
(its phase-1 "JS range CLEAN" was too broad). Pre-existing note for the maintainer list: `MpLocalDateTime`/
`MpLocalDate` decode honour a non-UTC `timezone` but always encode `"UTC"` (shifts `ts` on re-save of such payloads).

**Verdict: gate PASS** (round 3 clean). Open MINORs → maintainer list: `flooredMs` rename; `validBefore` ordering /
unreachable `offsetAt(0)` arm; JS wall-time NaN at the Date limit; JVM `DateTimeException` on 999999999-12-31; the
`MpLocalDateTime`/`MpLocalDate` non-UTC decode note. Follow-ups: `docs/tasks/20261019-datetime-native-zone-cache-growth.md`,
ULTRA-00211 (mpzoneddatetime overlap offset). No docs task: no public API changed (internal engine only).
