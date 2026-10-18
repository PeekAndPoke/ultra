package io.peekandpoke.ultra.datetime

/**
 * Remembers intervals in which one zone's offset is constant, so most lookups are a range check instead of a call to
 * [lookup] (an `Intl` call on JS).
 *
 * Trusts what [TimezoneEngine] already assumes: no two transitions within a day of each other. So equal offsets one day
 * apart mean a constant offset in between, and an interval grows by at most a day per probe. Not thread-safe.
 */
internal class OffsetIntervalCache(
    private val maxIntervals: Int = 4,
    private val lookup: (epochSeconds: Long) -> Int,
) {
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

        return remember(epochSeconds).offset
    }

    private fun remember(epochSeconds: Long): Interval {
        val offset = lookup(epochSeconds)

        val from = when (lookup(epochSeconds - DAY)) {
            offset -> epochSeconds - DAY
            else -> lastChangeBefore(epochSeconds, offset)
        }

        val until = when (lookup(epochSeconds + DAY)) {
            offset -> epochSeconds + DAY
            else -> firstChangeAfter(epochSeconds, offset)
        }

        // Join an interval with the same offset that this one touches, so a sequential caller keeps ONE interval.
        intervals.firstOrNull { it.offset == offset && it.from <= until && from <= it.until }?.let { joined ->
            joined.from = minOf(joined.from, from)
            joined.until = maxOf(joined.until, until)
            intervals.remove(joined)
            intervals.add(0, joined)
            return joined
        }

        if (intervals.size == maxIntervals) intervals.removeAt(intervals.lastIndex)

        return Interval(from, until, offset).also { intervals.add(0, it) }
    }

    /** The first second after [epochSeconds], within a day, whose offset is not [offset]. */
    private fun firstChangeAfter(epochSeconds: Long, offset: Int): Long {
        var lo = epochSeconds
        var hi = epochSeconds + DAY

        while (hi - lo > 1) {
            val mid = lo + (hi - lo) / 2
            if (lookup(mid) == offset) lo = mid else hi = mid
        }

        return hi
    }

    /** The first second at or before [epochSeconds], within a day, from which the offset is [offset]. */
    private fun lastChangeBefore(epochSeconds: Long, offset: Int): Long {
        var lo = epochSeconds - DAY
        var hi = epochSeconds

        while (hi - lo > 1) {
            val mid = lo + (hi - lo) / 2
            if (lookup(mid) == offset) hi = mid else lo = mid
        }

        return hi
    }

    private companion object {
        const val DAY = 86_400L
    }
}
