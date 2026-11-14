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
| `MpLocalTimeSlotOnDateSpec` "an end beyond what the platform can resolve is the open end (JS)" | Berlin: `hasEnd == false` | UTC: the exact end 8 701 705 312 800 000 ms on all three; Berlin: JS stops at 8.64e15 ms, JVM / native resolve beyond it (test-side `expect val zoneEngineLimitMillis`) |
| (new) `MpLocalTimeSlotOnDateSpec` "a start the platform cannot represent throws ..." | - | JS: `IllegalArgumentException` naming `300000-01-01`, `Europe/Berlin`, `275760-09-13`; JVM / native: a one-hour range; UTC resolves everywhere |
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
| 2 | JS: the fallback end at 8.64e15 ms sits on the edge of the zone engine | CONFIRMED by a JS probe: `to.atStartOfDay()` and `to.atTime(..)` throw ("Instant at 8640000086400 s is outside the range of JS dates"); `to` shows 00:00 where the JVM shows 02:00 (`round(NaN)` = 0). FIXED: local times are checked against the limit minus two days; the end falls back to `+275760-09-11T00:00` local; pinned on JS: the wall clock, the instant (8 639 999 820 000 000 ms, +02:00), `atStartOfDay`, `atStartOfHour`, `atTime`, the range formatter. The start check uses the same interval, both ends included, pinned at both edges |
| 2 | the NaN swallow in `intlOffsetSecondsAt` | STILL REACHABLE through public API outside slots: `MpInstant.fromEpochMillis(8_640_000_000_000_000).atZone(Europe/Berlin)` shows 00:00 (JVM 02:00). For a separate ticket (JS zone engine at its limit) |
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
  local; at the far end of the date range: the last instant of 999 999 999-12-31, on native two days earlier); a
  start it cannot represent throws `IllegalArgumentException` naming the date, the zone and the accepted interval
  (ULTRA-00343)."
- `ultra/codegen`, the TypeScript runtime: `MpLocalTimeSlot`'s constant is `MAX_DURATION_MILLIS` (the Unreleased
  entry "the TypeScript runtime has `MpLocalTimeSlot`" names no constant and stays).
- Line 20 (ULTRA-00215) "by 25 h or by `Duration.INFINITE` now yields no slots" stays true.

