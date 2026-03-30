package io.peekandpoke.ultra.security.user

import java.net.InetAddress
import java.net.UnknownHostException

/** Provides the current [User], supporting static, lazy, and system-level strategies. */
interface UserProvider {
    /** Returns the current user. */
    operator fun invoke(): User

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        /** Provider that always returns the anonymous user. */
        val anonymous: UserProvider = static(User.anonymous)

        /** Creates a provider for the system user, resolving the local host IP. */
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

        /** Creates a provider that always returns the given [user]. */
        fun static(user: User): UserProvider = Static(user)

        /** Creates a static provider from the given [record] and [permissions]. */
        fun static(
            record: UserRecord = UserRecord.anonymous,
            permissions: UserPermissions = UserPermissions.anonymous,
        ): UserProvider = static(
            User(record = record, permissions = permissions)
        )

        /** Creates a provider that lazily evaluates the given [provider] once. */
        fun lazy(provider: () -> User): UserProvider = Lazy(provider)
    }

    /** A [UserProvider] that always returns the same fixed [User]. */
    class Static(private val user: User) : UserProvider {
        override fun invoke(): User = user
    }

    /** A [UserProvider] that lazily resolves the [User] on first access. */
    class Lazy(provider: () -> User) : UserProvider {
        private val user: User by kotlin.lazy { provider() }

        override fun invoke(): User = user
    }
}
