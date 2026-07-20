package io.peekandpoke.ultra.security.password

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
                // The value is "id:salt:hash". The [id] and the encoded [hash] are colon-free, but
                // the [salt] (raw password4j salt bytes) may contain ':'. So bind id to before the
                // first ':' and hash to after the last ':' — everything between is the salt. A naive
                // `split(":")` would misalign whenever a random salt happens to contain a colon,
                // corrupting the parsed hash ("Invalid hashed value" on check).
                val firstColon = str.indexOf(':')
                val lastColon = str.lastIndexOf(':')

                return if (firstColon < 0 || lastColon <= firstColon) {
                    Hash(id = str.substringBefore(':'), salt = "", hash = str.substringAfter(':', ""))
                } else {
                    Hash(
                        id = str.substring(0, firstColon),
                        salt = str.substring(firstColon + 1, lastColon),
                        hash = str.substring(lastColon + 1),
                    )
                }
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
