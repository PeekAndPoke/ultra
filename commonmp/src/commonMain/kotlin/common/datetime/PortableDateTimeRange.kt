package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable
data class PortableDateTimeRange(
    val from: PortableDateTime,
    val to: PortableDateTime
) {
    companion object {
        val forever = PortableDateTimeRange(PortableGenesisDateTime, PortableDoomsdayDateTime)
    }

    val isValid: Boolean get() = from < to

    val hasStart: Boolean get() = from.timestamp > PortableGenesisDate.timestamp

    val hasEnd: Boolean get() = to.timestamp < PortableDoomsdayDate.timestamp

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
