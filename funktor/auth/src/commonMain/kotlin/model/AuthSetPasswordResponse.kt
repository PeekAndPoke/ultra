package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthSetPasswordResponse(
    val success: Boolean,
) {
    companion object {
        val failed = AuthSetPasswordResponse(success = false)
    }
}
