package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthRecoverAccountRequest {

    @Serializable
    @SerialName("init-password-reset")
    data class InitPasswordReset(
        val provider: String,
        val email: String,
    ) : AuthRecoverAccountRequest

    @Serializable
    @SerialName("validate-password-reset-token")
    data class ValidatePasswordResetToken(
        val provider: String,
        val token: String,
    ) : AuthRecoverAccountRequest

    @Serializable
    @SerialName("set-password-with-token")
    data class SetPasswordWithToken(
        val provider: String,
        val token: String,
        val password: String,
    ) : AuthRecoverAccountRequest
}
