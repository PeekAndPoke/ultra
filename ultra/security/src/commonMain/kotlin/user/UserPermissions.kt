package io.peekandpoke.ultra.security.user

import io.peekandpoke.ultra.common.containsAny
import kotlinx.serialization.Serializable

@Suppress("Detekt.TooManyFunctions")
@Serializable
data class UserPermissions(
    val isSuperUser: Boolean = false,
    /**
     * The single organisation selected for the current session, or `null` for org-less realms.
     *
     * An [OrgId] is always the collection-qualified `_id` — the type enforces it — so caller-binding
     * compares `_id` against `_id`. Where a bare `_key` is genuinely needed (a URL segment), project
     * it with [OrgId.key].
     */
    val org: OrgId? = null,
    /** All organisations the user may log into. Non-authz — drives the login org picker. */
    val accessibleOrgs: Set<OrgId> = emptySet(),
    val branches: Set<String> = emptySet(),
    val groups: Set<String> = emptySet(),
    val roles: Set<String> = emptySet(),
    val permissions: Set<String> = emptySet()
) {
    companion object {
        val anonymous = UserPermissions()
        val system = UserPermissions(isSuperUser = true)
    }

    /**
     * Merges the permission with the [other] permission.
     *
     * NOTICE that the order matters:
     *
     * this.mergeWith(other) != other.mergeWith(this)
     *
     * The [isSuperUser] flag will be taken from [other]. The selected [org] is taken from [other]
     * when it has one, otherwise from `this` (so merging in org-less extras does not clear it).
     */
    infix fun mergedWith(other: UserPermissions) = UserPermissions(
        isSuperUser = other.isSuperUser,
        org = other.org ?: org,
        accessibleOrgs = accessibleOrgs.plus(other.accessibleOrgs),
        branches = branches.plus(other.branches),
        groups = groups.plus(other.groups),
        roles = roles.plus(other.roles),
        permissions = permissions.plus(other.permissions),
    )

    /**
     * Return 'true' when the given [organisation] is the one selected for this session.
     */
    fun hasOrganisation(organisation: OrgId) =
        isSuperUser || this.org == organisation

    /**
     * Return 'true' when the selected [org] is one of the given [organisations].
     */
    fun hasAnyOrganisation(organisations: Collection<OrgId>) =
        isSuperUser || (org != null && organisations.contains(org))

    // NOTE: no `vararg` overload for organisations — Kotlin prohibits a vararg of an inline value
    // class. Pass a collection instead. (The branch/group/role/permission varargs below are plain
    // Strings and stay as they are.)

    /**
     * Return 'true' when the given [organisation] is among the [accessibleOrgs] the user may log into.
     */
    fun canAccessOrg(organisation: OrgId) =
        isSuperUser || this.accessibleOrgs.contains(organisation)

    /**
     * Return 'true' when the given [branch] is present
     */
    fun hasBranch(branch: String) =
        isSuperUser || this.branches.contains(branch)

    /**
     * Return 'true' when any of the given [branches] is present
     */
    fun hasAnyBranch(branches: Collection<String>) =
        isSuperUser || this.branches.containsAny(branches)

    /**
     * Return 'true' when any of the given [branches] is present
     */
    fun hasAnyBranch(vararg branches: String) =
        isSuperUser || hasAnyBranch(branches.toList())

    /**
     * Return 'true' when all the given [branches] are present
     */
    fun hasAllBranches(branches: Collection<String>) =
        isSuperUser || this.branches.containsAll(branches)

    /**
     * Return 'true' when all the given [branches] are present
     */
    fun hasAllBranches(vararg branches: String) =
        isSuperUser || hasAllBranches(branches.toList())

    /**
     * Return 'true' when the given [group] is present
     */
    fun hasGroup(group: String) =
        isSuperUser || this.groups.contains(group)

    /**
     * Return 'true' when any of the given [groups] is present
     */
    fun hasAnyGroup(groups: Collection<String>) =
        isSuperUser || this.groups.containsAny(groups)

    /**
     * Return 'true' when any of the given [groups] is present
     */
    fun hasAnyGroup(vararg groups: String) =
        isSuperUser || hasAnyGroup(groups.toList())

    /**
     * Return 'true' when all the given [groups] are present
     */
    fun hasAllGroups(groups: Collection<String>) =
        isSuperUser || this.groups.containsAll(groups)

    /**
     * Return 'true' when all the given [groups] are present
     */
    fun hasAllGroups(vararg groups: String) =
        isSuperUser || hasAllGroups(groups.toList())

    /**
     * Return 'true' when the given [role] is present
     */
    fun hasRole(role: String) =
        isSuperUser || this.roles.contains(role)

    /**
     * Return 'true' when any of the given [roles] is present
     */
    fun hasAnyRole(roles: Collection<String>) =
        isSuperUser || this.roles.containsAny(roles)

    /**
     * Return 'true' when any of the given [roles] is present
     */
    fun hasAnyRole(vararg roles: String) =
        isSuperUser || hasAnyRole(roles.toList())

    /**
     * Return 'true' when all the given [roles] are present
     */
    fun hasAllRoles(roles: Collection<String>) =
        isSuperUser || this.roles.containsAll(roles)

    /**
     * Return 'true' when all the given [roles] are present
     */
    fun hasAllRoles(vararg roles: String) =
        isSuperUser || hasAllRoles(roles.toList())

    /**
     * Return 'true' when the given [permission] is present
     */
    fun hasPermission(permission: String) =
        isSuperUser || this.permissions.contains(permission)

    /**
     * Return 'true' when any of the given [permissions] is present
     */
    fun hasAnyPermission(permissions: Collection<String>) =
        isSuperUser || this.permissions.containsAny(permissions)

    /**
     * Return 'true' when any of the given [permissions] is present
     */
    fun hasAnyPermission(vararg permissions: String) =
        isSuperUser || hasAnyPermission(permissions.toList())

    /**
     * Return 'true' when all the given [permissions] are present
     */
    fun hasAllPermissions(permissions: Collection<String>) =
        isSuperUser || this.permissions.containsAll(permissions)

    /**
     * Return 'true' when all the given [permissions] are present
     */
    fun hasAllPermissions(vararg permissions: String) =
        isSuperUser || hasAllPermissions(permissions.toList())
}
