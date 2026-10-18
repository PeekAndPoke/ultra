# kotlinx-datetime 0.8: keep zero-data time zones on Kotlin/JS

**Status:** TODO
**Plan:** none — split out of `.claude/tasks-archive/2026-10/20261018-kotlin-2421-upgrade.md` (2026-10-18), where 0.8.0 was attempted and held
**Security-critical:** no

## Why

kotlinx-datetime is held at **0.6.2** (`buildSrc/src/main/kotlin/Deps.kt`, `KotlinX.datetime_version`); latest is
**0.8.0**. Moving to it breaks every named time zone in the browser: on 2026-10-18 `:ultra:datetime:jsBrowserTest` ran
595 of its 1753 tests and failed 518, all `IllegalTimeZoneException: Invalid zone ID: <zone>`. JVM (1792) and native
(1752) stayed fully green.

**Root cause, read from the 0.8.0 sources** (`core/commonJs/src/internal/Platform.kt`, `core/js/src/PlatformSpecifics.kt`
at tag `v0.8.0`): since 0.7 the JS target no longer asks js-joda for zone RULES. It reads the raw packed IANA database
through `ZoneRulesProvider.getTzdbData()` — a function only `@js-joda/timezone` installs — and does the zone math in
its own Kotlin code (`TimeZoneRulesCommon`). Without that data, `timeZoneById` throws, and only UTC / fixed offsets
work.

`ultra/datetime` deliberately ships NO tz database on JS: `installNativeTimezones()`
(`ultra/datetime/src/jsMain/kotlin/NativeTimeZones.kt`) registers an Intl-backed js-joda `ZoneRulesProvider`, and the
whole JS suite runs against it (`ultra/datetime/src/jsTest/kotlin/ProjectConfig.kt`). 0.7+ bypasses that provider
entirely, so the design does not survive the upgrade as-is. `kraft/addons/datetime/src/jsMain/kotlin/timezones.kt`
offers opt-in js-joda data loaders on top — those would keep working, but are opt-in.

## Options (maintainer decides)

1. **Feed the new hook from Intl.** Install `ZoneRulesProvider.getTzdbData = () => ({ zones, links })` and generate the
   moment-packed format (`name|abbrs|offsets|indices|untils`) from `Intl.DateTimeFormat` by probing offsets and
   binary-searching transitions — the approach `NativeTimeZones.kt` already uses per zone. Keeps zero data. Open
   questions: `tzdb` in 0.8.0 is a top-level `val` computed once, so ALL zones are built up front (cost for ~600 zones
   × the probed range; check when K/JS runs that initializer); the range to cover (1970–2037?); and that this relies on
   an internal, undocumented hook that may change again.
2. **Depend on `@js-joda/timezone`** (full ~700 KB, or the 1970–2030 slice). Smallest change; reverses the zero-bundle
   design and makes every Kraft app heavier.
3. **Stay on 0.6.2** until upstream offers a pluggable zone source.

## Already known — the rest of the migration (saved as a patch)

`.claude/tasks/20261018-kotlinx-datetime-js-timezones.patch` (`git apply` on top of `fb1b93f8`) is the working-tree state
that compiled warning-free on 0.8.0 across the whole repo:

- `kotlinx.datetime.Clock` / `Instant` → `kotlin.time.Clock` / `Instant` (removed from kotlinx-datetime in 0.7); 8
  imports plus 22 fully-qualified `kotlinx.datetime.Clock.System` uses in Kraft test helpers (which also broke the
  no-FQCN rule).
- `LocalDate(year, monthNumber, dayOfMonth)` / `LocalDateTime(...)` constructors and `.monthNumber` / `.dayOfMonth`
  are deprecated → `month`/`day` and `month.number` inside `MpLocalDate` / `MpLocalDateTime`. Ultra's own public
  names stay.
- On the JVM `kotlinx.datetime.Month`/`DayOfWeek` are no longer aliases of `java.time` — `ConversionSpec` needed an
  alias import. **Consumer-visible:** JVM callers passing `java.time.Month` into `MpLocalDate.of(...)` break; the
  maintainer accepted consumer changes (2026-10-18) "as long as everything still works here".
- `Kronos.fromClock(clock)` takes `kotlin.time.Clock` afterwards — consumer-visible, same acceptance.

## Persistence — must be decided and mutation-checked (CLAUDE.md: persistence earns the security bar)

- **Wire value change:** `TimeZone.UTC.id` is `"UTC"` from 0.7 (was `"Z"`). `LocalDateTimeSlumberer`
  (`ultra/slumber/src/jvmMain/kotlin/builtin/datetime/kotlinx/LocalDateTimeCodec.kt`) writes `TimeZone.UTC.id`, so
  new records store `"UTC"` where old ones have `"Z"`. Its own `TODO(scan)` already calls `"Z"` the inconsistent
  value — every other UTC writer in slumber writes `"UTC"`. Pinned: `LocalDateTimeCodecSpec.kt:70` and `:92` went RED
  on 0.8.0, as they should. Decide: accept `"UTC"` (update the write pins, KEEP a read test proving legacy `"Z"` still
  decodes) or write the literal explicitly. Either way mutation-check both directions.
- `MpZonedDateTimeSerializer` writes `MpTimezone.id`, which already normalises `"Z"` → `"UTC"` (`MpTimezone.of`) —
  expected unchanged; confirm with a test, not by reading.
- 0.7 also changed the DEFAULT kotlinx serializers to `toString`/`parse` and 0.8 deprecates `TimeZone` serialization.
  The `Mp*` types use their own serializers (`@Serializable(with = …)`); grep for any `@Serializable` class holding a
  raw kotlinx type before claiming this is unaffected.
- 0.8.0 fixes bugs in `Instant.until` / `periodUntil` — check `Mp*` diff/period helpers against the specs.

## Acceptance

- [ ] Option chosen by the maintainer
- [ ] `:ultra:datetime:allTests` green on all three targets at the baseline counts (jsBrowserTest 1753, jvmTest 1792,
      linuxX64Test 1752 — the 0.111.0 numbers)
- [ ] The persistence items above decided, tested and mutation-checked
- [ ] `/feature-review` — this changes a persisted format and a public API
