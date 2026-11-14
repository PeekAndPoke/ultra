package io.peekandpoke.ultra.datetime

import io.peekandpoke.ultra.common.ComputedList
import io.peekandpoke.ultra.common.slumber.Slumber
import kotlinx.datetime.DateTimeArithmeticException
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A slot of the day: a start ([from]) and a [duration], e.g. 22:00 for 4 hours (ULTRA-00341).
 *
 * - [to] is derived: the clock time of the end ([MpLocalTime.EndOfDay] when it ends exactly at midnight), and
 *   [endDayOffset] says how many days later. A slot may cross midnight and may be longer than a day.
 * - Without a date a slot is a daily pattern on the 24-hour clock: [contains], [intersects], [touches],
 *   [isAdjacentTo], [mergeWith] and [cutAway] work on the clock face, and a slot of 24 hours or more covers it all.
 * - On a date ([MpLocalDate.toRange]) the slot ends by the clock, so a night slot is 7 or 9 real hours on DST nights.
 * - Created only by the factories, which normalise: the start wraps into the day (24:00 is 00:00), the duration is cut
 *   to whole milliseconds, below zero is zero (an empty slot), and every length from [MAX_DURATION_MILLIS] on,
 *   [Duration.INFINITE] included, is cut to it: a slot is always finite (ULTRA-00343).
 */
