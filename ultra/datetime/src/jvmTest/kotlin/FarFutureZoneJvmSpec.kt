package io.peekandpoke.ultra.datetime

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Beyond 2^53 seconds (~285 million years) a Double cannot hold every second. Zone math out there must still terminate
 * and stay within a couple of seconds of java.time; the in-range engine is exact (see ZoneParity*).
 */
@Suppress("unused")
class FarFutureZoneJvmSpec : StringSpec({

    "start of a day whose midnight is in a gap terminates, far beyond 2^53 seconds" {
        // Asia/Beirut springs forward at 00:00 on the last Sunday of March; year 300 000 000 has 2000's weekdays.
        var result: Long? = null
        val worker = Thread {
            result = MpLocalDate.of(300_000_000, 3, 26).atStartOfDay(MpTimezone.of("Asia/Beirut")).toEpochSeconds()
        }.apply { isDaemon = true; start() }

        worker.join(10_000)

        worker.isAlive shouldBe false

        val expected = LocalDate.of(300_000_000, 3, 26).atStartOfDay(ZoneId.of("Asia/Beirut")).toEpochSecond()
        val actual = result!!
        withClue("got $actual, java.time says $expected") {
            (abs(actual - expected) <= 2L) shouldBe true
        }
    }
})
