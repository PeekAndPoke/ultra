package de.peekandpoke.ultra.security.user

import java.net.InetAddress
import java.net.UnknownHostException

interface UserProvider {
    operator fun invoke(): User

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val anonymous: UserProvider = static(User.anonymous)

        fun system(): UserProvider {
            val ip = try {
                "${InetAddress.getLocalHost().hostName} ${InetAddress.getLocalHost().hostAddress}"
            } catch (e: UnknownHostException) {
                null
            }

            return static(
                User(
                    record = UserRecord.system(ip = ip),
                    permissions = UserPermissions.system,
                )
            )
        }

        fun static(user: User): UserProvider = Static(user)

        fun static(
            record: UserRecord = UserRecord.anonymous,
            permissions: UserPermissions = UserPermissions.anonymous,
        ): UserProvider = static(
            User(record = record, permissions = permissions)
        )

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
