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

    val hasStart: Boolean get() = from.timestamp > Genesis.timestamp

    val hasEnd: Boolean get() = to.timestamp < Doomsday.timestamp

    val isOpen: Boolean get() = !hasStart || !hasEnd

    val isNotOpen: Boolean get() = !isOpen

    val isValid: Boolean get() = from < to

    val asPartialDateRange: PartialPortableDateRange get() = PartialPortableDateRange(from = from, to = to)

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
