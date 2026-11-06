package io.peekandpoke.ultra.security.password

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
        /** Creates a [CompoundPasswordHasher] from the given [hashers]. */
        operator fun invoke(vararg hashers: PasswordHasher) = CompoundPasswordHasher(hashers.toList())

        /** Default compound hasher with Argon2, Bcrypt, and PBKDF2 variants. */
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
    internal val primary: PasswordHasher = hashers.firstOrNull()
        ?: error("You must provide at least one hasher")

    /** Maps hasher ids to hashers */
    internal val hashers = hashers.associateBy { it.id }

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

        // A corrupt or adversarial stored hash proves nothing: false, never an exception out of the sign-in (a 500
        // and an error oracle). The leaf hashers throw IllegalArgumentException (password4j's BadParametersException,
        // a bad base64) or, for too few parameters, IndexOutOfBoundsException on what they cannot decode. Narrow on
        // purpose: a BROKEN hasher must still fail loudly, not turn every sign-in into a silent "wrong password".
        return try {
            hasher.check(plaintext, hash)
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: IndexOutOfBoundsException) {
            false
        }
    }

    /** True unless the PRIMARY hasher would keep [hash]: a sign-in then moves it to the primary (ULTRA-00066). */
    override fun needsRehash(hash: PasswordHasher.Hash?): Boolean = primary.needsRehash(hash)
}
