package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable
data class PortableDateTimeRange(
    val from: PortableDateTime,
    val to: PortableDateTime
) {
    companion object {
        val forever = PortableDateTimeRange(GenesisDateTime, DoomsdayDateTime)
    }

    val isValid: Boolean = from < to

    val hasStart get() = from.timestamp > Genesis.timestamp

    val hasEnd get() = to.timestamp < Doomsday.timestamp

    fun contains(date: PortableDateTime): Boolean {
        return date >= from && date < to
    }

    fun contains(other: PortableDateTimeRange): Boolean {
        return (isValid && other.isValid) &&
                (from <= other.from && to >= other.to)
    }

    fun intersects(other: PortableDateTimeRange): Boolean {
        return (isValid && other.isValid) && (
                (other.from >= from && other.from < to) ||
                        (other.to > from && other.to <= to) ||
                        contains(other) ||
                        other.contains(this)
                )
    }
}
