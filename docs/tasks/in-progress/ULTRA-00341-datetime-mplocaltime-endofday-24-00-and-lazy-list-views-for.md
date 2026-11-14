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

## Batch 3b-1: plan as built

Built on `wt/datetime` in one commit with the new sentinels of ULTRA-00343; for the merge, the sentinels are back at
master's values (maintainer, 2026-11-14; see "Sentinel values restored for the merge"). 3b-2 (slots as start plus
duration) waits for the maintainer. 3b-3 (an estimator arm in the cache): the coordinator merges track B's estimator
first; this batch makes no claim about what it does (round 1).

- **`ComputedList<T>(size, compute)`** in `ultra/common` (new, public): a read-only `AbstractList` that computes each
  element from its index on demand and stores nothing; also `RandomAccess`. A negative size is an empty view; an
  index out of range throws `IndexOutOfBoundsException`. `subList` is a view (from `AbstractList`). `toString()` is
  bounded (round 1): `ComputedList(size=N, [e0, e1, e2, ...])`, it computes at most three elements. The KDoc lists
  what it means for a consumer (each read computes again, which operations build a real list, thread safety).
- **`MpLocalDateRange.asListOfDates()`:** same signature, returns `ComputedList(numberOfDays) { from.plusDays(it) }`;
  an invalid range still gives `emptyList()`. No cap and no exception: `forever.asListOfDates()` is a view of
  7,304,850 dates (-10000-01-01 to 9999-12-31, master's sentinels), cheap to create and to read at its ends.
  `MpClosedLocalDateRange.asListOfDates()` delegates (7,304,851 dates for its `forever`, through 10000-01-01: the
  clamp's half-day rounding counts Doomsday itself).
- **Size estimators:** the KDoc says only what holds whatever the estimator does: one that walks or samples the list
  sees computed elements and charges them as if stored, so it over-counts a view. (Before round 1 it described track
  B's sampling estimator, which is on another branch; on this branch and on master the estimator walks every
  element.)

### Test evidence (3b-1 with ULTRA-00343)

- **New:** `ComputedListSpec` (common commonTest, 10), `MpLocalDateRangeLazySpec` (datetime commonTest, 4), and the
  ULTRA-00343 specs (`GenesisDoomsdaySpec` 8, slumber `OldSentinelsCodecSpec` 1).
- **RED first:** `ComputedListSpec` as a compile failure (no class); `MpLocalDateRangeLazySpec` 2 of 4 (the `forever`
  sizes against the old sentinels), the type checks red against the eager `map` (mutant G3).
- **Round 1:** `ComputedListSpec` 10 → 11 (bounded `toString`; RED first with an `OutOfMemoryError` on a view of
  `Int.MAX_VALUE`; the always-true `shouldBeInstanceOf<List<Int>>()` removed; the `RandomAccess` test no longer names
  the cache). `MpLocalDateRangeLazySpec`: index 1,000,000 of `forever` is the literal 2738-11-29 (checked
  independently), no longer the view's own `plusDays`.
- Final run after round 1 (2026-11-14, `console/wt-gradle.sh`, `--rerun` on each test task, counts from the XML):
  datetime JVM 2814 / 0, JS 2156 / 0, native 2148 / 0; common JVM 389 / 0, JS 203 / 0 (1 skipped: the pre-existing
  `WeakReferenceSpec` GC case), native 197 / 0; slumber jvmTest 1259 / 0. `GenesisDoomsdaySpec` (12) and
  `ComputedListSpec` (11) ran on all three targets. Repo-wide compile sweep (JVM, JS, native, main and test):
  exit 0, 0 `e:` lines.
- Before round 1 (2026-11-14, `console/wt-gradle.sh`, `--rerun` on each test task, counts from the XML): datetime JVM
  2810 / 0, JS 2152 / 0, native 2144 / 0; common JVM 388 / 0, JS 202 / 0 (1 skipped: the pre-existing
  `WeakReferenceSpec` GC case), native 196 / 0; slumber jvmTest 1259 / 0. The new specs ran on all three targets.
  Repo-wide compile sweep (JVM, JS, native, main and test): 0 `e:` lines.

| Mutant (JVM; each restored with `cmp`) | Tests red |
|---|---|
| G3 `asListOfDates` back to the eager `(0 until n).map { … }` | 1 (the `ComputedList` / `RandomAccess` check on `forever`) |
| G5 `ComputedList.get` computes `index + 1` | 6 (common) |
| G6 `ComputedList.size` not coerced to ≥ 0 | 1 (common, negative size) |
| H2 `ComputedList.toString()` back to `super.toString()` | 1 (common, `OutOfMemoryError`) |
| H3 the ellipsis at `size >= 3` | 1 (common) |

### Docs lines for the DOCS follow-up (3b-1)

- `docs-site/src/pages/ultra/datetime/ranges.astro` and `docs-site/src/data/llms/datetime.md`: wherever
  `asListOfDates()` appears, say that it returns a lazy view (cheap to read, `toList()` / `map` build a real list).
- `docs-site/src/pages/ultra/common/*` and the common LLM mirror: `ComputedList` is new.

## Changelog text (3b-1)

**New and changed:**

- `ultra/common`, new: `ComputedList<T>(size) { index -> … }`, a read-only list view that computes each element on
  demand and stores nothing; `RandomAccess`; its `toString()` shows the size and the first three elements
  (ULTRA-00341).
- `ultra/datetime`, behaviour change (no compile signal): `MpLocalDateRange.asListOfDates()` and
  `MpClosedLocalDateRange.asListOfDates()` return a lazy view (`ComputedList`) instead of a built list. Same type
  (`List<MpLocalDate>`), same elements; any length is cheap to create and to read (`forever.asListOfDates()` included).
  - Each read computes the date again: equal, not the same instance.
  - `toList()`, `map`, `filter`, `sorted` build a real list of every date: for an open range that is millions.
  - It is not a `MutableList`: a cast to `MutableList` or `ArrayList` that used to work now fails.
  - `toString()` no longer lists every date: `ComputedList(size=N, [d0, d1, d2, ...])` (ULTRA-00341).

## Review record 3b-1: round 1 (blind, reviewer-fable-xhigh, two passes)

Verdict NOT CLEAN; the MAJOR (`numDaysInMonth`) and the sentinel items are recorded in ULTRA-00343. The 3b-1 items:

| Sev | Finding | Outcome |
|---|---|---|
| MINOR | `ComputedList` KDoc, a test title and this ticket describe track B's estimator, which is on another branch | FIXED: only a sentence true for any estimator; test title neutral; 3b-3 paragraph corrected |
| MINOR | `ComputedList.toString()` builds every element (about 140 MB for `forever.asListOfDates()`) | FIXED: bounded, tested, in the KDoc and changelog; RED first (`OutOfMemoryError`); mutants H2 / H3 red |
| MINOR | `MpLocalDateRangeLazySpec`: expected value from the view's own `plusDays`; `shouldBeInstanceOf<List<Int>>()` cannot fail | FIXED: literal 2738-11-29 (checked independently); line removed |

## Batch 3b-2: plan as built (the slot redesign)

Built on `wt/datetime` on top of 3b-1, after the maintainer's decisions of 2026-11-14 (master: "Batch 3b: decisions of
the maintainer"). **Status: 3b-2: BUILT, gate closed: round 1 NOT CLEAN (1 MAJOR), delta 2 CLEAN, delta 3 CLEAN.
3b-1 and 3b-2 carry master's sentinel constants again (maintainer, 2026-11-14: "ok please do this so the merge
works"); awaiting the delta review of that commit, then the merge. The choice of values stays open in ULTRA-00343.**

- **`MpLocalTimeSlot` is a start and a duration:** `data class MpLocalTimeSlot private constructor(from, duration)`
  with `@ConsistentCopyVisibility`. Created by `of(from, duration)`, `of(from, to)`, `ofSecondsOfDay(from, to)` and
  `completeDay` (00:00 for 24 h). The factories normalise:
  - the start wraps into the day, so 24:00 (`EndOfDay`) and a raw out-of-range `MpLocalTime` become a clock time in
    `[00:00, 24:00)`; **deviation from the plan** (it said "from as given"), because a slot starting at 24:00 has no
    meaning on the clock face and would need its own rules everywhere;
  - the duration is cut to whole milliseconds; zero or below is `Duration.ZERO` (an empty slot);
  - from `INFINITE_DURATION_MILLIS` = 2^53 - 1 ms (about 285,000 years) on, it is `Duration.INFINITE`. **New
    decision, for the maintainer:** stored, infinity is that number, the largest integer a JS client reads exactly
    (`Long.MAX_VALUE` would come back from a TypeScript client rounded up and fail to decode).
- **`of(from, to)`:** `to` after `from` is the difference, before `from` runs overnight, equal is empty (confirmed);
  equal is compared as given, so `of(EndOfDay, EndOfDay)` is empty too (round 1); a `to` of `EndOfDay` is 24:00, so
  `of(00:00, EndOfDay)` is the whole day. `ofSecondsOfDay` follows it; equal values are empty, also `(86400, 86400)`.
- **Derived, read-only:** `to` (the clock time of the end; `EndOfDay` for an end exactly at midnight; `from` for an
  empty or infinite slot), `endDayOffset: Long` (days after the start's date; `Long.MAX_VALUE` when infinite),
  `crossesMidnight`, `isEmpty`, `isInfinite`; `isValid` is "not empty".
- **Clock face** (a slot without a date is a daily pattern), as planned: `contains(time)` (24:00 counts as 00:00),
  `contains(slot)`, `intersects`, `touches` (also across midnight), `isAdjacentTo`, `mergeWith` (a union covering the
  clock is this slot's start for 24 h; a slot of 24 h or more absorbs the other), `cutAway` (in this slot's frame; the
  length beyond 24 h is dropped).
- **On a date** (`MpLocalDate.toRange(slot, zone)`): by the clock, from `from` on the date to `to` on the date
  `endDayOffset` days later.
  - A start or end in a skipped hour moves to the first existing time, the transition (round 1: 02:30 in a 02:00 to
    03:00 gap is 03:00, as agreed with the maintainer; `atTime` / `MpZonedDateTime.of` keep moving by the gap length,
    03:30). New `TimezoneEngine.toInstantOrTransition`; since round 2 `startOfDay` delegates to it too, so every
    `atStartOfDay` runs through it (same results, proven by trace and the untouched start-of-day tests). A time in a
    repeated hour takes its first occurrence.
  - So the end never lies before the start. On a skipped date (Apia 2011-12-30) every slot ending that day is empty at
    the next day's start; the 3a pins of Nuuk and Apia are now empty at the transition, no longer inverted.
  - An infinite slot, and a finite end the platform cannot resolve (JS dates end at 8.64e15 ms; round 1), end at the
    range's open end, decided in ONE private function (`endOnDate`, today `MpZonedDateTime.Doomsday`), so it follows
    whatever the sentinel decision (ULTRA-00343, analysis of 2026-11-14) is. The open end lying before the start (a
    date after it, or an end past the last date the platform holds, round 2) gives an empty range at the start: the
    only end-before-start case left.
- **`splitWithGaps(duration, gap)`:** walks the slot's own length from `from` (overnight and longer than a day work),
  returns a `ComputedList`; `maxSlots` and its exception are gone. An infinite slot gives `Int.MAX_VALUE` parts, or one
  part for an infinite gap (round 1: the count was an artefact of `Long.MAX_VALUE / step`); an infinite part fits only
  an infinite slot. The parts carry no day (KDoc says how to place them).
- **Stored shape `{from, duration, to}`** in milliseconds, `to` derived and ignored on read when `duration` is present:
  - new public `MpLocalTimeSlotRawData(from: Long, duration: Long? = <absent marker>, to: Long? = null)` with the
    extension
    functions `MpLocalTimeSlot.toRawData()` and `MpLocalTimeSlotRawData.toSlotOrNull()` (public because the slumber
    codec in another module uses them), `@Slumber.As(MpLocalTimeSlotRawData::class)` on the slot;
  - kotlinx `MpLocalTimeSlotSerializer`; slumber `MpLocalTimeSlotAwaker` / `MpLocalTimeSlotSlumberer`, registered in
    `MpDateTimeModule` (`from` / `to` are read through `MpLocalTimeAwaker`, so the 3a rules for 24:00 and fractional
    numbers hold);
  - the decoders fall back to the old `{from, to}` (decided) only when `duration` is ABSENT (round 1): an explicit
    `"duration": null` (a TypeScript client's `Infinity` or `NaN`) or a non-number is an error (kotlinx
    `SerializationException`, slumber `null`). kotlinx tells absent from null by the raw class's default value, a
    private marker `Long.MIN_VALUE` that no slot writes (an input of exactly that number reads as absent; the slumber
    awaker clamps to one above it, so any negative number is an empty slot);
  - one home for the rule: both codecs decode through `MpLocalTimeSlotRawData.toSlotOrNull()` (round 1; that is why it
    is public); slumber saturates a `BigInteger` / `BigDecimal` beyond `Long` to infinite or empty;
  - neither `duration` nor `to` is a `SerializationException` in kotlinx and `null` in slumber, as a missing field was
    before.
- **TypeScript (planned, outside the module):** `MpDateTimeTsContributor` claims `MpLocalTimeSlot`; `datetime.ts`
  declares `MpLocalTimeSlot = z.object({from, duration, to})` (numbers); `MpDateTimeFieldParitySpec` checks the
  slumbered keys and the declaration. `SlumberAsRoundTripSpec` (slumber) has a slot sample, as it demands for every
  `@Slumber.As` type.
- **`Partial`** keeps its two clock pickers; `asValidRange()` is `of(from, to)`, null for an empty slot;
  `asPartialRange()` is lossy for a slot of 24 h or more.

### Test evidence (3b-2)

- **New:** `MpLocalTimeSlotClockSpec` (19), `MpLocalTimeSlotOnDateSpec` (9), `MpLocalTimeSlotSerializationSpec`
  (6), all commonTest; slumber `MpLocalTimeSlotCodecSpec` (5); codegen parity row (1).
- **RED first:** the new specs did not compile against the old slot (37 errors: `endDayOffset`, `isEmpty`, `of(from,
  to)`, ...). After the build, the old specs failed exactly where the redesign changes behaviour (10 in datetime, 1
  slumber guard, 1 codegen guard); each is listed below.
- Final run (2026-11-14, `console/wt-gradle.sh`, `--rerun` on each test task, counts from the XML): datetime JVM
  2845 / 0, JS 2187 / 0, native 2179 / 0 (the three new specs green on all three); slumber jvmTest 1264 / 0; codegen
  test 326 / 0; karango ksp 11 / 0; monko ksp 6 / 0. Repo-wide compile sweep: exit 0, 0 `e:` lines.

| Mutant (JVM; each restored with `cmp`) | Tests red |
|---|---|
| S1 `endDayOffset` without the whole-day rule (`end / DAY`) | 7 |
| S2 `to` at midnight is 00:00, not `EndOfDay` | 11 |
| S3 the clock arc without its part after midnight | 5 |
| S4 `touches` without the meeting at midnight | 1 |
| S5 `isAdjacentTo` in one direction only | 2 |
| S6 `mergeWith`: the second branch starts at this start | 4 |
| S7 `cutAway` without the cut's part after midnight | 2 |
| S8 on a date by elapsed time (`start.plus(duration)`) instead of by the clock | 7 |
| S9 on a date without the empty-range rule (inverted again) | 3 |
| S10 `of(from, to)`: `to` before `from` is empty instead of overnight (persistence: the fallback) | 11 datetime + 1 slumber |
| S11 no threshold for an infinite duration | 2 datetime + 1 slumber |
| S12 the start not normalised | 2 |
| S13 `splitWithGaps` step not taken mod day | 0 at the time; the note "equivalent" was right only for the count of that build. Since round 1 an infinite slot gives `Int.MAX_VALUE` parts, `i * step` overflows, and the reduction is load-bearing: pinned in round 2, mutant red (see round 2) |
| P1 kotlinx decoder without the `{from, to}` fallback (persistence) | 1 |
| P2 kotlinx decoder reads `to` before `duration` (persistence) | 2 |
| P3 kotlinx / slumber write `Long.MAX_VALUE` for an infinite duration (persistence) | 1 datetime + 1 slumber |
| P4 stored 86400000 not read as 24:00 (persistence) | 1 |
| P5 slumber awaker without the `{from, to}` fallback (persistence) | 1 |
| P6 slumber awaker ignores `duration` (persistence) | 2 |

**Existing test lines changed (3b-2):**

| Spec | Before | After |
|---|---|---|
| `MpLocalTimeSlotSpec`, `MpLocalTimeEndOfDaySpec`, `test_helpers.kt` | `MpLocalTimeSlot(from, to)` (35 + 2 calls) | `MpLocalTimeSlot.of(from, to)`: the constructor is private |
| `MpLocalTimeSlotSpec` "duration must be correct" | 02:15 to 01:00 is `(-75).minutes` | `22.hours + 45.minutes` (overnight) |
| `MpLocalTimeSlotSpec` isValid row | "Negative duration": 12:00:01 to 12:00:00 is NOT valid | "To before from runs overnight": valid |
| `MpLocalTimeSlotSpec` split "an invalid slot" | 20:00 to 08:00 by 1 h: no slots | "an overnight slot": 12 slots 20:00 ... 07:00 |
| `MpLocalTimeSlotSpec` split "an invalid slot across midnight" | 22:00 to 02:00 by 2 h: no slots | `[22:00-00:00, 00:00-02:00]` |
| `MpLocalTimeSlotSpec` six `maxSlots` tests | default 100 000 works; 100 001 throws; whole day by 1 ms throws; custom limit; below 1 refused; infinite no throw | three tests: 100 000 and 100 001 both work; the quarter-hour splits without a limit; infinite duration or gap gives `[]` / one slot |
| `MpLocalTimeSlotSpec` "Partial.asValidRange returns null for invalid range" | `Partial(11:00, 10:00)` is null | equal times are null; `Partial(11:00, 10:00)` is `of(11:00, 23 h)` |
| `MpLocalTimeSlotSpec` "contains(time) / contains(other) / intersects - invalid slot" | used `12:00 to 10:00` as the invalid slot (still green, by coincidence) | an empty slot (`12:00 to 12:00`, `11:00 to 11:00`); titles say "empty" |
| `MpLocalTimeEndOfDaySpec` "MpLocalTimeSlot up to EndOfDay" | `slot.contains(endOfDay) shouldBe false` | `true` (24:00 is 00:00 on the clock face) |
| `MpLocalTimeEndOfDaySpec` "M1: completeDay serializes" | `{"from":0,"to":86400000}` | `{"from":0,"duration":86400000,"to":86400000}` |
| `MpLocalTimeEndOfDaySpec` "delta round 2: ofSecondsOfDay" | title "EndOfDay only for a from inside the day; otherwise both wrap" | title states the 3b-2 rules; rows unchanged and green |
| `MpLocalTimeEndOfDaySpec` "delta round 2: of(from, duration)" | raw `-3 600 000` + 25 h `to` is `Min`; `of(EndOfDay, 0) == of(EndOfDay, EndOfDay)`; `of(EndOfDay, 1 h) == of(EndOfDay, 01:00)` | the start wraps first: `== of(23:00, 25 h)`, `to == EndOfDay`; `of(EndOfDay, 0) == of(Min, 0)`; `of(EndOfDay, 1 h) == of(Min, 1 h)` |
| `MpLocalTimeEndOfDaySpec` Nuuk pin | "inverted range": `from > to` | "empty range": `from == to` |
| `MpLocalTimeEndOfDaySpec` Apia pin | `[09:00, 24:00)` inverted, `completeDay` empty | both empty |
| `MpLocalTimeSlotOnDateSpec` (new) TimeZone overload | Europe/Berlin through `TimeZone.of` | UTC: kotlinx on JS has no zone database in these tests (red on JS only) |
| slumber `SlumberAsRoundTripSpec` | six samples | plus an `MpLocalTimeSlot` sample (its guard demands one per `@Slumber.As` type) |
| codegen `MpDateTimeFieldParitySpec` | - | new row: the slot slumbers to `{from, duration, to}`, all numbers, and `datetime.ts` declares it |

**Existing test lines changed in round 1** (all in specs new in 3b-2, plus one import):

| Spec | Before | After |
|---|---|---|
| `MpLocalTimeSlotOnDateSpec` gap test | "moves forward": `02:30 + 2 h` = 01:30Z to 02:30Z; `01:00 + 90 min` = 00:00Z to 01:30Z | "moves to the first existing time": 01:00Z to 02:30Z; 00:00Z to 01:00Z; plus the reviewer's rows `02:30 + 1 h`, `01:00 + 2 h`, `02:00 + 1 h`, `02:15 + 30 min` |
| `MpLocalTimeSlotOnDateSpec` "a start moved past the end gives an empty range" | Nuuk: duration zero and `from == to`; Apia `09:00 + 15 h` and `completeDay`: duration zero | split in two tests that pin the instants: Nuuk empty at 2024-03-31T01:00Z; Apia `09:00 + 8 h`, `09:00 + 14 h 59 min`, `completeDay` empty at 2011-12-30T10:00Z, `09:00 + 16 h` = 10:00Z to 11:00Z |
| `MpLocalTimeSlotSpec` | `import io.kotest.assertions.throwables.shouldThrow` | removed (unused since the `maxSlots` tests went) |

### Docs lines for the DOCS follow-up (3b-2)

- `docs-site/src/pages/ultra/datetime/ranges.astro` 334-353 (the `MpLocalTimeSlot` section: the constructor,
  `completeDay`, the `ofSecondsOfDay` comments): start and duration, `of(from, to)` overnight, the clock face, on a
  date by the clock, the lazy `splitWithGaps`, the stored shape.
- `docs-site/src/data/llms/datetime.md` 228-230 (the slot line and factories).
- `docs-site/src/pages/ultra/datetime/ranges.astro` 361-377, 384 and 388, and `mp-local-date.astro` 164 (round 1:
  further slot examples and `toRange(slot, zone)`).
- `ultra/codegen/src/main/resources/ts/runtime/datetime.ts`: done in this batch (`MpLocalTimeSlot`,
  `INFINITE_DURATION_MILLIS`).

## Changelog text (3b-2)

**Breaking changes:**

- `ultra/datetime`: `MpLocalTimeSlot` is a start and a duration. Create it with `MpLocalTimeSlot.of(from, duration)`
  or `of(from, to)`; the constructor `MpLocalTimeSlot(from, to)` is gone (private), and `copy` with it.
  - `to` is derived: the clock time of the end (24:00 for an end at midnight); `endDayOffset` / `crossesMidnight` say
    whether it is a later day. Overnight slots (22:00 to 02:00) and slots longer than a day exist now.
  - New: `isEmpty`, `isInfinite`, `INFINITE_DURATION_MILLIS`; `Duration.INFINITE` is a slot without an end.
  - The start wraps into the day: `of(EndOfDay, d)` starts at 00:00 (ULTRA-00341).
- `ultra/datetime`, behaviour change (no compile signal):
  - `of(from, to)` and `ofSecondsOfDay(from, to)` with `to` before `from` are an overnight slot; they were an invalid
    one (`isValid` false, negative `duration`). Equal times are an empty slot, as before (also 24:00 to 24:00).
  - `of(from, duration)` keeps the whole duration: `of(10:00, 25.hours)` was the 1-hour slot 10:00 to 11:00 (the end
    wrapped), now it is 25 hours; `of(23:00, 2.hours)` was invalid, now it is overnight; a negative duration is an
    empty slot at `from`.
  - `Partial.asValidRange()` with `to` before `from` returned null (a form's "end before start" check) and now returns
    an overnight slot; only equal times give null.
  - A decoded slot is no longer bounded by one day: code that relied on `isValid` now accepts multi-year and endless
    slots (`isInfinite`).
  - `val (from, to) = slot`: `component2()` is the `duration` now.
  - Without a date a slot is a daily pattern on the 24-hour clock: `contains`, `intersects`, `touches`,
    `isAdjacentTo`, `mergeWith` and `cutAway` work across midnight; `contains(EndOfDay)` counts 24:00 as 00:00 (it was
    false); a slot of 24 hours or more covers the whole clock.
  - `MpLocalDate.toRange(slot, zone)` ends by the clock on the day `endDayOffset` later: a night slot lasts 7 or 9 real
    hours on DST nights. A start or end in a skipped hour moves to the first existing time (02:30 becomes 03:00; it
    moved by the gap length, to 03:30), a repeated hour takes its first occurrence, and a slot that starts and ends
    on a skipped date is empty at the next day's start (one that runs into the next day keeps that part); such cases
    gave inverted ranges before (ULTRA-00341).
  - `isAdjacentTo` with an empty slot is always false; it was true when the empty slot's point sat on the other's
    end (`10:00-11:00` and `11:00-11:00`).
- `ultra/datetime` and `ultra/slumber`, stored format: a slot is stored as `{from, duration, to}` in milliseconds
  (`to` derived, ignored on read; an infinite duration is 9007199254740991). Both decoders still read the old
  `{from, to}` (`to` before `from` as overnight; a stored `{0, 86399999}` reads as 23:59:59.999 long). A reader of an
  older version reads `{from, to}`: slumber ignores `duration`, a kotlinx `Json` without `ignoreUnknownKeys` fails on
  it. There an overnight slot comes out invalid, and a slot of 24 hours or more as its clock times only. Update
  readers before writers (ULTRA-00341).
- `ultra/datetime`, breaking: `splitWithGaps` loses `maxSlots` and never throws; it returns a lazy view
  (`ComputedList`; read-only, not a `MutableList`) of at most `Int.MAX_VALUE` parts, and walks overnight and
  multi-day slots. With user input, check `size` before `map` / `toList()`: nothing else bounds the count
  (ULTRA-00341).

**New:**

- `ultra/datetime`: `MpLocalTimeSlotRawData`, `MpLocalTimeSlotSerializer`, `MpLocalTimeSlot.toRawData()`,
  `MpLocalTimeSlotRawData.toSlotOrNull()`; `@Slumber.As` on the slot, so karango and monko query paths are `from`,
  `duration`, `to` (ULTRA-00341).
- `ultra/codegen`: the TypeScript runtime has `MpLocalTimeSlot` (`{from, duration, to}`) (ULTRA-00341).

**Removed API:** the public constructor `MpLocalTimeSlot(from, to)` and `copy`; the `maxSlots` parameter of
`splitWithGaps` (batch 1, never released).

**Existing Unreleased entries of `CHANGELOG.md` that become false (for the coordinator to replace at the merge):**

- ULTRA-00215 entry "`splitWithGaps` no longer takes durations and gaps modulo 24 h ...": its last sentence "An invalid
  slot (`to <= from`, e.g. 22:00-02:00) now yields no slots (was split across midnight)" (22:00-02:00 is an overnight
  slot now and is split across midnight).
- ULTRA-00215 entry "`MpLocalTimeSlot.splitWithGaps` takes `maxSlots` (default 100 000 ...) and throws
  `IllegalArgumentException` ...": `maxSlots` and the exception are gone.

## Review record 3b-2: round 1 (blind, reviewer-fable-xhigh, two passes)

**Verdicts:** implementation CLEAN (8 MINOR); domain and data NOT CLEAN (1 MAJOR, 9 MINOR). The reviewers recomputed
every expected value of the four specs (right) and ran nothing; every item below was shown by a test first (RED on
JVM, the JS row on JS).

| Sev | Finding | Outcome |
|---|---|---|
| MAJOR | a start or end in a skipped hour moved by the gap length (02:30 to 03:30), not to the first existing time as agreed with the maintainer | FIXED: `TimezoneEngine.toInstantOrTransition` (gap: the transition; overlap: the first occurrence), used only by the slot on a date; the reviewer's five Berlin rows, the split that sums to the whole, the repeated hour, Nuuk and Apia pinned with instants; RED first; mutants K1, K2 red |
| deviation | `of(EndOfDay, EndOfDay)` was 24 h (start wrapped, end not) | FIXED: equal as given is empty; factory, `Partial` and both decoders pinned; RED first; mutant K3 red |
| deviation | the start wraps into the day (my choice 1) | KEPT, as the reviewers found it sound; `date.toRange(of(EndOfDay, d))` starts at 00:00 of the date, stated in the KDoc |
| MINOR | `splitWithGaps` on an infinite slot: count an artefact of `Long.MAX_VALUE / step`; an infinite gap gave 1024 | FIXED: `Int.MAX_VALUE`, or one part for an infinite gap; RED first; mutants K4, K5 red; K6 (infinite part on infinite slot) red |
| MINOR | an explicit `duration: null` fell back to `to` (a TS client's `Infinity` / `NaN` became an empty slot) | FIXED: fallback only when ABSENT (kotlinx: a private default marker in the raw class, no custom decoder); RED first; mutants K7 (both codecs), K8 red |
| MINOR | slumber: a duration beyond `Long` wrapped (18446744073709551621 awoke as 5 ms) | FIXED: saturates; the bottom is clamped one above the absent marker (found while fixing: `-huge` collided with it); RED first; mutants K9, K10 red |
| MINOR | JS: `toRange` threw for a finite end beyond the JS date range | FIXED in `endOnDate` only: such an end is the open end; RED first on JS; mutant K16 red on JS |
| MINOR | `toSlotOrNull()` public but the awaker repeated the rule; dead `?: 0L` / `?: raw.from`; implied `coversClock` terms; `if (isEmpty) return` in `onDate` | FIXED: the awaker decodes through `toSlotOrNull()` (one home, public justified); dead arms and implied terms removed (equivalent by construction) |
| MINOR | surviving mutants: `!intersects` and the first `isEmpty` in `isAdjacentTo`; `mergeWith` covering branch always `this`; `cutAway` without the `coversClock` branch; the empty range at the end | FIXED: the reviewers' rows added; mutants K11 to K15 red (K11 through a date after the open end, the only remaining end-before-start case) |
| MINOR | the repeated hour unpinned | FIXED: Berlin 2024-10-27 `01:30 + 1 h` (23:30Z to 00:30Z) and 2024-10-26 `22:00 + 4.5 h` (to 00:30Z); first occurrence in the KDoc |
| MINOR | KDoc gaps: parts of a split carry no day; `formatHhMm` drops days; `endDayOffset` of an infinite slot; `toRange(of(EndOfDay, d))`; what a query on the stored `to` sees; the TS infinity number | FIXED: all in KDoc; `datetime.ts` exports `INFINITE_DURATION_MILLIS`, pinned against the Kotlin value by the parity spec |
| text | unused `shouldThrow` import; "two of the six"; docs list incomplete | FIXED |
| text | changelog: "equal times ... as before" and missing changes without a compile signal; the two `CHANGELOG.md` entries that become false | FIXED (see "Changelog text (3b-2)") |
| note | reviewers on my six choices: all sound except `toSlotOrNull()` public | settled by the one-home change above |
| counts | after round 1 (2026-11-14, `--rerun`, XML) | datetime JVM 2852 / 0, JS 2194 / 0, native 2186 / 0 (`MpLocalTimeSlotClockSpec` 20, `MpLocalTimeSlotOnDateSpec` 14, `MpLocalTimeSlotSerializationSpec` 7 on all three); slumber 1266 / 0 (`MpLocalTimeSlotCodecSpec` 7); codegen 326 / 0; compile sweep 0 `e:` |

| Mutant (round 1; each restored with `cmp`) | Tests red |
|---|---|
| K1 the slot's times on a date through `atTime` (gap moves by its length) | 4 |
| K2 a repeated hour takes the later occurrence | 1 |
| K3 `of(from, to)` without the equal-as-given check (persistence: both decoders) | 3 datetime + 1 slumber |
| K4 / K5 / K6 `splitWithGaps`: no one-part arm for an infinite gap / no endless arm / an infinite part gives nothing | 1 / 1 / 1 |
| K7 an explicit null duration falls back to `to` (persistence: kotlinx; slumber returns null before the shared rule, K8) | 1 |
| K8 slumber: a non-number duration falls back to `to` (persistence) | 1 |
| K9 slumber: `BigInteger` without saturation (persistence) | 1 |
| K10 slumber: no clamp above the absent marker (persistence) | 1 |
| K11 the empty range at the end, not the start | 1 |
| K12 `isAdjacentTo` without `!intersects` | 1 |
| K13 `isAdjacentTo` without the first `isEmpty` | 1 |
| K14 `mergeWith` covering branch always `this` | 1 |
| K15 `cutAway` without the `coversClock` branch | 1 |
| K16 JS: no open end for an unresolvable end (run on JS) | 1 |

## Review record 3b-2: delta round 2 (reviewer-fable-xhigh, all charters)

**Verdict:** CLEAN: 0 CRITICAL, 0 MAJOR; 9 MINOR, 6 surviving mutants, 4 text corrections. Every round-1 FIXED entry
holds; all DST instant pairs and every `MpLocalTimeSlotOnDateSpec` row recomputed and right. The reviewer ran
nothing; each item was settled by a test first.

| # | Finding | Outcome |
|---|---|---|
| 1 | slumber: `BigDecimal` expanded through `toBigInteger()` (costly, can throw for a huge exponent) | CONFIRMED: `1e400000000` took more than 5 s (RED: `TimeoutException`). FIXED: compared against `Long` bounds, never expanded; `1e400000000`, `-1e400000000`, `1e-400000000` pinned under a 5 s limit; mutant B3 red |
| 2 | the `BigDecimal("1e30")` row cannot fail | FIXED: `BigDecimal("1e19")` (wraps negative without the arm) is infinite; mutant B1 (no arm) red (2) |
| 3 | the `BigInteger` lower clamp unpinned | FIXED: `BigInteger("-18446744073709551611")` (low 64 bits +5) is empty; mutant B2 red |
| 4 | `step % DAY` load-bearing since round 1 | CONFIRMED: `of(10:00, INFINITE).splitWithGaps(60.days)[Int.MAX_VALUE - 1] == of(10:00, 60 days)` (whole days: every part at 10:00); mutant S13 red; the round-1 S13 note corrected |
| 5 | `plusDays` past the last date throws `DateTimeArithmeticException`, not caught | CONFIRMED: the last date is 999 999 999-12-31 on JVM, JS and native (probe of 2026-11-14: `LocalDate(1_000_000_000, 1, 1)` throws on all three); `of(22:00, 4 h)` there threw (RED). FIXED: caught in `endOnDate`, the open end; green on all three; mutant D1 red |
| 6 | KDoc "any count is free to create" false above 2^31 - 1 | FIXED: "at most `Int.MAX_VALUE` parts, for any slot", and a line for callers with user input (check `size` before `map` / `toList()`) |
| 7 | surplus: the `isEndOfDay` return in `firstExistingTime`; `startOfDay` duplicates the resolution | CONFIRMED equivalent: both removed / delegated (`startOfDay` = `toInstantOrTransition(date at 00:00)`), suites green on JVM, JS and native with the start-of-day tests untouched (Cairo, Beirut, Nuuk, Apia, Sao Paulo) |
| 8 | `NaN` duration awoke as an empty slot | CONFIRMED (RED). FIXED: null; `+Infinity` is an infinite slot, `-Infinity` an empty one (they saturate), all pinned; mutant B4 red |
| 9 | KDoc `transitionAfter`: also a slot's start | FIXED |
| 10 | surviving mutants: fixed-offset return, the marker as `-1L`, `>` to `>=` in the covering merge | FIXED: the reviewer's rows (`+02:00` gives 00:30Z to 01:30Z; `{"from":0,"duration":-1,"to":3600000}` is empty; two 24 h slots keep `this`); mutants F1 (3 red, also two converter rows through `startOfDay`), F2, F3 red |
| 11 | texts: the skipped-date sentence; `isAdjacentTo` with an empty slot; `splitWithGaps` bound and not a `MutableList`; the plan line of the raw class | FIXED |
| note | the round-1 note on `Long.MIN_VALUE` as an absent duration | WITHDRAWN by the reviewer (a stored slot never writes it; slumber clamps above it) |
| decision | `map` / `toList()` on a split are unbounded now that `maxSlots` is gone | FOR THE MAINTAINER: documented in the KDoc and the changelog; no limit added |
| counts | after round 2 (2026-11-14, `--rerun`, XML) | datetime JVM 2854 / 0, JS 2196 / 0, native 2188 / 0 (`MpLocalTimeSlotOnDateSpec` 16, `MpLocalTimeSlotClockSpec` 20, `MpLocalTimeSlotSerializationSpec` 7 on all three); slumber 1268 / 0 (`MpLocalTimeSlotCodecSpec` 9); codegen 326 / 0; compile sweep 0 `e:` |

| Mutant (round 2; each restored with `cmp`) | Tests red |
|---|---|
| B1 slumber: no `BigDecimal` arm (persistence) | 2 |
| B2 slumber: `BigInteger` without the lower clamp (persistence) | 1 |
| B3 slumber: `BigDecimal` expanded through `toBigInteger()` (persistence) | 1 (timeout) |
| B4 slumber: `NaN` accepted (persistence) | 1 |
| S13 `splitWithGaps` step not taken mod day | 1 |
| F1 `toInstantOrTransition` without the fixed-offset return | 3 |
| F2 the absent marker as `-1L` (persistence) | 1 |
| F3 covering merge with `>=` | 1 |
| D1 no catch for `DateTimeArithmeticException` | 1 |

## Review record 3b-2: delta round 3 (reviewer-fable-xhigh)

**Verdict:** CLEAN: 0 CRITICAL, 0 MAJOR, 7 MINOR. Both item-7 equivalences proven by trace; the catch in `endOnDate`
as narrow as it can be; every round-2 FIXED entry holds; every changelog sentence true. The reviewer ran nothing;
each item below was settled by a run. Close-out only: test rows, texts and dead code, no behaviour change.

| # | Finding | Outcome |
|---|---|---|
| M1 | the lower `BigDecimal` arm unpinned | FIXED: `BigDecimal("-1e19")` is empty (positive low 64 bits without the arm); mutant C1 red |
| M2 | the value of the in-range arms unpinned | FIXED: `BigDecimal("14400000.9")` and `BigInteger.valueOf(14_400_000)` give `night`; mutants C2 (`BigDecimal` else -> 0) and C3 (`BigInteger` -> 0) red |
| M3 | `EpochMath.epochSeconds(LocalDate)` and `instant(LocalDate, Int)` dead since the delegation | CONFIRMED by search (all source sets, all modules: only `EpochMathSpec` called the `LocalDate` overload; `EpochMath` is internal): both deleted with the imports `atStartOfDayIn` and `LocalDate`; `EpochMathSpec` uses `instant(LocalDateTime(date, LocalTime(0, 0)), offset)`; compile sweep clean |
| M4 | the last-date row pinned no end and no literal | FIXED: `to == from` (empty at the start) and the start as epoch second 31 556 889 832 773 600, recomputed independently (days-from-civil) and asserted on JVM, JS and native |
| M5 | `MpLocalTimeEndOfDaySpec` compared two values from the same function | FIXED: the instant 2024-03-30T23:00Z pinned |
| M6 | texts: "used only here"; "Not merged, not reviewed"; the only end-before-start case | FIXED in this ticket and the KDoc of `onDate` ("on a date after it, or past the last date the platform holds") |
| M7 | no start-of-day case with an overlap at midnight | FIXED: `StartOfDayCase("America/Havana", "2022-11-06", 1667707200L, "2022-11-06T00:00")`, recomputed independently (Python zoneinfo, fold 0 at -04:00); `ZoneParityJvmSpec` agrees with java.time; green on all three |
| withdrawn | two observations of the reviewer | WITHDRAWN by the reviewer |
| counts | after round 3 (2026-11-14, `--rerun`, XML) | datetime JVM 2855 / 0, JS 2197 / 0, native 2189 / 0 (`ZoneParitySpec` 55 on all three, `ZoneParityJvmSpec` 4); slumber 1268 / 0 (`MpLocalTimeSlotCodecSpec` 9); codegen 326 / 0; compile sweep 0 `e:`; mutants C1, C2, C3 red (1 each) |

## Sentinel values restored for the merge (2026-11-14)

The maintainer: "ok please do this so the merge works". One commit on top of the merge of master restores master's
`GENESIS_TIMESTAMP = -377_736_739_200_000L` (-10000-01-01) and `DOOMSDAY_TIMESTAMP = 253_402_300_800_000L`
(+10000-01-01) and nothing else of 3b-1. Which values (or none) is open in ULTRA-00343, after the merge.

- **Production, now identical to master:** `constants.kt`, `MpInstant.kt`, `MpLocalDateTime.kt`, `MpZonedDateTime.kt`;
  in `MpLocalDate.kt` the Genesis / Doomsday KDoc (`numDaysInMonth` from the calendar and the slot mapping stay).
  Kept, as it holds for any values: the `numberOfDays` KDoc on the clamp.
- **Stay:** `ComputedList`, the lazy `asListOfDates()`, `numDaysInMonth` from the calendar, all of 3b-2.
- **Tests back to master's files:** `ConstantsSpec`, `MpInstantSpec`, `MpLocalDateSpec` (plus the moved
  `numDaysInMonth` rows), `MpLocalDateTimeSpec`, `MpZonedDateTimeSpec`, `MpInstantRangeSpec`,
  `MpZonedDateTimeRangeZoneSpec`. Only the 3b-1 value commit had touched them.
- **Rewritten:** `GenesisDoomsdaySpec` for master's values (timestamps, text and parse of both, open on every range
  type, stored ts and read back open, the neighbours on both sides on all four types, `forever` contains 9999-12-31,
  `beginningAt(d).asOpenRange.to` = 10000-01-02, dates outside, the clamp counts, Genesis in New York on -10001-12-31,
  the converter clamp). `MpLocalDateRangeLazySpec`: `forever` is 7,304,850 dates, -10000-01-01 to 9999-12-31, index
  1,000,000 = -7263-11-28 (independent day count); the closed `forever` 7,304,851, through 10000-01-01.
- **Deleted:** slumber `OldSentinelsCodecSpec`. It pinned that ranges stored with master's values still read as open
  after the switch to year 1 / 9999; with master's values back there is nothing old to read (master had no
  equivalent, so nothing comes back).
- **3b-2 with +10000-01-01, proven by run:** every open-end row (the last date 999 999 999-12-31, the JS open end, the
  infinite slots, the empty range past the open end) is green unchanged on JVM, JS and native; no 3b-2 expectation
  changed.
- **Mutants:** `GENESIS_TIMESTAMP` one day later: red on JVM (15), JS (14) and native (14);
  `DOOMSDAY_TIMESTAMP` one day later: red on JVM (16), JS (15) and native (15); `asListOfDates()` back to the eager
  list: red (2, the `ComputedList` checks on both `forever` views). Each restored and checked with `cmp`.
- **Counts** (2026-11-14, `--rerun`, XML): datetime JVM 2855 / 0, JS 2197 / 0, native 2189 / 0 (`GenesisDoomsdaySpec`
  11, `MpLocalDateRangeLazySpec` 4 on all three); slumber 1267 / 0 (one spec fewer); codegen 326 / 0; common 389 / 0.
  No module outside `ultra/datetime` and slumber's tests uses the sentinels or the range types (searched funktor,
  kraft, vault, karango, monko, codegen). Compile sweep: 0 `e:` lines.

