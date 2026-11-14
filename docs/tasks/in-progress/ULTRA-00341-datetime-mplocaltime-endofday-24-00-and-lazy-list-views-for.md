# ULTRA-00341: datetime: MpLocalTime.EndOfDay (24:00), and lazy list views for date ranges and slot splits

> **Status (2026-11-14): IN PROGRESS.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Plan:** ULTRA-00316 (close the open epics), track A, batch 3.

## Why this exists

Two things the maintainer decided on 2026-11-14 for `ultra/datetime`:

- **3a, 24:00:** "yes to special 24:00 representation"; "MpLocalTime: OK as suggested". `MpLocalTime.EndOfDay` is an
  ordinary value of 86,400,000 ms, one millisecond after `Max`. Only the constant, `parse("24:00")` and the decoders
  give it; `of(24, 0)` keeps wrapping to 00:00; `DateTimeRangeConverter.fromHourToHour(_, 24)` uses it. Overnight
  slots (22:00 to 02:00) are not built ("only implement it once the necessity arises").
- **3b, no cap, a lazy view:** `MpLocalDateRange.asListOfDates()` keeps its signature and returns a lazy list view
  that computes each date on demand. The maintainer: "I am not a big fan of code that runs for years and then
  suddenly hits a runtime error"; "yes go for this and document that the returned list is a lazy view and what that
  means for the consumer". The view becomes a small reusable class. Open with the maintainer: whether
  `MpLocalTimeSlot.splitWithGaps` becomes such a view too, dropping `maxSlots` and its exception (batch 1, unreleased).

## Batch 3a: built

Commit `618a4860` on `wt/datetime` (it references "ULTRA-00316 track A batch 3a"). The track's report, with the plan
as built, the mutation table, the consumers and the changelog text, goes into this ticket with the round-1 ledger.

## Left out, for one follow-up ticket

- `MpLocalTime.jvm` drops the milliseconds (`Max.jvm` = 23:59:59).
- `MpLocalTime.parse`'s error message names `MpLocalDate`.
- `MpLocalDateRange.numberOfDays` miscounts ranges past Doomsday.

## History

- 2026-11-14: created by the coordinator for batch 3 of track A.
