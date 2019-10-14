package de.peekandpoke.ultra.security.csrf

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
