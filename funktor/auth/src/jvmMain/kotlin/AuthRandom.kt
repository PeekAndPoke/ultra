package de.peekandpoke.funktor.auth

import de.peekandpoke.ultra.common.toBase64
import java.security.SecureRandom

interface AuthRandom {

    companion object {
        val default: AuthRandom = Impl()

        val secureRandom: SecureRandom = SecureRandom.getInstanceStrong()
    }

    private class Impl : AuthRandom {

        override fun getToken(length: Int): ByteArray {
            val bytes = ByteArray(length)
            secureRandom.nextBytes(bytes)

            return bytes
        }
    }

    /**
     * Returns a ByteArray with the given length containing random bytes.
     */
    fun getToken(length: Int = 256): ByteArray

    /**
     * Returns a Base64 encoded String with the given length containing random bytes.
     */
    fun getTokenAsBase64(length: Int = 256): String = getToken(length).toBase64()
}
