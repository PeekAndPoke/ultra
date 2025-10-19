package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthRecoveryRequest {

    @Serializable
    @SerialName("reset_password")
    data class ResetPassword(
        override val provider: String,
        val email: String,
    ) : AuthRecoveryRequest

    val provider: String
}
