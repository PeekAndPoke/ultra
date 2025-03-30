package de.peekandpoke.ktorfx.messaging.api

import kotlinx.serialization.Serializable

/**
 * Email destinations
 */
@Serializable
data class EmailDestination(
    val toAddresses: List<String>,
    val ccAddresses: List<String> = emptyList(),
    val bccAddresses: List<String> = emptyList(),
) {
    companion object {
        fun to(address: String) = EmailDestination(
            toAddresses = listOf(address)
        )
    }
}
