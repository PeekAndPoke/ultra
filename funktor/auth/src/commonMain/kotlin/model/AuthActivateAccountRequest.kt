package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthActivateAccountRequest(
    /**
     * The provider that issued the activation token.
     *
     * Carried by the activation deep-link as `{provider}`, exactly like the password-reset link —
     * see `AuthFrontendRoutes.activateAccount`.
     */
    val provider: String,
    val token: String,
)
