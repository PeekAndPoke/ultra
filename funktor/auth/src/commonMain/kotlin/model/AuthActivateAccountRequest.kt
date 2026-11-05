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
    /** The account's first password, chosen on the activation page; must meet the realm's password policy. */
    val password: String,
    /**
     * The user's display name, chosen on the activation page; must meet [AuthDisplayName]. Sign-up takes none, so
     * whoever signs up someone else's address cannot choose what that account is called.
     */
    val displayName: String,
)
