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

    val hasStart get() = from.timestamp > Genesis.timestamp

    val hasEnd get() = to.timestamp < Doomsday.timestamp
}
