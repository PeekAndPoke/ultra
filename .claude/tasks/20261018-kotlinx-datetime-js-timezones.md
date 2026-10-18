# ultra/datetime: own zone engine, then kotlinx-datetime 0.8

**Status:** IN PROGRESS — steps A (`829f6a59`) and B done 2026-10-18; `/feature-review` next
**Plan:** none — split out of `.claude/tasks-archive/2026-10/20261018-kotlin-2421-upgrade.md` (2026-10-18), where 0.8.0 was attempted and held
**Security-critical:** no — but it touches persistence (mutation-check bar, CLAUDE.md)

## Why

kotlinx-datetime is held at **0.6.2** (`buildSrc/src/main/kotlin/Deps.kt`, `KotlinX.datetime_version`); latest is
**0.8.0**. On 0.8.0 every named zone fails in the browser: `:ultra:datetime:jsBrowserTest` ran 595 of 1753 tests and
failed 518, all `IllegalTimeZoneException`. JVM (1792) and native (1752) stayed green.

**Root cause** (0.8.0 sources, `core/commonJs/src/internal/Platform.kt`, `core/js/src/PlatformSpecifics.kt`): since 0.7
the JS target reads the packed IANA database through `ZoneRulesProvider.getTzdbData()` — installed only by
`@js-joda/timezone` — and parses ALL zones eagerly into its own rules. Our Intl-backed js-joda provider
(`ultra/datetime/src/jsMain/kotlin/NativeTimeZones.kt`) is never consulted.

