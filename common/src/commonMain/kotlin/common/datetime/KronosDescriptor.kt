package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class KronosDescriptor {

    /**
     * Creates a [Kronos] from the given descriptor
     */
    fun instantiate(): Kronos {
        return Kronos.from(this)
    }

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
