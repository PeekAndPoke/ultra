package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable

@Serializable
data class MpInstantRange(
    val from: MpInstant,
    val to: MpInstant
) {
    companion object {
        val forever = MpInstantRange(MpInstant.Genesis, MpInstant.Doomsday)
    }

    val hasStart: Boolean get() = from > MpInstant.Genesis

    val hasEnd: Boolean get() = to < MpInstant.Doomsday

    val isOpen: Boolean get() = !hasStart || !hasEnd

    val isNotOpen: Boolean get() = !isOpen

    val isValid: Boolean get() = from < to

    // TODO: Test
    fun contains(date: MpLocalDateTime, timezone: TimeZone): Boolean {
        return contains(date.toInstant(timezone))
    }

    // TODO: Test
    fun contains(instant: MpInstant): Boolean {
        return instant >= from && instant < to
    }

    // TODO: Test
    fun contains(other: MpInstantRange): Boolean {
        return (isValid && other.isValid) &&
                (from <= other.from && to >= other.to)
    }

    // TODO: Test
    fun intersects(other: MpInstantRange): Boolean {
        return (isValid && other.isValid) && (
                (other.from >= from && other.from < to) ||
                        (other.to > from && other.to <= to) ||
                        contains(other) ||
                        other.contains(this)
                )
    }
}
