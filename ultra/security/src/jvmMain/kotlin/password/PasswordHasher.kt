package de.peekandpoke.ultra.security.password

import java.security.SecureRandom

/**
 * Password hasher
 */
interface PasswordHasher {

    companion object {
        val strongRandom: SecureRandom by lazy {
            SecureRandom.getInstanceStrong() ?: SecureRandom()
        }
    }

    /** A hash of a password and a salt */
    data class Hash(
        /** Id of the hasher that was used to create this hash */
        val id: String,
        /** Salt used to create the hash */
        val salt: String,
        /** The hash itself */
        val hash: String,
    ) {
        companion object {
            fun fromString(str: String): Hash {
                val parts = str.split(":")

                return Hash(
                    id = parts.getOrNull(0) ?: "",
                    salt = parts.getOrNull(1) ?: "",
                    hash = parts.getOrNull(2) ?: "",
                )
            }
        }

        /** Formats the hashed password as string */
        fun asString() = "$id:$salt:$hash"
    }

    /** Id of the hasher */
    val id: String

    /** Hash a password */
    fun hash(password: String): Hash

    /** Hash a password as a string */
    fun hashAsString(password: String): String = hash(password).asString()

    /** Check if the given [plaintext] password matches the given [hash] */
    fun check(plaintext: String?, hash: String?): Boolean = check(
        plaintext = plaintext,
        hash = hash?.let { Hash.fromString(it) }
    )

    /** Check if the given [plaintext] password matches the given [hash] */
    fun check(plaintext: String?, hash: Hash?): Boolean
}
