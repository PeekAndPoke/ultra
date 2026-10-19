package io.peekandpoke.ultra.datetime

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Beyond 2^53 seconds (~285 million years) a Double cannot hold every second, so the engine switches to exact Long
 * lookups there. Everything must still match java.time exactly — around DST transitions too — and terminate.
 */
@Suppress("unused")
class FarFutureZoneJvmSpec : StringSpec({

    "start of a day whose midnight is in a gap terminates and is exact, far beyond 2^53 seconds" {
        // Asia/Beirut springs forward at 00:00 on the last Sunday of March; year 300 000 000 has 2000's weekdays.
        var result: Long? = null
        val worker = Thread {
            result = MpLocalDate.of(300_000_000, 3, 26).atStartOfDay(MpTimezone.of("Asia/Beirut")).toEpochSeconds()
        }.apply { isDaemon = true; start() }

        worker.join(10_000)

        worker.isAlive shouldBe false
        result shouldBe LocalDate.of(300_000_000, 3, 26).atStartOfDay(ZoneId.of("Asia/Beirut")).toEpochSecond()
    }

    "around every transition of a year beyond 2^53 and one beyond 2^54, the zone math matches java.time" {
        val mismatches = mutableListOf<String>()
        var checks = 0

        fun check(what: String, expected: Any, actual: Any) {
            checks++
            if (expected != actual) mismatches += "$what: expected $expected, got $actual"
        }

        listOf("Europe/Berlin", "America/New_York", "Asia/Beirut", "America/Santiago", "Australia/Lord_Howe").forEach { id ->
            val zone = ZoneId.of(id)
            val timezone = MpTimezone.of(id)

            listOf(290_000_000, 600_000_000).forEach { year ->
                var transition = zone.rules.nextTransition(LocalDate.of(year, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant())

                repeat(2) {
                    val t = transition.instant.epochSecond

                    (-2L..2L).forEach { delta ->
                        val at = t + delta
                        val javaZoned = Instant.ofEpochSecond(at).atZone(zone)

                        check("$id offset at $at", javaZoned.offset.totalSeconds, timezone.offsetSecondsAt(MpInstant.fromEpochSeconds(at)))
                        check("$id local at $at", javaZoned.toLocalDateTime().mp, MpInstant.fromEpochSeconds(at).atZone(timezone).toLocalDateTime())
                    }

                    val earliest = minOf(transition.dateTimeBefore, transition.dateTimeAfter)
                    val latest = maxOf(transition.dateTimeBefore, transition.dateTimeAfter)

                    listOf(earliest.minusSeconds(1), earliest, earliest.plusSeconds(1), latest.minusSeconds(1), latest).forEach { local ->
                        check(
                            "$id toInstant($local)",
                            ZonedDateTime.ofLocal(local, zone, null).toEpochSecond(),
                            local.mp.toInstant(timezone).toEpochSeconds(),
                        )
                    }

                    transition = zone.rules.nextTransition(transition.instant)
                }
            }
        }

        checks shouldBe 300
        mismatches.take(20).shouldBeEmpty()
    }

    "a stored ts one second before a DST transition beyond 2^53 survives decode and re-encode unchanged" {
        // Europe/Berlin falls back at 290000000-10-29T01:00Z = 9 151 453 938 877 200 s
        val ts = 9_151_453_938_877_199_000L
        val raw = """{"ts":$ts,"timezone":"Europe/Berlin","human":""}"""

        val decoded = Json.decodeFromString(MpZonedDateTime.serializer(), raw)

        withClue(decoded) {
            decoded.toLocalDateTime() shouldBe LocalDateTime.of(290_000_000, 10, 29, 2, 59, 59).mp
            decoded.toEpochMillis() shouldBe ts
            Json.encodeToString(MpZonedDateTime.serializer(), decoded).substringAfter("\"ts\":").substringBefore(",") shouldBe ts.toString()
        }
    }
})
