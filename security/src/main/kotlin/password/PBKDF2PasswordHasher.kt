package de.peekandpoke.ultra.security.password

import de.peekandpoke.ultra.common.fromBase64
import de.peekandpoke.ultra.common.toBase64
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PBKDF2PasswordHasher(
    private val iterations: Int = 65536,
    private val keyLength: Int = 256
) : PasswordHasher {

    private val algo = "PBKDF2WithHmacSHA256"

    private val random = SecureRandom()

    override fun hash(password: String): String {

        val salt = ByteArray(16).apply { random.nextBytes(this) }

        return calcHash(salt, password)
    }

    override fun check(plaintext: String, hash: String): Boolean {
        val parts = hash.split("|")

        if (parts.size != 3 || parts[0] != algo) {
            return false
        }

        val calculatedHash = calcHash(parts[1].fromBase64(), plaintext)

        return calculatedHash == hash
    }

    private fun calcHash(salt: ByteArray, password: String): String {
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val f = SecretKeyFactory.getInstance(algo)

        val hash = f.generateSecret(spec).encoded

        return "$algo|${salt.toBase64()}|${hash.toBase64()}"
    }
}
