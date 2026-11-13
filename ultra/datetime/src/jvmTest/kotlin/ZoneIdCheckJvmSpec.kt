package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.IllegalTimeZoneException
import java.time.ZoneId

/** ULTRA-00317: `MpTimezone.jvm` checks the id like every other platform entry, before `ZoneId.of`. */
@Suppress("unused")
class ZoneIdCheckJvmSpec : StringSpec({

    listOf(
        "Europe/Berlin" + Char(10) + "FAKE" to "Invalid zone id: 'Europe/Berlin?FAKE'",
        "Europe/./Berlin" to "Invalid zone id: 'Europe/./Berlin'",
        "A".repeat(200) to "Invalid zone id: 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'...",
    ).forEach { (id, expected) ->
        "MpTimezone.jvm refuses '${id.take(20)}' (${id.length} chars) with a bounded message" {
            val error = shouldThrow<IllegalTimeZoneException> { MpTimezone.of(id).jvm }

            error.message shouldBe expected
        }
    }

    listOf(
        "Z" to "UTC",
        "UTC" to "UTC",
        "UTC+01:00" to "UTC+01:00",
        "+05:30" to "+05:30",
        "GMT-08:00" to "GMT-08:00",
        "Europe/Berlin" to "Europe/Berlin",
    ).forEach { (id, zoneId) ->
        "MpTimezone.jvm still converts '$id'" {
            MpTimezone.of(id).jvm shouldBe ZoneId.of(zoneId)
        }
    }
})
