package io.peekandpoke.ultra.datetime

import kotlin.math.floor

/**
 * Remembers intervals in which one zone's offset is constant, so most lookups are a range check instead of a call to
 * [lookup] (an `Intl` call on JS).
 *
 * Relies on a property of the current tz data, not a guarantee of it (pinned for the JDK's copy by
 * `OffsetIntervalCacheJvmSpec`): no two transitions within a day of each other. So two seconds at most a day apart with
 * the same offset have that offset in between, and between two such seconds with different offsets there is exactly
 * one transition.
 *
 * Intervals grow lazily: a miss far from anything known costs one lookup and is remembered as a single second; a miss
 * within a day of a known interval with the same offset extends it. Only a miss within an hour of an interval — a
 * sequential walk — searches for a transition or probes a day further in the direction of travel, so a sequential
 * caller pays about two lookups per day it walks and scattered callers never pay for a search. Not thread-safe.
 */
internal class OffsetIntervalCache(
    private val maxIntervals: Int = 8,
    /** The seconds [lookup] can answer; probes outside are skipped. */
    private val domain: LongRange = Long.MIN_VALUE..Long.MAX_VALUE,
    private val lookup: (epochSeconds: Long) -> Int,
) {
    private val domainStart = domain.first.toDouble()
    private val domainEnd = domain.last.toDouble()

    // Bounds are Double, not Long: on Kotlin/JS a Long is emulated, and the range check on every hit should be plain
    // number comparisons. Exact for every second a JS Date can hold (±8.64e12 < 2^53).
    /** Every second in `[from, until)` has [offset]. */
    private class Interval(var from: Double, var until: Double, val offset: Int)

    // Most recently used first.
    private val intervals = ArrayList<Interval>(maxIntervals)

    /** The offset at [epochSeconds], in seconds. */
    fun offsetSecondsAt(epochSeconds: Long): Int {
        val at = epochSeconds.toDouble()

        for (i in intervals.indices) {
            val interval = intervals[i]

            if (at >= interval.from && at < interval.until) {
                if (i > 0) {
                    intervals.removeAt(i)
                    intervals.add(0, interval)
                }

                return interval.offset
            }
        }

        return learn(at)
    }

    private fun lookupAt(at: Double): Int = lookup(at.toLong())

    private fun learn(at: Double): Int {
        val offset = lookupAt(at)
        val near = nearestWithinADay(at)

        when {
            near == null -> remember(Interval(at, at + 1, offset))

            at >= near.until -> {
                val dense = at - near.until < DENSE
                val grown = when {
                    near.offset == offset -> near.also { it.until = at + 1.0 }
                    dense -> Interval(firstChangeAfter(near.until - 1.0, at, near.offset), at + 1, offset)
                        .also { near.until = it.from }
                    // A transition lies between, but searching for it only pays off for a sequential walk.
                    else -> Interval(at, at + 1, offset)
                }
                remember(grown)
                if (dense) probeAhead(grown, at, DAY)
            }

            else -> {
                val dense = near.from - at <= DENSE
                val grown = when {
                    near.offset == offset -> near.also { it.from = at }
                    dense -> Interval(at, firstChangeAfter(at, near.from, offset), offset)
                        .also { near.from = it.until }
                    else -> Interval(at, at + 1, offset)
                }
                remember(grown)
                if (dense) probeAhead(grown, at, -DAY)
            }
        }

        return offset
    }

    /** Extends [interval], which ends (or starts) at [at], by up to a day in the direction of [step]. */
    private fun probeAhead(interval: Interval, at: Double, step: Double) {
        val probe = at + step
        if (probe < domainStart || probe > domainEnd) return

        val offset = lookupAt(probe)

        when {
            offset == interval.offset && step > 0.0 -> interval.until = probe + 1.0
            offset == interval.offset -> interval.from = probe
            step > 0.0 -> {
                interval.until = firstChangeAfter(at, probe, interval.offset)
                remember(Interval(interval.until, probe + 1.0, offset))
            }
            else -> {
                interval.from = firstChangeAfter(probe, at, offset)
                remember(Interval(probe, interval.from, offset))
            }
        }

        absorbNeighbours(interval)
    }

    /** The interval closest to [at], if one is at most a day away. */
    private fun nearestWithinADay(at: Double): Interval? {
        var best: Interval? = null
        var bestDistance = DAY + 1.0

        for (interval in intervals) {
            val distance = if (at < interval.from) interval.from - at else at - (interval.until - 1.0)

            if (distance < bestDistance) {
                best = interval
                bestDistance = distance
            }
        }

        return best
    }

    /** Puts [interval] first, merging it with any interval of the same offset that it touches. */
    private fun remember(interval: Interval) {
        intervals.remove(interval)
        intervals.add(0, interval)
        absorbNeighbours(interval)

        while (intervals.size > maxIntervals) intervals.removeAt(intervals.lastIndex)
    }

    private fun absorbNeighbours(interval: Interval) {
        val iterator = intervals.iterator()

        while (iterator.hasNext()) {
            val other = iterator.next()

            if (other !== interval && other.offset == interval.offset &&
                other.from <= interval.until && interval.from <= other.until
            ) {
                interval.from = minOf(interval.from, other.from)
                interval.until = maxOf(interval.until, other.until)
                iterator.remove()
            }
        }
    }

    /**
     * The first second in `(lo, hi]` whose offset is not [offset], where [lo] has [offset], [hi] does not, and the two
     * are at most a day apart — so there is exactly one transition between them.
     */
    private fun firstChangeAfter(lo: Double, hi: Double, offset: Int): Double {
        var a = lo
        var b = hi

        while (b - a > 1.0) {
            val mid = floor((a + b) / 2.0)
            if (lookupAt(mid) == offset) a = mid else b = mid
        }

        return b
    }

    private companion object {
        const val DAY = 86_400.0

        // A miss this close to a known interval looks like a sequential walk: worth probing a day ahead.
        const val DENSE = 3_600.0
    }
}
