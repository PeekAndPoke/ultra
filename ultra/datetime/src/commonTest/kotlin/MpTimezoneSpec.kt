package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.TimeZone

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

    "MpTimezone.UTC must return UTC timezone" {
        val utc = MpTimezone.UTC
        utc.id shouldBe "UTC"
    }

    "MpTimezone.systemDefault must return a non-null timezone" {
        val systemDefault = MpTimezone.systemDefault
        systemDefault shouldNotBe null
        systemDefault.id shouldNotBe ""
    }

    "kotlinx property must convert to kotlinx TimeZone" {
        val mpTimezone = MpTimezone.of("Europe/Berlin")
        val kotlinxTz = mpTimezone.kotlinx

        kotlinxTz.shouldBeInstanceOf<TimeZone>()
        kotlinxTz.id shouldBe "Europe/Berlin"
    }
})
