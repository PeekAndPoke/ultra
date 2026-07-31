package io.peekandpoke.ultra.security.jwt

import kotlinx.serialization.Serializable

/**
 * The JOSE header of a token that has **not been authenticated yet**.
 *
 * The one structure in this package parsed from unverified input, which is why it is a fixed,
 * three-field shape rather than a `JsonObject`: whatever an attacker puts in the header can only
 * land in a `String?`, and only after [JwtSignatureGate] has capped the segment.
 *
 * Every field is nullable with a default, so a structurally valid but incomplete header — `{}` —
 * parses and is then rejected on its merits (an absent `kid` is not a lookup) instead of raising a
 * deserialization error the caller would have to distinguish.
 *
 * Unknown members are ignored — RFC 7515 §4 permits additional header parameters, and the header is
 * inside the signing input, so only a key holder can add any. [crit] is the one exception: it is
 * declared here precisely so it can be REJECTED rather than ignored.
 */
@Serializable
data class JwtHeader(
    /** The claimed algorithm. **Compared against the key's algorithm, never used to select one.** */
    val alg: String? = null,
    /**
     * The media type. Required to be `JWT`, case-insensitively (RFC 7519 §5.1 — `typ` is a media
     * type, and media types compare case-insensitively, unlike [alg], whose values are exact strings
     * from the JWA registry).
     */
    val typ: String? = null,
    /** The key id. Required: a token that names no key cannot be verified against one. */
    val kid: String? = null,
    /**
     * Critical header parameters (RFC 7515 §4.1.11). Any value at all rejects the token.
     *
     * The RFC is a MUST: a recipient must reject a JWS whose `crit` names an extension it does not
     * understand. This verifier understands none, so the correct handling of a non-null `crit` is
     * always rejection. Only a key holder can set it — but "unreachable by an attacker" is an
     * argument, and rejecting is a construction.
     */
    val crit: List<String>? = null,
)
