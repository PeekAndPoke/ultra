package io.peekandpoke.ultra.security.user

/**
 * Reserved STRUCTURAL organisation roles the framework assigns meaning to (org lifecycle and member
 * management), as opposed to app-defined DOMAIN roles carried in the same [OrgMembership.roles] set.
 *
 * - [OWNER] gates ownership-level actions (billing, member management, org deletion). CORE invariant:
 *   an organisation must always keep at least one owner (see [wouldRemoveLastOwner]).
 * - [ADMIN] gates member/settings management below ownership.
 *
 * Owner is treated as a superset of admin for management gating (see [OrgMembership.canManageMembers]).
 * App-defined roles (e.g. `"accountant"`) live alongside these in the same set and are opaque here.
 */
object OrgRole {
    const val OWNER: String = "owner"
    const val ADMIN: String = "admin"

    /** The roles the framework reserves; every other entry in [OrgMembership.roles] is app-defined. */
    val structural: Set<String> = setOf(OWNER, ADMIN)
}

/** True when this membership holds the structural [OrgRole.OWNER] role. */
val OrgMembership.isOwner: Boolean get() = OrgRole.OWNER in roles

/** True when this membership holds the structural [OrgRole.ADMIN] role. */
val OrgMembership.isAdmin: Boolean get() = OrgRole.ADMIN in roles

/**
 * May the holder manage the org's members (list/add/remove/change roles)? Owners and admins may;
 * owner is treated as a superset of admin.
 */
val OrgMembership.canManageMembers: Boolean get() = isOwner || isAdmin

/**
 * The member ids that own the org, given each member's [OrgMembership] in that org (keyed by the
 * member's user id).
 */
fun ownerIdsOf(members: Map<String, OrgMembership>): Set<String> =
    members.filterValues { it.isOwner }.keys

/**
 * Would removing [memberId] — or demoting them from [OrgRole.OWNER] — leave the org without an owner?
 *
 * [currentOwnerIds] is the org's current owner-member-id set. The core invariant is that every org
 * keeps at least one owner, so this is `true` exactly when [memberId] is the SOLE current owner. An
 * org that already has no owner is never blocked (nothing to protect).
 */
fun wouldRemoveLastOwner(currentOwnerIds: Set<String>, memberId: String): Boolean =
    currentOwnerIds == setOf(memberId)
