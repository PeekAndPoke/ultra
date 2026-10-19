package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

/**
 * "I did not sign up": retires a never-activated account.
 *
 * Authorized by the mail's cancel token (carried by the `cancelSignUp` deep-link, see `AuthFrontendRoutes`), which
 * lives as long as the sign-up — or by the activation token, from the activation page's own cancel button.
 */
@Serializable
data class AuthCancelSignUpRequest(
    val provider: String,
    val token: String,
)
