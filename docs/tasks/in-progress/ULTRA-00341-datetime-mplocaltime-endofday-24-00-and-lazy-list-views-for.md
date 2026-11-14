# ULTRA-00341: datetime: MpLocalTime.EndOfDay (24:00), and lazy list views for date ranges and slot splits

> **Status (2026-11-14): IN PROGRESS.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Plan:** ULTRA-00316 (close the open epics), track A, batch 3.

## Why this exists

Two things the maintainer decided on 2026-11-14 for `ultra/datetime`:

- **3a, 24:00:** "yes to special 24:00 representation"; "MpLocalTime: OK as suggested". `MpLocalTime.EndOfDay` is an
  ordinary value of 86,400,000 ms, one millisecond after `Max`. It comes from the constant, `parse("24:00")`, the
  decoders, `MpLocalTimeSlot.completeDay`, slot ends of exactly 24:00 (`MpLocalTimeSlot.of`, `ofSecondsOfDay`,
  `splitWithGaps`) and `DateTimeRangeConverter.fromHourToHour(_, 24)` (corrected in review round 1: an app that uses
  `completeDay` writes 86400000 after the upgrade without any code change). `of(24, 0)` keeps wrapping to 00:00. Overnight
  slots (22:00 to 02:00) are not built ("only implement it once the necessity arises").
- **3b, no cap, a lazy view:** `MpLocalDateRange.asListOfDates()` keeps its signature and returns a lazy list view
  that computes each date on demand. The maintainer: "I am not a big fan of code that runs for years and then
  suddenly hits a runtime error"; "yes go for this and document that the returned list is a lazy view and what that
  means for the consumer". The view becomes a small reusable class. Open with the maintainer: whether
  `MpLocalTimeSlot.splitWithGaps` becomes such a view too, dropping `maxSlots` and its exception (batch 1, unreleased).

## Batch 3a: plan as built

Commits on `wt/datetime`: `618a4860` (it references "ULTRA-00316 track A batch 3a") and the review round-1 commit.

- **`MpLocalTime.EndOfDay`:** an ordinary instance of 86,400,000 ms, one millisecond after `Max`. The unchecked public
  constructor is unchanged: `MpLocalTime(86_400_000) == EndOfDay`.
- **Fields and text:** `hour` is 24 for it (0 before), the other fields 0; `toIsoString()` gives `24:00:00.000`.
  Ordering: `Min < Max < EndOfDay`.
- **`parse`:** `24:00`, `24:00:00`, `24:00:00.000` give it; an hour of 24 with any other rest is refused
  (`IllegalArgumentException`; `tryParse` null). `of`, `ofMilliSeconds` and `ofSecondOfDay` keep wrapping.
- **Arithmetic:** `plus` / `minus(duration)`: `EndOfDay` counts as 00:00 of the next day and wraps. A duration below
  one millisecond keeps `EndOfDay`. Every other value behaves exactly as before (the round-1 guard is
  `isEndOfDay && duration.inWholeMilliseconds == 0L`).
- **Conversions:** `.kotlinx` gives `LocalTime(23, 59, 59, 999_999_999)` and `.jvm` gives `LocalTime.MAX` for it (they
  threw); this is lossy, `EndOfDay.kotlinx.mp == Max`.
- **Dates:** `MpLocalDateTime.of(date, EndOfDay)` / `MpLocalDate.atTime(EndOfDay)` give 00:00 of the next day.
  `MpLocalDate.atTime(EndOfDay, zone)` gives `plusDays(1).atStartOfDay(zone)`, also when the date itself is skipped by
  the zone (round 1). `MpZonedDateTime.atTime(EndOfDay)` gives the start of the next day.
- **Decoders:** kotlinx and slumber read exactly one day as `EndOfDay`; slumber compares as a Double, so 86400000.5
  wraps (round 1). Every other value wraps as before.
