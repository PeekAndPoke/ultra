package de.peekandpoke.ultra.security.user

data class UserPermissions(
    val groups: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList()
)

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
