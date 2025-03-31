package de.peekandpoke.funktor.core.model

import kotlinx.serialization.Serializable

@Serializable
data class InsightsConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "",
) {
    companion object {
        val Disabled = InsightsConfig(enabled = false)
    }
}