- **Slots:** `MpLocalTimeSlot.completeDay = [Min, EndOfDay)`. `MpLocalTimeSlot.of(from, duration)`,
  `ofSecondsOfDay(from, to)` and `splitWithGaps` end a slot of exactly 24:00 at `EndOfDay` (round 1 for `of` /
  `ofSecondsOfDay`, coordinator's call). Only for a `from` inside the day (delta round 2); every other end wraps as
  before, so `ofSecondsOfDay(86_400, 86_400)` stays the empty `[00:00, 00:00)`.
- **`DateTimeRangeConverter`:** `fromHourToHour(_, 24)` ends at the next day's start, while `fromHour` wraps as before
  (round 1 removed the `fromHour = 24` arm). `fromMorningToEvening` ends at `to.atTime(EndOfDay, zone)`, fixing a
  pre-existing hour-late end where that day's midnight is in a DST gap (round 1).
- **kraft:** unchanged; `MappingEndOfDaySpec` pins the mapping. ChromeHeadless drops "24:00:00" from an
  `<input type="time">` (probe, 2026-11-14), so a kraft time field shows `EndOfDay` as empty; that test was dropped in
  round 1 because no kraft or datetime change can turn it red.

### Known consequences, documented for the maintainer

- Where a DST gap ends at midnight (America/Nuuk 2024-03-30: 23:00 to 24:00 is missing), `[23:30, EndOfDay)` on that
  date gives an inverted zoned range: the start is shifted past the end by the gap rule. Pinned as current behaviour.
- On a date the zone skipped (Pacific/Apia 2011-12-30), `[x, EndOfDay)` with `x > 00:00` is an inverted range
  (`[09:00, 24:00)`), and `completeDay` there is empty (start == end). Pinned (delta round 2). The slot redesign's
  empty-range rule (batch 3b plan) covers both.

### Test evidence

- **New:** `MpLocalTimeEndOfDaySpec` (commonTest, 32), `MpLocalTimeEndOfDayJvmSpec` (2), slumber
  `MpLocalTimeEndOfDayCodecSpec` (4), kraft core `MappingEndOfDaySpec` (1).
- **RED first (3a):** with the constant only, 18 of 22, 1 of 2, the 3 adapted `completeDay` rows, and slumber 2 of 3.
- **RED first (round 1):** fix 1 (Apia one day late), fix 2 (`EndOfDay.plus(500 µs)` → 00:00, raw values not
  wrapped), fix 3 (`fromHour` 24 at the next day), fix 5 (`of(23:00, 1 h)` ending 00:00), fix 6 (Havana 05:00Z
  instead of 04:00Z), slumber fractional Double (truncated into `EndOfDay`).
- **Green before the fix, as intended:** the M1 pins, the Berlin rows of fix 6, the gap split of fix 7 and the Nuuk
  pin.
- Final run after round 1 (2026-11-14 17:17, `console/wt-gradle.sh`, `--rerun` on each test task): JVM (datetime
  jvmTest) 2795 / 0, JS (datetime jsBrowserTest) 2137 / 0, native (datetime linuxX64Test) 2129 / 0, slumber jvmTest
  1258 / 0, kraft core jsBrowserTest 217 / 0.

| Mutant (JVM; each restored with `cmp`) | Tests red |
|---|---|
| 3a E1 `hour` back to modulo | 2 |
| 3a E2 `parse` accepts 24 with any rest | 4 |
| 3a E3 kotlinx decoder without the one-day case (persistence) | 1 |
| 3a E4 slumber awaker without the one-day case (persistence) | 2 |
| 3a E5 `MpLocalDateTime.of(date, EndOfDay)` same day | 1 |
| 3a E6 zoned `atTime(EndOfDay)` via `withLocal` | 1 |
| 3a E7 split slot end via `ofMilliSeconds` | 2 |
| 3a E8 `plus(ZERO)` guard removed | 1 |
| 3a E9 / E10 kotlinx / `.jvm` converter branch removed | 1 / 1 |
| 3a E11 `fromHourToHour` 24 back to `of(24, 0)` | 1 |
| R1 `MpLocalDate.atTime(EndOfDay, zone)` via `atStartOfDay(zone).atTime` | 1 |
| R2 guard back to `duration == ZERO` / guard removed | 1 / 2 |
| R3 `fromHour = 24` → `EndOfDay` again | 1 |
| R4 slumber awaker `toLong() == one day` (persistence) | 1 |
| R5 / R5b `MpLocalTimeSlot.of` / `ofSecondsOfDay` wrap at 24:00 | 1 / 1 |
| R6 `fromMorningToEvening` back to start-of-day + 1 day | 1 |
| R7 split count `(span - duration - gap) / (duration + gap) + 1` | 2 |

**Existing test lines changed** (`MpLocalTimeSlotSpec`):

| Test | Before | After |
|---|---|---|
| "Creation - via completeDay" | `to shouldBe Max` | `to shouldBe EndOfDay` |
| "a slot ending at 23:59:59.999" (with `completeDay`) | 23 slots | renamed "completeDay, ending at 24:00": 24 slots, the last `23:00–EndOfDay` |
| (kept) | — | a new row "a slot ending at 23:59:59.999" on `[Min, Max]` keeps the 23-slot case |
| "…with a gap" | `completeDay`, 3 h + 3 h, 4 slots (same for `Max` and `EndOfDay`) | renamed "completeDay, ending at 24:00, with a gap": 3 h + 4 h, the last slot `21:00–EndOfDay` (round 1, pins the end) |
| 1 ms whole-day split | "86 399 999 slots" | "86 400 000 slots" |

### Consumers of `MpLocalTime` values

- **kraft/semanticui** `UiTimeFieldRenderer`: `timeToHms` / `stringToLocalTime(OrNull)` from kraft core `mapping.kt`
  render `EndOfDay` as "24:00:00" and read it back. The native time input drops that value, so the field shows empty;
  a field bound to `completeDay.to` showed 23:59:59 before.
- **kraft** date fields: use `MpLocalTime.Min` only. The kraft datetime addon has no `MpLocalTime`; the
  `FormWithTimes` example uses `toTime()`, never `EndOfDay`.
- **funktor:** no main-code use.
- **ultra/codegen** TS runtime: `MpLocalTime = z.number()` accepts 86400000; `localTimeToHhMm(86400000)` gives "24:00".
- **Karango / Monko:** the slumber `Long`; `EndOfDay` sorts after every other time.

### Docs lines for the DOCS follow-up

- `docs-site/src/pages/ultra/datetime/ranges.astro:353`: `completeDay  // 00:00 to 23:59:59.999` is false; it now ends
  at 24:00.
- `docs-site/src/pages/ultra/datetime/mp-local-time.astro:42`: `val endOfDay = MpLocalTime.Max` names `Max` what the API
  calls `EndOfDay`.
- `docs-site/src/data/llms/datetime.md:103-106`: the constants list lacks `EndOfDay` (24:00).
- `ultra/codegen/src/main/resources/ts/runtime/datetime.ts:42-48`: the `MpLocalTime` comment should say the number can
  be 86,400,000 (24:00).

## Changelog text

**Breaking changes, behaviour change (no compile signal):**

- `ultra/datetime`: `MpLocalTimeSlot.completeDay` now ends at `MpLocalTime.EndOfDay` (24:00) instead of `Max`
  (23:59:59.999). An app that uses `completeDay` writes `86400000` after the upgrade without any code change.
  - A reader of an older version, including a browser tab left open across a deploy, reads the slot
    `{"from":0,"to":86400000}` as `[00:00, 00:00)`: an invalid slot, so "open all day" becomes "never open". Saving
    it there persists `{"from":0,"to":0}`. Update readers before writers.
  - Slots stored from the old `completeDay` hold `to = 86399999`: `MpLocalTimeSlot(Min, Max) == completeDay` is now
    false, and `completeDay.cutAway(MpLocalTimeSlot(Min, Max))` is `[MpLocalTimeSlot(Max, EndOfDay)]`, a 1 ms slot
    (it was empty).
  - `completeDay.splitWithGaps(1.hours)` gives 24 slots (23 before).
  - A kraft time field bound to `completeDay.to` renders empty (it showed 23:59:59): the browser's time input does
    not accept 24:00 (ULTRA-00341).

**New and changed:**

- `ultra/datetime`, new: `MpLocalTime.EndOfDay`, the time 24:00, one millisecond after `Max`, for the end of a
  half-open time slot.
  - Its `hour` is 24 and it prints `24:00:00.000`; it counts as 00:00 of the next day in arithmetic, and adding less
    than a millisecond keeps it.
  - `MpLocalDate.atTime(EndOfDay)` is 00:00 of the next day; `atTime(EndOfDay, zone)` and
    `MpZonedDateTime.atTime(EndOfDay)` give the start of the next day.
  - It is produced by this constant, `parse("24:00")` (`"24:00:00"`, `"24:00:00.000"`), the decoders, `completeDay`,
    slot ends of exactly 24:00 (`MpLocalTimeSlot.of`, `ofSecondsOfDay`, `splitWithGaps`) and
    `DateTimeRangeConverter.fromHourToHour(_, 24)` (ULTRA-00341).
- `ultra/datetime`, behaviour change (no compile signal):
  - `hour` can be 24.
  - `MpLocalTime.of(t.hour, t.minute, …)` and `ofMilliSeconds(t.inWholeMilliSeconds())` turn `EndOfDay` into 00:00;
    `parse(toIsoString())`, the decoders and the unchecked constructor `MpLocalTime(t.inWholeMilliSeconds())`
    round-trip it.
  - A JSON client sending `86400000` used to be normalised to 00:00 and is now stored as 24:00.
  - `.kotlinx` and `.jvm` give the last instant of the day for it, which is lossy (`EndOfDay.kotlinx.mp == Max`);
    they threw before.
  - The value `MpLocalTime(86_400_000)` of the unchecked constructor now is `EndOfDay` (ULTRA-00341).
- `ultra/datetime`, behaviour change (no compile signal): `MpLocalTime.parse` refuses an hour of 24 with a non-zero
  rest ("24:00:01", "24:30"), which it used to wrap to 00:00:01 / 00:30; `tryParse` returns null for them.
  `of(24, 0)`, `ofMilliSeconds(86_400_000)` and `ofSecondOfDay(86_400)` still wrap to 00:00 (ULTRA-00341).
- `ultra/datetime`, behaviour change (no compile signal): `MpLocalTimeSlot.of(from, duration)` and
  `ofSecondsOfDay(from, to)` end a slot of exactly 24:00 at `EndOfDay`: `of(23:00, 1.hours)` is `[23:00, 24:00)`, it
  was the invalid `[23:00, 00:00)`. An end beyond 24:00 still wraps. `splitWithGaps` ends a slot reaching 24:00 at
  `EndOfDay` instead of building an invalid last slot ending at 00:00 (ULTRA-00341).
- `ultra/datetime`, behaviour change (no compile signal): `DateTimeRangeConverter.fromHourToHour(_, 24)` (e.g.
  `toZonedTimeRange(zone).fromHourToHour(8, 24)`) ends at the start of the day after the last day; it ended at 00:00
  of the last day. A `fromHour` of 24 still wraps to 00:00 (ULTRA-00341).
- `ultra/datetime`, fix: `DateTimeRangeConverter.fromMorningToEvening` ends at the start of the next day. Before:
  - where the last day's own midnight is in a DST gap it ended an hour late (America/Havana 2024-03-10,
    America/Santiago 2024-09-08);
  - on a last day the zone skipped it ended a day late (Pacific/Apia 2011-12-30: 2012-01-01T00:00+14:00, now
    2011-12-31T00:00+14:00);
  - where the next midnight is inside a gap that starts before midnight it ended at the wrong time
    (America/Toronto 1919-03-30: 01:00 EDT, now 00:30 EDT).
  - Ordinary days are unchanged (ULTRA-00341).
