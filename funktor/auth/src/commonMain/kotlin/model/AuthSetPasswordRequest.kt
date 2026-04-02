package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthSetPasswordRequest(
    val provider: String,
    val userId: String,
    val currentPassword: String,
    val newPassword: String,
)
