package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthRecoveryResponse(
    val success: Boolean,
) {
    companion object {
        val success = AuthRecoveryResponse(success = true)

        val failed = AuthRecoveryResponse(success = false)
    }
}
