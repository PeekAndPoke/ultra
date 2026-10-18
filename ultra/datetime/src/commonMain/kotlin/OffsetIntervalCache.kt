package io.peekandpoke.ultra.datetime

/**
 * Remembers intervals in which one zone's offset is constant, so most lookups are a range check instead of a call to
 * [lookup] (an `Intl` call on JS).
 *
 * Relies on what the IANA data guarantees (pinned by `OffsetIntervalCacheJvmSpec`): no two transitions within a day of
 * each other. So two seconds at most a day apart with the same offset have that offset in between, and between two
 * such seconds with different offsets there is exactly one transition.
 *
 * Intervals grow lazily: a miss far from anything known costs one lookup and is remembered as a single second; a miss
 * within a day of a known interval extends it, and a miss within an hour of one also probes a day further in the
 * direction of travel, so a sequential caller pays about two lookups per day it walks. Not thread-safe.
 */
internal class OffsetIntervalCache(
    private val maxIntervals: Int = 8,
    private val lookup: (epochSeconds: Long) -> Int,
) {
    /** Every second in `[from, until)` has [offset]. */
    private class Interval(var from: Long, var until: Long, val offset: Int)

    // Most recently used first.
    private val intervals = ArrayList<Interval>(maxIntervals)

    /** The offset at [epochSeconds], in seconds. */
    fun offsetSecondsAt(epochSeconds: Long): Int {
        for (i in intervals.indices) {
            val interval = intervals[i]

            if (epochSeconds >= interval.from && epochSeconds < interval.until) {
                if (i > 0) {
                    intervals.removeAt(i)
                    intervals.add(0, interval)
                }

                return interval.offset
            }
        }

        return learn(epochSeconds)
    }

    private fun learn(at: Long): Int {
        val offset = lookup(at)
        val near = nearestWithinADay(at)

        when {
            near == null -> remember(Interval(at, at + 1, offset))

            at >= near.until -> {
                val dense = at - near.until < DENSE
                val grown = when (near.offset) {
                    offset -> near.also { it.until = at + 1 }
                    else -> Interval(firstChangeAfter(near.until - 1, at, near.offset), at + 1, offset)
                        .also { near.until = it.from }
                }
                remember(grown)
                if (dense) probeAhead(grown, at, DAY)
            }

            else -> {
                val dense = near.from - at <= DENSE
                val grown = when (near.offset) {
                    offset -> near.also { it.from = at }
                    else -> Interval(at, firstChangeAfter(at, near.from, offset), offset)
                        .also { near.from = it.until }
                }
                remember(grown)
                if (dense) probeAhead(grown, at, -DAY)
            }
        }

        return offset
    }

    /** Extends [interval], which ends (or starts) at [at], by up to a day in the direction of [step]. */
    private fun probeAhead(interval: Interval, at: Long, step: Long) {
        val probe = at + step
        val offset = lookup(probe)

        when {
            offset == interval.offset && step > 0 -> interval.until = probe + 1
            offset == interval.offset -> interval.from = probe
            step > 0 -> {
                interval.until = firstChangeAfter(at, probe, interval.offset)
                remember(Interval(interval.until, probe + 1, offset))
            }
            else -> {
                interval.from = firstChangeAfter(probe, at, offset)
                remember(Interval(probe, interval.from, offset))
            }
        }

        absorbNeighbours(interval)
    }

    /** The interval closest to [at], if one is at most a day away. */
    private fun nearestWithinADay(at: Long): Interval? {
        var best: Interval? = null
        var bestDistance = DAY + 1

        for (interval in intervals) {
            val distance = if (at < interval.from) interval.from - at else at - (interval.until - 1)

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
    private fun firstChangeAfter(lo: Long, hi: Long, offset: Int): Long {
        var a = lo
        var b = hi

        while (b - a > 1) {
            val mid = a + (b - a) / 2
            if (lookup(mid) == offset) a = mid else b = mid
        }

        return b
    }

    private companion object {
        const val DAY = 86_400L

        // A miss this close to a known interval looks like a sequential walk: worth probing a day ahead.
        const val DENSE = 3_600L
    }
}
