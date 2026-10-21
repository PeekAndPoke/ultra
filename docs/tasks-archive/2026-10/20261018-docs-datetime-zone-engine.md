# Docs: ultra/datetime zone engine and kotlinx converters

**Status:** DONE (archived 2026-10-18)
**Plan:** follow-up DOCS task of ULTRA-00209 (kotlinx datetime js timezones) (public API changed)
**Security-critical:** no

## Spec

The code is settled (gated 2026-10-18). Rewrite, don't patch (CLAUDE.md "Keep docs compact"):

- [x] `docs-site/src/pages/ultra/datetime/js-timezones.astro` + its mirror `docs-site/src/data/llms/datetime.md`:
      `Mp*` types need NO time zone setup on JS (zone math via the browser's Intl); `installNativeTimezones()` is gone;
      the Kraft js-joda loaders are only for raw kotlinx `TimeZone.of(...)`; zone ids are case-sensitive everywhere
      (JS rejects case variants of known ids); semantics follow java.time (gap → shifted forward, overlap → earlier
      offset, date arithmetic keeps the offset)
- [x] Document the kotlinx converters (`conversionKotlinx.kt`): `.kotlinx` / `.mp` pairs, the normalisations
      (`DatePeriod` months→years, `DateTimePeriod` time components, `LocalTime` → milliseconds)
- [x] `kraft/addons/datetime/README.md`: drop `installNativeTimezones()`; loaders = raw kotlinx only
- [ ] → MOVED to the 0.112.0 release (no CHANGELOG.md exists; the notes go wherever the release puts them): kotlinx-datetime 0.8 consumer notes: `kotlin.time.Instant`/`Clock`, JVM `Month` no longer
      `java.time.Month`, `Kronos.fromClock(kotlin.time.Clock)`, `installNativeTimezones()` removed, Kraft no longer
      installs a tz provider by default
- [x] Grep `docs-site/`, `.claude/skills/`, READMEs for `installNativeTimezones`, `nativeTimezones`, `NativeTimeZones`
      — zero hits after

## Implementation notes

- Rewritten: `js-timezones.astro` (no setup; java.time semantics; case-sensitive ids; loaders for raw kotlinx only, with
  their Kraft builder names) and its mirror section in `llms/datetime.md`; new "kotlinx-datetime interop" section in
  `llms/datetime.md` and a converter snippet in `datetime/getting-started.astro`; asides in `mp-timezone.astro` and
  `kraft/getting-started.astro` (+ `llms/kraft.md`, `llms.txt`); `kraft/addons/datetime/README.md`.
- Every behavioural claim is backed by a test: gap/overlap/start-of-day rows in `ZoneParityTable.kt`, case rejection in
  `FixedOffsetIdSpec.kt`, converter normalisations in `ConversionKotlinxSpec.kt`.
- Prose-only change: no `/feature-review` (review-loop "scope by risk").

## Test evidence

- `pnpm run build` in `docs-site/`: 114 pages, no errors (2026-10-18).
- Grep for `installNativeTimezones|nativeTimezones|NativeTimeZones|NativeTimeZoneInitializer|timezone provider` over
  `docs-site/src`, `.claude/skills`, `kraft/addons/datetime/README.md`, `README.MD`: zero hits.

- [x] docs-site builds
