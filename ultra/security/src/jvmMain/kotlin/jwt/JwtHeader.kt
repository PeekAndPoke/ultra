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
 * Unknown members are ignored (RFC 7515 permits additional header parameters, and the header is
 * inside the signing input, so only the key holder can add any). `crit` is therefore not honoured —
 * see [JwtSignatureGate]'s KDoc for why that is safe here and what it would take to change.
 */
@Serializable
data class JwtHeader(
    /** The claimed algorithm. **Compared against the key's algorithm, never used to select one.** */
    val alg: String? = null,
    /** The claimed media type. Not checked — see [JwtSignatureGate]'s note on RFC 8725 §3.11. */
    val typ: String? = null,
    /** The key id. Required: a token that names no key cannot be verified against one. */
    val kid: String? = null,
)
