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

    /**
     * A hash of a password and a salt.
     *
     * Serialized as `"id:salt:hash"` by [asString]. The format contract is positional: [id] and
     * [hash] MUST be colon-free — only the [salt] in the middle may contain ':' (legacy records
     * stored raw salt bytes; new hashers must base64/hex-encode the salt anyway). A hasher whose
     * [id] or encoded [hash] output could contain ':' would silently corrupt the parse, so
     * [asString] enforces the contract at write time while [fromString] stays total (never throws)
     * so that adversarial or corrupted stored values fail closed in verification instead of
     * erroring.
     */
    data class Hash(
        /** Id of the hasher that was used to create this hash. Must not contain ':'. */
        val id: String,
        /** Salt used to create the hash. May contain ':' (legacy raw salts did). */
        val salt: String,
        /** The hash itself. Must not contain ':'. */
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

        /**
         * Formats the hashed password as string.
         *
         * Enforces the format contract (see [Hash]) so a violating hasher fails loudly the first
         * time it creates a password, instead of silently truncating on every later verification.
         */
        fun asString(): String {
            require(!id.contains(':')) { "Hasher id must not contain ':': $id" }
            require(!hash.contains(':')) { "Encoded hash must not contain ':' (encode it, e.g. base64)" }
            return "$id:$salt:$hash"
        }
    }

    /** Id of the hasher. Must not contain ':' — it is the first field of [Hash.asString]. */
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
