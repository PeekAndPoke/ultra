package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class KronosDescriptor {

    @Serializable
    @SerialName("system-clock")
    object SystemClock : KronosDescriptor()

    @Serializable
    @SerialName("advanced-by")
    data class AdvancedBy(
        val durationMs: Long,
        val inner: KronosDescriptor,
    ) : KronosDescriptor()
}
