package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable
data class PortableTimeSlot(
    val from: PortableTime,
    val to: PortableTime,
)
