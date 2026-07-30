package io.peekandpoke.ultra.security.user

import io.peekandpoke.ultra.common.isForbiddenInId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * The identity of an organisation, as carried by a session: [UserPermissions.org],
 * [UserPermissions.accessibleOrgs], [OrgMembership.orgId], [SelectedOrg.orgId] and the JWT's `org`
 * claim.
 *
 * CONTRACT: always the FULL Vault `_id` in `collection/key` form (e.g. `organisation/acme`) — the
 * project-wide rule that anything naming another document names it by its globally resolvable `_id`,
 * never by a bare `_key` that is only meaningful once you already know the collection. The `init`
 * block ENFORCES that shape, which is what makes this type the migration's own safety net: a site
 * that still hands over a bare `_key` fails loudly at construction instead of silently matching
 * nothing and 404ing every org-scoped request.
 *
 * URL segments are the ONE place a bare key still belongs, because there the collection comes from
 * the route's parameter TYPE rather than from the value — which is also why funktor's outgoing param
 * converter renders every entity as its `_key`. Use [key] at that boundary and nowhere else:
 * ```
 * Apis.members.list(orgId.key)     // -> /api/b2b/orgs/acme/members
 * ```
 *
 * Not a `Ref<Organisation>`: this is a session/claim-level identity that must survive in a JWT and
 * be comparable without a database, so it deliberately carries no resolver.
 */
@Serializable
@JvmInline
value class OrgId(val value: String) {
    init {
        require(value.isNotBlank()) { "OrgId must not be blank" }
        require(value.length <= MAX_LENGTH) {
            "OrgId must be at most $MAX_LENGTH chars, got ${value.length}"
        }
        require(value.none { it.isForbiddenInId() }) {
            "OrgId must not contain control or line-separator characters"
        }
        // Exactly one separator: `collection/key`. Vault derives the bare key as the segment AFTER
        // the first slash, so a second slash would make [key] ambiguous.
        require(value.count { it == '/' } == 1) {
            "OrgId must be a Vault _id in 'collection/key' form, got '$value' " +
                    "— a bare _key or a multi-segment id is not accepted"
        }
        require(!value.startsWith('/') && !value.endsWith('/')) {
            "OrgId must have a non-empty collection and key, got '$value'"
        }
    }

    /**
     * The bare `_key` — use ONLY when rendering a URL segment, where the collection is supplied by
     * the route's parameter type. Never store or compare this: the whole point of [value] is that it
     * is unambiguous on its own.
     */
    val key: String get() = value.substringAfter('/')

    /** The collection this organisation lives in. */
    val collection: String get() = value.substringBefore('/')

    override fun toString(): String = value

    companion object {
        /** See `UserId.MAX_LENGTH` — an ArangoDB collection (≤256) plus `/` plus a key (≤254). */
        const val MAX_LENGTH: Int = 512

        /** Builds an [OrgId] from its parts. */
        fun of(collection: String, key: String): OrgId = OrgId("$collection/$key")

        /**
         * Parses [raw] into an [OrgId], or returns `null` when it is absent or not a well-formed
         * `collection/key`.
         *
         * Use at boundaries reading ATTACKER-SUPPLIED input — notably the JWT `org` claim — where a
         * malformed value must degrade to "no selected org" rather than throw and turn a bad token
         * into a 500. Everywhere else construct directly so a violation fails loudly.
         */
        fun parseOrNull(raw: String?): OrgId? = raw?.let {
            try {
                OrgId(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
