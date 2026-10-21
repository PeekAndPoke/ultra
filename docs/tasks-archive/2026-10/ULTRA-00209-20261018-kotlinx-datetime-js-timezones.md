# ULTRA-00209: ultra/datetime: own zone engine, then kotlinx-datetime 0.8

**Status:** DONE (archived 2026-10-18) — A `829f6a59`, B `fed9393d`, gate PASS, maintainer-chosen MINOR batch applied
**Plan:** none — split out of ULTRA-00208 (kotlin 2421 upgrade) (2026-10-18), where 0.8.0 was attempted and held
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
0 warnings. Final-tree run (fed9393d): `:kraft:core-tests:jsBrowserTest` 412/0, `:kraft:addons:datetime:jsBrowserTest`
2/0 and the other 9 touched Kraft addon suites green. Mutation: dropping `installTimezones1970to2030()` from
`kraft/addons/datetime/src/jsTest/kotlin/TimezonesSpec.kt` → RED, so the js-joda loaders work on 0.8.0.

**C — docs** (follow-up task at archive time, but must land before the release): rewrite
`docs-site/src/pages/ultra/datetime/js-timezones.astro`, `docs-site/src/data/llms/datetime.md` and
`kraft/addons/datetime/README.md` (no setup needed; loaders only for raw kotlinx zones), document the kotlinx
converters; correct ULTRA-00034 (kraft timezone decoupling) (Kraft no longer installs zones by default).

## Acceptance

- [ ] `:ultra:datetime:allTests` green on all three targets, counts ≥ the 0.111.0 baseline (jsBrowserTest 1753,
      jvmTest 1792, linuxX64Test 1752) plus the parity tests
- [ ] Probe from step A holds on 0.8.0 too (no `@js-joda/timezone` in the JS test bundle)
- [ ] `:ultra:slumber:jvmTest` green; persistence items mutation-checked
- [ ] Compile sweep incl. `compileKotlinLinuxX64` clean
- [ ] `/feature-review` — public API + persisted format
- [ ] Docs follow-up task created

## Review record — the LEDGER (/feature-review)

