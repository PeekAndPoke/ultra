package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import java.time.Instant
import java.time.ZoneId

/**
 * The JS offset cache (`OffsetIntervalCache`, common code) against java.time, for every zone the JDK knows: around every
 * transition 1900–2040 in an order that jumps around, and a sequential hourly walk over decades for zones with odd
 * transitions.
 */
@Suppress("unused")
class OffsetIntervalCacheJvmSpec : StringSpec({

    val from = Instant.parse("1900-01-01T00:00:00Z")
    val until = Instant.parse("2040-01-01T00:00:00Z")

    fun javaOffset(zone: ZoneId, epochSeconds: Long) = zone.rules.getOffset(Instant.ofEpochSecond(epochSeconds)).totalSeconds

    "the cache matches java.time around every transition of every zone, 1900 to 2040" {
        val mismatches = mutableListOf<String>()
        var checks = 0

        ZoneId.getAvailableZoneIds().sorted().forEach { zoneId ->
            val zone = ZoneId.of(zoneId)
            val cache = OffsetIntervalCache { javaOffset(zone, it) }

            val probes = mutableListOf<Long>()
            var transition = zone.rules.nextTransition(from)

            while (transition != null && transition.instant < until) {
                val t = transition.instant.epochSecond
                probes += listOf(t - 86_400, t - 3_600, t - 1, t, t + 1, t + 3_600, t + 86_400, t + 43_200)
                transition = zone.rules.nextTransition(transition.instant)
            }

            // Forwards, then backwards, then interleaved: hits, misses, joins and evictions all happen.
            (probes + probes.reversed() + probes.filterIndexed { i, _ -> i % 2 == 0 } + probes.filterIndexed { i, _ -> i % 2 == 1 })
                .forEach { at ->
                    checks++
                    val expected = javaOffset(zone, at)
                    val actual = cache.offsetSecondsAt(at)
                    if (expected != actual) mismatches += "$zoneId at $at: expected $expected, got $actual"
                }
        }

        println("OffsetIntervalCacheJvmSpec: $checks checks")

        checks shouldBeGreaterThan 100_000
        mismatches.take(50).shouldBeEmpty()
    }

    "a sequential hourly walk 1970 to 2040 matches java.time" {
        val mismatches = mutableListOf<String>()

        listOf("Europe/Berlin", "America/New_York", "Australia/Lord_Howe", "Pacific/Apia", "Africa/Casablanca").forEach { zoneId ->
            val zone = ZoneId.of(zoneId)
            val cache = OffsetIntervalCache { javaOffset(zone, it) }

            var at = Instant.parse("1970-01-01T00:00:00Z").epochSecond
            val end = Instant.parse("2040-01-01T00:00:00Z").epochSecond

            while (at < end) {
                val expected = javaOffset(zone, at)
                val actual = cache.offsetSecondsAt(at)
                if (expected != actual) mismatches += "$zoneId at $at: expected $expected, got $actual"
                at += 3_600
            }
        }

        mismatches.take(50).shouldBeEmpty()
    }
})
