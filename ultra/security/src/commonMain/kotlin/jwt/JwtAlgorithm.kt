package io.peekandpoke.ultra.security.jwt

/**
 * A signing algorithm this issuer supports.
 *
 * **Deliberately one entry.** A weaker option in this enum is a weaker option in production: every
 * value here is something a configuration file can select, and the whole point of the `kid` design is
 * that the KEY decides the algorithm. Adding HS256 "for completeness" would add nothing but a way to
 * misconfigure a deployment down to a 32-byte key.
 *
 * The enum exists so that adding one later — when there is a reason — is a single line plus a
 * fixture, instead of a refactor. It is also what makes the header's `alg` checkable: the verifier
 * compares it against the key's algorithm rather than obeying it.
 *
 * `Class.isEnum` drives Slumber's [io.peekandpoke.ultra.slumber.builtin.objects.EnumCodec] selection
 * and is **false for a constant with a class body**, so entries here must stay body-less and carry
 * their differences as constructor parameters.
 */
enum class JwtAlgorithm(
    /** The value written to, and expected in, the token header's `alg` field (RFC 7518 §3.1). */
    val headerValue: String,
    /** The JCA `Mac` algorithm name used to compute the signature. */
    val jcaName: String,
    /**
     * Smallest acceptable key, in bytes. RFC 7518 §3.2: *"A key of the same size as the hash output
     * ... or larger MUST be used with this algorithm."*
     */
    val minKeyBytes: Int,
) {
    /** HMAC with SHA-512. The only algorithm this issuer signs and verifies with. */
    HS512(headerValue = "HS512", jcaName = "HmacSHA512", minKeyBytes = 64),
    ;

    companion object {
        /** The algorithm named by [headerValue], or null — an unknown `alg` is attacker input. */
        fun byHeaderValue(headerValue: String?): JwtAlgorithm? =
            entries.firstOrNull { it.headerValue == headerValue }
    }
}
