package io.peekandpoke.funktor.core.model

import kotlinx.serialization.Serializable

/** Configuration for the Funktor insights panel (debug toolbar). */
@Serializable
data class InsightsConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "",
) {
    companion object {
        val Disabled = InsightsConfig(enabled = false)
    }
}
