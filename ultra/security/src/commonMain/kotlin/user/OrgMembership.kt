package io.peekandpoke.ultra.security.user

import kotlinx.serialization.Serializable

/**
 * A user's membership in a single organisation: which branches they may access and their roles
 * within that org.
 *
 * The session-level membership value object — the source data from which the session's
 * [UserPermissions] slice for the selected org is built by [buildOrgPermissions]. A realm's
 * `AuthRealm.getMemberships()` hook produces these (e.g. the saas `OrgMember` collection maps into
 * them via `OrgMembersStorage.sessionMembershipsOf`).
 */
@Serializable
data class OrgMembership(
    val orgId: String,
    val branchIds: Set<String> = emptySet(),
    val roles: Set<String> = emptySet(),
)
