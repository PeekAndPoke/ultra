package io.peekandpoke.kraft.addons.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.TimeZone

class TimezonesSpec : StringSpec({

    "installTimezones1970to2030 makes a named zone resolvable" {
        installTimezones1970to2030()

        // Without any timezone dataset installed this throws IllegalTimeZoneException,
        // so reaching a non-null result proves the 1970-2030 rules were loaded.
        TimeZone.of("Europe/Berlin") shouldNotBe null
    }
})
