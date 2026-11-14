# ULTRA-00342: datetime: three small defects (MpLocalTime.jvm drops milliseconds, parse message names MpLocalDate, numberOfDays past Doomsday)

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Plan:** ULTRA-00316 (close the open epics), track A, after the lazy list views of ULTRA-00341.

## Why this exists

Three small defects in `ultra/datetime` that track A found and left out of its batches. The maintainer (2026-11-14):
"yes fix all in one ticket."

1. **`MpLocalTime.jvm` drops the milliseconds.** `MpLocalTime.Max.jvm` is 23:59:59, not 23:59:59.999: the converter
   goes through `ofSecondOfDay`. Confirmed by a probe (track A, 2026-11-14) and pinned as current behaviour in
   `MpLocalTimeEndOfDayJvmSpec`; the fix turns that pin into the right expectation.
2. **`MpLocalTime.parse`'s error message names the wrong type:** "Could not parse MpLocalDate ...".
3. **`MpLocalDateRange.numberOfDays` miscounts ranges that reach past Doomsday:** the end is clamped there by
   `DateTimeRangeConverter`, so a range from 2000 to 12000 counts as 8,000 years. Decide what the right answer is for
   a range with an end outside the supported span (count the real days, or refuse such a range at construction) once
   the maintainer has settled what Genesis and Doomsday are.

## The work

- Tests first for each; mutation check on the converter (1).
- CHANGELOG: (1) and (3) are behaviour changes without a compile signal.

## Acceptance

- The three cases give the right result on JVM, JS and native where they apply; datetime and slumber suites green.

## History

- 2026-11-14: created on the maintainer's word.
