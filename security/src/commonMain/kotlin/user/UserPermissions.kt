package de.peekandpoke.ultra.security.user

import de.peekandpoke.ultra.common.containsAny
import kotlinx.serialization.Serializable

@Suppress("Detekt.TooManyFunctions")
@Serializable
data class UserPermissions(
    val isSuperUser: Boolean = false,
    val organisations: Set<String> = emptySet(),
    val branches: Set<String> = emptySet(),
    val groups: Set<String> = emptySet(),
    val roles: Set<String> = emptySet(),
    val permissions: Set<String> = emptySet()
) {
    companion object {
        val anonymous = UserPermissions()
    }

    /**
     * Merges the permission with the [other] permission.
     *
     * NOTICE that the order matters:
     *
     * this.mergeWith(other) != other.mergeWith(this)
     *
     * The [isSuperUser] flag will be taken from [other].
     */
    infix fun mergedWith(other: UserPermissions) = UserPermissions(
        isSuperUser = other.isSuperUser,
        organisations = organisations.plus(other.organisations),
        branches = branches.plus(other.branches),
        groups = groups.plus(other.groups),
        roles = roles.plus(other.roles),
        permissions = permissions.plus(other.permissions),
    )

    /**
     * Return 'true' when the given [organisation] is present
     */
    fun hasOrganisation(organisation: String) =
        isSuperUser || this.organisations.contains(organisation)

    /**
     * Return 'true' when any of the given [organisations] is present
     */
    fun hasAnyOrganisation(organisations: Collection<String>) =
        isSuperUser || this.organisations.containsAny(organisations)

    /**
     * Return 'true' when any of the given [organisations] is present
     */
    fun hasAnyOrganisation(vararg organisations: String) =
        isSuperUser || hasAnyOrganisation(organisations.toList())

    /**
     * Return 'true' when all the given [organisations] are present
     */
    fun hasAllOrganisations(organisations: Collection<String>) =
        isSuperUser || this.organisations.containsAll(organisations)

    /**
     * Return 'true' when all the given [organisations] are present
     */
    fun hasAllOrganisations(vararg organisations: String) =
        isSuperUser || hasAllOrganisations(organisations.toList())

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
    fun hasRole(role: String) = this.roles.contains(role)

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
