package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable
data class MpTimeSlot(
    val from: MpLocalTime,
    val to: MpLocalTime,
)