@Serializable(with = MpLocalTimeSlotSerializer::class)
@Slumber.As(MpLocalTimeSlotRawData::class)
@ConsistentCopyVisibility
data class MpLocalTimeSlot private constructor(
    val from: MpLocalTime,
    val duration: Duration,
) {
    companion object {
        /**
         * The longest slot, 2^53 - 1 ms (about 285 616 years): the largest whole number a JS client reads and writes
         * exactly, so a stored length round-trips. Every longer length, [Duration.INFINITE] included, is cut to it.
         */
        const val MAX_DURATION_MILLIS: Long = 9_007_199_254_740_991L

        private const val DAY: Long = MpLocalTime.MILLIS_PER_DAY.toLong()

        /** The whole day, 00:00 for 24 hours; its [to] is [MpLocalTime.EndOfDay]. */
        val completeDay: MpLocalTimeSlot = of(MpLocalTime.Min, DAY.milliseconds)

        /** The slot from [from] for [duration]; see the class for how both are normalised. */
        fun of(from: MpLocalTime, duration: Duration): MpLocalTimeSlot {
            val millis = duration.inWholeMilliseconds

            return MpLocalTimeSlot(
                from = timeAt(from.inWholeMilliSeconds().toLong()),
                duration = when {
                    millis <= 0L -> Duration.ZERO
                    millis >= MAX_DURATION_MILLIS -> MAX_DURATION_MILLIS.milliseconds
                    else -> millis.milliseconds
                },
            )
        }

        /**
         * The slot from [from] to [to] on the clock: [to] after [from] is the difference, [to] before [from] runs
         * overnight, and equal times give an empty slot (also 24:00 to 24:00). A [to] of [MpLocalTime.EndOfDay] is
         * 24:00, so 00:00 to 24:00 is the whole day.
         */
        fun of(from: MpLocalTime, to: MpLocalTime): MpLocalTimeSlot {
            if (from == to) return of(from = from, duration = Duration.ZERO)

            val start = from.inWholeMilliSeconds().toLong().mod(DAY)
            val end = if (to.isEndOfDay) DAY else to.inWholeMilliSeconds().toLong().mod(DAY)

            val length = when {
                end == start -> 0L
                end > start -> end - start
                else -> end + DAY - start
            }

            return of(from = from, duration = length.milliseconds)
        }

        /** The slot between two seconds of the day, by the rules of `of(from, to)`; a [to] of 86 400 is 24:00. */
        fun ofSecondsOfDay(from: Int, to: Int): MpLocalTimeSlot {
            val start = MpLocalTime.ofSecondOfDay(from)

            if (from == to) return of(from = start, duration = Duration.ZERO)

            return of(
                from = start,
                to = if (to == MpLocalTime.SECONDS_PER_DAY) MpLocalTime.EndOfDay else MpLocalTime.ofSecondOfDay(to),
            )
        }

        private fun timeAt(millis: Long): MpLocalTime = MpLocalTime.ofMilliSeconds(millis.mod(DAY).toInt())
    }

    /** Two time pickers in a form: both ends as clock times, either one not filled in yet. */
    @Serializable
    data class Partial(
        val from: MpLocalTime?,
        val to: MpLocalTime?,
    ) {
        companion object {
            val empty = Partial(from = null, to = null)
        }

        /** The slot `of(from, to)`, or null when an end is missing or the slot is empty. */
        fun asValidRange(): MpLocalTimeSlot? = when (from != null && to != null) {
            true -> of(from = from, to = to).takeIf { it.isValid }
            false -> null
        }
    }

    /** A half-open arc `[start, end)` on the clock face, in milliseconds of the day. */
    private data class Arc(val start: Long, val end: Long)

    private val fromMillis: Long get() = from.inWholeMilliSeconds().toLong()

    private val durationMillis: Long get() = duration.inWholeMilliseconds

    private val coversClock: Boolean get() = durationMillis >= DAY

    /** True for a duration of zero. */
    val isEmpty: Boolean get() = duration == Duration.ZERO

    /** True when the slot is not empty. */
    val isValid: Boolean get() = !isEmpty

    val isNotValid: Boolean get() = !isValid

    /** The clock time of the end; [MpLocalTime.EndOfDay] for an end exactly at midnight. An empty slot gives [from]. */
    val to: MpLocalTime
        get() {
            if (isEmpty) return from

            val end = fromMillis + durationMillis

            return if (end % DAY == 0L) MpLocalTime.EndOfDay else timeAt(end)
        }

    /**
     * On how many days after the start's date the end lies: 0 for a slot ending by 24:00, 1 for 22:00 to 02:00. At most
     * 104 249 991 (the longest slot), so `toInt()` is always safe.
     */
    val endDayOffset: Long
        get() {
            if (isEmpty) return 0L

            val end = fromMillis + durationMillis

            return if (end % DAY == 0L) end / DAY - 1 else end / DAY
        }

    /** True when the slot runs past midnight; one ending exactly at 24:00 does not. */
    val crossesMidnight: Boolean get() = endDayOffset > 0L

    /** The two clock pickers of a form; lossy for a slot of 24 hours or more. */
    fun asPartialRange(): Partial {
        return Partial(from = from, to = to)
    }

    private fun arcs(): List<Arc> = when {
        isEmpty -> emptyList()
        coversClock -> listOf(Arc(0L, DAY))
        fromMillis + durationMillis <= DAY -> listOf(Arc(fromMillis, fromMillis + durationMillis))
        else -> listOf(Arc(fromMillis, DAY), Arc(0L, fromMillis + durationMillis - DAY))
    }

    /** The arcs as closed intervals; an empty slot is the point at its start. */
    private fun closedArcs(): List<Arc> = if (isEmpty) listOf(Arc(fromMillis, fromMillis)) else arcs()

    /** The end on the clock face, `[0, DAY)`. */
    private val endOnClock: Long get() = (fromMillis + durationMillis).mod(DAY)

    /** True when [time] lies in the slot on the clock face; 24:00 counts as 00:00. An empty slot contains nothing. */
    fun contains(time: MpLocalTime): Boolean {
        val point = time.inWholeMilliSeconds().toLong().mod(DAY)

        return arcs().any { point >= it.start && point < it.end }
    }

    /** True when every part of [other] lies in this slot on the clock face; never for an empty slot on either side. */
    fun contains(other: MpLocalTimeSlot): Boolean {
        if (isEmpty || other.isEmpty) return false

        val mine = arcs()

        return other.arcs().all { o -> mine.any { it.start <= o.start && o.end <= it.end } }
    }

    /** True when both share a positive length on the clock face; adjacent slots do NOT intersect. */
    fun intersects(other: MpLocalTimeSlot): Boolean {
        val theirs = other.arcs()

        return arcs().any { a -> theirs.any { b -> maxOf(a.start, b.start) < minOf(a.end, b.end) } }
    }

    /**
     * True when the union of both slots is contiguous on the clock face: they overlap or share an end, also across
     * midnight. An empty slot touches a slot whose closed arc holds its point.
     */
    fun touches(other: MpLocalTimeSlot): Boolean {
        val mine = closedArcs()
        val theirs = other.closedArcs()

        val overlapOrMeet = mine.any { a -> theirs.any { b -> a.start <= b.end && b.start <= a.end } }
        val meetAtMidnight = (mine.any { it.end == DAY } && theirs.any { it.start == 0L }) ||
                (theirs.any { it.end == DAY } && mine.any { it.start == 0L })

        return overlapOrMeet || meetAtMidnight
    }

    /**
     * True when one ends where the other begins, without overlap. Empty slots and slots of 24 hours or more never are.
     */
    fun isAdjacentTo(other: MpLocalTimeSlot): Boolean {
        // a slot of 24 hours or more intersects every non-empty slot, so it never gets past the intersects check
        if (isEmpty || other.isEmpty) return false

        return !intersects(other) && (endOnClock == other.fromMillis || other.endOnClock == fromMillis)
    }

    /**
     * Touching slots give one slot for their union on the clock face, starting where the union starts; a union that
     * covers the whole clock is this slot's start for 24 hours, and a slot of 24 hours or more absorbs the other.
     * Slots that do not touch stay two, sorted by start.
     */
    fun mergeWith(other: MpLocalTimeSlot): List<MpLocalTimeSlot> {
        if (!touches(other)) return listOf(this, other).sortedBy { it.fromMillis }

        if (coversClock || other.coversClock) return listOf(if (other.durationMillis > durationMillis) other else this)

        // the other's start, seen from this start
        val offset = (other.fromMillis - fromMillis).mod(DAY)

        val (start, length) = when {
            offset <= durationMillis -> fromMillis to maxOf(durationMillis, offset + other.durationMillis)
            else -> other.fromMillis to maxOf(other.durationMillis, DAY - offset + durationMillis)
        }

        return when {
            length >= DAY -> listOf(MpLocalTimeSlot(from = from, duration = DAY.milliseconds))
            else -> listOf(MpLocalTimeSlot(from = timeAt(start), duration = length.milliseconds))
        }
    }

    /**
     * Removes the time of [other] from this slot, on the clock face and in this slot's own frame: the result is 0, 1 or
     * 2 slots in the order they follow from [from]. A slot of 24 hours or more is cut as the whole clock from [from];
     * its length beyond 24 hours is dropped from the pieces. Nothing to cut gives this slot unchanged.
     */
    fun cutAway(other: MpLocalTimeSlot): List<MpLocalTimeSlot> {
        if (!intersects(other)) return listOf(this)

        val cuts = when {
            other.coversClock -> listOf(Arc(0L, DAY))
            else -> {
                val start = (other.fromMillis - fromMillis).mod(DAY)
                val end = start + other.durationMillis

                listOfNotNull(Arc(start, minOf(end, DAY)), if (end > DAY) Arc(0L, end - DAY) else null)
            }
        }

        val pieces = cuts.fold(listOf(Arc(0L, minOf(durationMillis, DAY)))) { remaining, cut ->
            remaining.flatMap { piece ->
                when {
                    cut.end <= piece.start || piece.end <= cut.start -> listOf(piece)
                    else -> listOfNotNull(
                        Arc(piece.start, cut.start).takeIf { cut.start > piece.start },
                        Arc(cut.end, piece.end).takeIf { cut.end < piece.end },
                    )
                }
            }
        }

        return pieces.sortedBy { it.start }.map {
            MpLocalTimeSlot(from = timeAt(fromMillis + it.start), duration = (it.end - it.start).milliseconds)
        }
    }

    /**
     * Splits the slot into parts of [duration], with [gap] between them, walking the slot's own length from [from]
     * (overnight and longer than a day work).
     *
     * - Returns a lazy view ([ComputedList], read-only, not a `MutableList`): each part is computed when read.
     * - At most [Int.MAX_VALUE] parts, for any slot (a list cannot be larger): beyond that the parts do not cover the
     *   whole slot (`of(00:00, 30.days).splitWithGaps(1.milliseconds)` would need 2 592 000 000).
     * - With user input, check `size` before `map` or `toList()`: they build every part, and nothing else bounds the
     *   count.
     * - Parts never reach past the end; a remainder shorter than [duration] is dropped.
     * - An empty slot, or a [duration] under one millisecond, gives no parts; a negative [gap] counts as zero.
     * - A [duration] or [gap] beyond [MAX_DURATION_MILLIS], [Duration.INFINITE] included, counts as that length.
     * - The parts carry no day: `date.toRange(part, zone)` puts a part after midnight on the morning of `date`, not of
     *   the next day. Part `i` starts `(from + i * (duration + gap)) / 24 h` days after the slot's own date.
     */
    fun splitWithGaps(duration: Duration, gap: Duration = Duration.ZERO): List<MpLocalTimeSlot> {
        val part = of(MpLocalTime.Min, duration)

        if (isEmpty || part.isEmpty) return emptyList()

        val partMillis = part.durationMillis
        val gapMillis = gap.inWholeMilliseconds.coerceIn(0L, MAX_DURATION_MILLIS)
        val step = partMillis + gapMillis

        val count = when {
            partMillis > durationMillis -> 0L
            else -> (durationMillis - partMillis) / step + 1
        }.coerceAtMost(Int.MAX_VALUE.toLong())

        // the start of part i on the clock: (from + i * step) mod day; i * step is at most the slot's own length
        // (2^53 - 1 ms), so it never overflows
        return ComputedList(count.toInt()) { index ->
            MpLocalTimeSlot(from = timeAt(fromMillis + index * step), duration = part.duration)
        }
    }
}

