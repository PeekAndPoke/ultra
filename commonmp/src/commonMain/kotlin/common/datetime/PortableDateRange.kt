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
}
