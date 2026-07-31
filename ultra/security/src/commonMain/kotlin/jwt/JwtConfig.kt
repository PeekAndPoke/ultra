package io.peekandpoke.ultra.security.jwt

import kotlinx.serialization.Serializable

@Serializable
data class JwtConfig(
    /**
     * The signing keys, newest first — **the first one signs, all of them verify**.
     *
     * A list rather than a single key so keys can be rotated without invalidating tokens that are
     * still within their expiry: prepend the new key, deploy, drop the old one a lifetime later.
     * Order is authoritative and deliberately not derived from [JwtSigningKey.issued], so that
     * editing a date cannot silently change which key signs.
     *
     * Validated at boot by `JwtSignatureGate.requireUsableKeys` — non-empty, unique ids, each secret
     * long enough for its algorithm.
     */
    val keys: List<JwtSigningKey>,
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
