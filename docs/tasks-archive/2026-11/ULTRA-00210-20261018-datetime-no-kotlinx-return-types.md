# ULTRA-00210: ultra/datetime: no kotlinx types as return values

> **Status (2026-11-14): DONE.** Merged 14376f44 with ULTRA-00211. DOCS follow-up: ULTRA-00339.
>
> **Part of:** ULTRA-00255 (Ultra libraries epic)

**Plan:** none — maintainer decision during the `/feature-review` of ULTRA-00209 (kotlinx datetime js timezones) (2026-10-18): "really eliminate the return of any kotlinx datetime types. This will be a breaking change but it is fine."
**Security-critical:** no

## Spec

Rule (maintainer, 2026-10-18): kotlinx types appear in ultra/datetime's public API only as INPUTS and in explicit
CONVERTERS (`conversionKotlinx.kt`, `MpTimezone.kotlinx`, `TimeZone.mp`). This task removes the remaining return values.
Breaking for consumers — accepted; the maintainer updates them (no third parties).

Known return sites (read 2026-10-18; re-grep before starting, treat as a lower bound):

- `Month` — `MpLocalDate.month`, `MpLocalDateTime.month`, `MpZonedDateTime.month`
- `DayOfWeek` — `.dayOfWeek` on the same three
- `UtcOffset` — `MpTimezone.offsetAt`, `TimeZone.offsetAt` (`extensions.kt`); `offsetSecondsAt` / `offsetMillisAt`
  already return plain numbers
- anything else a public-signature scan finds (a script did this once: list non-private declarations whose RETURN type
  is a kotlinx type)

- [x] Own `MpMonth` / `MpDayOfWeek` (or similar), with converters both ways; decide the names with the maintainer
- [x] Inputs keep accepting kotlinx `Month` / `DayOfWeek` (rule: inputs are fine) — or switch them too? decide: both (maintainer)
- [x] `offsetAt` returns an ultra type or is removed in favour of `offsetSecondsAt`: removed (maintainer)
- [x] Consumer migration notes for the release: "Changelog text" below

## Plan as built

Maintainer (2026-11-13): everything datetime returns is an Mp type; `MpMonth` and `MpDayOfWeek` are new; no backward
compatibility (no deprecated bridges); the converters to and from kotlinx and java.time stay and cover the new types.
Shipped together with ULTRA-00211.

Re-grepped 2026-11-13 (non-private declarations whose return type is a kotlinx type, converters excluded): exactly the
sites the ticket lists. `EpochMath`/`TimezoneEngine` are `internal`.

**Commits to (expensive to reverse marked ⚠):**

- ⚠ `enum class MpMonth { JANUARY … DECEMBER }` with `number` (1-12) and `MpMonth.of(number)`; ⚠ `enum class
  MpDayOfWeek { MONDAY … SUNDAY }` with `isoDayNumber` (1-7) and `MpDayOfWeek.of(isoDayNumber)`. Same entry names
  and order as kotlinx and java.time, so `name`, `ordinal` and `toString()` are unchanged for callers. Both
  `@Serializable` (by name, like any enum) so they can be fields.
- ⚠ `month: MpMonth` and `dayOfWeek: MpDayOfWeek` on `MpLocalDate`, `MpLocalDateTime`, `MpZonedDateTime`.
- ⚠ `MpTimezone.offsetAt(MpInstant): UtcOffset` and `TimeZone.offsetAt(MpInstant): UtcOffset` (`extensions.kt`) are
  removed: `offsetSecondsAt` / `offsetMillisAt` already return plain numbers. No new offset type.
- Converters, both ways: `Month.mp`, `MpMonth.kotlinx`, `DayOfWeek.mp`, `MpDayOfWeek.kotlinx` (common);
  `java.time.Month.mp`, `MpMonth.jvm`, `java.time.DayOfWeek.mp`, `MpDayOfWeek.jvm` (JVM).
- Inputs: every function that takes a kotlinx `Month` or `DayOfWeek` also takes the Mp type (`of(year, month, day)`,
  `atStartOfNext/Previous(dayOfWeek)`, `getDatesInMonth(weekdays)`). The kotlinx-typed inputs stay, as the ticket's
  rule allows and as every `TimeZone`/`MpTimezone` pair in the API already does.

