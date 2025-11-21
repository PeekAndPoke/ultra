package de.peekandpoke.ultra.security.password

import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.types.Argon2

class Argon2PasswordHasher(
    private val memory: Int,
    private val iterations: Int,
    private val parallelism: Int,
    private val outputLength: Int,
    private val type: Argon2,
    override val id: String = "argon2-$type-$memory-$iterations-$parallelism-$outputLength",
) : PasswordHasher {

    companion object {
        /**
         * Default instance with secure parameters for Argon2id.
         * - memory: 65536 KB (64 MB)
         * - iterations: 2
         * - parallelism: 4
         * - type: Argon2.ID (the recommended hybrid variant)
         */
        val id_m65536i2p4o32 = Argon2PasswordHasher(
            memory = 65536,
            iterations = 2,
            parallelism = 4,
            outputLength = 32,
            type = Argon2.ID,
        )
    }

    private val argon2 = Argon2Function.getInstance(memory, iterations, parallelism, outputLength, type)

    override fun hash(password: String): PasswordHasher.Hash {
        // Use char[] for better security and to avoid potential bugs with empty strings.
        val hash = Password.hash(password).with(argon2)

        return PasswordHasher.Hash(
            id = id,
            salt = hash.salt,
            hash = hash.result,
        )
    }

    override fun check(plaintext: String?, hash: PasswordHasher.Hash?): Boolean {
        if (plaintext == null || hash == null || hash.id != id) {
            return false
        }

        // Use char[] for better security and to avoid potential bugs with empty strings.
        return Password.check(plaintext, hash.hash).with(argon2)
    }
}
