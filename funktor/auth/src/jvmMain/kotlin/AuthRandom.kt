package de.peekandpoke.funktor.auth

import de.peekandpoke.ultra.common.toBase64
import java.security.SecureRandom

class AuthRandom {

    val random = SecureRandom()

    fun getToken(length: Int = 128): ByteArray {
        val bytes = ByteArray(length)
        random.nextBytes(bytes)

        return bytes
    }

    fun getTokenAsBase64(length: Int = 128): String {
        return getToken(length).toBase64()
    }
}
