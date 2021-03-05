package de.peekandpoke.ultra.security.user

import java.net.InetAddress
import java.net.UnknownHostException

interface UserRecordProvider {
    operator fun invoke(): UserRecord

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val anonymous: UserRecordProvider = static("anonymous", "unknown")

        fun systemUser() = static(
            UserRecord(
                userId = "system",
                clientIp = try {
                    InetAddress.getLocalHost().canonicalHostName
                } catch (e: UnknownHostException) {
                    "unknown"
                },
                desc = "system",
                type = "system",
            )
        )

        fun static(userId: String, clientIp: String): UserRecordProvider = static(UserRecord(userId, clientIp))

        fun static(user: UserRecord): UserRecordProvider = Static(user)

        fun lazy(provider: () -> UserRecord): UserRecordProvider = Lazy(provider)
    }

    class Static(private val user: UserRecord) : UserRecordProvider {
        override fun invoke(): UserRecord = user
    }

    class Lazy(provider: () -> UserRecord) : UserRecordProvider {

        private val userRecord by kotlin.lazy { provider() }

        override fun invoke(): UserRecord = userRecord
    }
}
