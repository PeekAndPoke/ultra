# ULTRA-00362: Datetime: fill the operation gaps across range types (after ULTRA-00343)

> **Status (2026-11-26): NOT STARTED.** 

## Why this exists

The cross-type check for ULTRA-00343 (2026-11-26) found operations that exist on some range types but not on
others. They are additions, not behaviour fixes, so the maintainer put them after ULTRA-00343 batch C ("Separate
ticket after C"). The guiding rule: "the behaviour must be equivalent across all types for the same operations /
predicates".

## The work

Decide per gap whether it belongs on every type, then add it with tests on JVM, JS and native:

- `isEmpty`: only on `MpLocalTimeSlot`.
- `plus` / `minus`: only on `MpInstantRange` and `MpZonedDateTimeRange`.
- `compareTo(period)`: missing on `MpInstantRange` and `MpLocalTimeSlot`.
- `asDateRange`: on `MpInstantRange.Partial`, not on the two-ended `MpInstantRange`.
- `contains(MpLocalDateTime, zone)`: only on `MpZonedDateTimeRange`.
- open-vs-closed `intersects`: only on the two-ended date ranges.
- Shortcuts between the open and closed date `Partial`s as extension methods (maintainer, 2026-11-26: "we can think
  about adding shortcuts as extension methods").

Source: the operations x types matrix in the ULTRA-00343 plan (section 9).

## Acceptance

- [ ] Each gap filled or recorded as not wanted, with the reason.
- [ ] Same behaviour across types for the same operation; docs (ULTRA-00339 pages) updated.

## History

- 2026-11-26: created.
