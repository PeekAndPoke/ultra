package de.peekandpoke.ultra.security.user

interface UserProvider {
    operator fun invoke(): User

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val anonymous: UserProvider = static(User.anonymous)

        fun system() = static(
            User(
                record = UserRecordProvider.system().invoke(),
                permissions = UserPermissions.system,
            )
        )

        fun static(user: User): UserProvider = Static(user)

        fun lazy(provider: () -> User): UserProvider = Lazy(provider)
    }

    class Static(private val user: User) : UserProvider {
        override fun invoke(): User = user
    }

    class Lazy(provider: () -> User) : UserProvider {
        private val user: User by kotlin.lazy { provider() }

        override fun invoke(): User = user
    }
}
