package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.TimeZone

@Suppress("unused", "SpellCheckingInspection")
class MpTimezoneSpec : StringSpec({

    "Creating a timezone from an id must work" {
        MpTimezone.of("Europe/Berlin").id shouldBe "Europe/Berlin"
    }

    MpTimezone.supportedIds
        .filter { it != "Z" }
        .forEach { timezoneId ->

            "Creating a TimeZone from id '$timezoneId' must work" {

                MpTimezone.of(timezoneId).id shouldBe timezoneId

                MpTimezone.of(timezoneId).kotlinx.let {
                    it.shouldBeInstanceOf<TimeZone>()
                    it.id shouldBe timezoneId
                }
            }
        }

    "Creating an MpTimezone from 'Z' must work" {
        MpTimezone.of("Z") shouldBe MpTimezone.UTC
    }
})
