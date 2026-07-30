package io.peekandpoke.ultra.security.user

/**
 * A named role together with the [UserPermissions] it grants.
 *
 * Used by introspection tooling to build API access matrices: each known role is evaluated against
 * every route's auth rules to show who can reach what, without issuing real requests.
 *
 * Lives beside [UserPermissions] rather than in `funktor:auth` because both the realm that declares
 * roles and the tooling that renders them need it, and neither should have to depend on the other.
 */
data class KnownRole(
    val name: String,
    val permissions: UserPermissions,
) {
    companion object {
        /** The role that passes every permission check. */
        val superUser = KnownRole("SuperUser", UserPermissions(isSuperUser = true))

        /** The unauthenticated caller — holds no permissions at all. */
        val anonymous = KnownRole("Anonymous", UserPermissions())

        /**
         * The two roles that exist in every application, regardless of what a realm declares.
         *
         * Appended by the introspection tooling, so a realm never needs to restate them (and cannot
         * duplicate them by doing so).
         */
        val universal = listOf(superUser, anonymous)
    }
}