**Decided here (the ticket left it open):** the names `MpMonth` / `MpDayOfWeek` (maintainer's names); the members
`number` / `isoDayNumber` (kotlinx's names); `of(...)` throws `IllegalArgumentException` outside 1-12 / 1-7 (no
sensible fallback for a month number); inputs keep the kotlinx overloads (above); `offsetAt` removed rather than given
a new type; `UtcOffset.totalMillis` (an extension on a kotlinx receiver returning `Long`) stays.

**Behaviour changes without a compile signal:** `date.month.equals(Month.APRIL)` (an explicit `equals` call, not
`==`) silently becomes `false`; anything that stores the value via `toString()`/`name` is unchanged.

## Implementation notes

- New `ultra/datetime/src/commonMain/kotlin/MpMonth.kt` and `MpDayOfWeek.kt`: `@Serializable` enums with `number` /
  `isoDayNumber`, and `of(n)` (throws `IllegalArgumentException`), `ofOrNull(n)`, `ofOrDefault(n, default)` (the last two
  added on the maintainer's word, 2026-11-13).
- Converters: `MpMonth.kotlinx`, `Month.mp`, `MpDayOfWeek.kotlinx`, `DayOfWeek.mp` (`conversionKotlinx.kt`);
  `MpMonth.jvm`, `java.time.Month.mp`, `MpDayOfWeek.jvm`, `java.time.DayOfWeek.mp` (`jvmMain/conversion.kt`).
  kotlinx-datetime 0.8.0 has its own `Month`/`DayOfWeek` classes on the JVM (no typealias to java.time), so both `.mp`
  extensions coexist.
- `month` / `dayOfWeek` return the Mp types on `MpLocalDate`, `MpLocalDateTime`, `MpZonedDateTime`.
- Mp-typed inputs added beside the kotlinx ones (maintainer: keep both): `MpLocalDate.of(year, MpMonth, day)`,
  `MpLocalDateTime.of(year, MpMonth, …)`, `atStartOfNext/Previous(MpDayOfWeek)` on `MpLocalDate` and
  `MpZonedDateTime`, `MpLocalDate.getDatesInMonth(Iterable<MpDayOfWeek>)` (`@JvmName("getDatesInMonth_by_mp_weekdays")`).
  The kotlinx overloads delegate to them.
- Removed: `MpTimezone.offsetAt(MpInstant): UtcOffset`, `TimeZone.offsetAt(MpInstant): UtcOffset`. Kept:
  `UtcOffset.totalMillis`.
- Tests inside the module adapted mechanically: `offsetAt(…) shouldBe UtcOffset(hours = h)` → `offsetSecondsAt(…)
  shouldBe h * 3600` (`ExtensionsSpec`, `KotlinxTimeZoneInputSpec`, `MpLocalDateSpec`), and `month shouldBe Month.X`
  / `dayOfWeek shouldBe DayOfWeek.X` → the Mp entries (`MpLocalDateSpec`, `MpLocalDateTimeSpec`,
  `MpZonedDateTimeSpec`). Those `shouldBe` lines compiled and failed at runtime: kotest's `shouldBe` is untyped, which
  is the no-compile-signal case the changelog names.

### Docs lines for the DOCS follow-up (become false)

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

## Test evidence

- `MonthAndDayOfWeekCharacterizationSpec` (commonTest): written before the change against `name` / `ordinal` only,
  green against the kotlinx returns (10 / 0) and unchanged and green against the Mp returns (10 / 0): the values did
  not change, only the types.
- `MpMonthAndDayOfWeekSpec` (commonTest, 18 tests): entries and numbers; `of` / `ofOrNull` / `ofOrDefault` for every
  valid number and for 0, 13 (8), -1, `Int.MIN_VALUE`, `Int.MAX_VALUE`; kotlinx converters both ways for every
  entry; `month`/`dayOfWeek` against the kotlinx values for every day of 2024-2025; the Mp inputs give the same results
  as the kotlinx inputs; kotlinx Json of a data class with both as fields (`{"month":"APRIL","dayOfWeek":"TUESDAY"}`).
- `MpMonthAndDayOfWeekJvmSpec`: java.time converters both ways for every entry.
- `ultra/slumber` `MpMonthAndDayOfWeekCodecSpec`: every month × day of week as fields round-trips, stored by name.
- After (with ULTRA-00211): datetime jvmTest 2679 / 0, jsBrowserTest 2028 / 0, linuxX64Test 2020 / 0; slumber jvmTest
  1253 / 0; `compileTestKotlinLinuxArm64` and `compileTestKotlinMingwX64` compile (their tests cannot run on this host).
- Compile sweep of the whole repo (`compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs
  compileKotlin compileTestKotlin --continue`): no errors; no call site outside the module needed a change for this
  ticket (the kotlinx inputs stay).
- Mutation-checked (JVM; each restored with `cmp`):
  - `MpMonth.kotlinx` shifted by one → red 1 (kotlinx converters);
  - `Month.mp` shifted by one → red 48 (every date property);
  - `DayOfWeek.mp` reversed → red 15 (characterization, Mp inputs);
  - `java.time.Month.mp` shifted → red 1 (java converters);
  - `MpMonth.ofOrNull` range `1..13` → red 1 (`of(13)`); a first attempt (`0..12` with `getOrNull`) was an
    equivalent mutant and survived, as expected.

## Changelog text

- `ultra/datetime`, breaking: new `MpMonth` and `MpDayOfWeek` enums (`number` / `isoDayNumber`; `of(n)`, which throws
  for a bad number, `ofOrNull(n)`, `ofOrDefault(n, default)`; converters `kotlinx` / `mp`, on the JVM `jvm` / `mp`).
  `month` and `dayOfWeek` on `MpLocalDate`, `MpLocalDateTime` and `MpZonedDateTime` return them instead of
  kotlinx's `Month` / `DayOfWeek`. Every function that takes a month or a day of week accepts both (ULTRA-00210).
- `ultra/datetime`, behaviour change (no compile signal): a comparison of `month` / `dayOfWeek` with a kotlinx entry
  through an untyped API compiles and is now false: `date.month.equals(Month.APRIL)`, kotest's
  `date.month shouldBe Month.APRIL`, `assertEquals(Month.APRIL, date.month)`, a lookup in a `Set<Any>` or `Map<Any, …>`.
  `==` and `when` against kotlinx entries do not compile. `name`, `ordinal` and `toString()` are unchanged, so values
  stored by name keep their meaning (ULTRA-00210).
- **Before you update** (removed API): `MpTimezone.offsetAt(MpInstant)` and `TimeZone.offsetAt(MpInstant)`, which
  returned kotlinx's `UtcOffset`: use `offsetSecondsAt` / `offsetMillisAt`. Changed return types: `month` → `MpMonth`,
  `dayOfWeek` → `MpDayOfWeek` (use `.kotlinx` where a kotlinx value is needed) (ULTRA-00210).

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR | `ExtensionsSpec.kt:35-48`, `KotlinxTimeZoneInputSpec.kt:53-66` | the "…offsetSecondsAt, by hours" tests repeat the tests below them | FIXED | verified identical instants and values; both deleted |
| 1 | MINOR | `MpLocalDateTime.kt:5` | unused import `kotlinx.datetime.DayOfWeek` | FIXED | removed (`value.dayOfWeek.mp` needs no type import) |
| 1 | MINOR | ticket | `llms/datetime.md:155` (`offsetAt`) missing from the docs list | FIXED | added; the "nothing becomes false" sentence corrected |

Stayed CLEAN (round 1, blind, reviewer-opus-high; implementation, domain and security charters all CLEAN, no
CRITICAL/MAJOR): no remaining caller of the removed API in the repo; old stored documents decode as before.

**Verdict:** round 1 CLEAN (no CRITICAL, no MAJOR); MINORs applied above.

## History

- 2026-10-21: numbered ULTRA-00210 (was `docs/tasks/20261018-datetime-no-kotlinx-return-types.md`).
- 2026-11-13: implemented on track A, batch 2 (with ULTRA-00211).
- 2026-11-14: DONE. Merged 14376f44 with ULTRA-00211. DOCS follow-up: ULTRA-00339.
