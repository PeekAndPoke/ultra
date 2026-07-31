package io.peekandpoke.ultra.security.jwt

import io.peekandpoke.ultra.common.model.Redacted
import kotlinx.serialization.Serializable

/**
 * One signing key, identified by its `kid`.
 *
 * An object rather than a bare string so a key can carry its own algorithm and metadata: rotation
 * means several keys coexist, and the day a second algorithm is supported it must be selectable per
 * key, not per deployment.
 *
 * In HOCON:
 *
 * ```hocon
 * keys = [
 *   { id = "2026-07", secret = "<openssl rand -base64 64>", alg = "HS512", issued = "2026-07-31" }
 *   { id = "2026-06", secret = "<previous key>" }
 * ]
 * ```
 *
 * **The FIRST key in [JwtConfig.keys] signs; every key verifies.** Rotation is therefore: prepend a
 * new key, deploy, and drop the oldest once no token signed under it can still be within its expiry.
 */
@Serializable
data class JwtSigningKey(
    /**
     * The `kid` written into every token signed with this key, and the value a verifier looks up.
     *
     * Attacker-controlled on the verify side — a token can name any `kid` — so it is only ever used
     * as a lookup key into the fixed map built from configuration. Never a path, never concatenated
     * into a query, never used to fetch anything.
     */
    val id: String,
    /** The key material. [Redacted] so no serializer can write it out — see that type's KDoc. */
    val secret: Redacted<String>,
    /**
     * The algorithm this key signs and verifies with — **authoritative**.
     *
     * A token's header `alg` is compared against this and never used to select anything, which is
     * what makes algorithm confusion unreachable rather than merely rejected.
     */
    val alg: JwtAlgorithm = JwtAlgorithm.HS512,
    /**
     * When this key was generated, ISO-8601. Metadata only: nothing reads it, and in particular it
     * does **not** decide which key signs — list order does, so that editing a date cannot silently
     * change the signer. It is the hook for a future "refuse keys older than N days" policy.
     */
    val issued: String? = null,
)
