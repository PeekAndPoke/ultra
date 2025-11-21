package de.peekandpoke.ultra.security.password

/**
 * Groups multiple [PasswordHasher]s.
 *
 * For hashing passwords it always uses the first hasher.
 * For checking passwords it uses the first hasher that matches with the given hash.id.
 */
class CompoundPasswordHasher(
    hashers: List<PasswordHasher>,
) : PasswordHasher {

    companion object {
        operator fun invoke(vararg hashers: PasswordHasher) = CompoundPasswordHasher(hashers.toList())

        val default = CompoundPasswordHasher(
            hashers = listOf(
                Argon2PasswordHasher.id_m65536i2p4o32,
                BcryptPasswordHasher.l13y,
                BcryptPasswordHasher.l12y,
                PBKDF2WithHmacSHA256PasswordHasher.i310000k256,
                PBKDF2WithHmacSHA256PasswordHasher.i65536k256,
            )
        )
    }

    /** The id of this hasher */
    override val id = "compound"

    /** Primary hasher */
    val primary: PasswordHasher = hashers.firstOrNull()
        ?: error("You must provide at least one hasher")

    /** Maps hasher ids to hashers */
    val hashers = hashers.associateBy { it.id }

    /** Uses the first of the [hashers] */
    override fun hash(password: String): PasswordHasher.Hash {
        return primary.hash(password)
    }

    /**
     * Checks the given [plaintext] against the given [hash]
     *
     * Uses the first hasher that matches with the given hash.id.
     */
    override fun check(plaintext: String?, hash: PasswordHasher.Hash?): Boolean {
        if (plaintext == null || hash == null) return false

        val hasher = hashers[hash.id] ?: return false

        return hasher.check(plaintext, hash)
    }
}
