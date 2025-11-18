package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthSignUpRequest {
    val provider: String

    @Serializable
    @SerialName("email_and_password")
    data class EmailAndPassword(
        override val provider: String,
        val email: String,
        val displayName: String? = null,
        val password: String,
    ) : AuthSignUpRequest

    @Serializable
    @SerialName("oauth")
    data class OAuth(
        override val provider: String,
        val token: String,
    ) : AuthSignUpRequest
}
