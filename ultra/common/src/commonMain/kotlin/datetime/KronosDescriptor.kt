package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface KronosDescriptor {

    @SerialName("system-clock")
    @Serializable
    object SystemClock : KronosDescriptor

    @SerialName("advanced-by")
    @Serializable
    data class AdvancedBy(
        val durationMs: Long,
        val inner: KronosDescriptor,
    ) : KronosDescriptor

    /**
     * Creates a [Kronos] from the given descriptor
     */
    fun instantiate(): Kronos {
        return Kronos.from(this)
    }
}
