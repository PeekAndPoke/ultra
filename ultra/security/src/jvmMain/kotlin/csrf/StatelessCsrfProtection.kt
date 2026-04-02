package io.peekandpoke.ultra.security.csrf

import io.peekandpoke.ultra.common.fromBase64
import io.peekandpoke.ultra.common.sha384
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
        val decoded = String(token.fromBase64())
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

    // Use null byte delimiters between fields to prevent field-boundary collisions
    private fun sign(salt: String, ttl: Long) =
        "$salt\u0000$userId\u0000$clientIp\u0000$ttl\u0000$csrfSecret".sha384().toBase64()
}
