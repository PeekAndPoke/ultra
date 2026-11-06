package io.peekandpoke.funktor.auth

import io.peekandpoke.ultra.common.toBase64
import java.security.SecureRandom

/**
 * Secure random used by the auth module
 */
interface AuthRandom {

    companion object {
        /** Default impl, on [secureRandom] */
        val default: AuthRandom = Impl()

        /** A plain [SecureRandom]: a CSPRNG that does not block, unlike `getInstanceStrong()` (ULTRA-00067) */
        val secureRandom: SecureRandom = SecureRandom()
    }

    /** Default impl */
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