- `ultra/datetime` and `ultra/slumber`, stored format: `EndOfDay` is stored as `86400000` milliseconds of day; exactly
  that number reads back as `EndOfDay` (slumber: only a number equal to one day, so `86400000.5` still wraps), and
  every other number wraps into the day as before. A reader of an older version reads `86400000` as 00:00 without an
  error, so update readers before writers (ULTRA-00341).

## Review record: round 1 (blind, reviewer-fable-xhigh)

**Verdicts:** implementation CLEAN (6 MINOR); domain and data integrity NOT CLEAN (1 MAJOR, 7 MINOR). The reviewers
ran nothing; every item below was verified by a test first.

| Sev | Finding | Outcome |
|---|---|---|
| MAJOR M1 | `completeDay` now ends at `EndOfDay`; its consequences are written nowhere | FIXED (record): who produces `EndOfDay` in the ticket, the `EndOfDay` and `completeDay` KDoc, and the changelog; pins `MpLocalTimeSlot(Min, Max) != completeDay` and `cutAway` → `[Max, EndOfDay)`, and the slot JSON `{"from":0,"to":86400000}`. `completeDay` stays (maintainer) |
| MINOR | `MpLocalDate.atTime(EndOfDay, zone)` one day late on a skipped date (Apia 2011-12-30) | FIXED; RED first (2011-12-31T10:00Z instead of 10:00Z on 12-30); mutant R1 red |
| MINOR | `plus` / `minus` guard too wide (all values at `ZERO`) and too narrow (`EndOfDay` + 500 µs) | FIXED: `isEndOfDay && inWholeMilliseconds == 0`; four cases pinned; RED first; mutants R2 / R2b red |
| MINOR | `fromHourToHour` `fromHour = 24` arm not approved, made 24 later than 25, reversed `(24, 12)` | FIXED: arm removed, `fromHour` wraps as before; pinned with literal instants; mutant R3 red |
| MINOR | slumber awaker truncates `86400000.5` into `EndOfDay` | FIXED: Double compare; Int, Long, whole and fractional Double pinned; mutant R4 red |
| MINOR | `MpLocalTimeSlot.of` / `ofSecondsOfDay` build `[23:00, 00:00)` | FIXED (coordinator's call): an end of exactly 24:00 is `EndOfDay`, beyond wraps; mutants R5 / R5b red |
| MINOR | `fromMorningToEvening` ends an hour late where midnight is in a gap (pre-existing) | FIXED: `to.atTime(EndOfDay, zone)`; Havana and Santiago RED first; seven Berlin rows equal the old formula; mutant R6 red |
| MINOR | no test of a gap split whose last slot ends exactly at `to`; gap row did not pin the end | FIXED: `completeDay.splitWithGaps(3 h, 4 h)` → …, `21:00–24:00`; the gap row now ends at 24:00; mutant R7 red (2) |
| MINOR | `MappingEndOfDaySpec` second case cannot go red | FIXED: dropped; browser behaviour noted in this ticket |
| MINOR | texts: `Max` KDoc "The minimal possible time"; formatter `HH` "0-23"; this ticket's "Only the constant, parse and the decoders" | FIXED |
| MINOR | DOCS list incomplete | FIXED: `ranges.astro:353`, `mp-local-time.astro:42`, `llms/datetime.md:103-106`, `datetime.ts:42-48` |
| MINOR | `timeOfDay` on a split slot's `from` | SIMPLIFIED: a slot that fits starts before 24:00, so `from` is always `ofMilliSeconds` |
| note | Nuuk `[23:30, EndOfDay)` inverted where a gap ends at midnight | DOCUMENTED, pinned as current behaviour, for the maintainer |

**Stayed clean** (reviewers): "the code of the EndOfDay arms is correct everywhere I traced it".

**Verdict:** round 1 NOT CLEAN (1 MAJOR, record only), all findings handled; this batch touches production code, so a
delta review comes before any merge.

## Review record: delta round 2 (reviewer-fable-xhigh, all charters)

**Verdict:** CLEAN: 0 CRITICAL, 0 MAJOR, 3 MINOR, 2 text NITs; all applied in one small commit (one production line in
each of the two slot factories).

| Sev | Finding | Outcome |
|---|---|---|
| MINOR | `ofSecondsOfDay` (new with fix 5): `from` wraps while `to == 86_400` does not, so `(86_400, 86_400)` = `completeDay`, `(90_000, 86_400)` valid `[01:00, 24:00)` | FIXED: `EndOfDay` only for a `from` inside the day; RED first for both; mutant D2 (no `from` check) red. Checked `of(from, duration)`: a raw out-of-range `from` whose end lands on one day (`MpLocalTime(-3_600_000)` + 25 h) gave `EndOfDay`; fixed the same way, RED first; mutant D3 red. `of(EndOfDay, ZERO)` = `[24:00, 24:00)`, `of(EndOfDay, 1 h)` = `[24:00, 01:00)`, pinned |
| MINOR | the lower edge of the `plus` guard untested (mutant `inWholeSeconds == 0L` survived) | FIXED: `EndOfDay.plus(1 ms)` = 00:00:00.001, `minus(1 ms)` = `Max`; mutant D1 red |
| MINOR | the seven Berlin rows of `fromMorningToEvening` took the expected value from library code | FIXED: literal instants (checked: next day's start in Berlin, +01:00 or +02:00) |
| NIT | changelog: "only `parse(toIsoString())` and the decoders round-trip it" | FIXED: the unchecked constructor round-trips it too |
| NIT | changelog: the `fromMorningToEvening` entry misses a skipped last day and a gap that starts before midnight | FIXED: Apia 2011-12-30 and Toronto 1919-03-30 named |
| note | on a skipped date `[x, EndOfDay)` is inverted and `completeDay` empty | DOCUMENTED and pinned (Apia 2011-12-30) |
| counts | final run after delta round 2 (2026-11-14 17:40, `--rerun`) | JVM 2798 / 0, JS 2140 / 0, native 2129 → 2132 / 0, slumber 1258 / 0 |
| tidy | repeated rows: the gap split in `MpLocalTimeEndOfDaySpec` (duplicates the `MpLocalTimeSlotSpec` row), two awake lines in `MpLocalTimeEndOfDayCodecSpec`, a second `SECONDS_PER_DAY` | DONE: the duplicate test and lines removed; `MpLocalTime.SECONDS_PER_DAY` is `internal` and shared |

## Left out, for one follow-up ticket

- `MpLocalTime.jvm` drops the milliseconds (`Max.jvm` = 23:59:59).
- `MpLocalTime.parse`'s error message names `MpLocalDate`.
- `MpLocalDateRange.numberOfDays` miscounts ranges past Doomsday.

## History

- 2026-11-14: created by the coordinator for batch 3 of track A.
- 2026-11-14: batch 3a built (`618a4860`); review round 1 handled on `wt/datetime`, awaiting the delta review.
