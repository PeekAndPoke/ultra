package io.peekandpoke.ultra.security.user

import io.peekandpoke.ultra.common.isForbiddenInId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * The identity of a request's subject — the "who" of a [UserRecord], an `AuthRecord.ownerId`, and an
 * `OrgMember.userId`.
 *
 * CANONICAL FORM for a real user is the Vault `_id` in `collection/key` shape (e.g.
 * `b2b_users/abc123`). It is realm-qualified, therefore globally unique across the per-realm user
 * stores and directly resolvable through a repository. Everything that PERSISTS a user reference
 * stores exactly that.
 *
 * The invariant deliberately does NOT require the `collection/key` shape, because this same type
 * also carries SYNTHETIC subjects that are not documents at all:
 * - [UserRecord.ANONYMOUS_ID] and [UserRecord.SYSTEM_ID] — the anonymous / internal-system actors,
 * - `role-eval` — the probe `ApiRoute.estimateAccess` synthesizes to ask "what would a user with
 *   these permissions be allowed to do?",
 * - opaque API-key subjects supplied by a `Caller.ApiKeyCaller`.
 *
 * Those synthetic ids are compared against real user ids (e.g. the set-password ownership check),
 * so they must share one type — splitting them would only reintroduce raw-`String` conversions at
 * exactly the comparison that matters.
 *
 * The invariant is therefore structural: non-blank, bounded, and free of control characters (which
 * could otherwise be smuggled into a composite cache key or a log line).
 *
 * Wire and storage format are unchanged from the plain `String` this replaces: a `@JvmInline value
 * class` over a `String` serializes as that bare string in both kotlinx and slumber.
 */
@Serializable
@JvmInline
value class UserId(val value: String) {
    init {
        require(value.isNotBlank()) { "UserId must not be blank" }
        require(value.length <= MAX_LENGTH) {
            "UserId must be at most $MAX_LENGTH chars, got ${value.length}"
        }
        require(value.none { it.isForbiddenInId() }) {
            "UserId must not contain control or line-separator characters"
        }
    }

    override fun toString(): String = value

    companion object {
        /**
         * Generous upper bound: a real id is `<collection>/<key>`, where an ArangoDB collection name
         * is at most 256 and a key at most 254 bytes (511 with the separator). Guards against
         * unbounded input reaching cache keys and log lines.
         */
        const val MAX_LENGTH: Int = 512

        /**
         * Parses [raw] into a [UserId], or returns `null` when it is absent or violates the
         * invariant.
         *
         * Use this at boundaries that read ATTACKER-SUPPLIED input — notably JWT claims — where a
         * malformed id must degrade to "no identity" rather than throw and turn a bad token into a
         * 500. Everywhere else, construct directly so a violation fails loudly.
         */
        fun parseOrNull(raw: String?): UserId? = raw?.let {
            try {
                UserId(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
