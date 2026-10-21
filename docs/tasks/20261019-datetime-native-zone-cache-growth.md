# ultra/datetime: native zone cache grows with path variants of zone ids

**Status:** TODO
**Plan:** none — out-of-scope finding from round 3 of the `/feature-review` of `docs/tasks-archive/2026-10/20261019-datetime-own-epoch-conversions.md` (2026-10-19)
**Security-critical:** yes (unbounded memory growth from attacker-controlled input) → red-team note below

## Spec

**Pre-existing**, introduced by the round-1 MINOR batch of
ULTRA-00209 (kotlinx datetime js timezones) (the native per-id `TimeZone` cache; its
ledger row says "NOT measured, NOT independently verified").

`ultra/datetime/src/nativeMain/kotlin/TimezoneEngineNative.kt` caches every zone id that `TimeZone.of(id)` resolves,
keyed by the RAW string. kotlinx-datetime 0.8 native (`TzdbOnFilesystem.rulesForId`, reviewer-read from the kotlinx
sources) rejects only absolute paths and `..`; `Path.fromString` drops empty components and keeps `.`. So
`Europe//Berlin`, `Europe/Berlin/`, `Europe/./Berlin`, `Europe/././Berlin`, … all open the same TZif file and each
becomes a permanent cache entry; the copy-on-write insert also copies the whole map (O(n) per insert).

Scenario: a Kotlin/Native server decoding user-supplied zone ids (`MpZonedDateTimeSerializer` → `MpTimezone.of` →
`atZone`) grows the map without bound. JVM is NOT affected (no ultra cache; `ZoneRulesProvider` rejects the variants),
JS is not either (Intl rejects them). This repo's servers are JVM, so today's exposure is nil.

- [ ] Verify by experiment on linuxX64 (`TimeZone.of("Europe/./Berlin")` resolves?)
- [ ] Accept only canonical ids before caching (no empty / `.` components, or membership in `MpTimezone.supportedIds`),
      and/or cap the map; reject non-canonical ids with `IllegalTimeZoneException` like the JVM does
- [ ] Test on native: variants are rejected (or at least not cached), the map stays bounded
- [ ] Red-team scenario (collect only, never execute here): flood a native endpoint with distinct id spellings

Related MINORs from the same gate, for the maintainer list (not this task's scope unless chosen):
- JS: a wall time past the JS Date limit (last/first 18 h of the range, year ±275 760) yields offset 0 silently
  (`TimezoneEngineJs.kt` `intlOffsetSecondsAt`: `setUTCHours` → NaN → `toInt()` 0).
- JVM/native: on 999999999-12-31 in a zone with recurring rules the +1-day probe leaves java.time's year range →
  raw `java.time.DateTimeException`.

## Implementation notes

## Test evidence

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**
