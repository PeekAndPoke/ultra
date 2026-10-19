# Kraft Addon: Datetime

Time zone data for RAW `kotlinx-datetime` zones on Kotlin/JS.

ultra's `Mp*` types (`MpTimezone`, `MpZonedDateTime`, ...) do not need this addon: they resolve named zones through
the browser's own IANA database via `Intl` and bundle no data. Only code that calls kotlinx directly, e.g.
`TimeZone.of("Europe/Berlin")`, needs a dataset — without one, kotlinx knows only UTC and fixed offsets.

Call ONE loader once at startup, before any zone is resolved:

```kotlin
import io.peekandpoke.kraft.addons.datetime.timezones1970to2030

val app = kraftApp {
    timezones1970to2030()
}
```

| Loader                          | Kraft builder            | Coverage                     | approx size (min / gzip) |
|---------------------------------|--------------------------|------------------------------|--------------------------|
| `installFullTimezones()`        | `fullTimezones()`        | all zones, full history      | ~713 KB / ~37 KB         |
| `installTimezones1970to2030()`  | `timezones1970to2030()`  | years 1970–2030              | ~130 KB / ~18 KB         |
| `installTimezones10YearRange()` | `timezones10YearRange()` | ~±5 years around build time  | ~40 KB / ~10 KB          |

Dead-code elimination keeps just the dataset you reference. The loaders are backed by `@js-joda/timezone`.
