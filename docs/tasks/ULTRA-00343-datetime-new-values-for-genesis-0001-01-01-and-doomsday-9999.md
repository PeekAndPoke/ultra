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

## History

- 2026-11-14: created on the maintainer's word.
- 2026-11-14: the three further points confirmed by the maintainer; the topic is closed.
