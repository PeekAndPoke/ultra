# ULTRA-00343: datetime: new values for Genesis (0001-01-01) and Doomsday (9999-12-31)

> **Status (2026-11-14): DESIGNED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Plan:** ULTRA-00316 (close the open epics), track A; built together with the lazy list views of ULTRA-00341
(maintainer, 2026-11-14: "ok to the new dates. New ticket and build it with the lazy list views").

## Why this exists

`Genesis` and `Doomsday` exist for one purpose: they stand in for "no start" and "no end" of a range
(`forever`, `beginningAt`, `endingAt`, `hasStart = from > Genesis`, `hasEnd = to < Doomsday`, and the clamp in
`DateTimeRangeConverter`). Nothing outside `ultra/datetime` uses them in main code. Their values today are
-10000-01-01T00:00:00Z and +10000-01-01T00:00:00Z (`constants.kt`: -377_736_739_200_000 and 253_402_300_800_000 ms).

What that costs: the values are stored with every open-ended range, and they lie outside what databases and tools
accept. ArangoDB's AQL date functions accept timestamps from -62167219200000 to 253402300799999, i.e.
"0000-01-01T00:00:00.000Z" to "9999-12-31T23:59:59.999Z"; outside it they return null with a warning (ArangoDB docs,
AQL date functions, read through webq on 2026-11-14, request 20261114T160814-caeb7e52e911). Today's Doomsday is one
millisecond past that limit. The text form has a five-digit year.

No production system uses the libraries (maintainer, 2026-11-14), so the values can change now. The nullable variants
(`MpLocalDateRange.Partial` and its siblings) already exist for callers who want explicit open ends; the sentinel
design stays.

## Decided (maintainer, 2026-11-14)

| | Value | As a date | Timestamp in ms |
|---|---|---|---|
| Genesis | 0001-01-01T00:00:00Z | 0001-01-01 | -62_135_596_800_000 |
| Doomsday | 9999-12-31T00:00:00Z | 9999-12-31 | 253_402_214_400_000 |

Genesis is the first day of year 1, Doomsday the last day of year 9999; as instants each is the start of that day in
UTC. Reasons: both lie inside ArangoDB's range; Doomsday keeps a four-digit year in every zone (at 00:00 UTC it is
9999-12-31T14:00 in the furthest-ahead zone); year 1 to 9999 is also the range of several other date types (from
memory, not checked: .NET `DateTime`, Python `datetime`, SQL Server `datetime2`).

**Three further points, confirmed by the maintainer (2026-11-14):**

1. **Dates outside Genesis to Doomsday stay valid as plain values** ("yes allowed"): `MpLocalDate.of(-500, 3, 15)` and
   `MpLocalDate.of(12000, 1, 1)` work as today. Genesis and Doomsday only mark "open" in ranges; they are not limits of
   the date types. Consequence, accepted: a range that starts before year 1 has `hasStart == false`.
2. **`numberOfDays` keeps its clamp, documented** ("yes keep clamp and document it"): a range that reaches to or past
   Doomsday counts as open-ended and its length is counted up to Doomsday; the same at Genesis. This settles item 3 of
   ULTRA-00342: no fix there, the KDoc states the rule and a test pins it.
3. **Genesis shown in a zone west of UTC falls on 0000-12-31** ("accept it").

## The work

- Tests first: the four constants on each type (`MpInstant`, `MpLocalDate`, `MpLocalDateTime`, `MpZonedDateTime`) and
  the JVM helpers in `jvmMain/constants.kt`; `forever`, `beginningAt`, `endingAt`, `hasStart`, `hasEnd` on every range
  type; a value stored with the OLD sentinels still reads as open (`to < Doomsday` is false for the old, later value;
  `from > Genesis` is false for the old, earlier one): kotlinx and slumber round trips; the clamp in
  `DateTimeRangeConverter`; `numberOfDays` of `forever` and of a range that reaches past Doomsday (item 3 of
  ULTRA-00342: decide and pin the rule here).