/**
 * This slot on [date] in [timezone], by the clock: from [MpLocalTimeSlot.from] on [date] to [MpLocalTimeSlot.to] on
 * the day [MpLocalTimeSlot.endDayOffset] later. Always a two-ended range.
 *
 * - A start or end that falls into a skipped hour moves to the first existing time, the transition: 02:30 in a 02:00
 *   to 03:00 gap is 03:00. (`MpZonedDateTime.of` and `MpLocalDate.atTime` move it by the length of the gap, to 03:30.)
 * - A time in a repeated hour takes its first occurrence. So the end never lies before the start, and an empty slot
 *   gives an empty range at its start.
 * - The platform's limits: throw when no correct answer exists, fall back when the answer is still right for
 *   everything the platform can express. A START the platform cannot represent throws [IllegalArgumentException],
 *   naming the date, the zone and the limit (only JS has one: in a region zone `Date` ends at +/-8.64e15 ms,
 *   -271821-04-20 to +275760-09-13). An END beyond what the platform can represent stops at its last representable
 *   instant: on JS in a region zone +275760-09-13T00:00Z, on every platform the last instant of 999 999 999-12-31. So
 *   the same call can give a different end on JS than on JVM and native once the end lies past the year 275760.
 */
internal fun MpLocalTimeSlot.onDate(date: MpLocalDate, timezone: MpTimezone): MpZonedDateTimeRange {
    return MpZonedDateTimeRange(from = startOnDate(date, timezone), to = endOnDate(date, timezone))
}

