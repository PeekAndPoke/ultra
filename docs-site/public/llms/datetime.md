# Datetime — Multiplatform Date & Time

Multiplatform date, time, timezone, and range types for Kotlin. Built on kotlinx-datetime with a type system designed to
prevent common date/time mistakes.

- Package: `io.peekandpoke.ultra:datetime`
- Platforms: JVM, JS
- Docs: https://peekandpoke.io/ultra/datetime/

## Design Philosophy: Guard Rails

The library enforces correct usage through its type system:

- **MpLocalDateTime has no arithmetic** — adding time to a local datetime is ambiguous near DST transitions. Convert to
  MpZonedDateTime or MpLocalDate first.
- **MpInstant requires a timezone for calendar operations** — `plus(1, DateTimeUnit.DAY, timezone)` is DST-aware;
  `plus(1.days)` adds exactly 24 hours. The library makes you choose.
- **MpLocalDate has calendar arithmetic without a timezone** — pure date math (plusMonths, plusDays) is unambiguous
  without a clock.
- **MpZonedDateTime has full arithmetic** — it carries its timezone, so DST is handled automatically.
- **Every local-to-absolute conversion requires a timezone** — `MpLocalDateTime.toInstant(timezone)`,
  `MpInstant.atZone(timezone)`.

### Duration vs DateTimeUnit

```kotlin
val instant = MpInstant.parse("2025-03-09T07:00:00Z")
val nyTz = MpTimezone.of("America/New_York")

// Duration: exactly 24 hours, always
instant.plus(1.days)

// DateTimeUnit: one calendar day, DST-aware (may be 23 or 25 hours)
instant.plus(1, DateTimeUnit.DAY, nyTz)
```

## Core Types

| Type              | Represents                   | Arithmetic                        | Timezone required |
|-------------------|------------------------------|-----------------------------------|-------------------|
| `MpInstant`       | Absolute point in time (UTC) | Duration: yes. Calendar: needs tz | For calendar ops  |
| `MpLocalDate`     | Calendar date (no time)      | Days, months, years               | No                |
| `MpLocalTime`     | Time of day (no date)        | Duration only                     | No                |
| `MpLocalDateTime` | Date + time (no timezone)    | **None** (by design)              | For conversion    |
| `MpZonedDateTime` | Date + time + timezone       | Full, DST-aware                   | Carries its own   |
| `MpTimezone`      | IANA timezone ID             | N/A                               | N/A               |

## MpInstant

```kotlin
val now = kronos.instantNow()
val parsed = MpInstant.parse("2025-03-15T10:30:00Z")
val fromEpoch = MpInstant.fromEpochMillis(1710499800000L)

// Convert — requires timezone
val zoned = parsed.atZone(MpTimezone.of("Europe/Berlin"))
val date = parsed.toLocalDate(MpTimezone.of("Europe/Berlin"))

// Arithmetic
val later = parsed + 2.hours           // absolute
val tomorrow = parsed.plus(1, DateTimeUnit.DAY, timezone)  // calendar, DST-aware

// Constants
MpInstant.Epoch    // 1970-01-01T00:00:00Z
MpInstant.Genesis  // -10000-01-01T00:00:00Z
MpInstant.Doomsday // +10000-01-01T00:00:00Z
```

## MpLocalDate

```kotlin
val date = MpLocalDate.of(2025, 3, 15)
date.year          // 2025
date.dayOfWeek     // SATURDAY
date.numDaysInMonth // 31

// Calendar arithmetic (no timezone needed)
date.plusMonths(1)  // 2025-04-15
date.plusDays(7)    // 2025-03-22
// Month-end clamping: Jan 31 + 1 month = Feb 28

// Anchoring
date.atFirstOfMonth()         // 2025-03-01
date.atLastOfMonth()          // 2025-03-31
date.atStartOfQuarterOfYear() // 2025-01-01
date.atStartOfNext(DayOfWeek.MONDAY)

// Week queries
date.getDatesInMonth(listOf(DayOfWeek.MONDAY)) // all Mondays

// Convert
date.atTime(MpLocalTime.of(14, 30))               // MpLocalDateTime
date.atStartOfDay(MpTimezone.of("Europe/Berlin"))  // MpZonedDateTime
```

## MpLocalTime

```kotlin
val time = MpLocalTime.of(14, 30, 15)
time.plus(2.hours)    // wraps around midnight
time.formatHhMm()     // "14:30"

// Constants
MpLocalTime.Min   // 00:00:00
MpLocalTime.Max   // 23:59:59.999
MpLocalTime.Noon  // 12:00:00
```

## MpLocalDateTime

```kotlin
val dt = MpLocalDateTime.of(2025, 3, 15, 14, 30)

// NO arithmetic — by design (DST-ambiguous)

// Decompose
dt.toDate()  // MpLocalDate
dt.toTime()  // MpLocalTime

// Convert to absolute — REQUIRES timezone
dt.toInstant(MpTimezone.UTC)
dt.atZone(MpTimezone.of("Europe/Berlin"))
```

## MpZonedDateTime

```kotlin
val zoned = MpZonedDateTime.of(
    MpLocalDateTime.of(2025, 6, 15, 14, 0),
    MpTimezone.of("Europe/Berlin"),
)

// Full DST-aware arithmetic
zoned.plus(1, DateTimeUnit.DAY)   // same local time tomorrow
zoned.plus(1.days)                // exactly 24 hours
zoned.atStartOfMonth()
zoned.atStartOfDay()

// Formatting (only type with timezone offset support)
zoned.format("dd MMM yyyy HH:mm Z")  // "15 Jun 2025 14:00 +02:00"

// Convert
zoned.toInstant()        // MpInstant
zoned.toLocalDate()      // MpLocalDate
zoned.toLocalDateTime()  // MpLocalDateTime
```