- Search the repo for the old numbers and years in code, tests, docs and the TypeScript runtime.
- Mutation checks on the constants and on the open-end predicates.
- CHANGELOG: breaking, a behaviour change without a compile signal (new values; what counts as open).
- Docs lines for ULTRA-00339: `mp-instant.astro:49-50`, `mp-local-date.astro:37-38`, `llms/datetime.md:65-66`.

## Acceptance

- The constants have the decided values on JVM, JS and native; an open range's stored timestamps lie inside
  ArangoDB's range; datetime and slumber suites green.
- Corrected in review round 1: one exception stays. `MpClosedLocalDateRange.beginningAt(d).asOpenRange` stores
  `to` = 10000-01-01 (Doomsday plus one day, which is the old Doomsday: 1 ms outside ArangoDB's range, a five-digit
  year). Pinned and named in the changelog.

## Plan as built

Built on `wt/datetime` in one commit with the lazy list views (ULTRA-00341, batch 3b-1).

- **`constants.kt`:** `GENESIS_TIMESTAMP = -62_135_596_800_000L` (0001-01-01T00:00:00Z), `DOOMSDAY_TIMESTAMP =
  253_402_214_400_000L` (9999-12-31T00:00:00Z). Every `Genesis` / `Doomsday` of `MpInstant`, `MpLocalDate`,
  `MpLocalDateTime`, `MpZonedDateTime` and the JVM helpers (`GenesisInstant`, …) derive from them; only the KDoc
  changed there. The KDoc says what they stand for and that they are not limits of the date types.
- **Unchanged code:** `hasStart = from > Genesis`, `hasEnd = to < Doomsday` on every range type, the clamp in
  `DateTimeRangeConverter`. Because the old values lie outside the new ones, a range stored with them still reads as
  open (pinned for kotlinx and slumber).
- **`numberOfDays`:** keeps its clamp (item 2); KDoc on `MpLocalDateRange.numberOfDays` and
  `MpClosedLocalDateRange.numberOfDays`. This settles item 3 of ULTRA-00342.
- **Found while pinning the clamp, pre-existing, for the maintainer:** `numberOfDays` is the noon-to-noon span in UTC,
  clamped to Doomsday (00:00), divided by a day and rounded with `kotlin.math.round` (half to even). A range past
  Doomsday therefore ends on a half day, and Doomsday itself is counted for every second start date:
  2024-01-01 → 12000-01-01 and 2024-01-02 → 12000-01-01 both give 2_913_173. Pinned as is, documented in the KDoc.
- **Round 1, `numDaysInMonth`:** it was the `numberOfDays` of the month as a closed range, so it went through the
  clamp: with Genesis at year 1 every month before year 1 got a wrong, mostly negative count (`of(-500, 3, 15)`:
  -182,897), and `getDatesInMonth` (all three overloads) lost dates. Now `atLastOfMonth().day`, from the calendar. The
  same fix repairs months from 10000-01 on, which were already wrong on master. Every other member of `MpLocalDate`
  and `MpLocalDateTime` was checked: none other reaches a range, the converter or a sentinel (`toRange`,
  `toClosedRange`, `toEnclosingMonthRange` build ranges, whose members are range behaviour).
- **Round 1, `numberOfDays` itself:** NOT changed; the consequences of the kept clamp for ranges before Genesis and
  around Doomsday are with the maintainer (probe results and a calendar-exact proposal in the track A report). The
  KDoc now also says that a range wholly before Genesis or after Doomsday gets a negative count.

### Test evidence

- **New:** `GenesisDoomsdaySpec` (commonTest, 8): the timestamps; every type's constants; `forever` / `beginningAt` /
  `endingAt` with `hasStart` / `hasEnd` on `MpInstantRange`, `MpLocalDateRange`, `MpClosedLocalDateRange`,
  `MpZonedDateTimeRange`; ranges with the OLD sentinels through kotlinx Json read as open (and `dates.to` stays
  10000-01-01); dates outside stay valid, a range from year -500 has `hasStart == false`; the `numberOfDays` clamp
  (2024-01-01 → 12000: 2_913_173, `forever`: 3_652_058, 2024-01-02 → 12000: 2_913_173); Genesis in America/New_York
  is 0000-12-31; the converter clamps to the sentinels. Slumber `OldSentinelsCodecSpec` (jvmTest, 1): the old values
  through the slumber codecs read as open.
