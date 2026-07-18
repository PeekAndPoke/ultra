package io.peekandpoke.ultra.security.user

import kotlinx.serialization.Serializable

/**
 * A user's membership in a single organisation: which branches they may access and their roles
 * within that org.
 *
 * Embedded on the app's user entity (see [HasOrgMemberships]). This is the source data from which
 * the session's [UserPermissions] slice for the selected org is built by [buildOrgPermissions].
 */
@Serializable
data class OrgMembership(
    val orgId: String,
    val branchIds: Set<String> = emptySet(),
    val roles: Set<String> = emptySet(),
)

/** Implemented by app user entities that carry organisation memberships. */
interface HasOrgMemberships {
    val memberships: Set<OrgMembership>
}
