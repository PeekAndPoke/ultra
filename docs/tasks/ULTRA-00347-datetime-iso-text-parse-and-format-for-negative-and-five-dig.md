# ULTRA-00347: datetime: ISO text, parse and format for negative and five-digit years (dates BC)

> **Status (2026-11-14): DESIGNED.** Its own ticket, built in the same work package as ULTRA-00343 (no sentinels),
> directly after it (maintainer, 2026-11-14: "separate ticket but needs to be done in this work package").
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))
> **Plan:** ULTRA-00316 (close the open epics from the foundation upwards)

**Found by:** the analysis in ULTRA-00343 (sentinels versus open ends), 2026-11-14.

## Why this exists

The maintainer's history projects need dates BC. Once ULTRA-00343 removes Genesis and Doomsday, such dates work
as values. Their text form does not:

```kotlin
MpLocalDate.of(-500, 3, 1).toIsoString()   // "-500-03-01"; ISO 8601 wants "-0500-03-01"
MpLocalDate.parse("-500-03-01")            // throws for the years -1 to -999
date.format("yyyy")                        // prints "0000" for every year BC
```

So `parse(toIsoString())` does not round-trip for the years -1 to -999, and years beyond 9999 need the sign and
width rules of ISO 8601 as well. The text is also stored: the `human` field of all four value types.

## Decide before implementing

The track writes a short plan first, with examples, for the maintainer to confirm: the written form for negative and
five-digit years on all four types (`MpInstant`, `MpZonedDateTime`, `MpLocalDateTime`, `MpLocalDate`); what `parse`
accepts (also what older versions wrote); what the format patterns print (`yyyy`, `yy`, `y`; an era pattern?); what a
stored `human` text written by an older version reads as; JVM, JS and native agreeing.

## The work

- Characterization first: what each platform writes and reads today for the years -10000, -1000, -999, -1, 0, 1,
  999, 9999, 10000, on all four types.
- Then the decided form, with round trips pinned on JVM, JS and native, and mutation checks for the codecs.

## Acceptance

- `parse(toIsoString(x)) == x` for every representable year on all four types and all platforms; the format
  patterns print what was decided; the gate is closed.

## History

- 2026-11-14: created on the maintainer's word, from question 4 of the ULTRA-00343 plan.
