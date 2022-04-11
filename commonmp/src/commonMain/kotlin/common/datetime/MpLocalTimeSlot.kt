package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
// TODO: test me
data class MpLocalTimeSlot(
    val from: MpLocalTime,
    val to: MpLocalTime,
) {
    companion object {
        val completeDay: MpLocalTimeSlot = MpLocalTimeSlot(
            from = MpLocalTime.Min,
            to = MpLocalTime.Max,
        )

        fun ofSecondsOfDay(from: Long, to: Long): MpLocalTimeSlot {
            return MpLocalTimeSlot(
                from = MpLocalTime.ofSecondOfDay(from),
                to = MpLocalTime.ofSecondOfDay(to),
            )
        }
    }

    val isValid: Boolean
        get() = duration > Duration.ZERO

    val isNotValid: Boolean
        get() = !isValid

    val duration: Duration
        get() = to - from

    /**
     * Returns 'true' when this timeslot touches or intersects the [other] time slot.
     */
    fun touches(other: MpLocalTimeSlot): Boolean {
        return to >= other.from && from <= other.to
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
            other.isNotValid -> listOf(this)

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
     */
    fun splitWithGaps(duration: Duration, gap: Duration = Duration.ZERO): List<MpLocalTimeSlot> {

        if (duration <= Duration.ZERO) {
            return emptyList()
        }

        val effectiveGap = maxOf(Duration.ZERO, gap)

        val result = mutableListOf<MpLocalTimeSlot>()

        var currentFrom = from
        var currentTo = currentFrom.plus(duration)

        while (currentTo <= to) {
            result.add(
                MpLocalTimeSlot(currentFrom, currentTo)
            )

            currentFrom = currentTo.plus(effectiveGap)
            currentTo = currentFrom.plus(duration)
        }

        return result.toList()
    }
}

fun MpLocalTimeSlot.formatHhMm(): String {
    return "${from.formatHhMm()} - ${to.formatHhMm()}"
}

fun MpLocalTimeSlot.formatHhMmSs(): String {
    return "${from.formatHhMmSs()} - ${to.formatHhMmSs()}"
}
