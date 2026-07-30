package io.peekandpoke.funktor.auth.model

import io.peekandpoke.ultra.security.user.UserId
import kotlinx.serialization.Serializable

@Serializable
data class AuthSetPasswordRequest(
    val provider: String,
    val userId: UserId,
    val currentPassword: String,
    val newPassword: String,
)
