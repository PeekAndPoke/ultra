package io.peekandpoke.funktor.demo.common.b2b

import kotlinx.serialization.Serializable

/**
 * A member of an organisation as shown in the b2b member-management list — the `OrgMember` row
 * enriched with the user's display info (resolved from the b2b user store on the server).
 */
@Serializable
data class OrgMemberModel(
    /** The `OrgMember` row id — addresses the change-roles / remove endpoints. */
    val id: String,
    /** The member's (realm-qualified) user id. */
    val userId: String,
    val name: String,
    val email: String,
    val roles: Set<String>,
)

/** Body for changing a member's roles within an organisation. */
@Serializable
data class ChangeMemberRolesRequest(
    val roles: Set<String>,
)

/** Body for adding an EXISTING b2b user (looked up by email) to an organisation with the given roles. */
@Serializable
data class AddMemberRequest(
    val email: String,
    val roles: Set<String>,
)
