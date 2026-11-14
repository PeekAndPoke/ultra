package io.peekandpoke.ultra.datetime

import io.peekandpoke.ultra.common.ComputedList
import io.peekandpoke.ultra.common.slumber.Slumber
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
 *   to whole milliseconds, below zero is zero (an empty slot), and from [INFINITE_DURATION_MILLIS] on it is
 *   [Duration.INFINITE].
 */
@Serializable(with = MpLocalTimeSlotSerializer::class)
@Slumber.As(MpLocalTimeSlotRawData::class)
@ConsistentCopyVisibility
data class MpLocalTimeSlot private constructor(
    val from: MpLocalTime,
    val duration: Duration,
) {
    companion object {
        /** Durations from 2^53 - 1 ms on are [Duration.INFINITE]; stored, that is the largest integer exact in JS. */
        const val INFINITE_DURATION_MILLIS: Long = 9_007_199_254_740_991L

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
                    millis >= INFINITE_DURATION_MILLIS -> Duration.INFINITE
                    else -> millis.milliseconds
                },
            )
        }

        /**
         * The slot from [from] to [to] on the clock: [to] after [from] is the difference, [to] before [from] runs
         * overnight, and equal times give an empty slot. A [to] of [MpLocalTime.EndOfDay] is 24:00.
         */
        fun of(from: MpLocalTime, to: MpLocalTime): MpLocalTimeSlot {
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

    private val durationMillis: Long get() = if (isInfinite) Long.MAX_VALUE else duration.inWholeMilliseconds

    private val coversClock: Boolean get() = durationMillis >= DAY

    /** True for a duration of zero. */
    val isEmpty: Boolean get() = duration == Duration.ZERO

    /** True for [Duration.INFINITE]: on a date the slot has an open end. */
    val isInfinite: Boolean get() = duration.isInfinite()

    /** True when the slot is not empty. */
    val isValid: Boolean get() = !isEmpty

    val isNotValid: Boolean get() = !isValid

    /**
     * The clock time of the end; [MpLocalTime.EndOfDay] for an end exactly at midnight. An empty or infinite slot gives
     * [from].
     */
    val to: MpLocalTime
        get() {
            if (isEmpty || isInfinite) return from

            val end = fromMillis + durationMillis

            return if (end % DAY == 0L) MpLocalTime.EndOfDay else timeAt(end)
        }

    /**
     * On how many days after the start's date the end lies: 0 for a slot ending by 24:00, 1 for 22:00 to 02:00.
     * [Long.MAX_VALUE] for an infinite slot.
     */
    val endDayOffset: Long
        get() {
            if (isEmpty) return 0L
            if (isInfinite) return Long.MAX_VALUE

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
        if (isEmpty || other.isEmpty || coversClock || other.coversClock) return false

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
     * - Returns a lazy view ([ComputedList]): each part is computed when read, so any count is free to create.
     * - Parts never reach past the end; a remainder shorter than [duration] is dropped.
     * - An empty slot, or a [duration] under one millisecond, gives no parts; a negative [gap] counts as zero.
     * - The count is capped at [Int.MAX_VALUE] (an infinite slot), as a list cannot be larger.
     */
    fun splitWithGaps(duration: Duration, gap: Duration = Duration.ZERO): List<MpLocalTimeSlot> {
        val part = of(MpLocalTime.Min, duration)

        if (isEmpty || part.isEmpty) return emptyList()

        if (part.isInfinite) return if (isInfinite) listOf(this) else emptyList()

        val partMillis = part.durationMillis
        val gapMillis = gap.inWholeMilliseconds.coerceIn(0L, INFINITE_DURATION_MILLIS)
        val step = partMillis + gapMillis

        val count = when {
            partMillis > durationMillis -> 0L
            else -> (durationMillis - partMillis) / step + 1
        }.coerceAtMost(Int.MAX_VALUE.toLong())

        // the start of part i on the clock: (from + i * step) mod day; the step taken mod day keeps i * step small
        val stepOnClock = step % DAY

        return ComputedList(count.toInt()) { index ->
            MpLocalTimeSlot(from = timeAt(fromMillis + index * stepOnClock), duration = part.duration)
        }
    }
}

/**
 * This slot on [date] in [timezone], by the clock: from [MpLocalTimeSlot.from] on [date] to [MpLocalTimeSlot.to] on
 * the day [MpLocalTimeSlot.endDayOffset] later. A start or end in a skipped hour moves forward; a start that then lies
 * past the end, or an empty slot, gives an empty range at the start.
 */
internal fun MpLocalTimeSlot.onDate(date: MpLocalDate, timezone: MpTimezone): MpZonedDateTimeRange {
    val start = date.atTime(from, timezone)

    if (isEmpty) return MpZonedDateTimeRange(from = start, to = start)

    val end = endOnDate(date, timezone)

    return when {
        end.toInstant() > start.toInstant() -> MpZonedDateTimeRange(from = start, to = end)
        else -> MpZonedDateTimeRange(from = start, to = start)
    }
}

/**
 * The end of this slot on [date]. The only place that decides what an end beyond the open end is: an infinite slot
 * gets the range's open end, today [MpZonedDateTime.Doomsday] (whatever ULTRA-00343 decides goes here).
 */
private fun MpLocalTimeSlot.endOnDate(date: MpLocalDate, timezone: MpTimezone): MpZonedDateTime {
    if (isInfinite) return MpZonedDateTime.Doomsday

    // a finite duration is below 2^53 ms, so the offset is below 2^27 days: an Int
    return date.plusDays(endDayOffset.toInt()).atTime(to, timezone)
}

fun MpLocalTimeSlot.formatHhMm(): String {
    return "${from.formatHhMm()} - ${to.formatHhMm()}"
}

fun MpLocalTimeSlot.formatHhMmSs(): String {
    return "${from.formatHhMmSs()} - ${to.formatHhMmSs()}"
}
