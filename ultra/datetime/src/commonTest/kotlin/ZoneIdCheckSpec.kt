package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.IllegalTimeZoneException

/** ULTRA-00317: ids not shaped like an IANA region id are refused in common code, before any platform lookup. */
@Suppress("unused")
class ZoneIdCheckSpec : StringSpec({

    val instant = MpInstant.fromEpochMillis(1_700_000_000_000)

    // Every entry into the platform zone lookup, plus the conversion to a kotlinx TimeZone.
    val entries: List<Pair<String, (MpTimezone) -> Any>> = listOf(
        "MpInstant.atZone" to { tz -> instant.atZone(tz) },
        "MpTimezone.offsetSecondsAt" to { tz -> tz.offsetSecondsAt(instant) },
        "MpLocalDateTime.atZone" to { tz -> MpLocalDateTime.of(2023, 11, 14, 12, 0).atZone(tz) },
        "MpLocalDate.atStartOfDay" to { tz -> MpLocalDate.of(2023, 11, 14).atStartOfDay(tz) },
        "MpInstant.plus in a zone" to { tz -> instant.plus(1, DateTimeUnit.DAY, tz) },
        "MpTimezone.kotlinx" to { tz -> tz.kotlinx },
    )

    val refused = listOf(
        "Europe/./Berlin",
        "Europe//Berlin",
        "Europe/Berlin/",
        "./Europe/Berlin",
        "/Europe/Berlin",
        "Europe/../Europe/Berlin",
        "/usr/share/zoneinfo/Europe/Berlin",
        "Europe/Berlin ",
        "Europe/Berlín",
        "",
        "A".repeat(65),
    )

    refused.forEach { id ->
        entries.forEach { (entry, call) ->
            "'${id.take(20)}' (${id.length} chars) is refused by $entry" {
                val error = shouldThrow<IllegalTimeZoneException> { call(MpTimezone.of(id)) }

                error.message shouldBe "Invalid zone id: " + id.take(64).replace('í', '?') +
                        (if (id.length > 64) "..." else "")
            }
        }
    }

    "every supported id passes the check" {
        val failing = MpTimezone.supportedIds.filter { id ->
            runCatching { TimezoneEngine.requireRegionIdShape(id) }.isFailure
        }

        failing shouldBe emptyList()
    }

    "the refusal echoes at most 64 characters, control characters replaced" {
        val id = "Europe/Berlin" + Char(10) + Char(27) + "x".repeat(100)

        val error = shouldThrow<IllegalTimeZoneException> { instant.atZone(MpTimezone.of(id)) }

        error.message shouldBe "Invalid zone id: Europe/Berlin??" + "x".repeat(49) + "..."
    }

    "an invalid fixed offset is refused with a bounded echo as well" {
        val id = "+99" + Char(10) + "9".repeat(100)

        val error = shouldThrow<IllegalTimeZoneException> { instant.atZone(MpTimezone.of(id)) }

        error.message shouldBe "Invalid zone id: +99?" + "9".repeat(60) + "..."
    }

    listOf("UTC+01:00", "+05:30", "GMT-08:00", "UT+1", "-03").forEach { id ->
        "the fixed offset '$id' keeps working" {
            instant.atZone(MpTimezone.of(id)).toInstant() shouldBe instant
            MpTimezone.of(id).kotlinx
        }
    }
})
