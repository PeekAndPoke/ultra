package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe

@Suppress("unused")
class OffsetIntervalCacheSpec : StringSpec({

    // A zone at +1h that switches to +2h at second 1_000_000 and back at 9_000_000.
    class FakeZone {
        var lookups = 0

        fun offsetAt(epochSeconds: Double): Int {
            lookups++
            return if (epochSeconds >= 1_000_000.0 && epochSeconds < 9_000_000.0) 7200 else 3600
        }
    }

    "answers exactly like the lookup, including right at and around the transitions" {
        val zone = FakeZone()
        val cache = OffsetIntervalCache(lookup = zone::offsetAt)

        listOf(1_000_000L, 9_000_000L).forEach { transition ->
            (-3L..3L).forEach { delta ->
                val at = transition + delta
                cache.offsetSecondsAt(at) shouldBe zone.offsetAt(at.toDouble())
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

        // about two lookups per day walked, plus one binary search for the transition
        zone.lookups shouldBeLessThan 2 * 10 + 20
    }

    "a cold lookup far from anything known costs exactly one lookup" {
        val zone = FakeZone()
        val cache = OffsetIntervalCache(lookup = zone::offsetAt)

        listOf(0L, 3_000_000L, 6_000_000L, 12_000_000L).forEach { at ->
            val before = zone.lookups
            cache.offsetSecondsAt(at)
            zone.lookups - before shouldBe 1
        }
    }

    "a miss within a day but more than an hour from what is known costs one lookup (no probe ahead)" {
        val zone = FakeZone()
        val cache = OffsetIntervalCache(lookup = zone::offsetAt)

        cache.offsetSecondsAt(3_000_000L)

        listOf(3_000_000L + 6 * 3_600L, 3_000_000L - 6 * 3_600L).forEach { at ->
            val before = zone.lookups
            cache.offsetSecondsAt(at) shouldBe 7200
            zone.lookups - before shouldBe 1
        }
    }

    "a daily stride costs about one lookup per call, not three" {
        val zone = FakeZone()
        val cache = OffsetIntervalCache(lookup = zone::offsetAt)

        // 110 days, one call per day, across both transitions
        var at = 0L
        repeat(110) {
            cache.offsetSecondsAt(at) shouldBe if (at in 1_000_000L until 9_000_000L) 7200 else 3600
            at += 86_400
        }

        // ~1 per call, plus one binary search (~17) per transition crossed
        zone.lookups shouldBeLessThan 100 + 2 * 20
    }

    "points with the same offset more than a day apart are not joined across a short period of another offset" {
        // +1h, except +2h for a day and a half; the two +1h points are ~1.9 days apart, so any threshold above a day
        // would join them across the +2h period
        val shortPeriod = 2_000_000L until 2_000_000L + 129_600L
        val cache = OffsetIntervalCache { if (it >= shortPeriod.first && it <= shortPeriod.last) 7200 else 3600 }

        cache.offsetSecondsAt(shortPeriod.first - 17_280L) shouldBe 3600
        cache.offsetSecondsAt(shortPeriod.last + 17_280L) shouldBe 3600
        cache.offsetSecondsAt(shortPeriod.first + 64_800L) shouldBe 7200
    }

    "a probe ahead outside the lookup's domain is skipped, not asked" {
        val cache = OffsetIntervalCache(domainStart = 0.0, domainEnd = 100_000.0) { at ->
            require(at >= 0.0 && at <= 100_000.0) { "outside: $at" }
            3600
        }

        cache.offsetSecondsAt(90_000L)
        // a dense miss: would probe 90_060 + 86_400, outside the domain
        cache.offsetSecondsAt(90_060L) shouldBe 3600
    }

    "two points less than a day apart across a transition, far from each other, cost no search" {
        val zone = FakeZone()
        val cache = OffsetIntervalCache(lookup = zone::offsetAt)

        // what TimezoneEngine.resolve asks for a wall time just after the transition
        cache.offsetSecondsAt(1_005_400L - 86_400L)
        cache.offsetSecondsAt(1_005_400L + 86_400L)
        val before = zone.lookups
        cache.offsetSecondsAt(1_005_400L - 3_600L) shouldBe 7200
        zone.lookups - before shouldBe 1
    }

    "a point learned on the other side of a transition is not stretched over it" {
        listOf(-3_600L to 3 * 3_600L, 3 * 3_600L to -3_600L).forEach { (first, second) ->
            val zone = FakeZone()
            val cache = OffsetIntervalCache(lookup = zone::offsetAt)
            val transition = 1_000_000L

            cache.offsetSecondsAt(transition + first)
            cache.offsetSecondsAt(transition + second)

            cache.offsetSecondsAt(transition - 60) shouldBe 3600
            cache.offsetSecondsAt(transition + 60) shouldBe 7200
        }
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

/** The tests think in whole Long seconds; the cache takes them as a Double. */
internal fun OffsetIntervalCache.offsetSecondsAt(epochSeconds: Long): Int = offsetSecondsAt(epochSeconds.toDouble())