- **RED first** (new tests and changed rows against the old constants, JVM): 17 red, among them
  `GenesisDoomsdaySpec` 6 of 8, `MpLocalDateRangeLazySpec` 2 of 4 (the `forever` sizes), and the changed rows of
  `ConstantsSpec`, `MpLocalDateTimeSpec` (parse rows) and `MpZonedDateTimeRangeZoneSpec`. The old-sentinel tests are
  green before and after by design (the old values lie outside both); their teeth come from mutants G7 / G8.
- **Round 1, new tests:** `GenesisDoomsdaySpec` 8 → 12: old sentinels now on all four range types (kotlinx), the
  open-end predicates at their neighbours (both sides, every range type but zoned), the end boundary at
  9999-12-31T00:00Z (`9999-12-31T23:59:59.999Z` reads as "no end"; `forever` no longer contains the last day),
  `beginningAt(d).asOpenRange.to` = 10000-01-01 with its stored `ts`, the mixed old / new `cutAway` sliver;
  "dates outside stay valid" now pins `numDaysInMonth` ((-500, 3) 31, (0, 12) 31, (0, 6) 30, (0, 2) 29, (1, 2) 28,
  (9999, 12) 31, (10000, 1) 31, (12000, 2) 29) and `getDatesInMonth` for months before year 1 and after 9999 in all
  three overloads. Literals from an independent count (Hinnant's days-from-civil, cross-checked against Python's
  `datetime` through the 400-year cycle). Slumber `OldSentinelsCodecSpec` adds `MpClosedLocalDateRange`.
- **RED first (round 1):** "dates outside stay valid" (`expected 31 but was -182897`). The other new rows pin today's
  behaviour; their teeth come from mutants H4 to H10.
- Counts: see the 3b-1 section of ULTRA-00341.

| Mutant (JVM; each restored with `cmp`) | Tests red |
|---|---|
| G1 `GENESIS_TIMESTAMP` one day later | 13 (datetime) |
| G2 `DOOMSDAY_TIMESTAMP` one day later | 13 (datetime) |
| G4 `MpLocalDateRange.hasEnd` with `<=` | 1 (`forever` / `beginningAt` / `endingAt` row) |
| G7 `MpLocalDateRange.hasEnd` as `!=` (persistence: old values read as closed) | 4 datetime (incl. the kotlinx old-sentinel test) + 1 slumber (`OldSentinelsCodecSpec`) |
| G8 `MpInstantRange.hasStart` as `!=` (persistence: old values read as closed) | 1 datetime (the kotlinx old-sentinel test) + 1 slumber (`OldSentinelsCodecSpec`) |
| H1 `numDaysInMonth` back to the closed range's `numberOfDays` | 1 |
| H4 `MpClosedLocalDateRange.hasEnd` as `to != Doomsday` (persistence) | 2 datetime (kotlinx old-sentinel, neighbours) + 1 slumber |
| H5 `MpLocalDateRange.hasEnd` as `to < Doomsday.minusDays(1)` (survived before round 1) | 1 (neighbours) |
| H6 `MpLocalDateRange.hasStart` as `from > Genesis.plusDays(1)` | 1 (neighbours) |
| H7 / H8 `MpInstantRange.hasEnd` 1 ms early / `hasStart` 1 ms late | 1 / 1 (neighbours) |
| H9 `MpClosedLocalDateRange.hasEnd` as `to < Doomsday` (not through `asOpenRange`) | 1 (neighbours) |
| H10 `MpClosedLocalDateRange.hasStart` as `from > Genesis.plusDays(1)` | 1 (neighbours) |

**Existing test lines changed:**

