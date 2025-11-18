package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

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
