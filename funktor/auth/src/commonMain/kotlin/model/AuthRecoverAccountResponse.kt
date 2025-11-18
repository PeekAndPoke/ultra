package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthRecoverAccountResponse {

    @Serializable
    @SerialName("init-password-reset")
    data object InitPasswordReset : AuthRecoverAccountResponse

    @Serializable
    @SerialName("validate-password-reset-token")
    data class ValidatePasswordResetToken(
        val success: Boolean,
    ) : AuthRecoverAccountResponse

    @Serializable
    @SerialName("set-password-with-token")
    data class SetPasswordWithToken(
        val success: Boolean,
    ) : AuthRecoverAccountResponse
}
