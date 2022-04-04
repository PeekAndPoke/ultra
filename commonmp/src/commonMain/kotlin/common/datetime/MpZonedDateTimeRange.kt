package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

// TODO: test me
@Serializable
data class MpZonedDateTimeRange(
    val from: MpZonedDateTime,
    val to: MpZonedDateTime
) {
    companion object {
        val forever = MpZonedDateTimeRange(MpZonedDateTime.Genesis, MpZonedDateTime.Doomsday)
    }
}
