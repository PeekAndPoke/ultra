# ULTRA-00339: Docs: ultra/datetime after ULTRA-00210 and ULTRA-00211 (Mp enums, zoned date-time keeps its instant)

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00260 (Epic: Docs backlog)

## Why this exists

ULTRA-00210 and ULTRA-00211 changed public API and behaviour of `ultra/datetime` (`MpMonth`, `MpDayOfWeek`, removed
`offsetAt`, `MpZonedDateTime` built from an instant, equality and ordering, `isSameInstant`). Part of the docs sweep the
maintainer wants after the foundation modules and Slumber are done (2026-11-14). The 24:00 value of `MpLocalTime` and
the `MpLocalDateRange` cap are still to come on the datetime track: write the docs after those are merged too.

## The work

Pages `docs-site/src/pages/ultra/datetime/*` and the mirror `docs-site/src/data/llms/datetime.md`, which move together.

From ULTRA-00210:

- `docs-site/src/pages/ultra/datetime/mp-local-date.astro` 46 (`date.month // Month.MARCH`), 48 (`date.dayOfWeek //
  DayOfWeek.SATURDAY`): now `MpMonth.MARCH`, `MpDayOfWeek.SATURDAY`.
- `docs-site/src/pages/ultra/datetime/mp-local-date-time.astro` 50 (`Month.MARCH`), 56 (`DayOfWeek.SATURDAY`): same.
- `docs-site/src/pages/ultra/datetime/mp-zoned-date-time.astro` 66 (`Month.JUNE`), 72 (`DayOfWeek.SUNDAY`): same.
- `docs-site/src/pages/ultra/datetime/mp-timezone.astro` 49-50 (`berlin.offsetAt(summer) // +02:00`): `offsetAt` is
  removed; `offsetSecondsAt` / `offsetMillisAt`.
- Still compile, could show the Mp types: `mp-local-date.astro` 32, 117-121; `mp-local-date-time.astro` 32;
  `mp-zoned-date-time.astro` 131-132; `index.astro` 94 (kotlinx `Month.MARCH` / `DayOfWeek.X` inputs).
- `docs-site/src/data/llms/datetime.md` 155 (`berlin.offsetAt(instant) // +02:00 in summer…`): `offsetAt` is removed
  (corrected in review round 1; my first list missed it). Line 74 (`date.dayOfWeek // SATURDAY`) is still true by
  name; lines 86, 89 (kotlinx `DayOfWeek.MONDAY` inputs) still compile.

From ULTRA-00211:

- `docs-site/src/pages/ultra/datetime/js-timezones.astro` 31-36: still true, but add that an `MpZonedDateTime` built
  from an instant keeps it, so both sides of an overlap are distinct values (`MpInstant.atZone`), and that a time built
  from a gap reads back as the resolved time (02:30 → 03:30 in `.hour`/`.datetime`).
- `docs-site/src/pages/ultra/datetime/mp-zoned-date-time.astro`: two lines are false, and were before this change
  (corrected in delta round 2; my first list said none): line 41 shows
  `MpZonedDateTime.parse("2025-06-15T14:00:00+02:00[Europe/Berlin]")`, which the parser refuses (probe: "Could not parse
  MpZonedDateTime from '…'"); lines 155-156 show `toIsoString()` as `"2025-06-15T14:00:00.000+02:00[Europe/Berlin]"`, but
  it never carried the offset (probe: `2025-06-15T14:00:00.000[Europe/Berlin]`). A constructor or `copy` is not shown
  anywhere; worth a line that `MpZonedDateTime` is built with `atZone` / `of`, and that `toString()` now shows the offset.
- `docs-site/src/data/llms/datetime.md`: no line becomes false.

## Acceptance

- Every statement and example on the datetime pages is true against the merged code; `pnpm astro build` is green.

## History

- 2026-11-14: created with the archive of ULTRA-00210 and ULTRA-00211.
