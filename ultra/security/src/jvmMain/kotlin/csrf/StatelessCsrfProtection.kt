package io.peekandpoke.ultra.security.csrf

import io.peekandpoke.ultra.common.fromBase64OrNull
import io.peekandpoke.ultra.common.hmacSha384
import io.peekandpoke.ultra.common.toBase64
import io.peekandpoke.ultra.security.user.UserProvider
import java.security.MessageDigest

/** Stateless [CsrfProtection] that signs tokens with a secret, user identity, and TTL. */
class StatelessCsrfProtection(
    private val csrfSecret: String,
    private val csrfTtlMillis: Long,
    userProvider: UserProvider,
) : CsrfProtection {

    init {
        require(csrfSecret.isNotBlank()) { "CSRF secret must not be blank" }
    }

    internal val glue = "#"

    private val user by lazy { userProvider() }

    private val userId get() = user.record.userId
    private val clientIp get() = user.record.clientIp

    override fun createToken(salt: String): String {
        val ttl = System.currentTimeMillis() + csrfTtlMillis
        val signature = sign(salt, ttl)

        return "$ttl$glue$signature".toBase64()
    }

    @Suppress("Detekt:ReturnCount")
    override fun validateToken(salt: String, token: String): Boolean {
        // a token that is not even base64 is simply an invalid token, like every other
        // malformed shape handled below
        val decoded = token.fromBase64OrNull()?.let { String(it) } ?: return false
        val parts = decoded.split(glue)

        if (parts.size != 2) {
            return false
        }

        val (receivedTtl, receivedSignature) = parts

        // Check the ttl of the token
        val receivedTtlLong = receivedTtl.toLongOrNull()

        if (receivedTtlLong == null || receivedTtlLong < System.currentTimeMillis()) {
            return false
        }

        // Validate the signature using constant-time comparison to prevent timing attacks
        val expectedSignature = sign(salt, receivedTtlLong)

        return MessageDigest.isEqual(
            expectedSignature.toByteArray(),
            receivedSignature.toByteArray(),
        )
    }

    /**
     * Signs the fields with HMAC-SHA-384, keyed by the secret.
     *
     * The secret is the KEY, not the last field of the message: `H(fields || secret)` is a
     * hand-rolled construction whose strength rests on the hash being collision resistant,
     * where HMAC is built for exactly this and does not.
     *
     * Fields stay separated by null bytes so that moving a boundary — a user id ending where
     * the ip begins — cannot produce the same message.
     */
    private fun sign(salt: String, ttl: Long) =
        "$salt\u0000$userId\u0000$clientIp\u0000$ttl".hmacSha384(csrfSecret).toBase64()
}
