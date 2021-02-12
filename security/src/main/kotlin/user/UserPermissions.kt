package de.peekandpoke.ultra.security.user

import de.peekandpoke.ultra.common.containsAny

data class UserPermissions(
    val isSuperUser: Boolean = false,
    val organisations: Set<String> = emptySet(),
    val branches: Set<String> = emptySet(),
    val groups: Set<String> = emptySet(),
    val roles: Set<String> = emptySet(),
    val permissions: Set<String> = emptySet()
) {
    /**
     * Return 'true' when the given [organisation] is present
     */
    fun hasOrganisation(organisation: String) = this.organisations.contains(organisation)

    /**
     * Return 'true' when any of the given [organisations] is present
     */
    fun hasAnyOrganisation(organisations: Collection<String>) = this.organisations.containsAny(organisations)

    /**
     * Return 'true' when all of the given [organisations] are present
     */
    fun hasAllOrganisations(organisations: Collection<String>) = this.groups.containsAll(organisations)

    /**
     * Return 'true' when the given [branch] is present
     */
    fun hasBranch(branch: String) = this.branches.contains(branch)

    /**
     * Return 'true' when any of the given [branches] is present
     */
    fun hasAnyBranch(branches: Collection<String>) = this.branches.containsAny(branches)

    /**
     * Return 'true' when all of the given [branches] are present
     */
    fun hasAllBranches(branches: Collection<String>) = this.branches.containsAll(branches)

    /**
     * Return 'true' when the given [group] is present
     */
    fun hasGroup(group: String) = this.groups.contains(group)

    /**
     * Return 'true' when any of the given [groups] is present
     */
    fun hasAnyGroup(groups: Collection<String>) = this.groups.containsAny(groups)

    /**
     * Return 'true' when all of the given [groups] are present
     */
    fun hasAllGroups(groups: Collection<String>) = this.groups.containsAll(groups)

    /**
     * Return 'true' when the given [role] is present
     */
    fun hasRole(role: String) = this.roles.contains(role)

    /**
     * Return 'true' when any of the given [roles] is present
     */
    fun hasAnyRole(roles: Collection<String>) = this.roles.containsAny(roles)

    /**
     * Return 'true' when all of the given [roles] are present
     */
    fun hasAllRoles(roles: Collection<String>) = this.roles.containsAll(roles)

    /**
     * Return 'true' when the given [permission] is present
     */
    fun hasPermission(permission: String) = this.permissions.contains(permission)

    /**
     * Return 'true' when any of the given [permissions] is present
     */
    fun hasAnyPermission(permissions: Collection<String>) = this.permissions.containsAny(permissions)

    /**
     * Return 'true' when all of the given [permissions] are present
     */
    fun hasAllPermissions(permissions: Collection<String>) = this.permissions.containsAll(permissions)
}


