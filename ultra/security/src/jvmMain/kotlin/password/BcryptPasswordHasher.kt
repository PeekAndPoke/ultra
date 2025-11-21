package de.peekandpoke.ultra.security.password

import com.password4j.BcryptFunction
import com.password4j.Password
import com.password4j.types.Bcrypt

class BcryptPasswordHasher(
    logRounds: Int,
    version: Bcrypt,
    override val id: String = "bcrypt-${version.name}-$logRounds",
) : PasswordHasher {

    companion object {
        val l13y = BcryptPasswordHasher(logRounds = 13, version = Bcrypt.Y)
        val l12y = BcryptPasswordHasher(logRounds = 12, version = Bcrypt.Y)
    }

    private val bcrypt = BcryptFunction.getInstance(version, logRounds)

    override fun hash(password: String): PasswordHasher.Hash {
        // Let Password4j generate the salt internally.
        // It is the recommended and safest way.
        val hash = Password.hash(password).with(bcrypt)

        return PasswordHasher.Hash(
            id = id,
            // The salt is embedded in the hash output by bcrypt,
            // but Password4j's `hash` object makes it available.
            salt = hash.salt,
            hash = hash.result,
        )
    }


    override fun check(plaintext: String?, hash: PasswordHasher.Hash?): Boolean {
        if (plaintext == null || hash == null || hash.id != id) {
            return false
        }

        return Password.check(plaintext, hash.hash)
            .with(bcrypt)
    }
}