## Left out, for one follow-up ticket

- `MpLocalTime.jvm` drops the milliseconds (`Max.jvm` = 23:59:59).
- `MpLocalTime.parse`'s error message names `MpLocalDate`.
- ~~`MpLocalDateRange.numberOfDays` miscounts ranges past Doomsday.~~ Moved to ULTRA-00343: the clamp stays,
  documented and pinned; its consequences before Genesis and around Doomsday are with the maintainer (round 1).

## Batch 3b: decisions of the maintainer (2026-11-14)

The track's plan is `track-a-plan-batch3b.md` in the session scratchpad; it goes into this ticket as built.

**3b-1, started:** `ComputedList` in `ultra/common` (package `io.peekandpoke.ultra.common`, also `RandomAccess`),
`asListOfDates()` on both date range types as a lazy view ("yes go for this and document that the returned list is a
lazy view and what that means for the consumer"), and the new Genesis and Doomsday of ULTRA-00343.

**3b-2, the slot redesign, decided so far:**

- `MpLocalTimeSlot` is a start and a duration ("two different params: start: MpLocalTime and duration ... then we can
  represent everything also overnight"); "no backward compat needed"; "no limits to the duration".
- `to: MpLocalTime` is a derived property, and on a date the range ends at that clock time ("what this returns must
  be the time on the result, so we have to respect DST"): by the clock, not by elapsed time.
- Elapsed time: "out for now". If wanted later, a second function, not a flag.
- Predicates work on the 24-hour clock face; a duration of 24 hours or more covers the whole clock ("yes correct").
- Edge rules without exceptions ("yes to the exception rules"): a start or end in a skipped hour moves forward; a start
  past the end gives an empty range; a duration of zero or below is an empty slot.
- Stored shape: "we write {from, duration, to}", `to` derived and read-only; the decoders fall back to the old
  `{from, to}` ("we could build a fallback in the deserializers for the shape {from, to}"): `to` after `from` is the
  difference, `to` before `from` is an overnight slot, `to` equal to `from` is empty.
- `splitWithGaps` returns the lazy view; `maxSlots` and its exception go ("yes to splitWithGaps using the same
  mechanism").

**Confirmed by the maintainer (2026-11-14): `of(from, to)` with equal times gives an empty slot** ("yes equal times
in an empty slot"). A full day is written `of(from, 24.hours)` or `completeDay`.

**Taken as the track proposed, announced to the maintainer without objection:** `contains(EndOfDay)` counts 24:00 as
00:00; a merge that covers the whole clock gives one 24-hour slot starting at the first slot's start; `cutAway` on a
slot longer than 24 hours works on the full clock and drops the length beyond 24 hours; the constructor is private and
durations are cut to whole milliseconds; `Duration.INFINITE` gives an open end on a date.

3b-2 starts when round 1 of 3b-1 is back: no edits in the worktree while a review round reads it.

## History

- 2026-11-14: created by the coordinator for batch 3 of track A.
- 2026-11-14: batch 3a built (`618a4860`); review round 1 handled on `wt/datetime`, awaiting the delta review.
- 2026-11-14: batch 3a merged (c6ecca2a). Batch 3b-1 (lazy views, with ULTRA-00343) built on `wt/datetime`,
  awaiting the review gate; 3b-2 waits for the maintainer.
- 2026-11-14: 3b-1 round 1 and delta round 2 handled; 3b-1 held (the sentinel values are in question). Batch 3b-2
  (the slot redesign) built on `wt/datetime` on top of it, tests first; not reviewed, not merged.
- 2026-11-14: 3b-2 round 1 NOT CLEAN (1 MAJOR: the gap rule), all findings handled; awaiting the delta review.
- 2026-11-14: 3b-2 delta round 2 CLEAN; its MINORs, mutants and texts handled; a short delta review before the merge.
- 2026-11-14: 3b-2 delta round 3 CLEAN; close-out applied. Gate closed; 3b-2 held with 3b-1 for the maintainer's word on
  the sentinel constants.
- 2026-11-14: master merged into `wt/datetime`; master's sentinel constants restored on top for the merge (maintainer);
  the choice of values stays open in ULTRA-00343.