## MpTimezone

```kotlin
val berlin = MpTimezone.of("Europe/Berlin")
val utc = MpTimezone.UTC
val system = MpTimezone.systemDefault

berlin.offsetAt(instant)          // +02:00 in summer, +01:00 in winter
berlin.offsetSecondsAt(instant)   // 7200

MpTimezone.supportedIds  // 600+ IANA zones
```

## Periods

```kotlin
// Date-only period (years, months, days)
val datePeriod = MpDatePeriod.of(years = 1, months = 2, days = 15)
val parsed = MpDatePeriod.parse("P1Y2M15D")

// Full period with time components
val fullPeriod = MpDateTimePeriod.of(years = 0, months = 0, days = 5, hours = 3, minutes = 30, seconds = 0)

// Apply to dates and instants
date.plus(datePeriod)
instant.plus(fullPeriod, timezone)
```

## Comparisons

All types implement `ComparableTo<T>` with standard operators and readable infix functions:

```kotlin
val a = MpLocalDate.of(2025, 1, 1)
val b = MpLocalDate.of(2025, 6, 15)

a < b                          // true
a.isLessThan(b)                // true
b.isGreaterThan(a)             // true
a.isLessThanOrEqualTo(b)       // true
```

## Ranges

All range types share these common properties: `hasStart`, `hasEnd`, `isOpen`, `isNotOpen`, `isValid`, `isNotValid`.

All range types support these set operations: `contains(point)`, `contains(range)`, `intersects()`, `touches()`,
`isAdjacentTo()`, `mergeWith()`, `cutAway()`.

- **MpLocalDateRange** — open-ended `[from, to)`, with `numberOfDays`, `numberOfNights`, `asListOfDates()`,
  `asDatePeriod`, `asClosedRange`, `toZonedTimeRange(tz)`, `asPartialRange()`
  - Factory: `MpLocalDateRange.of()`, `.forever`, `.beginningAt()`, `.endingAt()`
- **MpClosedLocalDateRange** — closed `[from, to]`, with `numberOfDays`, `numberOfNights`, `asOpenRange`,
  `asDatePeriod`, `asListOfDates()`, `toZonedTimeRange(tz)`
  - Factory: `MpClosedLocalDateRange.of()`, `.forever`, `.beginningAt()`, `.endingAt()`
- **MpInstantRange** — absolute time range with `duration`, `atZone(tz)`, `atSystemDefaultZone()`, `plus(Duration)`,
  `minus(Duration)`, `plus(value, DateTimeUnit, TimeZone)`, `minus(value, DateTimeUnit, TimeZone)`
  - Factory: `MpInstantRange.of(from, duration)`, `.forever`, `.beginningAt()`, `.endingAt()`
- **MpZonedDateTimeRange** — timezone-aware range with `duration`, `asDateRange()`, `toInstantRange()`, `atZone(tz)`,
  `plus(Duration)`, `minus(Duration)`, `plus(value, DateTimeUnit)`, `minus(value, DateTimeUnit)`,
  `compareTo(MpTemporalPeriod)`
  - Factory: `MpZonedDateTimeRange.of(from, duration)`, `.of(from, period)`, `.forever`, `.beginningAt()`, `.endingAt()`
- **MpLocalTimeSlot** — intra-day time range with `duration`, `splitWithGaps(duration, gap)`, `formatHhMm()`,
  `formatHhMmSs()`
  - Factory: `MpLocalTimeSlot.of(from, duration)`, `.ofSecondsOfDay(from, to)`, `.completeDay`

All range types (except MpLocalTimeSlot) have a **Partial** variant with optional start/end, convertible via
`asValidRange()`:

```kotlin
// Open-ended range
val partial = MpLocalDateRange.Partial(from = someDate, to = null)
val concrete = partial.asValidRange()  // fills Doomsday as end

// Also available: MpClosedLocalDateRange.Partial, MpInstantRange.Partial, MpZonedDateTimeRange.Partial
// MpInstantRange.Partial also has: asDateRange(timezone) -> MpClosedLocalDateRange.Partial
// MpZonedDateTimeRange.Partial also has: asDateRange() -> MpClosedLocalDateRange.Partial
```

Create ranges from types:

```kotlin
val range = instant.toRange(2.hours)                    // MpInstantRange
val dateRange = date.toRange(MpDatePeriod.of(days = 7)) // MpLocalDateRange
val zonedRange = zoned.toRange(MpDateTimePeriod.of(months = 1))
```

## Kronos — Testable Clock

```kotlin
// Production
val kronos = Kronos.systemUtc
kronos.instantNow()

// Tests — fixed clock
val frozen = Kronos.fixed(MpInstant.parse("2025-01-01T00:00:00Z"))
frozen.instantNow()  // always the same

// Tests — advancing clock
val advanced = frozen.advanceBy(2.hours)

// Inject via Kontainer
val blueprint = kontainer {
    singleton(Kronos::class) { Kronos.systemUtc }
}
```

## Formatting Tokens

`yyyy`, `yy`, `MM`, `MMM`, `dd`, `HH`, `mm`, `ss`, `SSS`, `Z`

The formatter never throws — malformed patterns degrade gracefully.

## Serialization

All types are `@Serializable` with kotlinx.serialization. Slumber codec support via `MpDateTimeModule`.

## JVM Interop

```kotlin
// Mp -> java.time
val javaInstant = mpInstant.jvm
val javaDate = mpDate.jvm

// java.time -> Mp
val mpInstant = javaInstant.mp
val mpDate = javaDate.mp
```
