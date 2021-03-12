package de.peekandpoke.ultra.security.csrf

import de.peekandpoke.ultra.common.fromBase64
import de.peekandpoke.ultra.common.sha384
import de.peekandpoke.ultra.common.toBase64
import de.peekandpoke.ultra.security.user.UserRecordProvider

class StatelessCsrfProtection(
    private val csrfSecret: String,
    private val csrfTtlMillis: Int,
    userRecordProvider: UserRecordProvider
) : CsrfProtection {

    internal val glue = "#"

    private val userId = userRecordProvider().userId
    private val clientIp = userRecordProvider().clientIp

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

        // Validate the signature
        val expectedSignature = sign(salt, receivedTtlLong)

        return expectedSignature == receivedSignature
    }

    private fun sign(salt: String, ttl: Long) = "$salt$userId$clientIp$ttl$csrfSecret".sha384().toBase64()
}
