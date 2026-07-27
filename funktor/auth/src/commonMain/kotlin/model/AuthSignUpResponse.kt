package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthSignUpResponse(
    val signIn: AuthSignInResponse? = null,
    val requiresActivation: Boolean = false,
) {
    companion object {
        val failed = AuthSignUpResponse(signIn = null, requiresActivation = false)
    }

    /**
     * Did the sign-up succeed?
     *
     * NOT "am I signed in": an account that must be activated first legitimately gets no session, and
     * the sign-up still succeeded. Callers that want the session check [signIn] for null.
     */
    val success: Boolean = signIn != null || requiresActivation
}
