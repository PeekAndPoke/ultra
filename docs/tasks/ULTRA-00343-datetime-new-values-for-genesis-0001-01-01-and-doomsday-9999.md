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
