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

// Core predicates over a raw role set — the SINGLE source of truth. Reused by the session
// [OrgMembership] (below) AND by the stored `OrgMember` row in funktor/saas (`member.roles.isOrgOwner`),
// so ownership is never re-decided in two divergent places.

/** True when this role set holds the structural [OrgRole.OWNER] role. */
val Set<String>.isOrgOwner: Boolean get() = OrgRole.OWNER in this

/** True when this role set holds the structural [OrgRole.ADMIN] role. */
val Set<String>.isOrgAdmin: Boolean get() = OrgRole.ADMIN in this

/**
 * May a holder of this role set manage the org's members (list/add/remove/change roles)? Owners and
 * admins may; owner is treated as a superset of admin. Gate management on THIS, never on bare
 * [isOrgAdmin] — an owner-only membership is not [isOrgAdmin].
 */
val Set<String>.canManageOrgMembers: Boolean get() = isOrgOwner || isOrgAdmin

/** True when this membership holds the structural [OrgRole.OWNER] role. */
val OrgMembership.isOwner: Boolean get() = roles.isOrgOwner

/** True when this membership holds the structural [OrgRole.ADMIN] role. */
val OrgMembership.isAdmin: Boolean get() = roles.isOrgAdmin

/** @see canManageOrgMembers — owners and admins may manage members. */
val OrgMembership.canManageMembers: Boolean get() = roles.canManageOrgMembers

/**
 * The member ids that own the org, given each member's [OrgMembership] in that org (keyed by the
 * member's user id).
 */
fun ownerIdsOf(members: Map<UserId, OrgMembership>): Set<UserId> =
    members.filterValues { it.isOwner }.keys

/**
 * Would removing [memberId] — or demoting them from [OrgRole.OWNER] — leave the org without an owner?
 *
 * [currentOwnerIds] is the org's current owner-member-id set. The core invariant is that every org
 * keeps at least one owner, so this is `true` exactly when [memberId] is the SOLE current owner. An
 * org that already has no owner is never blocked (nothing to protect).
 */
fun wouldRemoveLastOwner(currentOwnerIds: Set<UserId>, memberId: UserId): Boolean =
    currentOwnerIds == setOf(memberId)
