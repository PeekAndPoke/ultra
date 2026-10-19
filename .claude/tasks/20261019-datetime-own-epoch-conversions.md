# ultra/datetime: own epoch ↔ local date-time conversion (no emulated Long on JS)

**Status:** IN REVIEW (Ultra-Main-Dev) — implemented 2026-10-19, `/feature-review` next
**Plan:** follow-up of `.claude/tasks-archive/2026-10/20261018-datetime-js-performance.md`; maintainer: before the 0.112.0 release (2026-10-19)
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

- [ ] `TimezoneEngine` splits epoch seconds + offset into date and time itself, and back, with `Int`/`Double`
      arithmetic (Hinnant's days-from-civil / civil-from-days) instead of kotlinx's `toLocalDateTime(UTC)` /
      `toInstant(UTC)` / `atStartOfDayIn(UTC)`; kotlinx stays the holder of the values. Outside the range where
      `Int` days and `Double` seconds are exact, fall back to kotlinx.
- [ ] Parity: a common test comparing the own conversion with kotlinx's over a wide spread of instants, offsets and
      dates (incl. negative years, leap days, centuries, the fallback edges), on all three targets; the existing
      ZoneParity table + JVM sweeps stay green.
- [ ] Measured: the benchmark rows before/after; then spross re-profiles against a snapshot.
- [ ] Mutation-checked (persistence bar).

## Implementation notes

- `ultra/datetime/src/commonMain/kotlin/EpochMath.kt`: civil-from-days / days-from-civil (Hinnant) on `Int` days and
  `Double` seconds; kotlinx takes over beyond ±1 million years (`MAX_EXACT_YEAR`), where `Int` days would overflow.
- `TimezoneEngine` runs on `Double` epoch seconds end to end (`resolve`, `transitionAfter`, `startOfDay`, `plus`);
  the platform primitive and `OffsetIntervalCache` take `Double` seconds, so a JS conversion creates no emulated
  `Long` except reading `Instant.epochSeconds` and building the result `Instant`. Date-based `plus` still adds the
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
- Benchmark (ns/op, ChromeHeadless 155, load average ~5; before = the `60bdd5d3` figures):

  | Row | before | after |
  |---|---|---|
  | `instant.atZone(berlin).toLocalDateTime()` sequential | 10 000 – 12 600 | 3 100 – 3 700 |
  | `local.toInstant(berlin)` | 16 300 – 17 700 | 3 900 – 4 200 |
  | `atZone`, daily stride | 14 100 – 15 900 | 7 600 – 9 100 |
  | `atZone`, scattered over 50 years | 21 700 – 26 200 | 18 600 |

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**
