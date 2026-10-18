package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe

@Suppress("unused")
class OffsetIntervalCacheSpec : StringSpec({

    // A zone at +1h that switches to +2h at second 1_000_000 and back at 9_000_000.
    class FakeZone {
        var lookups = 0

        fun offsetAt(epochSeconds: Long): Int {
            lookups++
            return if (epochSeconds in 1_000_000L until 9_000_000L) 7200 else 3600
        }
    }

    "answers exactly like the lookup, including right at and around the transitions" {
        val zone = FakeZone()
        val cache = OffsetIntervalCache(lookup = zone::offsetAt)

        listOf(1_000_000L, 9_000_000L).forEach { transition ->
            (-3L..3L).forEach { delta ->
                val at = transition + delta
                cache.offsetSecondsAt(at) shouldBe zone.offsetAt(at)
            }
        }
    }

    "a sequential caller asks the lookup a few times per simulated day, not per call" {
        val zone = FakeZone()
        val cache = OffsetIntervalCache(lookup = zone::offsetAt)

        // 10 days in minute steps, across the first transition
        var at = 1_000_000L - 5 * 86_400L
        repeat(10 * 1440) {
            cache.offsetSecondsAt(at) shouldBe if (at >= 1_000_000L) 7200 else 3600
            at += 60
        }

        // a few lookups per day of time covered, plus one binary search per side of the transition
        zone.lookups shouldBeLessThan 100
    }

    "jumping between more places than it keeps still answers correctly" {
        val zone = FakeZone()
        val cache = OffsetIntervalCache(maxIntervals = 2, lookup = zone::offsetAt)
        val places = listOf(0L, 500_000L, 1_000_000L, 5_000_000L, 9_000_000L, 20_000_000L)

        repeat(3) {
            places.forEach { at -> cache.offsetSecondsAt(at) shouldBe if (at in 1_000_000L until 9_000_000L) 7200 else 3600 }
        }
    }
})
