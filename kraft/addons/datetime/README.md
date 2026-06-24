# Kraft Addon: Datetime

Opt-in timezone database loading for `kotlinx-datetime` on Kotlin/JS.

`kraft:core` no longer loads any timezone data automatically, so apps that don't use named time
zones pay nothing. Apps that *do* (e.g. `TimeZone.of("Europe/Berlin")`) add this addon and call
one loader once at startup, before any zone is resolved:

```kotlin
import io.peekandpoke.kraft.addons.datetime.installTimezones1970to2030

fun main() {
    installTimezones1970to2030() // pick the smallest dataset that covers your date range
    // ... kraft.mount(...)
}
```

| Loader                          | dataset                       | approx size (min / gzip) |
|---------------------------------|-------------------------------|--------------------------|
| `installFullTimezones()`        | all zones, full history       | ~713 KB / ~37 KB         |
| `installTimezones1970to2030()`  | years 1970–2030 (recommended) | ~130 KB / ~18 KB         |
| `installTimezones10YearRange()` | ~±5 years around build time   | ~40 KB / ~10 KB          |

Call only one — dead-code elimination keeps just the dataset you reference.

> Backed by `@js-joda/timezone` (now deprecated upstream). A future `installNativeTimezones()`
> mode will use the browser's native IANA database via `Intl`, shipping zero bundled data.
