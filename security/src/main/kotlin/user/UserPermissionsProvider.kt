package de.peekandpoke.ultra.security.user

interface UserPermissionsProvider {
    operator fun invoke(): UserPermissions

    companion object {
        val anonymous: UserPermissionsProvider = static(UserPermissions())

        fun static(permissions: UserPermissions): UserPermissionsProvider = Static(permissions)

        fun lazy(provider: () -> UserPermissions): UserPermissionsProvider = Lazy(provider)
    }

    class Lazy(private val provider: () -> UserPermissions) : UserPermissionsProvider {

        private val permissions by kotlin.lazy { provider() }

        override fun invoke(): UserPermissions = permissions
    }

    class Static(private val permissions: UserPermissions) : UserPermissionsProvider {
        override fun invoke(): UserPermissions = permissions
    }
}
