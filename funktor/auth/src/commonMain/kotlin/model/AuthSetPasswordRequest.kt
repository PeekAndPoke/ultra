package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthSetPasswordRequest(
    val provider: String,
    val userId: String,
    val newPassword: String,
)
