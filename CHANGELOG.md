# Changelog

## 0.112.0

### Breaking changes

- **kotlinx-datetime 0.8.0** (was 0.6.2). `Instant` and `Clock` now come from `kotlin.time`;
  `Kronos.fromClock(...)` takes a `kotlin.time.Clock`. On the JVM, kotlinx `Month` / `DayOfWeek` are no longer
  aliases of the `java.time` types.
- **`installNativeTimezones()` is removed** (ultra and the Kraft datetime addon), and Kraft no longer installs a time
  zone provider by default. `Mp*` types need no setup in the browser any more: ultra resolves zones itself, through the
  browser's own IANA database (`Intl`), with no bundled data. The Kraft js-joda loaders (`fullTimezones()`, …) remain,
  for raw kotlinx `TimeZone.of(...)` only.
- **Zone ids are case-sensitive on JS too**: case variants of known ids (`europe/berlin`) throw
  `IllegalTimeZoneException`, as on the JVM.
- **`MpLocalTime` is `Int`-based**: `ofMilliSeconds(Int)`, `ofSecondOfDay(Int)`, `inWholeMilliSeconds(): Int`,
  `MpLocalTimeSlot.ofSecondsOfDay(Int, Int)`. Values outside a day now WRAP into it, also negative ones
  (`ofMilliSeconds(-3_600_000)` is 23:00). The stored value is unchanged (still a `Long` on the wire).
- `TimeZone.UTC` is written as `"UTC"` (was `"Z"`) by the slumber codec for raw kotlinx `LocalDateTime`; old `"Z"`
  records still read back.

### New

- Converters between `Mp*` types and kotlinx-datetime types (`.kotlinx` / `.mp`), next to the existing `java.time`
  ones.
- `MpTimezone` overloads where only kotlinx `TimeZone` ones existed (`MpInstantRange.plus/minus`,
  `MpZonedDateTimeRange.contains`, `MpZonedDateTime.parse/tryParse`); `MpTimezone.of("SYSTEM")` is the system zone.
- pixijs addon: `ApplicationOptions.backgroundAlpha`.

### Fixed

- **pixijs addon works with webpack 5.108** (Kotlin 2.4): webpack dropped pixi's side-effect imports, so `app.ticker`
  was undefined and a `Graphics` rendered nothing. The addon now registers pixi's extensions itself; consumers need no
  webpack rule (Klang can drop `webpack.config.d/04-pixi-side-effects.js`).
- Named zones gave wrong offsets for dates before the common era on JS.
- `MpLocalTime.of(...)` with large minute / second values.

### Performance (browser)

ultra's zone and date arithmetic no longer goes through `Intl` on every call nor through kotlinx 0.8's emulated-`Long`
conversion. In spross's browser profile, datetime fell from 8.7 % to 1.7 % of wall time against 0.111.0;
in ultra's headless-Chrome benchmark `instant.atZone(zone).toLocalDateTime()` went from ~33 µs to ~3 µs.

Zone math follows java.time semantics on every target and is checked against java.time for every JDK zone.
