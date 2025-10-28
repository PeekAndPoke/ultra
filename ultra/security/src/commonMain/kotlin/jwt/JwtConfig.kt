package de.peekandpoke.ultra.security.jwt

import kotlinx.serialization.Serializable

@Serializable
data class JwtConfig(
    /** The secret signing key for the JWT token */
    val signingKey: String,
    /** The issuer to be applied to the tokens */
    val issuer: String,
    /** The audience to be applied to the tokens */
    val audience: String,
    /** The namespace for permissions */
    val permissionsNs: String,
    /** The namespace for user data */
    val userNs: String,
)
