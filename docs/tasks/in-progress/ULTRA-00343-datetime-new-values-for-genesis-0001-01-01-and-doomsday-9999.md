# ULTRA-00343: datetime: new values for Genesis (0001-01-01) and Doomsday (9999-12-31)

> **Status (2026-11-15): IN PROGRESS.** Decided by the maintainer (sections "Decided" and "Confirmed with the maintainer on the track's plan"). Batch A (slots are finite) is merged into master as `ebd62a3d`. Open: batch B (queries on `Partial`), batch C (Genesis and Doomsday removed, the factories return `Partial`). Then ULTRA-00347 (dates BC as text).
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

## REOPENED (maintainer, 2026-11-14, later the same day)

"i am actually planning on some history projects that would need dates BC ... so we need different values or think
again about getting rid of genesis and doomsday. What would it mean to leave them behind? Bring your analysis up when
i am back."

So the values decided below (year 1 and year 9999) are in question, and with them the clamp of `numberOfDays`
("keep the clamp", said before the BC requirement came up). The reviews of batch 3b-1 showed what the year-1 Genesis
does to ranges before year 1: a negative day count and an empty date list for a range wholly in BC. Batch 3b-1
(`wt/datetime`, commits `4e9b26c1`, `65514c4a`) is reviewed clean but NOT merged. Track A writes the analysis
(keep the sentinels with other values, or explicit open ends, or a hybrid); nothing is built before the maintainer
has seen it.

## Analysis: leaving the sentinels behind (track A, 2026-11-14, plan only)

For the maintainer's decision; nothing of it is built.

Track A (ultra/datetime), plan only, 2026-11-14. Every claim is marked by where it comes from:

- **[code]** the source at `wt/datetime` (3b-1 + round 1 + delta 2).
- **[probe]** a throwaway spec run on JVM, JS (ChromeHeadless) and native (linuxX64), and slumber on JVM. The probes are deleted.
- **[memory]** general knowledge, not checked in this session.

### The answer

**Today, a range of dates BC does not work.** The single dates do work. The 3b-1 sentinels (year 1 and year 9999) break every range that starts before year 1, and so did master's sentinels for ranges before -10000. On this branch, the range 501 BC, 15 to 20 March gives:

- `hasStart = false`, as if it had no start;
- `numberOfDays = -182,909`;
- `asListOfDates() = []`;
- `toZonedTimeRange(Berlin).fromMorningToEvening` starts at 0001-01-01.

The cause is the sentinel design itself: a real date before Genesis is read as "open", and arithmetic clamps to the sentinel. [probe, all three platforms]

**The types themselves go far.** `MpLocalDate` holds years ±999,999,999 and `MpInstant` ±1,000,000,000 years, the same on JVM, JS and native. The practical limits are lower, and they come from storage and JS, not from the types:

- **`ts` in milliseconds as a Long:** ±292 million years.
- **Safe integers for a TypeScript client:** ±285,426 years around 1970.
- **JS `Date`, which the JS zone engine uses:** ±8.64e15 ms, about ±273,790 years (-271821-04-20 to +275760-09-13). Beyond that, a zone conversion throws on JS only.

[probe; arithmetic]

**My recommendation: B, explicit open ends.**

- `from` and `to` of the four range types become nullable, and null means open.
- `Genesis` and `Doomsday` leave the range types and the stored data.
- The `Partial` types stay as they are: they mean "form not filled in", which is a different thing from "open".

B is the only option where:

- BC ranges work with no edge at all;
- the zone oddities disappear by construction;
- shifting a range cannot close an open end;
- the compiler shows every caller that touches an end.

Its cost is moderate. In this repo only `ultra/datetime` uses the range types (searched; plus two slumber test files). The real price is in queries: "contains t" needs `to == null OR to.ts > t`.

If the maintainer prefers to keep the sentinels, the fallback is **C with A2's values**:

