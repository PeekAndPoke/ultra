package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthSignUpRequest {
    val provider: String

    /**
     * Email first: the address only. The activation mail's link opens "Choose your password", and setting it and
     * the display name activates the account ([AuthActivateAccountRequest]), so an account that has not proven its
     * address holds no credential and nothing its creator chose.
     */
    @Serializable
    @SerialName("email_and_password")
    data class EmailAndPassword(
        override val provider: String,
        val email: String,
    ) : AuthSignUpRequest

    @Serializable
    @SerialName("oauth")
    data class OAuth(
        override val provider: String,
        val token: String,
    ) : AuthSignUpRequest
}
