package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthSignUpRequest {
    val provider: String

    /**
     * Email first: no password. The activation mail's link opens "Choose your password", and setting it activates
     * the account ([AuthActivateAccountRequest.password]), so an account that has not proven its address never
     * holds a credential.
     */
    @Serializable
    @SerialName("email_and_password")
    data class EmailAndPassword(
        override val provider: String,
        val email: String,
        val displayName: String? = null,
    ) : AuthSignUpRequest

    @Serializable
    @SerialName("oauth")
    data class OAuth(
        override val provider: String,
        val token: String,
    ) : AuthSignUpRequest
}