| Spec | Before | After |
|---|---|---|
| `ConstantsSpec` (jvmTest) | `GenesisInstant` "-10000-01-01T00:00:00Z", `DoomsdayInstant` "+10000-01-01T00:00:00Z" | "0001-01-01T00:00:00Z", "9999-12-31T00:00:00Z" |
| `ConstantsSpec` | `GenesisLocalDate` "-10000-01-01", `DoomsdayLocalDate` "+10000-01-01" | "0001-01-01", "9999-12-31" |
| `ConstantsSpec` | `GenesisLocalDateTime` "-10000-01-01T00:00", `DoomsdayLocalDateTime` "+10000-01-01T00:00" | "0001-01-01T00:00", "9999-12-31T00:00" |
| `MpInstantSpec` | Genesis / Doomsday `toIsoString` "-10000-01-01T00:00:00.000Z" / "+10000-01-01T00:00:00.000Z" | "0001-01-01T00:00:00.000Z" / "9999-12-31T00:00:00.000Z" |
| `MpLocalDateSpec` "Genesis and Doomsday" | `of(-10000, JANUARY, 1)`, `of(10000, JANUARY, 1)`, the old ISO literals | `of(1, JANUARY, 1)`, `of(9999, DECEMBER, 31)`, the new ISO literals |
| `MpLocalDateTimeSpec` | the same constants; "parse - Genesis" / "parse - Doomsday" on "-10000-01-01T00:00:00(.000Z, Z, none)" / "+10000-01-01T00:00:00(.000Z)" | the same rows on "0001-01-01…" / "9999-12-31…" |
| `MpZonedDateTimeSpec` | Genesis / Doomsday `toIsoString` as in `MpInstantSpec` | the new literals |
| `MpInstantRangeSpec` "Creation - forever" | `duration shouldBe 7304850.days` | `3652058.days` |
| `MpZonedDateTimeRangeZoneSpec` converter tie test | dates (-10000, 1, 1) / (10000, 1, 1); `past` from (-10000, 1, 1) | (1, 1, 1) / (9999, 12, 31); `past` from (1, 1, 1); comment updated |
| `MpZonedDateTimeRangeZoneSpec` round-4 test | "-01:00" to-date 9999-12-31 | 9999-12-30, which keeps it a tie at the new Doomsday; title updated |

### Repo search (old numbers -377736739200000, 253402300800000 and the years ±10000)

- Main code: only `ultra/datetime` (`constants.kt`, the KDoc of `MpInstant`, `MpLocalDate`, `MpZonedDateTime`); all
  changed. The JVM helpers derive from the constants.
