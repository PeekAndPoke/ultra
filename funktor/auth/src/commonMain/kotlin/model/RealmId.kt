package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Type-safe wrapper for an auth realm identifier (e.g. `RealmId("b2b")`, `RealmId("operators")`).
 *
 * Prevents accidental mix-ups between a realm id and the many other string ids in auth (userId,
 * ownerId, orgId, tokens, ...). Serializes as its plain underlying string — kotlinx (value-class
 * inlining) AND slumber (value-class support) both emit the bare scalar — so the wire format and the
 * stored DB value are IDENTICAL to the previous `realm: String`; this is not a data migration.
 *
 * INVARIANT: a realm id is a registered identifier restricted to `[A-Za-z0-9._-]` (1..[MAX_LENGTH]
 * chars). This is a SECURITY boundary, not just a sanity check: realm is the tenant-isolation
 * discriminator and is used verbatim in a NUL-delimited session-cache key and in DB field
 * comparisons, so the charset restriction makes those collision-proof by construction rather than by
 * convention. The `init` check runs on EVERY construction, including deserialization (kotlinx/slumber
 * and the incoming param converter all call the ctor), so a malformed realm from an untrusted URL
 * param or a tampered DB row is rejected at the boundary rather than flowing on.
 */
@Serializable
@JvmInline
value class RealmId(val value: String) {
    init {
        require(value.isNotEmpty()) { "RealmId must not be empty" }
        require(value.length <= MAX_LENGTH) { "RealmId must be at most $MAX_LENGTH chars" }
        require(value.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '.' || it == '_' || it == '-' }) {
            "RealmId must contain only [A-Za-z0-9._-]"
        }
    }

    override fun toString(): String = value

    companion object {
        const val MAX_LENGTH: Int = 128
    }
}
