package io.peekandpoke.ultra.datetime

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class MpLocalTimeSlot(
    val from: MpLocalTime,
    val to: MpLocalTime,
) {
    companion object {
        val completeDay: MpLocalTimeSlot = MpLocalTimeSlot(
            from = MpLocalTime.Min,
            to = MpLocalTime.Max,
        )

        fun of(from: MpLocalTime, duration: Duration): MpLocalTimeSlot {
            return MpLocalTimeSlot(
                from = from,
                to = from.plus(duration),
            )
        }

        fun ofSecondsOfDay(from: Int, to: Int): MpLocalTimeSlot {
            return MpLocalTimeSlot(
                from = MpLocalTime.ofSecondOfDay(from),
                to = MpLocalTime.ofSecondOfDay(to),
            )
        }
    }

    @Serializable
    data class Partial(
        val from: MpLocalTime?,
        val to: MpLocalTime?,
    ) {
        companion object {
            val empty = Partial(from = null, to = null)
        }

        fun asValidRange(): MpLocalTimeSlot? = when (from != null && to != null) {
            true -> MpLocalTimeSlot(from = from, to = to).takeIf { it.isValid }
            false -> null
        }
    }

    val duration: Duration by lazy { to - from }

    val isValid: Boolean get() = duration > Duration.ZERO

    val isNotValid: Boolean get() = !isValid

    fun asPartialRange(): Partial {
        return Partial(from = from, to = to)
    }

    /**
     * Checks if this time slot contains the given [time].
     *
     * Returns `true` when the time slot is valid and
     * [time] >= [from] and [time] < [to].
     */
    fun contains(time: MpLocalTime): Boolean {
        return isValid && time in from..<to
    }

    /**
     * Checks if this time slot fully contains the [other] time slot.
     *
     * Returns `true` when both are valid and
     * this [from] <= other [from] and this [to] >= other [to].
     */
    fun contains(other: MpLocalTimeSlot): Boolean {
        return (isValid && other.isValid) &&
                (from <= other.from && to >= other.to)
    }

    /**
     * Checks if this time slot intersects the [other] time slot.
     *
     * Adjacent time slots (one ends where the other begins) do NOT intersect.
     */
    fun intersects(other: MpLocalTimeSlot): Boolean {
        return (isValid && other.isValid) && (
                (other.from in from..<to) ||
                        (other.to > from && other.to <= to) ||
                        contains(other) ||
                        other.contains(this)
                )
    }

    /**
     * Returns `true` when this time slot touches or overlaps the [other] time slot.
     *
     * In other words, returns `true` when the union of both time slots is contiguous (has no gap).
     */
    fun touches(other: MpLocalTimeSlot): Boolean {
        return to >= other.from && from <= other.to
    }

    /**
     * Returns `true` when this time slot is adjacent to the [other] without overlapping.
     *
     * Two time slots are adjacent when one ends exactly where the other begins.
     */
    fun isAdjacentTo(other: MpLocalTimeSlot): Boolean {
        return to == other.from || other.to == from
    }

    /**
     * Merges this timeslot with the [other] one, by taking the minimal [from] and the maximal [to]
     */
    fun mergeWith(other: MpLocalTimeSlot): List<MpLocalTimeSlot> = when {
        touches(other) -> listOf(
            MpLocalTimeSlot(
                from = minOf(from, other.from),
                to = maxOf(to, other.to),
            )
        )

        else -> listOf(this, other).sortedBy { it.from }
    }

    /**
     * Removes all overlapping time with the [other] from this time slot.
     */
    fun cutAway(other: MpLocalTimeSlot): List<MpLocalTimeSlot> {

        return when {
            // other has invalid length
            this.isNotValid || other.isNotValid -> listOf(this)

            // all eaten up
            other.from <= from && to <= other.to -> emptyList()

            // the other one is inside and cuts the timeslot into two pieces
            from < other.from && other.to < to -> listOf(
                MpLocalTimeSlot(from = from, to = other.from),
                MpLocalTimeSlot(from = other.to, to = to)
            )

            // eating away on the right side
            other.from < to && to <= other.to -> listOf(
                MpLocalTimeSlot(from = from, to = other.from)
            )

            // eating away on the left side
            other.from <= from && from < other.to -> listOf(
                MpLocalTimeSlot(from = other.to, to = to)
            )

            // Nothing happened
            else -> listOf(this)
        }
    }

    /**
     * Splits the timeslot in multiple of [duration] while keeping [gap] between each of them.
     *
     * - Slots never reach past [to]; a remainder shorter than [duration] is dropped.
     * - An invalid slot ([to] <= [from], e.g. 22:00-02:00) yields no slots.
     * - [duration] and [gap] are cut to whole milliseconds: a [duration] under one millisecond yields no slots.
     *   A negative [gap] counts as zero.
     *
     * [maxSlots] caps the result, so a caller-chosen [duration] cannot make one call allocate millions of slots
     * (a whole day in 1 ms steps is 86.4 million). A split above it throws [IllegalArgumentException] before any
     * slot is created, as does a [maxSlots] below 1.
     */
    fun splitWithGaps(
        duration: Duration,
        gap: Duration = Duration.ZERO,
        maxSlots: Int = 100_000,
    ): List<MpLocalTimeSlot> {

        require(maxSlots >= 1) { "maxSlots must be at least 1, was $maxSlots" }

        // Counts in milliseconds of day: MpLocalTime.plus wraps at midnight, so stepping with it past midnight would
        // land earlier and never end (ULTRA-00215).
        val durationMs = duration.inWholeMilliseconds

        if (durationMs <= 0L) {
            return emptyList()
        }

        val fromMs = from.inWholeMilliSeconds().toLong()
        val spanMs = to.inWholeMilliSeconds().toLong() - fromMs
        // A gap of a day or more leaves no room for a second slot either way; the cap keeps the step from overflowing.
        val gapMs = gap.inWholeMilliseconds.coerceIn(0L, MpLocalTime.MILLIS_PER_DAY.toLong())

        // Compares the remainder, not a sum: an infinite duration is Long.MAX_VALUE milliseconds.
        val count = when {
            durationMs > spanMs -> 0L
            else -> (spanMs - durationMs) / (durationMs + gapMs) + 1
        }

        require(count <= maxSlots) { "splitWithGaps would create $count slots, more than maxSlots = $maxSlots" }

        // Runs only for count >= 1, where duration and gap are each at most a day: no overflow.
        return List(count.toInt()) { index ->
            val slotFromMs = fromMs + index * (durationMs + gapMs)

            MpLocalTimeSlot(
                from = MpLocalTime.ofMilliSeconds(slotFromMs.toInt()),
                to = MpLocalTime.ofMilliSeconds((slotFromMs + durationMs).toInt()),
            )
        }
    }
}

fun MpLocalTimeSlot.formatHhMm(): String {
    return "${from.formatHhMm()} - ${to.formatHhMm()}"
}

fun MpLocalTimeSlot.formatHhMmSs(): String {
    return "${from.formatHhMmSs()} - ${to.formatHhMmSs()}"
}
