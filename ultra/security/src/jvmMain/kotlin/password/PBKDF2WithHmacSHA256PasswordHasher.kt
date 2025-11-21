package de.peekandpoke.ultra.security.password

import de.peekandpoke.ultra.common.fromBase64
import de.peekandpoke.ultra.common.toBase64
import java.security.MessageDigest
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PBKDF2WithHmacSHA256PasswordHasher(
    private val iterations: Int,
    private val keyLength: Int,
    override val id: String = "pbkdf2-$iterations-$keyLength",
) : PasswordHasher {

    companion object {
        private const val ALGO = "PBKDF2WithHmacSHA256"

        val i310000k256 = PBKDF2WithHmacSHA256PasswordHasher(iterations = 310_000, keyLength = 256)
        val i65536k256 = PBKDF2WithHmacSHA256PasswordHasher(iterations = 65536, keyLength = 256)
    }

    override fun hash(password: String): PasswordHasher.Hash {

        val salt = ByteArray(16).apply { PasswordHasher.strongRandom.nextBytes(this) }

        return calcHash(salt, password)
    }

    override fun check(plaintext: String?, hash: PasswordHasher.Hash?): Boolean {
        if (plaintext == null || hash == null || hash.id != id) {
            return false
        }

        val storedHashBytes = hash.hash.fromBase64()
        val saltBytes = hash.salt.fromBase64()

        val calculatedHashBytes = calculateHashBytes(saltBytes, plaintext)

        // The MessageDigest.isEqual method performs a constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(storedHashBytes, calculatedHashBytes)
    }

    private fun calcHash(salt: ByteArray, password: String): PasswordHasher.Hash {
        val hash = calculateHashBytes(salt, password)

        return PasswordHasher.Hash(
            id = id,
            salt = salt.toBase64(),
            hash = hash.toBase64(),
        )
    }

    private fun calculateHashBytes(salt: ByteArray, password: String): ByteArray {
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val f = SecretKeyFactory.getInstance(ALGO)

        return f.generateSecret(spec).encoded
    }

}
