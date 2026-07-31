package io.peekandpoke.ultra.security.jwt

import io.peekandpoke.ultra.common.model.Redacted
import kotlinx.serialization.Serializable

@Serializable
data class JwtConfig(
    /** The secret signing key for the JWT token */
    val signingKey: Redacted<String>,
    /** The issuer to be applied to the tokens */
    val issuer: String,
    /** The audience to be applied to the tokens */
    val audience: String,
    /** The namespace for permissions */
    val permissionsNs: String,
    /** The namespace for user data */
    val userNs: String,
) {
    // No hand-written redacting toString(): [Redacted] redacts itself, so the generated one is safe.
    // The previous hand-written one covered ONLY toString and looked like protection while every
    // serializer wrote the key in full — which is how it reached every insights record.
}