Change set `52ab08b1..fed9393d` (829f6a59, fed9393d). Round 1 BLIND, three `reviewer-high` (implementation & style,
domain expert, security). **Zero CRITICAL/MAJOR in the change set → clean round, loop ends.** Open MINORs go to the
maintainer as a list (review-loop: not applied as an unreviewed batch). Reviewers I = implementation, D = domain,
S = security.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `MpZonedDateTime.kt:151` | Instant in the 2nd half of a fall-back overlap does not survive `atZone` / every deserialize (1h early); re-save writes wrong `ts` (D, I) | out-of-scope → follow-up task | Verified real; PRE-EXISTING (kotlinx gave the same earlier offset). `docs/tasks/20261018-mpzoneddatetime-overlap-offset.md` |
| 1 | MINOR | `MpZonedDateTime.kt:313` | `MpZonedDateTime.atStartOfDay()` (copy) disagrees with `MpLocalDate.atStartOfDay(tz)` in gaps (D) | out-of-scope → same follow-up task | Pre-existing; same root as the row above |
| 1 | MINOR | `TimezoneEngineJs.kt:14,57` | Cache is a plain `{}`: ids like `constructor` hit `Object.prototype` → JS `TypeError` instead of `IllegalTimeZoneException` (S, D, I) | fixed (MINOR batch) | `js("Object.create(null)")`; mutation → `'constructor' is rejected` RED |
| 1 | MINOR | `TimezoneEngineJs.kt:65` | Intl accepts ids case-insensitively → `europe/berlin` works in the browser, persists, then throws on JVM; cache grows per case variant (S, D) | fixed — maintainer: REJECT case variants | Chrome probe: Intl case-folds AND canonicalises aliases (`US/Pacific`→`America/Los_Angeles`, `Asia/Kolkata`→`Asia/Calcutta`), so `resolvedOptions()` cannot tell; check against `MpTimezone.supportedIds` (no lowercase collisions, pinned). Mutation → `'europe/berlin' is rejected` RED |
| 1 | MINOR | `TimezoneEngineJs.kt:19` | Instants beyond JS `Date` range (±8.64e15 ms) → native `RangeError`; a stored huge `ts` breaks decoding in the browser (S) | fixed (MINOR batch) | `require` → `IllegalArgumentException`; mutation → `TimezoneEngineJsSpec` RED |
| 1 | MINOR | `MpTimezone.kt:15` | `MpTimezone.of("SYSTEM")` now fails on JS (only `TimeZone.mp` maps it) (D) | fixed (MINOR batch) | `MpTimezone.of("SYSTEM")` → `systemDefault`; `systemDefault` builds directly (via `of` a platform reporting "SYSTEM" would recurse); mutation → SYSTEM test RED |
| 1 | MINOR | `TimezoneEngineNative.kt:8` | kotlinx 0.8 native re-reads the TZif file per `TimeZone.of`; the engine calls it 3–4x per conversion, ~20x for a start-of-day in a gap (I, D) | fixed (MINOR batch) | Copy-on-write `AtomicReference<Map>` of resolved zones in `TimezoneEngineNative.kt`; NOT measured, NOT independently verified that kotlinx re-reads the file |
| 1 | MINOR | `TimezoneEngine.kt:151-187` | Fixed-offset parser mostly unpinned (seconds, separator rules, 18h bound, `UT` survive mutation) (I) | fixed (MINOR batch) | `FixedOffsetIdSpec` (all targets) + `FixedOffsetIdJvmSpec` (every row vs `ZoneId.of`). Mutations RED: seconds ignored, `UT` dropped, 18h total bound removed. The `hours > 18` clause SURVIVED — redundant with the total bound, deleted |
| 1 | MINOR | `TimezoneEngine.kt:19-20`, `ZoneParityTable.kt:5`, slumber `MpTimezoneCodec.kt:18` | Three comments factually wrong (native DOES use kotlinx tz data; spec name `ZoneParityJvmSpec`; unknown id no longer fails "at `.kotlinx`") (I) | fixed (MINOR batch) | All three corrected |
| 1 | MINOR | `conversionKotlinx.kt:13-56`, `extensions.kt:20` | New converters and the `"SYSTEM"` branch untested (I) | fixed (MINOR batch) | `ConversionKotlinxSpec` |
| 1 | MINOR | `conversionKotlinx.kt:32` | `MpLocalTime.kotlinx` throws for negative values `MpLocalTime` itself produces (`ofMilliSeconds` does not wrap) (D) | fixed (MINOR batch) | `ofMilliSeconds` uses `mod` (wraps below midnight); mutation → wrap test RED |
| 1 | MINOR | `KotlinxTimeZoneInputSpec.kt:16` | A test that exercises only kotlinx (I) | fixed (MINOR batch) | Removed |
| 1 | MINOR | `TimezoneEngine.kt:153` | `"Z"` branch unreachable (`MpTimezone.of("Z")` → UTC, constructor private) (I, surplus) | fixed (MINOR batch) | Removed; KDoc says `Z` never arrives |
| 1 | MINOR | `extensions.kt:27,48` | `offsetAt` still RETURNS kotlinx `UtcOffset` — against the inputs-and-converters rule (D) | out-of-scope → follow-up task | Maintainer: keep for now, eliminate ALL kotlinx return types in `docs/tasks/20261018-datetime-no-kotlinx-return-types.md` (breaking, accepted) |
| 1 | MINOR | `kraft/addons/datetime/README.md:10-33`, ULTRA-00034 (kraft timezone decoupling) | Stale: still recommend the removed `installNativeTimezones()` / say Kraft installs native zones by default (D, I) | fixed (task doc) / follow-up (README) | ULTRA-00034 (kraft timezone decoupling) marked superseded + archived to `docs/tasks-archive/2026-06/`; README → `docs/tasks-archive/2026-10/20261018-docs-datetime-zone-engine.md` (done) |
| 1 | MINOR | step B evidence | Kraft suites / js-joda loaders never run on 0.8.0 (D, I) | reject | Final-tree run exists (412/0, 2/0, 9 addon suites green) — was only missing from this file, now recorded; loader mutation RED |
| 1 | MINOR | `MpInstant.kt:227` | Periods added component by component, resolving after each (≠ java's single resolution) (D) | reject | Documented behaviour: KDoc "Each component is added individually"; pre-existing |

**Probed and CLEAN** (reviewers, recorded so nobody re-treads it): `resolve()` branch analysis incl. 24h gaps and
overlaps (Apia 2011, Sitka 1867); ±1-day window bound by |offset| ≤ 18h; `transitionAfter` invariants and
termination; java.time parity of `startOfDay` and date-based `plus`; fixed-offset parser accept/reject set vs
`ZoneId.of` (~20 strings); `UtcOffset` component signs; nanoseconds; ReDoS in both regexes (bounded / linear); binary
search can't overflow or loop; JVM/native exception types stay `IllegalTimeZoneException`; slumber awake change is
lenient, not lossy; no raw kotlinx zone use left in browser code paths (kraft/, funktor/, ultra/).
Coordinator note: the JVM sweep's 2 "offset at" checks per transition compare java.time with kotlinx-JVM, which
wraps java.time — not independent evidence; the other ~21 per transition are.

**Verdict: gate PASS** — no open CRITICAL/MAJOR in the change set; the pre-existing MAJOR has its follow-up task.

**MINOR batch** (maintainer chose the fixes 2026-10-18; per review-loop no re-review round): final-tree run
`:ultra:datetime:allTests` jvmTest 2483/0, linuxX64Test 1843/0, jsBrowserTest 1847/0; `:ultra:slumber:jvmTest` 1243/0;
`:kraft:addons:datetime:jsBrowserTest` 2/0. Every mutation restored + `cmp`-verified.

**Follow-ups:** docs `docs/tasks-archive/2026-10/20261018-docs-datetime-zone-engine.md` (done);
`docs/tasks/20261018-mpzoneddatetime-overlap-offset.md`; `docs/tasks/20261018-datetime-no-kotlinx-return-types.md`;
performance `docs/tasks-archive/2026-10/20261018-datetime-js-performance.md` (from spross, before 0.112.0). No red-team task: not
security-critical. The applied patch is archived next to this file.

## History

- 2026-10-21: numbered ULTRA-00209 (was `docs/tasks-archive/2026-10/20261018-kotlinx-datetime-js-timezones.md`).
