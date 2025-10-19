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

@Serializable
data class AuthSignUpResponse(
    val signIn: AuthSignInResponse? = null,
    val requiresActivation: Boolean = false,
) {
    companion object {
        val failed = AuthSignUpResponse(signIn = null, requiresActivation = false)
    }

    val success: Boolean = signIn != null
}

@Serializable
data class AuthActivateRequest(
    val token: String,
)

@Serializable
data class AuthActivateResponse(
    val success: Boolean,
)
