# Kraft Addon: Datetime

Opt-in timezone support for `kotlinx-datetime` on Kotlin/JS.

`kraft:core` no longer loads any timezone data automatically, so apps that don't use named time
zones pay nothing. Apps that *do* (e.g. `TimeZone.of("Europe/Berlin")`) add this addon and call
one installer once at startup, before any zone is resolved:

```kotlin
import io.peekandpoke.kraft.addons.datetime.installNativeTimezones

fun main() {
    installNativeTimezones() // recommended: browser-native, zero bundled data
    // ... kraft.mount(...)
}
```

| Installer                       | source                                       | approx size (min / gzip) |
|---------------------------------|----------------------------------------------|--------------------------|
| `installNativeTimezones()`      | browser IANA DB via `Intl` (**recommended**) | **~0 KB**                |
| `installFullTimezones()`        | js-joda, all zones, full history             | ~713 KB / ~37 KB         |
| `installTimezones1970to2030()`  | js-joda, years 1970–2030                     | ~130 KB / ~18 KB         |
| `installTimezones10YearRange()` | js-joda, ~±5 years around build time         | ~40 KB / ~10 KB          |

Call only one — for the js-joda loaders, dead-code elimination keeps just the dataset you reference.

**Native** resolves offsets from the browser's own (always up-to-date) IANA database via
`Intl.DateTimeFormat`, registered as a custom js-joda `ZoneRulesProvider`. It passes the full
`ultra:datetime` JS test suite (named zones + DST). Use a js-joda loader only for pre-`Intl`
engines or historical accuracy beyond what `Intl` provides.

> The js-joda loaders are backed by `@js-joda/timezone` (deprecated upstream); native is the
> forward path until `kotlinx-datetime` adopts the `Temporal` API.
