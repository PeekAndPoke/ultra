package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthActivateAccountResponse(
    val success: Boolean,
    /**
     * The session for the account just activated. Best-effort, as after sign-up: null when the realm could not
     * sign the user in (e.g. an org realm and a user with no org yet), while [success] is still true.
     */
    val signIn: AuthSignInResponse? = null,
)
