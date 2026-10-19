package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

/**
 * "I did not sign up": retires a never-activated account.
 *
 * Authorized by the activation token from the mail — whoever can activate the account may also decline
 * it. Carried by the `cancelSignUp` deep-link, see `AuthFrontendRoutes`.
 */
@Serializable
data class AuthCancelSignUpRequest(
    val provider: String,
    val token: String,
)
