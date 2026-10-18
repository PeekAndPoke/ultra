package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

@Suppress("unused")
class TimezoneEngineJsSpec : StringSpec({

    "an instant beyond the range of JS dates fails with an IllegalArgumentException, not a RangeError" {
        shouldThrow<IllegalArgumentException> {
            MpInstant.fromEpochMillis(9_000_000_000_000_000).atZone(MpTimezone.of("Europe/Berlin"))
        }
    }
})
