# Docs: ultra/datetime zone engine and kotlinx converters

**Status:** TODO — must land before the 0.112.0 release
**Plan:** follow-up DOCS task of `.claude/tasks-archive/2026-10/20261018-kotlinx-datetime-js-timezones.md` (public API changed)
**Security-critical:** no

## Spec

The code is settled (gated 2026-10-18). Rewrite, don't patch (CLAUDE.md "Keep docs compact"):

- [ ] `docs-site/src/pages/ultra/datetime/js-timezones.astro` + its mirror `docs-site/src/data/llms/datetime.md`:
      `Mp*` types need NO time zone setup on JS (zone math via the browser's Intl); `installNativeTimezones()` is gone;
      the Kraft js-joda loaders are only for raw kotlinx `TimeZone.of(...)`; zone ids are case-sensitive everywhere
      (JS rejects case variants of known ids); semantics follow java.time (gap → shifted forward, overlap → earlier
      offset, date arithmetic keeps the offset)
- [ ] Document the kotlinx converters (`conversionKotlinx.kt`): `.kotlinx` / `.mp` pairs, the normalisations
      (`DatePeriod` months→years, `DateTimePeriod` time components, `LocalTime` → milliseconds)
- [ ] `kraft/addons/datetime/README.md`: drop `installNativeTimezones()`; loaders = raw kotlinx only
- [ ] kotlinx-datetime 0.8 consumer notes for the release: `kotlin.time.Instant`/`Clock`, JVM `Month` no longer
      `java.time.Month`, `Kronos.fromClock(kotlin.time.Clock)`, `installNativeTimezones()` removed, Kraft no longer
      installs a tz provider by default
- [ ] Grep `docs-site/`, `.claude/skills/`, READMEs for `installNativeTimezones`, `nativeTimezones`, `NativeTimeZones`
      — zero hits after

## Implementation notes

## Test evidence

- [ ] docs-site builds
