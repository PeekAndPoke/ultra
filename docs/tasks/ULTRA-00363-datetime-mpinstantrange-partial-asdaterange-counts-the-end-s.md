# ULTRA-00363: Datetime: MpInstantRange.Partial.asDateRange counts the end's date

> **Status (2026-11-26): NOT STARTED.** 

## Why this exists

Found by the ULTRA-00343 cross-type check (2026-11-26). `MpInstantRange.Partial.asDateRange` turns a half-open instant
range into a closed date range that includes the end's date:
`[2024-01-01T00:00Z, 2024-01-03T00:00Z)`, two days, becomes `[2024-01-01..2024-01-03]`, three days. The maintainer:
a separate ticket.

## The work

- RED test first with the example above (and an end not on midnight, and a null end).
- The end date comes from the last instant before `to` (half-open), in the zone the conversion uses.
- Check every other instant-to-date conversion for the same off-by-one.

## Acceptance

- [ ] The example gives two days; tests on JVM, JS and native; CHANGELOG entry (behaviour change).

## History

- 2026-11-26: created.
