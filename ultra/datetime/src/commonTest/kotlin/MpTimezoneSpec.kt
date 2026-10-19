package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MpTimezoneSpec : StringSpec({

    "Creating a timezone from an id must work" {
        MpTimezone.of("Europe/Berlin").id shouldBe "Europe/Berlin"
    }

    MpTimezone.supportedIds
        .filter { it != "Z" }
        .forEach { timezoneId ->

            "Creating a TimeZone from id '$timezoneId' must work" {

                val timezone = MpTimezone.of(timezoneId)

                timezone.id shouldBe timezoneId

                // Resolving the zone must work on every target, the browser included, without any tz data.
                val instant = MpInstant.parse("2022-07-01T12:00:00Z")
                instant.atZone(timezone).toInstant() shouldBe instant
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
})
