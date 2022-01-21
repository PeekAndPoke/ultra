package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable
data class PortableDateRange(
    val from: PortableDate,
    val to: PortableDate
) {
    companion object {
        val forever = PortableDateRange(Genesis, Doomsday)
    }

    val hasStart get() = from.timestamp > Genesis.timestamp

    val hasEnd get() = to.timestamp < Doomsday.timestamp

    val isOpen get() = !hasStart || !hasEnd

    val isNotOpen get() = !isOpen

    val isValid: Boolean = from < to

    fun contains(date: PortableDate): Boolean {
        return date >= from && date < to
    }

    fun contains(other: PortableDateRange): Boolean {
        return (isValid && other.isValid) &&
                (from <= other.from && to >= other.to)
    }

    fun intersects(other: PortableDateRange): Boolean {
        return (isValid && other.isValid) && (
                (other.from >= from && other.from < to) ||
                        (other.to > from && other.to <= to) ||
                        contains(other) ||
                        other.contains(this)
                )
    }
}