**The real defect is ours:** every `MpTimezone` overload delegates to its kotlinx `TimeZone` twin via
`timezone.kotlinx` (= `TimeZone.of(id)`) — 18 sites, e.g. `MpLocalDateTime.kt:163`, `MpInstant.kt:112/189/235`,
`MpLocalDate.kt:244/272/336`, `Kronos.kt:265-285`, `MpAbsoluteDateTime.kt:23`, `MpInstantRange.kt:112`,
`MpZonedDateTimeRange.kt:112`, `jvmMain/conversion.kt:105`. Plus direct `TimeZone.of(...)` calls the `.kotlinx`
grep misses: `MpDateTimeParser.kt:34` (bracketed zone in ISO strings), and the kotlinx serializers
`MpLocalDateSerializer.kt:30`, `MpLocalDateTimeSerializer.kt:30`, `MpZonedDateTimeSerializer.kt:30` (persistence —
mutation-check). Treat the grep count as a lower bound; let the compiler find the rest by making
`MpTimezone.kotlinx` unusable internally. So kotlinx is our zone engine. Upstream's pluggable
`TimeZoneDatabase` (PR Kotlin/kotlinx-datetime#610, merged 2026-08, unreleased) does not let us install a default either.

## Decisions (maintainer, 2026-10-18)

- **Rule:** kotlinx types appear in the public API only as **inputs** and in **explicit converters**. Nothing in
  ultra relies on kotlinx doing zone math. Exception: `Month` / `DayOfWeek` stay as return values for now.
- **Delegation flips:** `MpTimezone` overloads are the implementation; `TimeZone` overloads become input adapters
  (`= f(timezone.mp)`, needs only `.id`).
- **Own zone engine**, internal `expect`/`actual`, two primitives: offset at an instant; local date-time → offset
  with **java.time semantics** (gap: shift forward by the gap length; overlap: earlier offset). JVM/native actuals
  may use java.time / kotlinx internally; JS actual uses Intl (lift `offsetSecondsAt` / `localInfo` from
  `NativeTimeZones.kt`).
- **Converters for kotlinx types**, mirroring `jvmMain/conversion.kt` (both directions, `commonMain`):
  `MpInstant` ↔ `kotlin.time.Instant`, `MpLocalDate` ↔ `LocalDate`, `MpLocalDateTime` ↔ `LocalDateTime`,
  `MpLocalTime` ↔ `LocalTime`, `MpTimezone` ↔ `TimeZone` (exists), `MpDatePeriod`/`MpDateTimePeriod` ↔
  `DatePeriod`/`DateTimePeriod`. java.time converters stay unchanged. KDoc on `MpTimezone.kotlinx`: throws on JS for
  named zones unless js-joda tz data is loaded.
- **`installNativeTimezones()` is removed outright** (no deprecation; maintainer updates the consumers — no third
  parties).
- **Kraft js-joda loaders stay** (`kraft/addons/datetime/src/jsMain/kotlin/timezones.kt`), re-documented as "only for
  raw kotlinx `TimeZone` on JS".
- **`"Z"` → `"UTC"` accepted** for `LocalDateTimeSlumberer` writes; legacy `"Z"` must keep decoding.

## Steps

**A — engine and API, still on 0.6.2** (proves independence before the bump)

- [x] Internal zone engine, java.time semantics — `ultra/datetime/src/commonMain/kotlin/TimezoneEngine.kt`. Only
      "offset at an instant" is platform code (`platformOffsetSecondsAt`); local → instant, start of day and
      date-based arithmetic are ONE common algorithm on all targets (probe ±1 day, validate both candidate offsets —
      the Temporal-polyfill approach; gap → offset before; overlap → preferred, else earlier; start of day in a gap →
      binary-searched transition). Fixed-offset ids (`Z`, `+hh:mm`, `UTC+1`, …) are parsed in common code.
      Actuals: JVM + native `TimezoneEngineJvm.kt` / `TimezoneEngineNative.kt` (kotlinx `TimeZone.of(id).offsetAt`,
      so unknown ids still throw `IllegalTimeZoneException`); JS `TimezoneEngineJs.kt` (Intl, lifted from the
      deleted `NativeTimeZones.kt`).
- [x] All delegation sites flipped; `TimeZone` overloads are `= f(timezone.mp)` adapters. Also flipped: the parser
      (`MpDateTimeParser.kt:32-33`) and the three serializers' deserialize. Added the `MpTimezone` twins that were
      missing: `MpInstantRange.plus/minus(…, MpTimezone)`, `MpZonedDateTimeRange.contains(…, MpTimezone)`,
      `MpZonedDateTime.parse/tryParse(…, MpTimezone)`.
- [x] `MpTimezone.systemDefault` → `platformSystemDefaultZoneId()`; JS reads Intl. `TimeZone.mp` maps kotlinx's
      `"SYSTEM"` placeholder to `MpTimezone.systemDefault`.
- [x] kotlinx converters — `ultra/datetime/src/commonMain/kotlin/conversionKotlinx.kt` (`MpLocalDate.value` went
      `private` → `internal` for it).
- [x] `installNativeTimezones()` and the js-joda provider removed; `jsTest/ProjectConfig.kt` deleted. Kraft:
      `NativeTimeZoneInitializer` default removed (`kraft/core/src/jsMain/kotlin/index-jsMain.kt`),
      `Builder.timezones(…)` kept for the js-joda loaders; addon `installNativeTimezones()` / `nativeTimezones()`
      removed, loaders re-documented (`kraft/addons/datetime/src/jsMain/kotlin/timezones.kt`).
- [x] Parity tests: `commonTest/ZoneParityTable.kt` + `ZoneParitySpec.kt` (53 rows, all three targets);
      `jvmTest/ZoneParityJvmSpec.kt` re-derives every row from java.time AND sweeps the engine against java.time
      around every transition of all 604 JDK zones 1900–2040 (892 011 checks, 0 mismatches). Row generator:
      a throwaway `Gen.java` — regenerate rows with `ZonedDateTime.ofLocal` / `atStartOfDay` / `plusDays`.
- [x] Tests that built a NAMED kotlinx `TimeZone.of(…)` in `commonTest` now use `MpTimezone.of(…)` (impossible on JS
      without tz data, by design); the named-kotlinx-input coverage moved to `jvmTest/KotlinxTimeZoneInputSpec.kt`.
      `commonTest` keeps `TimeZone.UTC` / `TimeZone.of("+02:00")` inputs, which are data-free on every target.

**Found on the way:** the old Intl provider read BC years as positive era years (Intl `en-US` without `era`), so named zones were
wrong before the common era on JS. Fixed (`TimezoneEngineJs.kt` `wallYear`) and pinned by the `-0099` parity row.

**Step A evidence (2026-10-18, still on 0.6.2):** `:ultra:datetime:allTests` jvmTest 2439/0, linuxX64Test 1801/0,
jsBrowserTest 1804/0 (no provider, no `@js-joda/timezone`); `:ultra:slumber:jvmTest` 1241/0;
`:kraft:core-tests:jsBrowserTest` 412/0; `:kraft:addons:datetime:jsBrowserTest` 2/0. Compile sweep incl. linuxX64:
0 errors. Mutations, each RED for the right reason, each restored + `cmp`-verified: overlap → later offset; preferred
offset ignored; gap → after offset; start-of-day gap without the transition (first survived on the table — only the
sweep caught it — because every midnight gap in the table STARTED at midnight; added America/Toronto 1919-03-31,
gap 23:30→00:30, now RED); JS era handling removed (`-0099` row RED); JS system zone forced to UTC
(`SystemDefaultTimezoneJsSpec` RED — only bites on a non-UTC machine, this one is Europe/Berlin); each of the three
serializers' deserialize zone → UTC; parser `[zone]` → UTC.

**B — bump to 0.8.0**

- [x] Patch applied (`git apply -3`); only `Deps.kt` conflicted (the hold comment) → `datetime_version = "0.8.0"`,
      re-checked on Maven Central 2026-10-18 (the `-0.6.x-compat` builds are not for us). Step-A files moved
      `kotlinx.datetime.Instant` → `kotlin.time.Instant`.
- [x] Slumber `MpLocalDateCodec` / `MpLocalDateTimeCodec` / `MpZonedDateTimeCodec` → `MpTimezone`. Behaviour note: an
      invalid zone id no longer throws while the `ts` entry is missing (it awakes to null); with `ts` present it
      throws at `atZone` as before.
- [x] Persistence: `LocalDateTimeSlumberer` writes the shared `utc` constant ("UTC") instead of `TimeZone.UTC.id`;
      pins at `LocalDateTimeCodecSpec.kt` updated; new read tests prove `"UTC"` AND legacy `"Z"` both awake as UTC.
      `MpZonedDateTimeSerializer` output pinned by `MpZonedDateTimeSerializationSpec` (unchanged, green).
      Probed CLEAN: no `@Serializable` class in the repo holds a raw kotlinx date type (grep over property
      declarations after `@Serializable`); `ultra/i18n` and `ultra/common` mention kotlinx-datetime in comments only.
      Mutations RED + restored/`cmp`: slumberer writes "Z"; awaker mis-reads "Z"; each `Mp*` codec's zone → UTC;
      `MpZonedDateTimeSerializer` writes "UTC".
- [x] `Mp*` diff/period specs green on 0.8.0 (part of `:ultra:datetime:allTests`).

**Step B evidence (2026-10-18, 0.8.0):** `:ultra:datetime:allTests` jvmTest 2439/0, linuxX64Test 1801/0,
jsBrowserTest 1804/0 (no tz data on JS); `:ultra:slumber:jvmTest` 1243/0. Compile sweep incl. linuxX64: 0 errors,
0 warnings.

**C — docs** (follow-up task at archive time, but must land before the release): rewrite
`docs-site/src/pages/ultra/datetime/js-timezones.astro` and `docs-site/src/data/llms/datetime.md` (no setup needed;
loaders only for raw kotlinx zones), document the kotlinx converters.

## Acceptance

- [ ] `:ultra:datetime:allTests` green on all three targets, counts ≥ the 0.111.0 baseline (jsBrowserTest 1753,
      jvmTest 1792, linuxX64Test 1752) plus the parity tests
- [ ] Probe from step A holds on 0.8.0 too (no `@js-joda/timezone` in the JS test bundle)
- [ ] `:ultra:slumber:jvmTest` green; persistence items mutation-checked
- [ ] Compile sweep incl. `compileKotlinLinuxX64` clean
- [ ] `/feature-review` — public API + persisted format
- [ ] Docs follow-up task created