- Tests: the rows above. Slumber's `MpZonedDateTimeRangeCodecSpec` uses `beginningAt` without literals.
- No hits in the TypeScript runtime, karango, monko or funktor (funktor's hits are the English word "forever").
- Docs (for ULTRA-00339): see below.

### Docs lines for ULTRA-00339

- `docs-site/src/data/llms/datetime.md:65-66`: Genesis / Doomsday as -10000-01-01 / +10000-01-01.
- `docs-site/src/pages/ultra/datetime/mp-local-date.astro:37-38`: the same.
- `docs-site/src/pages/ultra/datetime/mp-instant.astro:49-50`: the same.
- New to say: they stand for "no start" / "no end" in ranges and are not limits of the date types; a stored range
  with the old values still reads as open; `numberOfDays` counts an open-ended range up to Doomsday; `forever` does
  not contain 9999-12-31.

## Changelog text

**Breaking changes, behaviour change (no compile signal):**

- `ultra/datetime`: `Genesis` is now 0001-01-01T00:00:00Z and `Doomsday` 9999-12-31T00:00:00Z on `MpInstant`,
  `MpLocalDate`, `MpLocalDateTime`, `MpZonedDateTime` and the JVM helpers (they were -10000-01-01 and +10000-01-01);
  `GENESIS_TIMESTAMP` = -62_135_596_800_000 and `DOOMSDAY_TIMESTAMP` = 253_402_214_400_000 (public constants). Both
  now lie inside the range of ArangoDB's date functions, and the text form has a four-digit year (ULTRA-00343).
  - What counts as open moved: an end at or after 9999-12-31T00:00Z is "no end", so `9999-12-31T23:59:59.999Z`
    (another system's maximum) and a half-open date range with `to` = 9999-12-31 read as open-ended; a start at or
    before 0001-01-01T00:00Z is "no start", and so is a range that starts before year 1.
  - `forever` and `beginningAt(x)` no longer contain the last day: `MpLocalDateRange.forever.contains(9999-12-31)`
    and `MpInstantRange.forever.contains(9999-12-31T12:00Z)` are false (true before).
  - Ranges stored with the old values still read as open (`hasStart` / `hasEnd` false), but they are not equal to
    the new `forever` / `beginningAt` / `endingAt`, and their stored `to` stays 10000-01-01. Mixing them leaves
    slivers: an old `beginningAt(x).cutAway(new beginningAt(y))` keeps `[9999-12-31, 10000-01-01)`, which reads as
    open-ended.
  - An OLDER reader (a service or browser tab not yet upgraded) reads a range written with the new values as closed:
    an end in 9999, a start in year 1. Update readers before writers.
  - `MpClosedLocalDateRange.beginningAt(d).asOpenRange` stores `to` = 10000-01-01 (Doomsday plus one day): 1 ms
    outside ArangoDB's date range, with a five-digit year.
  - `forever.numberOfDays` is 3,652,058 (was 7,304,850); `MpInstantRange.forever.duration` is 3,652,058 days.
  - `forever.asListOfDates()` ends at 9999-12-30 (`MpClosedLocalDateRange.forever` at 9999-12-31).
  - Genesis shown in a zone west of UTC falls on 0000-12-31.
  - Dates outside Genesis to Doomsday stay valid plain values.
  - `numberOfDays` counts a range that reaches past Doomsday only up to Doomsday (unchanged, now documented).

**Fixes:**

- `ultra/datetime`: `MpLocalDate.numDaysInMonth` and `getDatesInMonth` come from the calendar. They went through a
  date range clamped to Genesis / Doomsday and returned wrong counts (and lost dates) for months from 10000-01 on
  (ULTRA-00343).

## Review record: round 1 (blind, reviewer-fable-xhigh, two passes)

**Verdict:** NOT CLEAN on both passes, the same MAJOR from each. Reviewers recomputed the constants, every derived
value and the stored forms: correct. They ran nothing; every item below was checked by a test or probe first.

| Sev | Finding | Outcome |
|---|---|---|
| MAJOR | `numDaysInMonth` goes through `numberOfDays` and the clamp: months before year 1 wrong (`(0, 12)` → 1, `(-500, 3)` → -182,897), `getDatesInMonth` loses dates | CONFIRMED by probe (exact values as reported); FIXED: `atLastOfMonth().day`; RED first; mutant H1 red; all `MpLocalDate` / `MpLocalDateTime` members checked, no other reaches a range |
| decision | consequences of the kept `numberOfDays` clamp (negative counts before Genesis, clamped count with an unclamped start, half-day rounding drops 9999-12-31 for every second `beginningAt`, `(9999-10-01, 9999-12-31)` → 91) | CONFIRMED by probe, every number as reported; NOT changed: with the maintainer; calendar-exact proposal in the track A report |
| MINOR | old-sentinel tests miss `MpClosedLocalDateRange` | FIXED: kotlinx and slumber; mutant H4 red (2 + 1) |
| MINOR | open-end predicates pinned from one side; `to < Doomsday.minusDays(1)` survives | FIXED: neighbours on both sides; mutants H5 to H10 red |
| MINOR | changelog imprecise (boundary, `forever` loses the last day, older readers, `asOpenRange.to` = 10000-01-01, mixed slivers); acceptance sentence wrong | FIXED: changelog and acceptance; each pinned by a test |
| note | Genesis on 0000-12-31 in a western zone is outside what .NET `DateTime`, Python `datetime` and an HTML date input hold | RECORDED for the maintainer, no change |

**Found while probing, pre-existing, not fixed (outside this round):** `MpZonedDateTime.toIsoString` pads a negative
year as a number (`year.pad(4)`): year -2 prints `00-2-12-30`, year -500 prints `-500-03-15` (ISO: `-0002`, `-0500`).
It also writes the stored `human` field of `MpLocalDate` and the other datetime types; the decoders read `ts`, so
values round-trip, but the stored text is malformed for years -1 to -999.

## History

- 2026-11-14: created on the maintainer's word.
- 2026-11-14: the three further points confirmed by the maintainer; the topic is closed.
- 2026-11-14: built on `wt/datetime` with ULTRA-00341 batch 3b-1; awaiting the review gate (the coordinator moves
  the ticket).
