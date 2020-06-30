package de.peekandpoke.ultra.security.user

import de.peekandpoke.ultra.common.containsAny

data class UserPermissions(
    val groups: Set<String> = emptySet(),
    val roles: Set<String> = emptySet(),
    val permissions: Set<String> = emptySet()
)

/**
 * Return 'true' when the given [group] is present
 */
fun UserPermissions.hasGroup(group: String) = this.groups.contains(group)

/**
 * Return 'true' when any of the given [groups] is present
 */
fun UserPermissions.hasAnyGroup(groups: List<String>) = this.groups.containsAny(groups)

/**
 * Return 'true' when all of the given [groups] are present
 */
fun UserPermissions.hasAllGroups(groups: List<String>) = this.groups.containsAll(groups)

/**
 * Return 'true' when the given [role] is present
 */
fun UserPermissions.hasRole(role: String) = this.roles.contains(role)

/**
 * Return 'true' when any of the given [roles] is present
 */
fun UserPermissions.hasAnyRole(roles: List<String>) = this.roles.containsAny(roles)

/**
 * Return 'true' when all of the given [roles] are present
 */
fun UserPermissions.hasAllRoles(roles: List<String>) = this.roles.containsAll(roles)

/**
 * Return 'true' when the given [permission] is present
 */
fun UserPermissions.hasPermission(permission: String) = this.permissions.contains(permission)

/**
 * Return 'true' when any of the given [permissions] is present
 */
fun UserPermissions.hasAnyPermission(permissions: List<String>) = this.permissions.containsAny(permissions)

/**
 * Return 'true' when all of the given [permissions] are present
 */
fun UserPermissions.hasAllPermissions(permissions: List<String>) = this.permissions.containsAll(permissions)

interface UserPermissionsProvider {
    operator fun invoke(): UserPermissions

    companion object {
        val anonymous: UserPermissionsProvider = static(UserPermissions())

        fun static(permissions: UserPermissions): UserPermissionsProvider = StaticUserPermissionsProvider(permissions)

        fun lazy(provider: () -> UserPermissions): UserPermissionsProvider = LazyUserPermissionsProvider(provider)
    }
}

internal class StaticUserPermissionsProvider(private val permissions: UserPermissions) : UserPermissionsProvider {
    override fun invoke(): UserPermissions = permissions
}

internal class LazyUserPermissionsProvider(private val provider: () -> UserPermissions) : UserPermissionsProvider {

    private val permissions by lazy { provider() }

    override fun invoke(): UserPermissions = permissions
}
