package io.peekandpoke.ultra.security.csrf

/** Provides CSRF token creation and validation. */
interface CsrfProtection {

    /**
     * Creates a valid csrf token for the given user
     */
    fun createToken(salt: String): String

    /**
     * Validates the given csrf token
     */
    fun validateToken(salt: String, token: String): Boolean
}