- sentinels at -100000-01-01 / +100000-01-01 (kotlinx's `DISTANT_PAST` / `DISTANT_FUTURE`, aligned to the millisecond);
- every range operation handles the open end explicitly.

The fallback is cheaper and keeps queries a single numeric comparison. It still has an edge at ±100,000 years, and nothing stops a caller from computing with a sentinel as if it were a date.

**Batch 3b-1: split it.**

- Merge `ComputedList`, the lazy views, `numDaysInMonth` from the calendar and the bounded `toString`. None of them depends on the sentinels.
- Park ULTRA-00343 (the year 1 / year 9999 values): every option replaces it.

### The options side by side

| Question | A1 master ±10000 | A2 ±100000 (DISTANT_*) | C hybrid (A2 values, explicit open in all arithmetic) | B nullable ends |
|---|---|---|---|---|
| 1. BC ranges | work after -10000-01-01; break before (as today at year 1) | work after -100000; break before | work after -100000; an edge remains beyond ±100000 | work fully; only platform limits (JS zones ±273,790 y) |
| 2. Stored open end | `{"ts":253402300800000,…}` | `{"ts":3093527980800000,…}` | as A2 | `"to":null` |
| 2. "contains t" query | `from.ts <= t AND to.ts > t` | same | same | needs `OR to == null` (and `from == null`) |
| 2. AQL date functions | Doomsday 1 ms past their range | sentinels outside their range | as A2 | null instead of a number |
| 3. API break | values only, no compile signal | values only, no compile signal | values + behaviour of ~10 operations, no compile signal | types change: compile signal at every use |
| 4. Open range length | clamp to ±10000 | clamp, never bites below ±100k; `forever` = 73,048,500 days | exact clamp formula, as A2 | `null` (proposal) |
| 5. Zone oddities | stay | stay (at ±100k) | fixed by explicit handling | gone (no value to shift) |
| 6. JS | fine | fine (inside safe ints and `Date`) | fine | fine |
| 7. Data with master's values | reads as open | reads as closed unless mapped | as A2 | map exact old values to null on read |
| 8. Work | 1 small commit | 1 small commit (+ legacy map) | ~1 batch | ~2 batches |
| Survives from 3b-1 | ComputedList, lazy views, numDaysInMonth, toString | same | same | same |

### Details per question

#### 1. Do ranges BC work?

Today [probe, identical on JVM, JS and native], `MpLocalDateRange(-500-03-15, -500-03-20)` gives:

- `hasStart` false, `numberOfDays` -182,909, list empty;
- `contains` and `intersects` correct;
- `fromMorningToEvening` in Berlin starts at 0001-01-01T00:00Z;
- `MpInstantRange` around 501 BC: `hasStart` false.

The single values do work for 501 BC (`MpLocalDate.of(-500, 3, 15)`):

- `plusDays(30)` = -500-04-14 and `plusMonths(11)` = -499-02-15;
- `dayOfWeek` = Thursday, which matches an independent count;
- `numDaysInMonth` = 31, and year 0 has a 29 February;
- kotlinx round trips work for the date, the date-time and the zoned value; slumber round trips work too (JVM);
- `atZone` works in Berlin (+00:53:28 LMT), New York (-04:56:02 LMT) and Tokyo (+09:18:59 LMT), with the same results on all three platforms.

The calendar is proleptic Gregorian with astronomical year numbering (year 0 = 1 BC, -500 = 501 BC) [code: kotlinx `LocalDate`].

How each option fares:

- **A1:** BC ranges work after -10000-01-01. Before that they break exactly as they do today before year 1.
- **A2 / C:** the same edge, at -100000.
- **B:** no edge. A range of any two valid dates is a plain range.

#### 2. Stored shape and databases

Today's stored shape [probe]:

- `MpLocalDateRange.beginningAt(2024-01-01)` → `{"from":{"ts":1704067200000,"timezone":"UTC","human":"2024-01-01T00:00:00.000Z"},"to":{"ts":253402214400000,"timezone":"UTC","human":"9999-12-31T00:00:00.000Z"}}` (kotlinx).
- Slumber writes the same maps.
- A null end is written explicitly: kotlinx `"to":null`, slumber `to=null` (probed on `Partial`).
- Karango and Monko see the `@Slumber.As(MpDateTimeRawData)` shape, so the queryable paths are `range.from.ts` and `range.to.ts` [code: `MpInstant.kt:14`, `MpDateTimeRawData.kt`].

Queries under A and C:

- "contains t": `r.from.ts <= t AND r.to.ts > t`.
- "open-ended": `r.to.ts >= DOOMSDAY_TIMESTAMP`.
- Both are plain numeric conditions, so a persistent or compound index on `to.ts` applies [memory].

Queries under B:

- "open-ended": Karango `IS_NULL(r.to)` [code: `karango/core/.../functions_typecheck.kt:79`]; Monko `eq(null)` on a nullable path [code: `monko/.../dsl/filters.kt:14`].
- "contains t" needs the null case explicitly: `(r.from == null OR r.from.ts <= t) AND (r.to == null OR r.to.ts > t)`.
- In AQL, null sorts below every number, so `r.from.ts <= t` already holds for a null `from`, but `r.to.ts > t` is false for a null `to` [memory].
- In MongoDB, `$gt` / `$lte` never match null or missing values, so both ends need `$or` [memory].
- Indexes keep nulls unless the index is sparse or partial; `$or` / `OR` can reduce index use [memory].

Two things hold whatever we choose:

- AQL date functions accept only 0000-01-01 to 9999-12-31 (webq, request 20261114T160814-caeb7e52e911, quoted in ULTRA-00343). Every BC date lies outside them. A history project compares `ts` numerically, which works for any date down to ±292 million years (Long ms).
- `human` is never read back [code: `MpDateTimeRawData.kt` KDoc], so its format does not matter to the database.

#### 3. API break

In this repo, no main code outside `ultra/datetime` uses a range type or a sentinel [code search]. The only uses outside the module are two slumber test files. Specifically:

- kraft has no range form field;
- karango, monko and funktor have no range use in main code;
- the TypeScript runtime has no range code; generated types follow the Kotlin types.

Unknown: the maintainer's own applications.

What changes under each option:

- **A1 / A2:** the values of `Genesis` / `Doomsday` on four types, `GENESIS_TIMESTAMP` / `DOOMSDAY_TIMESTAMP` (public), and the six JVM helpers. There is no compile signal.
- **C:** as A2, plus a changed behaviour in these operations: `numberOfDays`, `numberOfNights`, `asListOfDates`, `toZonedTimeRange` / `DateTimeRangeConverter`, `atZone`, `asDateRange`, `toInstantRange`, `plus` / `minus` on ranges, `isAdjacentTo`, `mergeWith`, `cutAway`. There is no compile signal.
- **B:** a compile signal at every use. The changes:
  - `from` / `to` of `MpInstantRange`, `MpLocalDateRange`, `MpClosedLocalDateRange` and `MpZonedDateTimeRange` become nullable;
  - `forever` becomes `(null, null)`, `beginningAt` becomes `(x, null)`, `endingAt` becomes `(null, x)`;
  - `duration` becomes `Duration?`, or `Duration.INFINITE` for an open range;
  - `numberOfDays` / `numberOfNights` become `Int?`, and `asListOfDates()` becomes `List<MpLocalDate>?`;
  - `Genesis` / `Doomsday` on the four value types and the constants are removed, preferably deprecated for one release.

Size of the change inside the module [code, grep counts]:

- about 65 sentinel references in 11 main files;
- about 175 in 13 spec files;
- range code 1,078 lines (four range files and the converter);
- range specs 2,834 lines.

#### 4. `numberOfDays`, `numberOfNights`, `asListOfDates`

The maintainer asked to keep the clamp for open-ended ranges. Today's formula is a noon-to-noon UTC span, clamped and then rounded [code: `MpLocalDateRange.kt:96-104`]. On random ordinary ranges it is exact; at the sentinels it is wrong (see the earlier probe table).

- **A1 / A2 / C:** use the calendar-exact rule with an explicit clamp: `epochDays(min(to, Doomsday)) - epochDays(max(from, Genesis))`, 0 when invalid, closed ranges +1. That honours "keep the clamp" exactly, without the half-day rounding. Under A2/C the clamp never bites a date inside ±100,000 years.
  - The lists: `forever` has 73,048,500 days, which fits in an `Int`, and the lazy view is fine.
- **B:**
  - Both ends set: the exact difference.
  - Any end open: `null`.
  - The alternatives are worse: a runtime error (against the maintainer's rule), `Int.MAX_VALUE` (a lie), or counting to an arbitrary horizon.
  - `asListOfDates()` is `null` when open. A range with an open start has no first element. For a range with an open end, an optional `datesFrom(): Sequence` could be added if wanted.

`numDaysInMonth` no longer depends on any of this (3b-1 round 1).

#### 5. Zones

Oddities met this session [probe, identical on all three platforms, current values]:

- `Genesis` shown in New York is 0000-12-31. `MpInstantRange.forever` as dates in New York runs from 0000-12-31 to 9999-12-30, and in Tokyo from 0001-01-01 to 9999-12-31.
- `hasEnd` flips in a zone conversion:
  - `MpLocalDateRange.beginningAt(2024-01-01).toZonedTimeRange(Tokyo).fromMorningToEvening.hasEnd` is **true** (it ends at 9999-12-31T00:00 Tokyo, before Doomsday). In New York it is false (clamped).
  - `endingAt(…)` in New York has `hasStart` **true**.
- `isAdjacentTo`:
  - `forever.isAdjacentTo(beginningAt(Doomsday))` is true, for both instants and dates, although `beginningAt(Doomsday)` is an invalid, empty range.
  - `forever.mergeWith(beginningAt(x))` gives `forever` (correct).
- `plus` / `minus(duration)` on a range shift the sentinel, so `forever.plus(1.days)` has a start [code: `MpInstantRange.kt:231-236`, `from.plus(duration)`].

How each option fares:

- **A1 / A2:** all of these remain, at the sentinel's year.
- **C:** fixes them if every conversion maps an open end to an open end.
- **B:** they cannot occur: null maps to null.

#### 6. JS

- **Numbers:**
  - In Kotlin/JS, `Long` is exact.
  - A TypeScript client parsing JSON is exact within ±2^53 ms, i.e. ±285,426 years around 1970 [arithmetic].
  - A2's sentinels (about ±3.2e15) are inside that; JSON of -9.1e15 ms is written exactly [probe].
- **Zone conversion:**
  - A region zone on JS throws beyond ±8.64e15 ms ("Instant … is outside the range of JS dates") [code: `TimezoneEngineJs.kt:14,83`; probe: -300000-01-01 in Berlin throws on JS only]. UTC and fixed offsets work.
  - The JS engine already handles BC eras via `Intl` (`era: 'short'`) [code: `TimezoneEngineJs.kt:117-130`]; parity is tested at year -99 [code: `ZoneParityTable.kt:66`].
- **Year text:** see question 9.

#### 7. Reading old data

No production data exists (maintainer). The rule anyway:

| Option | Master's values (-10000 / +10000) | 3b-1's values (year 1 / 9999) |
|---|---|---|
| A1 | read as open | read as closed |
| A2 / C | read as closed | read as closed |

That is, unless the decoders map the known old values.

For every option the proposal is the same: on read, map the exact known values to the current open representation in both codecs, for one release, with a test per value:

- master's -377736739200000 and 253402300800000;
- 3b-1's -62135596800000 and 253402214400000.

The map uses exact values only, never "≤ some year", so a real BC date is never swallowed. Under B this needs a custom deserializer for the four range types (today they are plain `@Serializable` data classes) and a slumber awaker.

#### 8. Size of the work, and what of 3b-1 survives

**Survives in every option:**

- `ComputedList` (with the bounded `toString`);
- the lazy `asListOfDates`;
- `numDaysInMonth` from the calendar;
- the neighbour and old-sentinel test patterns. They are reusable as the open-end test matrix.

**Thrown away:** the ULTRA-00343 constant values and the rows that pin them: about 25 test lines, and the `GenesisDoomsdaySpec` literals.

Effort per option:

- **A1:** constants back to master, about 30 test lines.
- **A2:** two constants and about 60 test lines, plus the legacy map (~80 lines with tests).
- **C:** A2, plus explicit open-end handling in about 12 operations: about 200 main lines and 400 test lines; one batch.
- **B:** four range types and the converter rewritten at their ends: about 400 main lines, about 500 changed and 500 new test lines, plus the legacy map and the range codecs. Two batches:
  1. types, predicates and codecs;
  2. conversions, lengths and docs.
- **BC text fixes** (question 9), in every option: about 80 main and 150 test lines.

#### 9. What BC support needs beyond the sentinels

Every item below was probed on all three platforms; the slumber round trips on JVM only.

1. **`toIsoString` pads a negative year as a number** (`year.pad(4)`) [code: `MpZonedDateTime.kt:295-298`].
   - Year -500 prints `-500-…` and year -2 prints `00-2-12-30…`. ISO 8601 expanded form is `-0500` / `-0002`.
   - The text goes into the stored `human` field of all four value types.
2. **Parsing.**
   - `MpLocalDate.parse(bc.toIsoString())` throws, and `MpInstant.parse("-500-03-15T00:00:00.000Z")` throws.
   - `parse("-0500-03-15")` works, so fixing (1) fixes the round trip through text.
3. **`format("yyyy")` gives `0000` for every BC year** (`year.coerceAtLeast(0)`) [code: `MpDateTimeFormatter.kt:70`].
   - kraft's `dateToYmd` uses it [code: `kraft/core/.../mapping.kt:20`], so a BC date in a kraft date field shows year 0000 and saves back as year 0.
   - The formatter has no era (`501 BC`); a history UI needs one.
4. **HTML date inputs do not accept year 0 or below** [memory], so kraft needs a non-native picker or a text field for BC.
5. **Calendar:** proleptic Gregorian only. Historians usually write dates before 1582 in the Julian calendar. Display conversion is outside the library today; say whether it is needed.
6. **Tests:** negative years are untested apart from one zone-parity row at -99 and this session's pins. Needed: the round-trip tests above, for `MpLocalDate`, `MpLocalDateTime`, `MpInstant` and `MpZonedDateTime`, on all three platforms.
7. **Fine today:** day arithmetic, weekday, month length, kotlinx and slumber round trips (by `ts`), and zone conversion before 1970 and before year 1 on all three platforms (LMT offsets agree).

### Decisions for the maintainer

1. **Sentinels or explicit open ends?** Recommendation: **B, nullable ends**.
   - It is the only option where BC ranges work with no edge, where the zone oddities and the "shift closes an open end" bug cannot occur, and where the compiler finds every caller.
   - Within this repo only `ultra/datetime` is affected.
   - The cost is in queries: an open end needs `== null` in the condition.
   - If index-friendly single comparisons matter more than these points, take C with ±100,000.
2. **What is the length of an open range?** Recommendation: `numberOfDays` / `numberOfNights` / `asListOfDates()` return `null` when an end is open, and `duration` returns `Duration.INFINITE`.
   - No runtime error, no invented number, and a compile signal.
   - Under C, the calendar-exact count with an explicit clamp.
3. **BC text and display.** Recommendation:
   - fix the ISO text (`-0500`), the parser round trip and `format("yyyy")` now, in every option;
   - decide on era display (`501 BC`) and on a Julian calendar view when the history project starts.

**Batch 3b-1:** split it.

- Merge the lazy views part now: `ComputedList`, `asListOfDates`, `numDaysInMonth` and the round-1 tests that do not pin sentinel values.
- Hold ULTRA-00343. Under B it is replaced by the open-ends ticket; under A or C it gets new values.
- Mechanics: keep master's constants on the merged branch. I can prepare a commit on `wt/datetime` that restores master's values and their test rows, so the branch merges cleanly without them. Not done yet; it waits for your word.

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
  same fix repairs months from 10000-01 on and before -10000-01, which were already wrong on master. Every other
  member of `MpLocalDate` and `MpLocalDateTime` was checked: none other reaches a range, the converter or a sentinel
  (`toRange`, `toClosedRange`, `toEnclosingMonthRange` build ranges, whose members are range behaviour).
- **Round 1, `numberOfDays` itself:** NOT changed; the consequences of the kept clamp for ranges before Genesis and
  around Doomsday are with the maintainer (probe results and a calendar-exact proposal in the track A report). The
  KDoc now also says that a range wholly before Genesis or after Doomsday gets a wrong count: negative, or 1 when it
  ends at Genesis (delta round 2: `MpLocalDateRange(of(0, 12, 30), of(1, 1, 1))` counts 1 and lists `[0000-12-30]`;
  probed).

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
  open-end predicates at their neighbours (both sides, all four range types; zoned added in delta round 2), the end
  boundary at 9999-12-31T00:00Z (`9999-12-31T23:59:59.999Z` reads as "no end"; `forever` no longer contains the last
  day), `beginningAt(d).asOpenRange.to` = 10000-01-01 with its stored `ts`, the mixed old / new `cutAway` sliver;
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
| K1 `MpZonedDateTimeRange.hasEnd` one day early (survived before delta round 2) | 1 (neighbours) |
| K2 `MpZonedDateTimeRange.hasStart` one day late (survived before delta round 2) | 1 (neighbours) |

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
    (another system's maximum), a half-open date range with `to` = 9999-12-31 and a closed date range ending
    9999-12-30 (through `asOpenRange`) read as open-ended; a start at or
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
  date range clamped to Genesis / Doomsday and returned wrong counts (and lost dates) for months before -10000-01 and
  from 10000-01 on (ULTRA-00343).

## Review record: round 1 (blind, reviewer-fable-xhigh, two passes)

**Verdict:** NOT CLEAN on both passes, the same MAJOR from each. Reviewers recomputed the constants, every derived
value and the stored forms: correct. They ran nothing; every item below was checked by a test or probe first.

| Sev | Finding | Outcome |
|---|---|---|
| MAJOR | `numDaysInMonth` goes through `numberOfDays` and the clamp: months before year 1 wrong (`(0, 12)` → 1, `(-500, 3)` → -182,897), `getDatesInMonth` loses dates | CONFIRMED by probe (exact values as reported); FIXED: `atLastOfMonth().day`; RED first; mutant H1 red; all `MpLocalDate` / `MpLocalDateTime` members checked, no other reaches a range |
| decision | consequences of the kept `numberOfDays` clamp (negative counts before Genesis, clamped count with an unclamped start, half-day rounding drops 9999-12-31 for every second `beginningAt`, `(9999-10-01, 9999-12-31)` → 91) | CONFIRMED by probe, every number as reported; NOT changed: with the maintainer; calendar-exact proposal in the track A report |
| MINOR | old-sentinel tests miss `MpClosedLocalDateRange` | FIXED: kotlinx and slumber; mutant H4 red (2 + 1) |
| MINOR | open-end predicates pinned from one side; `to < Doomsday.minusDays(1)` survives | FIXED: neighbours on both sides for `MpLocalDateRange`, `MpInstantRange`, `MpClosedLocalDateRange`; mutants H5 to H10 red. Zoned was missing: added in delta round 2 |
| MINOR | changelog imprecise (boundary, `forever` loses the last day, older readers, `asOpenRange.to` = 10000-01-01, mixed slivers); acceptance sentence wrong | FIXED: changelog and acceptance; each pinned by a test |
| note | Genesis on 0000-12-31 in a western zone is outside what .NET `DateTime`, Python `datetime` and an HTML date input hold | RECORDED for the maintainer, no change |

**Found while probing, pre-existing, not fixed (outside this round):** `MpZonedDateTime.toIsoString` pads a negative
year as a number (`year.pad(4)`): year -2 prints `00-2-12-30`, year -500 prints `-500-03-15` (ISO: `-0002`, `-0500`).
It also writes the stored `human` field of `MpLocalDate` and the other datetime types; the decoders read `ts`, so
values round-trip, but the stored text is malformed for years -1 to -999.

## Review record: delta round 2 (reviewer-fable-xhigh, all charters)

**Verdict:** CLEAN: 0 CRITICAL, 0 MAJOR, 2 MINOR. The `numDaysInMonth` fix holds; every pinned literal and weekday row
recomputed and right. Applied in one commit, tests and texts only (the KDoc sentence is the one production-file line).

| Sev | Finding | Outcome |
|---|---|---|
| MINOR | neighbour pins miss `MpZonedDateTimeRange`: day-early `hasEnd` / day-late `hasStart` mutants survive | FIXED: four rows in Europe/Berlin (Doomsday - 1 ms has an end, Doomsday none; Genesis + 1 ms has a start, Genesis none); mutants K1 / K2 red |
| MINOR | KDoc / ticket: "wholly before Genesis ... negative count" false at the Genesis edge | FIXED: "a wrong count: negative, or 1 when it ends at Genesis"; probed: every range with `to` = Genesis counts 1 |
| text | changelog: months before -10000-01 were wrong on master too; a closed range ending 9999-12-30 reads as open-ended | FIXED |
| deferred | `numDaysInMonth` is `by lazy` | comes with the `numberOfDays` decision (production code) |
| note | `toIsoString` pads a negative year wrongly; the text goes into the stored `human` field | RECORDED for the maintainer, no action |
| counts | after delta round 2 (2026-11-14, `--rerun`, XML) | datetime JVM 2814 / 0, JS 2156 / 0, native 2148 / 0 (rows added inside an existing test, so the totals stay) |

## Master's values restored for the merge (2026-11-14)

The maintainer: "ok please do this so the merge works". For the merge of ULTRA-00341 (3b-1, 3b-2), `wt/datetime`
carries master's values again: Genesis -10000-01-01T00:00:00Z (`-377_736_739_200_000`), Doomsday
+10000-01-01T00:00:00Z (`253_402_300_800_000`). The values built above (year 1 / 9999) are NOT merged. Which values,
or whether the sentinels go (the analysis of 2026-11-14: explicit open ends), is open and discussed after the merge.

**What 3b-1 did besides the values, and what it does now with master's values (each pinned in `GenesisDoomsdaySpec`
or `MpLocalDateRangeLazySpec`, green on JVM, JS and native):**

- Serialization: 3b-1 changed no decoder; nothing maps old values to new ones. Now an open range is stored with ts
  -377736739200000 / 253402300800000 and reads back open (kotlinx; slumber unchanged from master).
- Text: `Genesis.toIsoString()` is `-10000-01-01T00:00:00.000Z`, `Doomsday` `+10000-01-01T00:00:00.000Z`; both parse
  back to the same instant.
- `forever.asListOfDates()`: a lazy view (`ComputedList`) of 7,304,850 dates, -10000-01-01 to 9999-12-31; nothing is
  materialised (a mutant back to the eager list fails). The closed `forever`: 7,304,851 dates through 10000-01-01.
- `numberOfDays` and its clamp: `forever` 7,304,850; a range past Doomsday counts up to it with the half-day rounding:
  2024-01-01 to 12000-01-01 is 2,913,175 (counts Doomsday itself), from 2024-01-02 it is 2,913,173.
- `numDaysInMonth` from the calendar (round 1): independent of the values, kept.

**Master's behaviour that the reviews had called a defect, back with these values (NOT fixed here; for the decision):**

- Doomsday is 1 ms past the range of ArangoDB's date functions; Genesis lies outside it too; both texts have a
  five-digit year.
- `MpClosedLocalDateRange.beginningAt(d).asOpenRange.to` is 10000-01-02 (ts 253402387200000).
- Genesis in a zone west of UTC falls on -10001-12-31.
- The `numberOfDays` clamp counts Doomsday itself for every second start date (above), and
  `MpClosedLocalDateRange.forever` ends on 10000-01-01 in its list.
- A range wholly before -10000-01-01 or after 10000-01-01 gets a wrong count (negative, or 1 when it ends at Genesis)
  and an empty list; BC ranges between -10000 and year 0 work.
- Independent of the values, still open: `toIsoString` pads years -1 to -999 wrongly (`00-2`), `format("yyyy")` prints
  `0000` for every BC year (see the analysis of 2026-11-14).


## Plan for batches B and C (2026-11-26, planner)

Where this plan and the section "Decided (maintainer, 2026-11-26, second set)" differ, the decisions win
(S1-S3, D2-D7, G, O1, serializer).

Master at `e2196fba`. Nothing was built, compiled or run for this plan. Markers:

- **[code]** read on master; file:line refer to master.
- **[model]** a Python model of master's `numberOfDays` (noon-to-noon UTC, clamp, round half to even, +1) on
  Hinnant's days-from-civil, sanity-checked against Python `datetime` and against three literals master pins
  (`GenesisDoomsdaySpec.kt:141-142`: 2,913,175 / 2,913,173; `MpLocalDateRangeLazySpec.kt:26`: 7,304,851). All three
  match. The implementer confirms every [model] number on master by a RED run before relying on it.
- **[memory]** database semantics from general knowledge, not checked in this session.

Settled and not reopened here: the list in the brief (Partial with null ends, factories return Partial, the query
set, plain two-ended classes, `hasStart`/`hasEnd`/`isOpen` on Partial only, lengths on two-ended only, Genesis and
Doomsday gone without decoder translation, `numberOfDays` calendar-exact).

**Update 2026-11-26 (master `b044d015`):** the maintainer answered the ten questions. The answers are in ULTRA-00343,
section "Decided (maintainer, 2026-11-26): the batch B and C plan questions". Questions 1-8 and 10 went as recommended.
Question 9 was overturned: `MpLocalTimeSlot.Partial` gets the null defaults too. The guiding rule: "the behaviour must
be equivalent across all types for the same operations / predicates". The sections below are updated. Section 8
covers the slot Partial, section 9 the operations x types matrix and its divergences, and section 7 the questions
still open.

Build in the worktree `wt/datetime` through `console/wt-gradle.sh` (CLAUDE.md), B first, then C, each its own gate.

---

### 1. API changes

### Batch B: queries on `Partial` (additive; no sentinel touched; buildable alone)

Same pattern on the four Partials: `MpInstantRange.Partial` (`MpInstantRange.kt:39-57`), `MpZonedDateTimeRange.Partial`
(`MpZonedDateTimeRange.kt:48-66`), `MpLocalDateRange.Partial` (`MpLocalDateRange.kt:37-50`),
`MpClosedLocalDateRange.Partial` (`MpClosedLocalDateRange.kt:37-50`). `P` = the Partial, `R` = its two-ended range,
`X` = the point type (`MpAbsoluteDateTime` for instant and zoned, `MpLocalDate` for the date types).

| Member | Before | After |
|---|---|---|
| constructor | `Partial(val from: X?, val to: X?)` | `Partial(val from: X? = null, val to: X? = null)` (confirmed "defaults only") |
| `hasStart` | - | `val hasStart: Boolean get() = from != null` |
| `hasEnd` | - | `val hasEnd: Boolean get() = to != null` |
| `isOpen` / `isNotOpen` | - | `from == null \|\| to == null` / its negation (isNotOpen mirrors the two-ended API) |
| `contains(point: X)` | - | null end = unbounded; ends in wrong order (or empty, half-open `from == to`) contain nothing |
| `contains(other: R)` | - | `other.isValid` and each set end of this encloses other's end |
| `contains(other: P)` | - | as above; a null end of `other` is only enclosed by a null end of this |
| `intersects(other: R)`, `intersects(other: P)` | - | overlap with null read as unbounded; wrong order or empty on either side: false |
| `asValidRange()` | both ends set and in order | unchanged |
| KDoc | none | "null is unbounded for the queries; a form that needs both ends uses `asValidRange()`" |

Rules per kind (both ends compared with `<` on the half-open side, as the two-ended code does):

- Half-open (instant, zoned, `MpLocalDateRange`): `inOrder = from == null || to == null || from < to`;
  `contains(p) = inOrder && (from == null || p >= from) && (to == null || p < to)`;
  `intersects(o) = inOrder && o in order && (from == null || o.to == null || from < o.to) && (o.from == null || to == null || o.from < to)`.
- Closed (`MpClosedLocalDateRange`): `inOrder = ... from <= to`; `<=` on both ends.
  - Decided (3): the two-ended closed `intersects` is fixed in B to agree.
  - Write it directly as `isValid && other.isValid && other.from <= to && from <= other.to`, not through
    `asOpenRange`, which throws on 999999999-12-31 (D7 in section 9).
- Zoned: every comparison by instant (`toInstant()`), as the two-ended zoned range does since ULTRA-00211
  (`MpZonedDateTimeRange.kt:74-78`). Zoned `contains` also gets the two `(MpLocalDateTime, zone)` overloads of the
  two-ended range (`MpZonedDateTimeRange.kt:137-146`).
- Two-ended equivalence used for the tests: for valid ranges master's `intersects` (`MpInstantRange.kt:148-155`) is
  `other.from < to && from < other.to` [code, by case analysis].

Null-preserving conversions (decided 1, in B):

| New on | Signature | Mirrors |
|---|---|---|
| `MpInstantRange.Partial` | `fun atZone(timezone: MpTimezone): MpZonedDateTimeRange.Partial` (+ `TimeZone` overload) | `MpInstantRange.kt:101-113` |
| `MpZonedDateTimeRange.Partial` | `fun atZone(timezone: MpTimezone)` (+ `TimeZone`), `fun toInstantRange(): MpInstantRange.Partial` | `MpZonedDateTimeRange.kt:110-119, 204-209` |
| `MpLocalDateRange.Partial` | `val asClosedRange: MpClosedLocalDateRange.Partial` (`to?.minusDays(1)`) | `MpLocalDateRange.kt:52-54` |
| `MpClosedLocalDateRange.Partial` | `val asOpenRange: MpLocalDateRange.Partial` (`to?.plusDays(1)`) | `MpClosedLocalDateRange.kt:52-54` |

Already there and already null-preserving: `MpInstantRange.Partial.asDateRange(tz)` (`MpInstantRange.kt:53-56`),
`MpZonedDateTimeRange.Partial.asDateRange()` (`MpZonedDateTimeRange.kt:62-65`). Not on Partial (decided 1 and 2):
`plus` / `minus`, `mergeWith`, `cutAway`, `touches`, `isAdjacentTo`, `toZonedTimeRange` / the converter, and
cross-type queries between the two date Partials. Shortcuts as extension methods may come later, not in B or C.

`MpLocalTimeSlot.Partial` (`MpLocalTimeSlot.kt:91-106`) also gets `= null` defaults (decided 9). What a null means
for a slot, and which members follow, is in section 8 (questions S1-S4).

### Batch C: no sentinels; factories return `Partial` (breaking)

Removed:

| What | Where |
|---|---|
| `GENESIS_TIMESTAMP`, `DOOMSDAY_TIMESTAMP` (file deleted) | `commonMain/kotlin/constants.kt:6, 11` |
| `GenesisInstant`, `DoomsdayInstant`, `Genesis/DoomsdayLocalDateTime`, `Genesis/DoomsdayLocalDate`, `internal val utc`; the JVM facade `ConstantsJvm` (file deleted; `utc` has no other user [code]) | `jvmMain/kotlin/constants.kt:1-19` |
| `MpInstant.Genesis`, `.Doomsday` | `MpInstant.kt:62-74` |
| `MpLocalDate.Genesis`, `.Doomsday` | `MpLocalDate.kt:27-39` |
| `MpLocalDateTime.Genesis`, `.Doomsday` | `MpLocalDateTime.kt:125-131` |
| `MpZonedDateTime.Genesis`, `.Doomsday` | `MpZonedDateTime.kt:154-162` |
| `hasStart`, `hasEnd`, `isOpen`, `isNotOpen` on two-ended ranges | `MpInstantRange.kt:68-86`; `MpLocalDateRange.kt:110-116`; `MpClosedLocalDateRange.kt:63-69`; `MpZonedDateTimeRange.kt:80-86` |

Changed (per range type `R`, its `R.Partial` = `P`):

| Member | Before | After | Where |
|---|---|---|---|
| `forever` | `val forever: R = R(Genesis, Doomsday)` | `val forever: P = P()` (equal to `P.empty`) | `MpInstantRange.kt:15-18`, `MpLocalDateRange.kt:16-19`, `MpClosedLocalDateRange.kt:12-16`, `MpZonedDateTimeRange.kt:16-19` |
| `beginningAt(from: X)` | `: R` = `R(from, Doomsday)` | `: P` = `P(from = from)` | `MpInstantRange.kt:28-31`, `MpLocalDateRange.kt:31-34`, `MpClosedLocalDateRange.kt:30-34`, `MpZonedDateTimeRange.kt:37-40` |
| `endingAt(to: X)` | `: R` = `R(Genesis, to)` | `: P` = `P(to = to)` | `MpInstantRange.kt:33-36`, `MpLocalDateRange.kt:26-29`, `MpClosedLocalDateRange.kt:24-28`, `MpZonedDateTimeRange.kt:42-45` |
| `numberOfDays` (half-open) | `by lazy`: noon-to-noon UTC via the converter, clamped, `round(...)`, +1 | `get() = if (isNotValid) 0 else (to.value.toEpochDays() - from.value.toEpochDays()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()` | `MpLocalDateRange.kt:89-104` |
| `numberOfDays` (closed) | `asOpenRange.numberOfDays` | same formula with `+ 1`, validity `from <= to`, computed directly (no `to.plusDays(1)`, which throws at 999999999-12-31) | `MpClosedLocalDateRange.kt:58-59` |
| `numberOfNights` | `max(0, numberOfDays - 1)` | unchanged formula; `get()` | `MpLocalDateRange.kt:106-108`, `MpClosedLocalDateRange.kt:61` |
| `asListOfDates()` | `ComputedList(numberOfDays) { from.plusDays(it) }` | code unchanged; KDoc drops "counted up to Doomsday", gains "at most `Int.MAX_VALUE` dates" | `MpLocalDateRange.kt:126-140`, `MpClosedLocalDateRange.kt:79-80` |
| `numDaysInMonth` | `by lazy { atLastOfMonth().day }` | `get() = atLastOfMonth().day` (deferred MINOR of 3b-1 delta round 2); KDoc without "Genesis / Doomsday clamp" | `MpLocalDate.kt:124-127` |
| `DateTimeRangeConverter.create` | clamps `from`/`to` to Genesis/Doomsday by instant | `MpZonedDateTimeRange(from, to)`, comment gone | `DateTimeRangeConverter.kt:42-49` |
| KDoc of `numberOfDays` | the clamp, half-even rounding, "open with the maintainer" | "the calendar days from `from` to `to` (closed: +1); 0 when invalid; at most `Int.MAX_VALUE`" | `MpLocalDateRange.kt:89-95`, `MpClosedLocalDateRange.kt:58` |

`numberOfDays` was checked against its neighbours:

- **`numberOfNights`:** nothing changes but the input. The existing rows (`MpLocalDateRangeSpec.kt:160-191`,
  `MpClosedLocalDateRangeSpec.kt:230-269`) stay green. On UTC noon-to-noon is an exact multiple of a day, so the old
  and new counts agree wherever no clamp acts [code + model].
- **`asListOfDates()`:** no code change. It is exact now for every range: BC, before -10000, after 10000.
- **`numDaysInMonth` / `getDatesInMonth`:** already from the calendar (`MpLocalDate.kt:124-127`, `:523-551`). Only
  `by lazy` becomes `get()`, and the KDoc changes.
- **`asDatePeriod`, `compareTo(MpDatePeriod)`:** already calendar-iterative (`MpLocalDateRange.kt:56-87, 142-144`),
  unchanged. The year loop is O(years); a range of a million years walks a million steps (pre-existing, not changed).
- **The cap:** at `Int.MAX_VALUE` days (about 5,879,610 years). On master such a span gives `round(x).toInt() + 1` =
  `Int.MIN_VALUE`, so an empty list [code reading, not run]; after C it gives `Int.MAX_VALUE` and a list cut there
  (decided 6).
- **What the length no longer depends on:** `toZonedTimeRange` / the zone engine.

Two-ended range classes otherwise unchanged: same fields, same `isValid`, same set operations, same wire shape.

---

### 2. Compile breaks and behaviour changes in the repo

Search on master (`grep -rln` for the four range types, the converter and `toZonedTimeRange` outside `ultra/datetime`,
excluding `build`): one file. No main code outside `ultra/datetime` uses a range type, a sentinel or `Partial`
[code]: karango, monko, funktor, kraft, funktor-demo, codegen and the TS runtime (`ultra/codegen/src/main/resources/ts/runtime/datetime.ts`,
93 lines, no range code) are clean. No committed generated TS file contains a range type (`git grep` over codegen,
funktor/codegen, funktor-demo).

| File:line | Batch | Break / change | Fix |
|---|---|---|---|
| `ultra/slumber/src/jvmTest/kotlin/builtin/datetime/mp/MpZonedDateTimeRangeCodecSpec.kt:17-26` | C | `beginningAt(..)` is a Partial: `.atZone` resolves only with decided 1; `codec.awake(MpZonedDateTimeRange::class, ..)` of `{from, to=null}` gives null (non-null `to`), so the test goes RED, not a compile error (`shouldBe` is untyped) | awake `MpZonedDateTimeRange.Partial::class`; assert `to == null`, `hasEnd == false`, equality |
| same file `:12` | C | KDoc names Doomsday | "an open end stays null through `atZone` and slumber" |
| `ultra/datetime/src/commonMain/kotlin/MpLocalDate.kt:124` | C | KDoc names the clamp | reword |
| `ultra/datetime/src/commonMain/kotlin/MpLocalTime.kt:129` | - | names `DateTimeRangeConverter.fromHourToHour(_, 24)` only | none |

Datetime's own tests (C unless noted; B adds new specs only):

| Spec:lines | What | Becomes |
|---|---|---|
| `GenesisDoomsdaySpec.kt:1-158` | sentinel values, open predicates, clamp, converter clamp | deleted. The open-range rows move to the new `OpenRangeSpec` (B: queries; C: factories). The length rows move to `NoSentinelsSpec` (C, section 5) |
| `jvmTest/ConstantsSpec.kt:1-31` | JVM helpers | deleted |
| `MpInstantSpec.kt:29-34, 65-73`; `MpLocalDateSpec.kt:31-39`; `MpLocalDateTimeSpec.kt:41-49, 91-147`; `MpZonedDateTimeSpec.kt:119-126` | text and parse of ±10000 through the constants | kept as literal rows (`of(-10000, 1, 1)`, `fromEpochMillis(-377_736_739_200_000)`, ...). They pin five-digit-year text that ULTRA-00347 builds on |
| `MpInstantRangeSpec.kt:21-84` | `forever` / `beginningAt` / `endingAt` with predicates and `duration` | `shouldBe MpInstantRange.Partial(...)`, the predicates on the Partial; `duration` row deleted |
| `MpLocalDateRangeSpec.kt:716-761`; `MpClosedLocalDateRangeSpec.kt:563-607` | the same, plus `hasStart`/`hasEnd`/`isNotOpen` on two-ended | Partial rows; the two-ended predicate rows deleted |
| `MpZonedDateTimeRangeSpec.kt:18-88` | the same, plus `forever.isValid` | Partial rows |
| `MpZonedDateTimeRangeZoneSpec.kt:23-36, 82-91` | ULTRA-00211: an open end survives `atZone` and kotlinx | `Partial.atZone(zone).to == null`; kotlinx round trip of `MpZonedDateTimeRange.Partial` |
| `MpZonedDateTimeRangeZoneSpec.kt:94-108, 119-124` | converter clamp ties | replaced by "no clamp": `DateTimeRangeConverter(+01:00, -12000-01-01, 12000-01-01).fromHourToHour(1, 1)` keeps both ends |
| `MpLocalDateRangeLazySpec.kt:11-28` | `forever` lists | `MpLocalDateRange(of(-10000,1,1), of(10000,1,1))`: 7,304,850 dates, first -10000-01-01, last 9999-12-31, `[1_000_000]` = -7263-11-28 (the same literals); closed with the same ends: 7,304,851, last 10000-01-01 [model] |

Behaviour changes without a compile signal, for consumers: kotlinx JSON of a Partial omits a null end (B); a range
stored with the old sentinels decodes as a two-ended range of 20,000 years (C, no translation, settled); the
converter no longer clamps (C); `numberOfDays` exact (C).

Docs (for ULTRA-00339, the DOCS follow-up, written after C settles):

- `docs-site/src/data/llms/datetime.md:65-66, 209, 214-227`
- `docs-site/src/pages/ultra/datetime/ranges.astro:47-55, 135-143, 186-195`
- `mp-instant.astro:49-50`, `mp-local-date.astro:37-38`, `mp-local-date-time.astro:41-42`, `mp-zoned-date-time.astro:51-52`

---

### 3. The list "to settle before building"

### 3.1 Stored shape of an open range; database queries on a null end

Example: `MpInstantRange.beginningAt(MpInstant.parse("2024-07-01T10:00:00Z"))`.

| | Master (two-ended, sentinel) | After C (Partial) |
|---|---|---|
| kotlinx `Json` (default) | `{"from":{"ts":1719828000000,"timezone":"UTC","human":"2024-07-01T10:00:00.000Z"},"to":{"ts":253402300800000,"timezone":"UTC","human":"+10000-01-01T00:00:00.000Z"}}` | `{"from":{"ts":1719828000000,"timezone":"UTC","human":"2024-07-01T10:00:00.000Z"}}` (a null end equals its default, so it is not written) |
| kotlinx with `encodeDefaults = true` | - | `..., "to":null}` |
| slumber (karango, monko documents) | `{from={ts=..}, to={ts=253402300800000, ..}}` | `{from={ts=1719828000000, timezone=UTC, human=..}, to=null}`: `DataClassSlumberer` writes every ctor field, null included (`ultra/slumber/src/jvmMain/kotlin/builtin/objects/DataClassSlumberer.kt:201-205`) [code] |
| reads | - | missing key and `null` both read as null: kotlinx through the default; slumber nullable-first (`DataClassAwaker.kt:100-105`) [code] |

Query paths: karango and monko generate paths for ctor fields of any reachable data class
(`KarangoKspProcessor.kt:164-184`, blacklist only `java.`, `javax.`, `kotlin.`, `kotlinx.` at `:34-39`). Nullability
is dropped in the path type (`toFullyQualifiedString`, `:251-270`), so `r.period.to` is a path of `MpInstant`, and
`.ts` exists through `@Slumber.As(MpDateTimeRawData)` (`MpInstant.kt:14`; karango `SlumberAsCodeGenSpec.kt:71`)
[code; not compiled with a Partial field].

"Bookings whose period contains t" (`period: MpInstantRange.Partial`, `t = instant.toEpochMillis()`):

```kotlin
// karango (karango/core/src/main/kotlin/aql/functions_typecheck.kt:79, operator_boolean.kt:110-149, 205-215)
FOR(repo) { r ->
    FILTER(
        (IS_NULL(r.period.from) OR (r.period.from.ts LTE t)) AND
                (IS_NULL(r.period.to) OR (r.period.to.ts GT t))
    )
    RETURN(r)
}

// monko (monko/core/src/main/kotlin/lang/dsl/filters.kt:14-35, 94; path_utils.kt toFieldPath)
find { r ->
    filter(
        and(
            or(Filters.eq(r.period.from.toFieldPath(), null), r.period.from.ts lte t),
            or(Filters.eq(r.period.to.toFieldPath(), null), r.period.to.ts gt t),
        )
    )
}
```

- `r.period.to eq null` does not compile in monko: `eq(value: T)` on an invariant `MongoPropertyPath<*, T>` with
  `T = MpInstant` [code reading]. The precedent is `Filters.eq(path.toFieldPath(), null)` in `notDeleted`
  (`monko/core/src/main/kotlin/lang/dsl/softdelete.kt:19`), which matches null and a missing field.
- AQL: `IS_NULL` is true for null and for a missing attribute. `null.ts` is null, and `null > t` is false, so the
  `IS_NULL` branch on `to` is required. On `from`, `null <= t` holds by itself (null sorts first), but the explicit
  branch is clearer [memory].
- MongoDB: `$gt` / `$lte` never match null or missing, so both ends need the `$or` [memory].
- Indexes: a persistent index on `period.to.ts` keeps nulls unless it is sparse. `OR` / `$or` can lower index use
  [memory].
- Wrong-order and empty ranges match nothing in these queries, the same as `Partial.contains` (`from <= t < to` cannot
  hold) [arithmetic].
- Date Partials store `ts` at the start of the day in UTC (`MpLocalDateCodec.kt:44-45`). A closed range uses
  `to.ts GTE d.ts`.

Batch C adds a DB spec per backend that stores `forever`, `beginningAt`, `endingAt`, a two-ended range and a
wrong-order Partial, and then runs exactly these queries (decided 8). No query helper goes into karango or monko.

### 3.2 `Partial` for form input vs `asValidRange()`

Example: a form binds `MpLocalDateRange.Partial`; the user fills in only "from" = 2024-07-01.

- `Partial(from = 2024-07-01).asValidRange()` is null: the form stays invalid, unchanged.
- The same value as a query: `contains(2030-01-01)` is true, `isOpen` true, `hasEnd` false (new in B).
- `Partial.empty == MpLocalDateRange.forever` (C): "nothing entered" and "no bounds" are one value.

B and C: `asValidRange()` stays "both set and in order" (`MpLocalDateRange.kt:46-49` and the three siblings). The
Partial KDoc states both readings. No new member is needed: `asValidRange() != null` is the form check. Settled by
decided 7 (keep both names).

### 3.3 Master's behaviours "back and not fixed" (ULTRA-00343, section "Master's values restored")

| Behaviour on master | Example | After |
|---|---|---|
| Doomsday 1 ms past ArangoDB's date range, Genesis outside it, five-digit text stored | `beginningAt(d)` stores `to.ts = 253402300800000`, `human "+10000-01-01..."` | C removes it: the open end is stored as null. Real dates outside 0000-9999 still lie outside AQL's date functions; compare `ts` numerically |
| `MpClosedLocalDateRange.beginningAt(d).asOpenRange.to` = 10000-01-02 | `ts 253402387200000` (`GenesisDoomsdaySpec.kt:117-123`) | C: `beginningAt` is a Partial; with decided 1 `asOpenRange.to == null` |
| Genesis in a zone west of UTC on -10001-12-31 | `GenesisDoomsdaySpec.kt:145-148` | C: gone (no Genesis) |
| The clamp counts Doomsday for every second start; the closed `forever` lists 10000-01-01 | 2024-01-01 to 12000-01-01: 2,913,175; from 2024-01-02: 2,913,173 (`GenesisDoomsdaySpec.kt:141-142`) | C: 3,643,659 and 3,643,658 [model]; `forever` has no list (Partial) |
| A range wholly outside -10000..10000 gets a wrong count and an empty list | `(-12000-03-15, -12000-03-20)`: -730,405 and `[]` [model] | C: 5 and five dates |
| `toIsoString` pads years -1..-999 wrongly; `format("yyyy")` prints 0000 for BC | `MpLocalDate.of(-2,12,30).toIsoString()` = `"00-2-12-30..."` | left to ULTRA-00347 |

### 3.4 Sentinel-restore review items

1. **A range that starts before Genesis lists wrong dates.**
   - Example: `MpLocalDateRange(of(-12000,1,1), of(2024,1,1))`. On master `numberOfDays` = 4,391,677 and the list
     ends on 0024-01-01. Exact: 5,122,161, ending 2023-12-31 [model].
   - The count-1 case: `(of(-10001,12,30), of(-10000,1,1))` counts 1 and lists `[-10001-12-30]`; exact is 2 [model].
   - C fixes both (calendar-exact, no clamp). RED rows on master first.
2. **An open end lost in a zone conversion.**
   - Example: `MpLocalDateRange.beginningAt(2024-01-01).toZonedTimeRange(Asia/Tokyo).fromMorningToEvening.hasEnd` is
     true on master. The end is 9999-12-31 at 24:00 Tokyo = 9999-12-31T15:00Z, before Doomsday, so the converter
     does not clamp it. In New York it is false. [code reading with master's values; probed with 3b-1's values in
     the analysis]
   - C removes it by construction: `beginningAt` is a Partial, and every Partial conversion maps null to null
     (decided 1). `Partial.toZonedTimeRange` is not offered (decided 1).
3. **Shifting an open range closes it.**
   - Example: `MpInstantRange.forever.plus(1.days).hasStart` is true (`MpInstantRange.kt:231-236` shifts Genesis).
   - C: `forever` is a Partial and Partial has no `plus`, so the call does not compile. A two-ended range has no
     open end to lose.
4. **An empty range counts as adjacent.**
   - Example: `MpInstantRange.forever.isAdjacentTo(MpInstantRange.beginningAt(MpInstant.Doomsday))` is true; the
     other range is `(Doomsday, Doomsday)` (`MpInstantRange.kt:171-173`).
   - After C the sentinel form is gone, but the general case stays on every two-ended type:
     `MpInstantRange(a, b).isAdjacentTo(MpInstantRange(b, b))` is true, and
     `MpClosedLocalDateRange(d1, d5).isAdjacentTo(MpClosedLocalDateRange(d6, d5))` (invalid) is true via
     `asOpenRange` [code reading].
   - Decided 4 (require both valid, as slots do since 0.119.0, `CHANGELOG.md:149-150`).
5. **The two date range types disagree on the open end by a day.**
   - Examples on master: `MpClosedLocalDateRange.forever.contains(of(10000,1,1))` is true, the half-open `forever`
     gives false. `MpLocalDateRange.forever.asClosedRange != MpClosedLocalDateRange.forever` (to 9999-12-31 vs
     10000-01-01). `numberOfDays` is 7,304,850 vs 7,304,851.
   - C: both are `Partial(null, null)`; `asOpenRange` / `asClosedRange` on Partial keep null, so they agree.

Found while checking 4 and the Partial rules, pre-existing and not on the ticket's list: the two-ended **closed
`intersects` misses a shared boundary day**. `MpClosedLocalDateRange(2024-01-01, 2024-01-05).intersects(MpClosedLocalDateRange(2024-01-05, 2024-01-09))`
is false (`MpClosedLocalDateRange.kt:101-108` uses half-open logic). The same days through
`intersects(MpLocalDateRange)` (`:110-112`, via `asOpenRange`) give true [code reading; no test covers it:
`MpClosedLocalDateRangeSpec.kt:369-395`]. The closed Partial's `intersects` must agree with one of them: decided 3, the two-ended is fixed in B.

### 3.5 The three text / test MINORs

| MINOR | State on master | B/C |
|---|---|---|
| The count-1 case lists one date, not none (KDoc `MpLocalDateRange.kt:93-94` says "negative (with an empty list), or 1") | the KDoc is still there | C deletes the sentence. The exact count pins `(-10001-12-30, -10000-01-01)` at 2 days and 2 dates |
| `numDaysInMonth` is pinned in `MpLocalDateSpec` | done: `MpLocalDateSpec.kt:842-869` [code] | C keeps those rows when `GenesisDoomsdaySpec` goes; nothing new |
| `MpLocalTimeSlotOnDateSpec.kt:134` needs `+12000-01-01T09:00:00Z` | moot: batch A deleted that test (ticket, batch A table); `:134` is now a JS row of another test; no `12000` in the file [code] | nothing |

---

### 4. Serialization

| Codec | Two-ended ranges | Partials |
|---|---|---|
| kotlinx (`@Serializable` data classes) | unchanged | B: null ends are no longer written (default `Json`); a missing key now decodes to null (was `MissingFieldException`). Old payloads with `"to":null` read the same. C: the factories produce Partials, so a field that held `beginningAt(..)` changes type to `R.Partial` |
| slumber (karango, monko) | unchanged | unchanged: writes `to=null`, reads missing as null |
| stored old data | a range with ts -377736739200000 / 253402300800000 reads as two-ended, 20,000 years long; into a Partial field: both ends set (settled, no translation) | - |
| TypeScript (codegen) | unchanged | the generator marks a ctor parameter with a default as optional (`ultra/codegen/src/main/kotlin/model/TypeWalker.kt:500-514`). After B: `from?: MpInstant \| null`, schema `MpInstant.nullable().optional()`. Clients regenerate. `datetime.ts` has no range code: no change |

A test must serialize (B, `commonTest` on all three platforms, plus slumber on the JVM):

- each of the four Partials through kotlinx: encode `P()`, `P(from=x)`, `P(to=y)`, `P(x, y)` and pin the exact JSON
  (null ends absent); decode the same from `{}`, from `{"to":null}`, from missing keys and from explicit nulls.
- the same four through slumber: `to=null` written, a missing key read as null.
- C adds one row: a two-ended range written by master's `forever` (`ts` -377736739200000 / 253402300800000) decodes as
  a two-ended range with those instants (pins "no translation").

---

### 5. Test plan

**Specs.**

- B: new `OpenRangeSpec` (commonTest), one block per Partial type. It covers `hasStart`, `hasEnd`, `isOpen`, the
  predicates at both ends and at the boundary (half-open: `to` excluded; closed: `to` included), wrong order and
  empty, null on each side, zoned in two zones (by instant), the conversions, and serialization.
- C: `NoSentinelsSpec` covers the factories (`beginningAt(x) shouldBe P(from = x)`, `forever shouldBe P.empty`) and
  the length rows. It also has rows for BC ranges, ranges beyond ±10000 and the cap row, no clamp in the converter,
  and the slumber spec fix.
- C: the karango e2e `karango/core/src/test/kotlin/e2e/E2E-OpenRangeQuery-Spec.kt`, with a testdomain entity that uses
  the generated paths (`kspTest` is on, `karango/core/build.gradle.kts:48`).
- C: monko `monko/core/src/test/kotlin/io/peekandpoke/monko/MonkoOpenRangeQuerySpec.kt`, modelled on
  `MonkoRepositoryFilterCombineSpec.kt` (string paths; monko/core has no `kspTest`).

**RED first, for the right reason.**

| Batch | Row | Red on master because |
|---|---|---|
| B | kotlinx decode `{"from":{..}}` into each Partial | `MissingFieldException` (no default yet) |
| B | kotlinx encode `P(from = x)` has no `"to"` key | master writes `"to":null` |
| B | predicates | members added first as stubs returning a wrong constant, then run; every predicate row red; then implemented |
| B (d3) | closed `[01-01..01-05].intersects([01-05..01-09])` is true | master gives false |
| C | `beginningAt(x) shouldBe P(from = x)` (untyped `shouldBe` compiles on master) | master returns a two-ended range |
| C | `(-12000-01-01, 2024-01-01).numberOfDays` = 5,122,161; list last 2023-12-31 | master 4,391,677 [model] |
| C | `(2024-01-01, 12000-01-01)` = 3,643,659; `(2024-01-02, 12000-01-01)` = 3,643,658 | master 2,913,175 / 2,913,173 (pinned today) |
| C | `(-12000-03-15, -12000-03-20)` = 5, five dates | master -730,405, `[]` [model] |
| C | `(-10001-12-30, -10000-01-01)` = 2, two dates | master 1 [model] |
| C (d6) | `(of(-3_000_000,1,1), of(3_000_000,1,1)).numberOfDays == Int.MAX_VALUE` | master `Int.MIN_VALUE` [code reading] |
| C (d4) | `MpInstantRange(a, b).isAdjacentTo(MpInstantRange(b, b))` is false; the same on the three other types | master true |
| C | converter `(+01:00, -12000-01-01, 12000-01-01).fromHourToHour(1, 1)` keeps the ends | master clamps to Genesis / Doomsday |

The removals (constants, predicates on two-ended) need no RED: the compile sweep proves every use is gone.

**Mutation targets** (persistence and query building, per CLAUDE.md; each restored with `cmp`, backups keep the
source-set path):

- MB1, persistence: drop `= null` on `to` of one Partial. The kotlinx decode row goes red.
- MB2, persistence: `@EncodeDefault` on `to`. The "no `to` key" row goes red.
- MB3: in `contains(point)`, `to == null ||` becomes `to != null &&`, per type.
- MB4: `<` becomes `<=` on the half-open end, and the reverse on the closed end.
- MB5: drop the wrong-order guard.
- MB6: zoned Partial compares with `MpZonedDateTime.compareTo` instead of by instant (the two-zone row).
- MB7: `Partial.atZone` drops `to`; `asOpenRange` maps null to `from`.
- MC1: closed `numberOfDays` without `+1`.
- MC2: drop `isNotValid -> 0` (negative count).
- MC3: drop the cap (`toInt()` wraps).
- MC4: back to noon-to-noon. After C, without the clamp, it agrees except at the cap, so the cap row is its only
  killer. Say so in the record.
- MC5: `isAdjacentTo` without the validity check (decided 4).
- MQ1, query building: karango without the `IS_NULL(to)` branch; `forever` and `beginningAt` rows go red.
- MQ2, query building: monko without `Filters.eq(to, null)`.
- MQ3, query building: monko without the `from` null branch. `endingAt` / `forever` go red here, while MQ3's karango
  twin is expected to survive (AQL null <= t). Record it as equivalent, with the reason.

**Platforms and counts.**

- datetime `commonTest` on the JVM, JS (ChromeHeadless, the module's fixed Karma port) and native (linuxX64).
- slumber on the JVM.
- karango and monko on the JVM against docker Arango / Mongo.
- codegen (326) re-run: the TS optional flag.
- A repo-wide compile sweep including `compileKotlinLinuxX64`; 0 `e:` lines.
- Counts come from the XML with `--rerun`, not from a filtered gradle call (`.claude/skills/testing/`).

---

### 6. CHANGELOG draft (Unreleased; plain ASCII)

### New

- `ultra/datetime`: `MpInstantRange.Partial`, `MpZonedDateTimeRange.Partial`, `MpLocalDateRange.Partial`,
  `MpClosedLocalDateRange.Partial` and `MpLocalTimeSlot.Partial` answer queries. On a slot a null end is the edge
  of the day: no start is 00:00, no end is 24:00 (if S1-A, section 8).
  - A null end is unbounded: `hasStart`, `hasEnd`, `isOpen`, `isNotOpen`, `contains(point)`, `contains(range)`,
    `contains(partial)`, `intersects(range)`, `intersects(partial)`. Ends in the wrong order contain nothing; no
    exception. The zoned Partial compares by instant.
  - Conversions keep a null end null: `atZone` and `toInstantRange` on the instant and zoned Partials, `asOpenRange`
    and `asClosedRange` on the date Partials.
  - `asValidRange()` is unchanged: both ends set and in order (ULTRA-00343).

### Breaking changes

- `ultra/datetime`: `Genesis` and `Doomsday` are removed from `MpInstant`, `MpLocalDate`, `MpLocalDateTime` and
  `MpZonedDateTime`, together with `GENESIS_TIMESTAMP`, `DOOMSDAY_TIMESTAMP` and the JVM helpers `GenesisInstant`,
  `DoomsdayInstant`, `GenesisLocalDateTime`, `DoomsdayLocalDateTime`, `GenesisLocalDate`, `DoomsdayLocalDate`.
  - An open range is a `Partial` with a null end: `forever`, `beginningAt(x)` and `endingAt(x)` return the
    `Partial` of their range type (`from = null` is no start, `to = null` no end). `forever` equals `Partial.empty`.
  - `hasStart`, `hasEnd`, `isOpen` and `isNotOpen` are removed from the two-ended ranges: a two-ended range always
    has both ends.
  - Code that stored `beginningAt(..)` in a field of a two-ended type changes the field to the `Partial`.
  - Dates BC and beyond year 9999 work in ranges with no edge (ULTRA-00343).
- `ultra/datetime`, behaviour change (no compile signal):
  - The `from` and `to` of the five `Partial`s (the four ranges and `MpLocalTimeSlot`) default to null. kotlinx `Json` (default settings) no longer
    writes a null end: `{"from":{...}}` instead of `{"from":{...},"to":null}`. A missing key reads as null; it was
    an error. Slumber still writes `to: null`.
  - Generated TypeScript types mark both ends optional (`from?: ... | null`); regenerate clients (ULTRA-00343).
- `ultra/datetime`, stored data: nothing translates the old values. A range stored with -10000-01-01 /
  +10000-01-01 (ts -377736739200000 / 253402300800000) reads as a two-ended range of those dates, also into a
  `Partial` field. Querying an open range needs the null case: karango
  `(IS_NULL(r.p.to) OR r.p.to.ts GT t)`, monko `or(Filters.eq(path, null), r.p.to.ts gt t)` (ULTRA-00343).
- `ultra/datetime`, behaviour change (no compile signal): `numberOfDays` of `MpLocalDateRange` and
  `MpClosedLocalDateRange` counts calendar days: `to` minus `from`, plus 1 for the closed range, 0 when invalid, at
  most `Int.MAX_VALUE`. `numberOfNights` and `asListOfDates()` follow. Ordinary ranges count as before.
  - Ranges past the old sentinels count in full: 2024-01-01 to 12000-01-01 is 3,643,659 days (was 2,913,175).
  - A range before -10000-01-01 counts and lists its own dates: -12000-03-15 to -12000-03-20 is 5 days (was
    -730,405 with an empty list).
  - A span of `Int.MAX_VALUE` days or more gives `Int.MAX_VALUE` and a list cut there (was `Int.MIN_VALUE`, an empty
    list) (ULTRA-00343).
- `ultra/datetime`, behaviour change (no compile signal): `DateTimeRangeConverter` (`toZonedTimeRange`) no longer
  clamps to -10000-01-01 / +10000-01-01 (ULTRA-00343).
- `ultra/datetime`, behaviour change (no compile signal): `isAdjacentTo` on the four range types is false when
  either range is empty or invalid. `10:00-11:00` and `11:00-11:00` were adjacent; slots behave this way since 0.119.0
  (ULTRA-00343).

(Entries for D2 to D7 of section 9 follow the maintainer's answers to D2, D3 and D5, and for D6 and D7 the gate.)

### Fixes

- `ultra/datetime`: `MpClosedLocalDateRange.intersects(MpClosedLocalDateRange)` is true for two ranges that
  share only a boundary day (2024-01-01..2024-01-05 and 2024-01-05..2024-01-09). It was false, while
  `intersects(MpLocalDateRange)` with the same days said true (ULTRA-00343).

---

### 7. Decided, and the questions still open

**Decided (2026-11-26, ULTRA-00343), folded in above:**

- 1: null-preserving conversions on Partial, no arithmetic on it (B).
- 2: no cross-type queries between the date Partials.
- 3: closed `intersects` fixed (B, direct formula).
- 4: `isAdjacentTo` requires both valid (C).
- 5: delete outright.
- 6: cap at `Int.MAX_VALUE`.
- 7: keep `empty` and `forever`.
- 8: karango and monko DB pins (C).
- 9: slot Partial gets defaults (section 8).
- 10: plain getters (C).

**Open (each one line plus a recommendation; the details are in sections 8 and 9):**

- S1. What does null mean on `MpLocalTimeSlot.Partial`? Rec A: the edge of the day (no start = 00:00, no end =
  24:00), with the same queries as the range Partials; a slot stays finite.
- S2. `MpLocalTimeSlot.beginningAt(t)`, `endingAt(t)`, `forever` returning the slot Partial, like the four ranges?
  Rec: yes, for the guiding rule. `forever` is the whole day in queries; KDoc names `completeDay` as its two-ended
  twin.
- S3. Confirm two differences that come from the slot's nature: a Partial with `to` before `from` is overnight, not
  "contains nothing"; `asValidRange()` on the slot Partial means "both set and not empty". Rec: confirm.
- D2. Zoned `isAdjacentTo` compares by instant, like every other zoned predicate, instead of by equality (instant and
  zone)? Rec: yes, in C. It reverses the "current behaviour" pinned in `MpZonedDateTimeRangeZoneSpec.kt:9-11`.
- D3. `touches` (and with it `mergeWith`) is false when either range is reversed (`from > to`), while an empty range
  still touches as a point, as an empty slot does? Rec: yes, in C, with decided 4.
- D5. `duration` of an invalid `MpInstantRange` / `MpZonedDateTimeRange` is `Duration.ZERO` instead of negative,
  like `numberOfDays` (0) and slots (never negative)? Rec: yes, in C.
- D7. A closed date range ending on 999999999-12-31 throws in predicates (`asOpenRange` cannot add a day). Make the
  closed predicates direct, and give `mergeWith` / `cutAway` / lengths KDoc only? Rec: yes, predicates direct in C.
- G. Fill the gaps of section 9 (operations absent on some types) now, or in a ticket after C? Rec: a ticket after
  C; they are additions, not divergences of behaviour.

D1 is decided (4). D4 and D6 need no decision: D4 follows D3, and D6 is a plain fix in C (section 9).

---

### 8. `MpLocalTimeSlot.Partial` with null defaults (decided 9): implications

**What the type is** [code]:

- The slot is a start and a length, `MpLocalTimeSlot(from: MpLocalTime, duration: Duration)` with a private
  constructor (`MpLocalTimeSlot.kt:26-29`). Its `to` is derived, the end's clock time (`:126-133`), and the factories
  normalise (`:43-88`).
- Its Partial is the two time pickers of a form: `Partial(from: MpLocalTime?, to: MpLocalTime?)` (`:91-106`). So the
  Partial has `from` / `to` like the ranges, but `to` is a clock time, not a length.
- `asValidRange()` = `of(from, to)` unless an end is missing or the slot is empty (`:101-105`). `to` before `from`
  is overnight there, not an error (`MpLocalTimeSlotClockSpec.kt:99-105`).
- `asPartialRange()` is lossy for a slot of 24 hours or more (`:151-154`).

**S1, what null means.** Ranges read null as unbounded on the time line. A slot lives on the 24-hour clock face, and
since batch A a slot is finite (`MAX_DURATION_MILLIS`, `:35`; `isInfinite` and the open end of `toRange` are gone).

| Option | `from = null` | `to = null` | Examples | Verdict |
|---|---|---|---|---|
| A (rec) | 00:00 | 24:00 (`EndOfDay`) | `Partial()` = whole day (= `completeDay` in queries); `Partial(22:00)` = 22:00-24:00, no wrap past midnight; `Partial(to = 06:00)` = 00:00-06:00; `Partial(to = 00:00)` and `Partial(EndOfDay)` are empty (equal times, the `of(from, to)` rule) | "unbounded within the type's universe": the analogue of the ranges; finite, so batch A holds; queries delegate to `of(from ?: Min, to ?: EndOfDay)` and its clock-face rules |
| B | not filled in, no meaning | same | defaults only; no `contains` / `hasStart` on the slot Partial | the one Partial without queries: against the guiding rule |
| C | around the clock | until `from` again | `Partial(22:00)` = 24 h from 22:00, so every Partial with a null covers the whole clock | `hasEnd` without effect; reads like the infinite slot batch A removed; rejected |

Members under A (in B, next to the range Partials):

- `hasStart`, `hasEnd`, `isOpen`, `isNotOpen`: `from` / `to` set or not.
- `contains(time: MpLocalTime)`, `contains(slot)`, `contains(partial)`, `intersects(slot)`, `intersects(partial)`:
  on the resolved slot with the two-ended slot's rules (`:170-190`). 24:00 counts as 00:00, so
  `Partial(10:00).contains(EndOfDay)` is false: the clock wraps. That is the slot's nature, as is
  `night.contains(EndOfDay)` today (`MpLocalTimeSlotClockSpec.kt:114-115`).
- With a null on the other side, resolving gives the same answers as the ranges' rule "a null end is enclosed only by a
  null end": `Partial(10:00).contains(Partial(12:00))` is true; `Partial(10:00, 23:00).contains(Partial(12:00))`
  is false.
- `asValidRange()` is unchanged (S3). `Partial(10:00).asValidRange()` stays null, as on the ranges: both ends must be
  set.
- No conversion to a date. Under A, null is 24:00 of that day. Mapping it to an open `MpZonedDateTimeRange.Partial`
  end (the null-preserving rule of decided 1) would change its meaning. Callers write
  `partial.asValidRange()?.let { date.toRange(it, zone) }`. Decided 1 lists no such conversion anyway.
- Factories (S2): `MpLocalTimeSlot.beginningAt(t) = Partial(from = t)`, `endingAt(t) = Partial(to = t)`,
  `forever = Partial()`.

**Batch A:** untouched. A resolved Partial is at most 24 h (`of(from, to)`, `:61-74`), so `MAX_DURATION_MILLIS`,
`splitWithGaps` and `toRange(slot, zone)` (`:336-417`, two-ended only) see only two-ended slots.

**Serialization and stored data:**

- **kotlinx:** `Partial` is a plain `@Serializable` data class with `MpLocalTime` as a number
  (`MpLocalTimeSerializer.kt:17`). With the defaults: `Partial(10:00)` -> `{"from":36000000}` (was
  `{"from":36000000,"to":null}`), `Partial()` -> `{}`. A missing key reads as null; it was `MissingFieldException`.
- **slumber:** the generic data class codec, unchanged: `{from=36000000, to=null}`; a missing key reads as null.
- **TypeScript:** codegen makes both optional, `from?: MpLocalTime | null` (`TypeWalker.kt:500-514`). `datetime.ts`
  has only the two-ended `MpLocalTimeSlot` (`:58-69`) and no Partial, so nothing changes there.
- **Stored data:** nothing in the repo stores a slot Partial (grep: only datetime's tests) [code].

**Consumers:**

- In the repo: `MpLocalTimeSlotSpec.kt:451-490` and `MpLocalTimeSlotClockSpec.kt:99-105` use positional arguments,
  so they compile unchanged. The slumber `MpLocalTimeSlotCodecSpec` covers the two-ended slot only. No kraft, funktor
  or TypeScript use.
- Side finding, not in scope: `MpLocalTimeSerializer` declares `PrimitiveKind.STRING` but encodes a `Long`
  (`MpLocalTimeSerializer.kt:12-13, 17`). JSON does not care; a format that checks the kind would. Worth a ticket.

**Tests (B):**

- The rows of `OpenRangeSpec` again for the slot Partial: null at each end, overnight, empty, `EndOfDay`, the
  resolve-equals-slot rows.
- kotlinx and slumber round trips of `Partial(10:00)` and `Partial()`.
- Mutants: drop `= null` on the slot Partial's `to` (kotlinx decode red); resolve a null `to` as 00:00 instead of
  24:00.

**Changelog:** the slot Partial is part of the New entry and of the defaults entry in section 6.

---

### 9. Operations x types (after B and C as planned), checked on master

Columns: **I** `MpInstantRange`, **Z** `MpZonedDateTimeRange`, **D** `MpLocalDateRange`, **C**
`MpClosedLocalDateRange`, **S** `MpLocalTimeSlot`, **P** the four range Partials (B), **SP** the slot Partial
(section 8, option A). "empty" = half-open `from == to` or a slot of length 0; "rev" = `from > to`. Line numbers are
master's.

| Operation | I | Z | D | C | S | P / SP |
|---|---|---|---|---|---|---|
| bounds | `[from,to)` | `[from,to)` by instant | `[from,to)` | `[from,to]` | `[from,from+d)` on the clock, cyclic | as their two-ended type; SP null = 00:00 / 24:00 |
| `isValid` | `from<to` (:91) | by instant (:88) | `from<to` (:118) | `from<=to` via `asOpenRange` (:71): throws at the last date (**D7**) | not empty (:121) | none; `asValidRange()` both set and in order (SP: and not empty) |
| `isEmpty` | - | - | - | - | yes (:118) | - (**G1**) |
| `contains(point)` | valid and in (:130-132) | by instant (:129-132), + LocalDateTime overloads (:137-146) | `in from..<to` (:152-154), same result | `in from..to` (:92-94) | arcs; empty nothing; 24:00 = 00:00 (:170-174) | null unbounded; rev / empty nothing |
| `contains(range)` | both valid, enclose (:143-146) | by instant (:151-154) | same (:159-162) | same (:96-99) | both non-empty (:177-183) | same + Partial overload |
| `intersects` | positive overlap; shared boundary false (:148-155) | by instant (:156-163) | same (:164-171), + closed (:173-175) | half-open logic on closed ends (:101-108): shared day false; fixed in B (decided 3) | positive overlap; adjacent false (:186-190) | null unbounded; closed P inclusive |
| `touches` | `to>=o.from && from<=o.to`, no validity (:162-164) | by instant (:170-172) | same (:182-184) | via open (:119-121) | closed arcs; empty as a point (:196-205) | absent (decided 1) |
| `isAdjacentTo` | `to==o.from \|\| o.to==from`, no validity (:171-173) | by EQUALITY, zones must match (:177-182) (**D2**) | same as I (:191-193) | via open (:128-130) | false for empty and >= 24 h; not if intersecting (:210-215) | absent |
| `mergeWith` | touches -> min/max, else sorted (:180-189) | by instant, tie keeps this (:189-199) | same as I (:200-209) | via open (:137-139) | union on the clock (:222-239) | absent |
| `cutAway` | invalid -> `[this]`; 0-2 pieces (:199-226) | via instants, pieces re-zoned to `from`'s zone, even when nothing is cut (:219-222) (**D6**) | same as I (:219-245) | via open (:149-151) | no intersect -> `[this]` (:246-274) | absent |
| `plus` / `minus` | Duration; unit + zone (:231-326) | Duration; unit in each end's zone (:227-294) | - | - | - | absent (decided 1) (**G2**) |
| length | `duration = to-from`, negative when rev (:66) (**D5**) | same (:72) | `numberOfDays` 0 when invalid (C) | same, +1 | `duration` >= 0 | absent (settled) |
| `compareTo(period)` | - | yes | yes | yes | - | - (**G3**) |
| `hasStart`/`hasEnd`/`isOpen` | removed (C) | removed | removed | removed | never had | `from/to != null` |
| zone conversion | `atZone` | `atZone`, `toInstantRange`, `asDateRange` | `toZonedTimeRange` | `toZonedTimeRange` | `toRange(slot, zone)` | I/Z: `atZone`, `toInstantRange`, `asDateRange`; I has `asDateRange`, two-ended I not (**G4**); SP none |
| factories | `of`, P-returning `forever`/`beginningAt`/`endingAt` (C) | same | same | same | `of`, `completeDay`; no open factories (S2) | `empty` |

Empty and reversed, the same rows read across. "Empty" behaves alike everywhere except `isAdjacentTo` (D1, decided
4). "Reversed" exists only on ranges (on a slot, `to` before `from` is overnight: nature). There `contains` and
`intersects` say false, while `touches`, `isAdjacentTo` and `mergeWith` answer by arithmetic (D3, D4).

**Divergences not explained by the types' nature:**

| # | Divergence (example) | Proposal |
|---|---|---|
| D1 | `isAdjacentTo` with an empty or reversed range: ranges true (`MpInstantRange(a,b).isAdjacentTo(MpInstantRange(b,b))`; closed `(d1,d5)` vs `(d6,d5)`), slot false | **C**, decided 4 |
| D2 | Zoned `isAdjacentTo` by equality: `[10Z,12Z]` Berlin vs `[12Z,14Z]` UTC: `touches` true, `intersects` false, `isAdjacentTo` false; the same instants in one zone are adjacent | **question D2**, rec: by instant, in C |
| D3 | `touches` ignores validity: `(10,5).touches((1,20))` true, `(10,5).touches((6,7))` false, while `contains` / `intersects` are false for any reversed range | **question D3**, rec in C: false when either is reversed; empty stays a point, as on slots |
| D4 | `mergeWith` swallows a reversed range: `(1,5).mergeWith((4,2))` = `[(1,5)]` | **C**, follows D3 (not touching -> both returned, sorted) |
| D5 | `duration` of a reversed I/Z range is negative (`to - from`); `numberOfDays` is 0 when invalid; a slot's duration is never negative | **question D5**, rec: `Duration.ZERO` when invalid, in C |
| D6 | Zoned `cutAway` with nothing to cut returns a copy with `to` in `from`'s zone instead of `this` (`(10Z Berlin, 12Z New York)` cut by a far range gives `(10Z Berlin, 12Z Berlin)`, not equal to `this`; equality compares zones); every other type returns `this` | **C**: `listOf(this)` when nothing is cut; each piece keeps the zone of the end it comes from. A fix with a RED row; no question |
| D7 | `MpClosedLocalDateRange` ending on 999999999-12-31: `isValid` and everything via `asOpenRange` (`to.plusDays(1)`, :52-54) throw [code + memory: kotlinx throws past its last date]; no other type throws in a predicate | **question D7**, rec in C: closed `isValid`, `contains`, `intersects`, `touches` and `isAdjacentTo` direct and inclusive; `mergeWith` / `cutAway` / `asOpenRange` keep it with KDoc |

**Gaps** (an operation absent on some types; not a divergence of behaviour). Question G, rec: a ticket after C.

- G1: `isEmpty` exists only on S.
- G2: `plus` / `minus` exist only on I and Z.
- G3: `compareTo(period)` is missing on I and S.
- G4: `asDateRange(tz)` is on the I Partial but not on the two-ended I.
- G5: `contains(MpLocalDateTime, zone)` exists only on Z.
- G6: cross-type `intersects` open <-> closed exists only on the two-ended date ranges (decided 2 keeps it off the
  Partials).

**Found, outside the rule (each needs its own ticket, not B or C):**

- O1: `asDateRange` turns a half-open instant range into a closed date range that includes the end's date.
  `MpInstantRange.Partial(2024-01-01T00:00Z, 2024-01-03T00:00Z).asDateRange(UTC)` is `[01-01..01-03]`, three days
  for a two-day range (`MpInstantRange.kt:53-56`; also `MpZonedDateTimeRange.kt:62-65, 102-105`).
- O2: the `MpLocalTimeSerializer` descriptor kind (section 8).

**Tests for the fixes (C):**

- RED rows on master for D1 (four types), D2 (two zones), D3 / D4 (a reversed range), D5 (`(b, a).duration`), D6
  (mixed-zone `cutAway`) and D7 (a closed range on the last date).
- Mutants: each validity guard removed; the zoned `isAdjacentTo` back to equality; the D6 no-cut branch removed.

## History

- 2026-11-14: created on the maintainer's word.
- 2026-11-14: the three further points confirmed by the maintainer; the topic is closed.
- 2026-11-14: built on `wt/datetime` with ULTRA-00341 batch 3b-1; awaiting the review gate (the coordinator moves
  the ticket).
- 2026-11-14: round 1 NOT CLEAN, handled; delta round 2 CLEAN, its two MINORs applied. `numberOfDays` waits for the
  maintainer.
- 2026-11-14: master's values restored on `wt/datetime` for the merge of ULTRA-00341 (maintainer: "ok please do this so
  the merge works"); the values built here are not merged; the decision (other values, or no sentinels) is open and
  discussed after the merge.
- 2026-11-14: master's values were restored for the merge of ULTRA-00341 (`e7c882a9`); the maintainer's decision
  below replaces the question of which values to use.
- 2026-11-14: batch A (slots are finite) built on `wt/datetime`; awaiting the gate. Batches B and C wait for the
  coordinator's word.
- 2026-11-14: batch A gate round 1 CLEAN (9 MINOR), all handled; a short delta review follows.
- 2026-11-15: batch A delta round 2 CLEAN (4 MINOR), handled: the zone id is checked before the usable interval
  (`21eb5143`), tests and texts. Status: batch A BUILT, gate round 1 CLEAN, delta round 2 CLEAN.

## Decided (maintainer, 2026-11-14): no sentinels, open ranges are `Partial`, slots are finite

The other projects do not use the constants. In the maintainer's words: "we return Partial and add the query methods
you proposed, where from = null means open start and to = null means open end. Use the same pattern for all ranges
that need it and drop doomsday and genesis please."

- **Open ranges:** `beginningAt()`, `endingAt()` and `forever` return `Partial`. `from = null` is an open start,
  `to = null` an open end. The same pattern for every range type that needs it (`MpInstantRange`,
  `MpZonedDateTimeRange`, `MpLocalDateRange`, `MpClosedLocalDateRange`).
- **Queries on `Partial`:** `contains` and `intersects`, null read as unbounded. A `Partial` with its ends in the
  wrong order contains nothing; no exception.
- **The two-ended ranges stay plain,** not part of a sealed hierarchy ("this would clutter the usage sides with when
  statements, it would also break the serialization"). `hasStart`, `hasEnd` and `isOpen` belong to `Partial`. Length
  questions (`numberOfDays`, `numberOfNights`, `asListOfDates`, `asDatePeriod`) stay on the two-ended ranges only.
- **Genesis and Doomsday are dropped** from all date types and `constants.kt`. No decoder translates old sentinel
  values: nothing in production holds them.
- **Slots are finite:** "the underlying problem is that we allow Duration.INFINITE for the timeslot. I think we should
  not allow this one." `MpLocalTimeSlot.of(from, Duration.INFINITE)` gives a slot cut to the largest finite length it
  can store ("Yes take A"); no exception. `isInfinite`, the open end of `toRange(slot, zone)`, and the "counts as
  infinite" rule of the decoders and of `datetime.ts` go. `toRange` always returns a two-ended range; an end beyond
  what the platform can represent ends at the last representable instant.
- Considered and not taken: a sealed family per range type; separate `OpenStart` / `OpenEnd` types; a separate
  `OpenEnded` type next to `Partial`.

To settle in the track's plan before building, each with an example: the stored shape of an open range and what a
database query on a null end looks like in karango and monko; what `Partial` means for form input now that null
also means unbounded (`asValidRange()` stays "both ends set and in order"); the behaviours of master listed above as
"back and not fixed", which this change removes or leaves; from the review of the sentinel restore: a range that
starts before Genesis lists wrong dates, an open end lost in a zone conversion, shifting an open range closes it, an
empty range counts as adjacent, the two date range types disagree on the open end by a day; and three text or test
MINORs of that review (the count-1 case lists one date, not none; `numDaysInMonth` is pinned in `MpLocalDateSpec`;
`MpLocalTimeSlotOnDateSpec.kt:134` needs the literal `+12000-01-01T09:00:00Z`).


## Confirmed with the maintainer on the track's plan (2026-11-14)

- `Partial` gets `contains(point)`, `contains(twoEndedRange)` and `contains(Partial)` ("yes add both contain
  variants"), next to `intersects`.
- `toRange(slot, zone)`: a START the platform cannot represent throws, with a message naming the date and the limit
  ("this is a case where we should actually throw an error when the platform does not support it"). An END beyond
  the limit stops at the last representable instant ("as proposed ... will not really happen in normal
  applications"). The rule: throw when no correct answer exists, fall back when the answer is still right for
  everything the platform can express.
- `Partial`: `from` and `to` get the default `null` in every range class ("yes defaults only"). kotlinx then reads
  a missing key as null, as slumber does. No `@EncodeDefault`: whether a null end is written as a key is the app's
  choice through `Json { encodeDefaults = true }`; a missing key and a null key mean the same. No codec in this repo
  sets it today.
- Dates BC as text: ULTRA-00347, a separate ticket in the same work package, directly after this one.

## Decided (maintainer, 2026-11-26): the batch B and C plan questions

Plan drafted 2026-11-26 (coordinator's planner, read on master e2196fba). The maintainer's answers, verbatim where
quoted:

- **Guiding rule (with 4 and 9):** "the behaviour must be equivalent across all types for the same operations /
  predicates"; "Exception to the rule always makes it hard to learn things."
1. `Partial` gets conversions that keep a null end null (`atZone`, `toInstantRange`, `asOpenRange` /
   `asClosedRange`); no arithmetic (`plus`, `minus`, `mergeWith`, `cutAway`, `toZonedTimeRange`) on `Partial`. In B.
2. The open and closed date `Partial`s do not query each other directly; callers convert with `asOpenRange`. "we can
   think about adding shortcuts as extension methods" (later, not in B or C).
3. Fix closed `intersects` on a shared boundary day ([01-01..01-05] vs [01-05..01-09] is false today). In B, RED first.
4. `isAdjacentTo` requires both ranges valid, as slots do since 0.119.0. In C. Plus the guiding rule above.
5. Delete the sentinels and the two-ended `hasStart` / `hasEnd` / `isOpen` / `isNotOpen` outright: "no backward compat
   needed".
6. `numberOfDays` is capped at `Int.MAX_VALUE`, with KDoc.
7. `Partial.empty` equals `forever`; keep both names, say so in the KDoc.
8. A karango e2e and a monko spec pin the documented open-range query ("range contains t"), mutation-checked, no
   query helper. In C.
9. `MpLocalTimeSlot.Partial` ALSO gets nullable defaults, like the other `Partial`s: "The behaviour has to match
   across types for user comfort." Implications to be checked before building.
10. `numberOfDays`, `numberOfNights`, `numDaysInMonth`: plain getters instead of `by lazy`. In C.
- Also accepted (2026-11-26): `numberOfDays` on two-ended ranges becomes calendar-exact (end epoch day minus start
  epoch day, +1 for closed ranges, 0 when invalid).

## Decided (maintainer, 2026-11-26, second set): slots and the cross-type check

Asked one by one, with an example each. Verbatim where quoted.

- **S1, a null end of `MpLocalTimeSlot.Partial`:** the edge of the day. A null `from` is 00:00, a null `to` is 24:00
  (`EndOfDay`): `Partial()` is the whole day, `Partial(from = 22:00)` is 22:00 to 24:00 (no wrap past midnight),
  `Partial(to = 06:00)` is 00:00 to 06:00. The slot `Partial` gets the same members as the range `Partial`s
  (`hasStart`, `hasEnd`, `isOpen`, `isNotOpen`, the `contains` and `intersects` queries), and null defaults.
- **S2, factories on `MpLocalTimeSlot`:** "beginningAt, endingAt and completeDay ... not forever as this makes no sense
  when we assume that nulls for from or to mean 00:00 or 24:00". So `beginningAt` and `endingAt` return the `Partial`;
  `completeDay` stays the whole day; no `forever` on slots.
- **S3, what stays different because of a slot's nature:** overnight stays (`to` before `from` is the next morning).
  "for the timeslot we should rename the predicate to isValidSlot() to make it clear that we have a different concept
  here"; asked which member: **rename the `Partial` conversion**, `MpLocalTimeSlot.Partial.asValidRange()` becomes
  `asValidSlot()`. The slot's `isValid` / `isNotValid` keep their names.
- **D2:** zoned `isAdjacentTo` compares by instant, in C (reverses the choice pinned at
  `MpZonedDateTimeRangeZoneSpec.kt:9-11`).
- **D3:** `touches` is false when either range is reversed, in C; `mergeWith` follows. An empty range still touches
  as a point.
- **D5:** `duration` of an invalid (reversed) instant or zoned range is `Duration.ZERO`, in C.
- **D6** (no question, a plain bug): zoned `cutAway` with nothing to cut returns `this`, in C, RED first.
- **D7:** the closed date range's predicates are written directly on the inclusive ends, so none throws on the last
  date. "Check if other ranges do similar conversion "tricks" and also apply the predicate check directly without
  converting first. This is anyway better for performance." So in C: every predicate on every range type is computed
  directly, without converting to another range type first (e.g. via `asOpenRange`, `toInstantRange`); list each one
  found and changed in the ticket. `mergeWith` and `cutAway`, which build new ranges, get a KDoc note where they still
  convert.
- **G:** the missing operations (`isEmpty`, `plus` / `minus`, `compareTo(period)`, `asDateRange`,
  `contains(MpLocalDateTime, zone)`, open-vs-closed `intersects`) go to a separate ticket after C.
- **O1:** `MpInstantRange.Partial.asDateRange` counting the end's date: a separate ticket.
- **`MpLocalTimeSerializer`** declares `PrimitiveKind.STRING` but encodes a Long: a separate ticket.

## Batch A: slots are finite (plan as built, 2026-11-14)

Built on `wt/datetime`, at master `f750497f`, as confirmed with the maintainer. Batches B (queries on `Partial`) and
C (no sentinels) wait for the coordinator's word. Batch A does not touch Genesis or Doomsday anywhere else: the slot
simply stops using Doomsday.

- **`MpLocalTimeSlot.MAX_DURATION_MILLIS = 9_007_199_254_740_991`** (2^53 - 1 ms, about 285,000 years; renamed from
  `INFINITE_DURATION_MILLIS`, same value). It is the largest whole number a JS client reads and writes exactly, so a
  stored length round-trips. `of(from, duration)` cuts every longer length to it, `Duration.INFINITE` included; no
  exception.
- **Gone:** `isInfinite`; `openEnd()`; every infinite branch (`durationMillis`, `to`, `endDayOffset`,
  `splitWithGaps`, `toRawData`, the KDoc of the formatters). `endDayOffset` is at most 104 249 992 (the longest
  slot from 22:00 on; corrected in round 1).
- **`splitWithGaps`:** one finite formula, still capped at `Int.MAX_VALUE` parts with the KDoc line about checking
  `size`. A `gap` beyond the longest length counts as it. The `step % DAY` reduction is gone again: `i * step` is at
  most the slot's own length (2^53 - 1 ms), so it cannot overflow any more (it could while slots were infinite).
- **Decoders:**
  - Both read through `toSlotOrNull()` into `of(from, duration)`, so everything beyond the longest length is cut to
    it: a stored 9007199254740991 (today's writer) is the longest slot, and so is `Long.MAX_VALUE`.
  - slumber saturates a `BigInteger` / `BigDecimal` beyond `Long`, and `+Infinity`, before the cut.
  - NaN stays a decode failure; a negative duration stays empty; an explicit `"duration": null` stays an error.
- **`toRange(slot, zone)` is always two-ended.** The rule, in the KDoc: throw when no correct answer exists, fall back
  when the answer is still right for everything the platform can express.
  - Where the zone engine has a limit, only local times it can fully work with are used: the limit minus two days
    (its one-day probe window plus a zone offset). Round 1 changed this; see the ledger. One `expect` function,
    `platformResolvableSeconds(zoneId)`:
    - JVM: no limit;
    - JS, region zone: the range of `Date`, ±8.64e12 s;
    - native, region zone: the second to the last date of the `LocalDate` range (kotlinx-native refuses an instant in
      year 1 000 000 000, which the engine's +1-day probe from the last date reaches);
    - fixed offsets: no limit anywhere.
  - A START outside throws `IllegalArgumentException` (the type the zone engine throws), naming the date, the time,
    the zone and exactly the accepted interval. Example (JS): `The start of a time slot, +300000-01-01 10:00:00 in
    Europe/Berlin, lies outside what this platform can represent in that zone: a local date and time from
    -271821-04-22T00:00 to +275760-09-11T00:00`. Native: `-999999999-01-04T00:00 to +999999999-12-29T00:00`. A zone
    error (`IllegalTimeZoneException`) passes through unchanged (pinned in `ZoneIdCheckSpec`).
  - An END past the interval stops at the interval's last local time in the zone (JS: +275760-09-11T00:00 local);
    an END whose date, or 24:00 on it, runs past 999 999 999-12-31 stops at that date's last instant in the zone
    (JVM, and fixed offsets everywhere). Only `DateTimeArithmeticException` (the date types' own limit) leads to a
    fallback; every other exception passes through (round 1).
  - So the same call can differ by platform once the end lies past the year 275760; the KDoc says so.
  - The "empty range at the start" guard in `onDate` is gone. With the first-existing-time rule and an end that
    falls back to the platform's last instant, the end can no longer lie before the start (the start is
    representable, so the fallback is at or after it).
- **TypeScript:** `datetime.ts` exports `MAX_DURATION_MILLIS` ("a slot is always finite: a longer length is cut to
  this"); `INFINITE_DURATION_MILLIS` is gone. The parity spec pins the value against Kotlin.
- **Repo search:** no `isInfinite` on a slot and no `INFINITE_DURATION_MILLIS` left in code. The remaining
  `isInfinite()` uses are on `Duration` / `Double` (`Kronos.kt:78`, streams `sampled.kt:123`, funktor
  `slices.kt:67`), unrelated. Master's `CHANGELOG.md` still names them (below).

### Tests: the 3b-2 rows that pinned infinite behaviour, and what each became

| Spec, test | Was | Now |
|---|---|---|
| `MpLocalTimeSlotClockSpec` "Duration.INFINITE is kept; a duration from 2^53 - 1 ms on is infinite" | `duration == INFINITE`, `isInfinite`, `to == from`, `endDayOffset == Long.MAX_VALUE` | "... are cut to it": `MAX_DURATION_MILLIS == 9_007_199_254_740_991`, `duration == MAX ms`, `MAX + 1` and `Long.MAX_VALUE` give the same slot, `MAX - 1` stays; `to` 18:59:00.991, `endDayOffset` 104 249 991 (by hand), `crossesMidnight` |
| `MpLocalTimeSlotClockSpec` "splitWithGaps ... an infinite slot by 1 h" | `Int.MAX_VALUE` parts, the last at 16:00 | the same numbers for the longest slot (2.5e9 parts, capped); title only |
| `MpLocalTimeSlotClockSpec` "splitWithGaps on an infinite slot ..." | `Int.MAX_VALUE` parts by 60 days, the overflow pin at index `Int.MAX_VALUE - 1`; infinite gap gives one part | "on the longest slot": 1 737 499 parts by 60 days, the last `10:00 + 60 d`; 1 736 295 by 1 h + 60 d (by hand); an infinite gap or part is the longest length (one part, or the slot itself) |
| `MpLocalTimeSlotClockSpec` `night.cutAway(slot(10, 0, INFINITE))` | empty | unchanged (a slot of 24 h or more covers the clock) |
| `MpLocalTimeSlotOnDateSpec` "an end beyond what the platform can resolve is the open end (JS)" | Berlin: `hasEnd == false` | UTC: the exact end 8 701 705 312 800 000 ms on all three; Berlin: JS stops at +275760-09-11T00:00 local (since round 1), JVM / native resolve beyond it (test-side `expect val isJsPlatform`) |
| (new) `MpLocalTimeSlotOnDateSpec` "a start the platform cannot represent throws ..." | - | JS: `IllegalArgumentException` with the whole message, the interval `-271821-04-22T00:00 to +275760-09-11T00:00` (since round 1); JVM / native: a one-hour range; UTC resolves everywhere |
| `MpLocalTimeSlotOnDateSpec` "an end past the last date ... is the open end" | empty at the start, `hasEnd == false` | the end is the last instant of 999 999 999-12-31 (epoch second 31 556 889 832 780 799, .999), also for an end at 24:00 |
| `MpLocalTimeSlotOnDateSpec` "an end before the start can only come from the open end: past Doomsday ..." | infinite slot on 12000-01-01 empty at the start | deleted: the case cannot happen any more (the guard is gone); the literal `+12000-01-01T09:00:00Z` asked for in the review is moot |
| `MpLocalTimeSlotOnDateSpec` "an infinite slot gives a range with an open end" | Berlin, `hasEnd == false` | "Duration.INFINITE on a date: the longest slot, a two-ended range": UTC, end 9 008 904 567 540 991 ms (by hand) |
| `MpLocalTimeSlotSerializationSpec` "an infinite duration is stored as 2^53 - 1 ms ... reads back infinite" | `{"from":36000000,"duration":9007199254740991,"to":36000000}`; `Long.MAX_VALUE` reads as infinite | `..."to":68340991}` (the clock time of the end); `Long.MAX_VALUE` reads back as the longest slot |
| slumber `MpLocalTimeSlotCodecSpec` (four tests) | `isInfinite` for `Duration.INFINITE`, `1e400000000`, `+Infinity`, a `BigInteger` / `BigDecimal` beyond `Long` | `duration == MAX_DURATION_MILLIS ms`; the stored `to` of the longest slot is 68340991 |
| codegen `MpDateTimeFieldParitySpec` | `INFINITE_DURATION_MILLIS` in `datetime.ts` | `MAX_DURATION_MILLIS` |
| `MpLocalTimeSlotSpec` rows "an infinite duration" (split part) and the infinite gap | `[]` and one slot | unchanged and green: a part of the longest length does not fit a 12-hour slot |

**RED first:** with `MAX_DURATION_MILLIS` added as a temporary alias of the old constant (no behaviour change), the
rewritten rows failed for the right reason: JVM 5, JS 7 (with the two JS-only rows: the end not at 8.64e15 ms, the
start's plain engine message), slumber 4, codegen 1.

| Mutant (Batch A; each restored with `cmp`) | Tests red |
|---|---|
| A1 `of`: a length from the longest on is `Duration.INFINITE` again | 4 |
| A2 `of`: cut one millisecond below the longest | 3 |
| P1 kotlinx / slumber write `Long.MAX_VALUE` for the longest slot (persistence: both codecs) | 1 datetime + 1 slumber |
| P2 kotlinx decoder wraps a length beyond the longest instead of cutting it (persistence; first survived: `Long.MAX_VALUE mod 2^53` is exactly the longest; killed by the row 9007199254740997) | 1 |
| P3 slumber: a `BigInteger` beyond `Long` wraps (persistence) | 1 |
| P4 slumber: `+Infinity` read as 0 (persistence) | 1 |
| E1 the end's date overflow (year 999 999 999) rethrown | 1 |
| E2 the last date's fallback at 00:00 instead of 23:59:59.999 | 1 |
| E3 JS: an end beyond 8.64e15 ms rethrown (run on JS) | 1 |
| E4 JS: the start error without date, zone and limit (run on JS) | 1 |
| E5 JS: no limit for region zones (run on JS) | 2 |
| G1 the gap not capped at the longest length | 0: equivalent. Any gap of at least the slot's remaining length gives one part; without the cap `part + gap` wraps negative and the truncating division happens to give the same count. The cap only keeps the arithmetic from overflowing |

**Counts** (2026-11-14, `console/wt-gradle.sh`, `--rerun` on each test task, from the XML):

- datetime JVM 2855 / 0, JS 2197 / 0, native 2189 / 0. `MpLocalTimeSlotClockSpec` 20, `MpLocalTimeSlotOnDateSpec` 16
  and `MpLocalTimeSlotSerializationSpec` 7 on all three; one test deleted, one added, so the totals stay.
- slumber 1267 / 0; codegen 326 / 0.
- Repo-wide compile sweep: exit 0, 0 `e:` lines.

### Review record: round 1 (reviewer-fable-xhigh, all three charters)

**Verdict:** CLEAN: 0 CRITICAL, 0 MAJOR, 9 MINOR. The cap holds on every path, the three removals are sound, G1 is
equivalent, the stored shape is unchanged for finite slots. The reviewer ran nothing; every item was settled by a
test first.

| # | Finding | Outcome |
|---|---|---|
| 1 | public KDoc of `MpLocalDate.toRange(slot, zone)` still named the open end | FIXED; the words "infinite", "endless", "open end", "Doomsday" near slots searched repo-wide (code, docs-site, tickets): the remaining hits name `Duration.INFINITE` as an input; the test variable `endless` and a slumber comment fixed |
| 2 | JS: the fallback end at 8.64e15 ms sits on the edge of the zone engine | CONFIRMED by a JS probe: `to.atStartOfDay()` and `to.atTime(..)` throw ("Instant at 8640000086400 s is outside the range of JS dates"); `to` shows 00:00 where the JVM shows 02:00 (`round(NaN)` is NaN, `.toInt()` makes it 0). FIXED: local times are checked against the limit minus two days; the end falls back to `+275760-09-11T00:00` local; pinned on JS: the wall clock, the instant (8 639 999 820 000 000 ms, +02:00), `atStartOfDay`, `atStartOfHour`, `atTime`, the range formatter. The start check uses the same interval, both ends included, pinned at both edges |
| 2 | the NaN swallow in `intlOffsetSecondsAt` | STILL REACHABLE through public API outside slots, for a range of instants: within the zone's positive offset of the upper limit (e.g. `MpInstant.fromEpochMillis(8_640_000_000_000_000).atZone(Europe/Berlin)` shows 00:00, JVM 02:00), mirrored at the lower limit west of Greenwich. Already listed in the archived ULTRA-00216 (round 2 notes, finding 3) and ULTRA-00217 ("Related MINORs") without a ticket number: it needs a ticket of its own (JS zone engine at its limit) |
| 3 | the last-date fallback pinned for UTC only; native unverified | CONFIRMED on native: kotlinx-native throws `DateTimeArithmeticException` ("Instant +1000000000-01-01T00:00:00Z is not representable as LocalDateTime") for the engine's +1-day probe from the last date. FIXED: native has a limit for region zones (a day inside the date range) and uses the same two-day scheme. In Berlin: JVM ends at 999 999 999-12-31T23:59:59.999+01:00 and accepts a start on the last date; native ends at local 999 999 999-12-29T00:00 and refuses that start with the clear message; JS refuses the start |
| 4 | `endDayOffset` "at most 104 249 991" off by one | FIXED: 104 249 992 (`of(22:00, INFINITE)`), pinned, texts corrected |
| 5a | mutant: delete the `IllegalTimeZoneException` rethrow | the code path is gone (the start check no longer catches); a bad zone id through `toRange` is pinned in `ZoneIdCheckSpec` for every refused id on JVM, JS and native |
| 5b | mutant: swap or mis-sign the message's bounds | the whole message is a literal (JS and native); mutant R1 red |
| 5c | a start before the lower limit (year -300000) | pinned on JS with the whole message |
| 6 | slumber `longest` derived from the constant; no rows at the cap | FIXED: the literal 9_007_199_254_740_991 ms; rows `BigInteger` 2^53 - 1 and 2^53, `BigDecimal` 9007199254740990.5 (stays, truncated) and 9007199254740991.5 (cut), Double 9.007199254740993e15 (cut) |
| 7 | the END catch wider than needed | FIXED: no `IllegalArgumentException` catch at all; the platform limit is checked before the engine is asked. Only `DateTimeArithmeticException` (the end's date, or 24:00 on it, past 999 999 999-12-31: the date types' own limit) leads to the fallback |
| 8 | "about 285 616 years" counts 365-day years; the changelog advice lacked the master-written record | FIXED: "about 285,000 years"; the clause on `"duration":9007199254740991,"to":36000000` |
| 9 | the kotlinx decoder reads `duration: Long.MIN_VALUE` as absent; docs pages `ranges.astro:341`, `mp-local-date.astro:164` show the removed public constructor | RECORDED: the marker note is settled (round 2 of 3b-2); the docs lines go to ULTRA-00339 |

| Mutant (round 1; each restored with `cmp`) | Tests red |
|---|---|
| R1 the message's two bounds swapped | 1 JS + 1 native |
| R2 no start check | 2 JS + 1 native |
| R3 the engine margin one day instead of two | 4 JS |
| R4 no end check against the usable interval | 3 JS |
| R5 the last-date fallback at 00:00 instead of 23:59:59.999 | 2 JVM |
| R6 no catch for the date types' own limit (`DateTimeArithmeticException`) | 2 JVM |
| R7 native: the usable range a day short | 1 native |
| R8 native: no limit for region zones | 1 native |

The first R2 / R4 runs did not compile (a `false &&` in front breaks the smart cast). They were rerun with the
condition at the end; the numbers above are from the rerun.

**Counts after round 1** (2026-11-14, `--rerun`, from the XML):

- datetime JVM 2869 / 0, JS 2211 / 0, native 2203 / 0 (`MpLocalTimeSlotOnDateSpec` 19 and `ZoneIdCheckSpec` 87 on
  all three).
- slumber 1268 / 0 (`MpLocalTimeSlotCodecSpec` 10); codegen 326 / 0.
- Repo-wide compile sweep: 0 `e:` lines.

### Review record: delta round 2 (reviewer-fable-xhigh)

**Verdict:** CLEAN: 0 CRITICAL, 0 MAJOR, 4 MINOR. Margin, edges, the Berlin literal (+02:00 on 11 September), the
catch, no inversion on any platform and `endDayOffset` 104 249 992 confirmed by trace; none of R1 to R8 survives.
Status: **batch A: BUILT, gate round 1 CLEAN, delta round 2 CLEAN.**

| # | Finding | Outcome |
|---|---|---|
| 1 | a bad region id masked by the range error, echoed raw (JS: year 300 000; native: 999 999 999-12-31) | CONFIRMED (RED: 12 on JS, 12 on native: the 11 refused ids and a well-shaped unknown id, `IllegalArgumentException` instead of `IllegalTimeZoneException`; JVM green). FIXED in its own commit `21eb5143`: `startOnDate` validates the zone (`TimezoneEngine.offsetSecondsAt`) before refusing the date. `ZoneIdCheckSpec` has the `toRange` entry on 999 999 999-12-31 for every refused id, and the unknown id `Mars/Olympus_Mons` at three dates. Mutant V1 (validation removed): 12 JS + 12 native red. The END cannot see an invalid id first: `onDate` evaluates the start first, and a start inside the interval goes through the engine, which validates |
| 2 | KDoc of `onDate` and `MpLocalDate.toRange`: native interval missing, "every day operation" overstated, a pointer to a class KDoc without the details | FIXED: both KDocs name the JS and native intervals and the JVM / fixed-offset last-date fallback; `atTime(MpLocalTime.EndOfDay)` throws on the last instant of the last date (no next day); the pointer dropped |
| 3 | ticket texts | FIXED: the changelog sentence names native's 999 999 999-12-29T00:00; the two "Now" rows give the round-1 state; `round(NaN)` is NaN, `.toInt()` makes it 0; the NaN item is a range of instants, already in the archived ULTRA-00216 and ULTRA-00217 without a ticket number. **Which is true today about the JVM at the last date** (archived ULTRA-00217: a raw `java.time.DateTimeException`): on these paths, no exception. A start on 999 999 999-12-31 10:00 in Berlin (recurring rules) and the fallback at that day's last instant resolve on the JVM; both make the engine probe into year 1 000 000 000. Pinned in `MpLocalTimeSlotOnDateSpec`. Other zones or instants were not probed |
| 4 | tests: a stale title; the second margin day not pinned; edge rows; JS-only tests asserting nothing elsewhere | FIXED: title; JS `atTime(Max)` = 8 639 999 906 399 999 ms and `atTime(EndOfDay)` = 8 639 999 906 400 000 ms on the 09-11 end (by hand, matched by the browser); native `atStartOfDay`, `atTime(Max)`, `atTime(EndOfDay)` on the 12-29T00:00 end; the upper edge accepted and 1 ms past it refused on JS and native; the two JS-only tests say so in their names |
| 5 | surplus: `usableLocalTimes` computed several times; `zoneEngineLimitMillis` only a flag | FIXED: computed once in `onDate` and passed down (plain refactoring, in `21eb5143`); the test flag is `isJsPlatform` |

**Counts after delta round 2** (2026-11-15, `--rerun`, from the XML):

- datetime JVM 2881 / 0, JS 2223 / 0, native 2215 / 0 (`MpLocalTimeSlotOnDateSpec` 19 and `ZoneIdCheckSpec` 99 on
  all three).
- slumber 1268 / 0; codegen 326 / 0.
- Compile sweep: 0 `e:` lines.
- Mutant V1 (zone validation removed): 12 JS + 12 native red.

### Changelog text (Batch A)

Against release 0.118.0 a slot never was infinite (it was `from` / `to` within one day), so batch A adds no entry of
its own against the release. It corrects the Unreleased entries of ULTRA-00341 that master's `CHANGELOG.md` carries
(for the coordinator at the merge):

- Line 27: replace "New: `isEmpty`, `isInfinite`, `INFINITE_DURATION_MILLIS`; `Duration.INFINITE` is a slot without
  an end" with: "New: `isEmpty`, `MAX_DURATION_MILLIS` (2^53 - 1 ms, about 285,000 years). A slot is always finite:
  `of(from, duration)` cuts every longer length to it, `Duration.INFINITE` included (ULTRA-00343)."
- Lines 37-38: "code that relied on `isValid` now accepts multi-year and endless slots (`isInfinite`)" becomes "code
  that relied on `isValid` now accepts slots of up to about 285,000 years".
- Line 51: "an infinite duration is 9007199254740991" becomes "a duration is at most 9007199254740991; a larger
  stored number reads back as that". (A record master's unreleased writer made for an infinite slot,
  `"duration":9007199254740991,"to":36000000` for 10:00, reads as the longest slot and writes back `"to":68340991`.)
- Add to the `MpLocalDate.toRange(slot, zone)` bullet (line 43): "Always a two-ended range. An end the platform
  cannot represent stops at its last representable time (in the browser, in a region zone: +275760-09-11T00:00
  local; at the far end of the date range: the last instant of 999 999 999-12-31; on native in a region zone
  999 999 999-12-29T00:00 local); a start it cannot represent throws `IllegalArgumentException` naming the date, the
  zone and the accepted interval; an invalid zone id is an `IllegalTimeZoneException`, whatever the date
  (ULTRA-00343)."
- `ultra/codegen`, the TypeScript runtime: `MpLocalTimeSlot`'s constant is `MAX_DURATION_MILLIS` (the Unreleased
  entry "the TypeScript runtime has `MpLocalTimeSlot`" names no constant and stays).
- Line 20 (ULTRA-00215) "by 25 h or by `Duration.INFINITE` now yields no slots" stays true.
- 2026-11-15: IN PROGRESS.
- 2026-11-26: batch B and C plan drafted; the maintainer answered its ten questions (section above). Next: the
  implications of item 9 and a cross-type equivalence check, then build B and C in a fresh worktree.
- 2026-11-26: second set of decisions (S1-S3, D2-D7, G, O1, serializer), section above. Next: build B, then C,
  in a fresh worktree; separate tickets for G, O1 and the serializer.