/** The start of this slot on [date]; throws when the platform cannot represent it (see [onDate]). */
private fun MpLocalTimeSlot.startOnDate(date: MpLocalDate, timezone: MpTimezone): MpZonedDateTime {
    return try {
        firstExistingTime(date, from, timezone)
    } catch (e: IllegalTimeZoneException) {
        throw e
    } catch (e: IllegalArgumentException) {
        val limit = platformInstantLimitMillis(timezone.id) ?: throw e

        throw IllegalArgumentException(
            "The start of a time slot, ${date.toIsoString().substringBefore('T')} ${from.formatHhMmSs()} in " +
                    "${timezone.id}, lies outside what this platform can represent in that zone: " +
                    "${MpInstant.fromEpochMillis(-limit).toIsoString()} to " +
                    MpInstant.fromEpochMillis(limit).toIsoString(),
            e,
        )
    }
}

/**
 * The end of this slot on [date]: when the platform cannot represent it, its last representable instant (see
 * [onDate]). The only place that decides it.
 */
private fun MpLocalTimeSlot.endOnDate(date: MpLocalDate, timezone: MpTimezone): MpZonedDateTime {
    return try {
        // the longest slot ends 104 249 991 days later at most: an Int
        firstExistingTime(date.plusDays(endDayOffset.toInt()), to, timezone)
    } catch (_: IllegalArgumentException) {
        // the zone is valid (the start resolved in it): the platform cannot resolve an instant this far out
        lastRepresentableTime(timezone)
    } catch (_: DateTimeArithmeticException) {
        // the end's date lies past the last date the platform holds (year 999 999 999)
        lastRepresentableTime(timezone)
    }
}

/** The last instant this platform represents in [timezone]: its zone-engine limit, else the end of the last date. */
private fun lastRepresentableTime(timezone: MpTimezone): MpZonedDateTime {
    platformInstantLimitMillis(timezone.id)?.let { return MpInstant.fromEpochMillis(it).atZone(timezone) }

    return MpLocalDate.of(999_999_999, 12, 31).atTime(MpLocalTime.Max, timezone)
}

/**
 * [time] on [date] in [timezone]; a time that does not exist is the first existing time after it, 24:00 the next day.
 */
private fun firstExistingTime(date: MpLocalDate, time: MpLocalTime, timezone: MpTimezone): MpZonedDateTime {
    // 24:00 needs no branch: MpLocalDateTime.of(date, EndOfDay) is 00:00 of the next day
    val instant = TimezoneEngine.toInstantOrTransition(timezone, MpLocalDateTime.of(date, time).value)

    return MpZonedDateTime.of(instant = MpInstant(instant), timezone = timezone)
}

/**
 * The slot as clock times, e.g. `22:00 - 02:00`. It drops the days: an empty slot, 24 hours and 48 hours from one start
 * print the same text.
 */
fun MpLocalTimeSlot.formatHhMm(): String {
    return "${from.formatHhMm()} - ${to.formatHhMm()}"
}

/** As [formatHhMm], with seconds; the same caveat about the days. */
fun MpLocalTimeSlot.formatHhMmSs(): String {
    return "${from.formatHhMmSs()} - ${to.formatHhMmSs()}"
}
