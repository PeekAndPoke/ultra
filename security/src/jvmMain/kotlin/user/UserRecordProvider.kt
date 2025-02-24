package de.peekandpoke.ultra.security.user

import java.net.InetAddress
import java.net.UnknownHostException

interface UserRecordProvider {
    operator fun invoke(): UserRecord

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val anonymous: UserRecordProvider = static(UserRecord.anonymous)

        fun system() = static(
            userId = "system",
            clientIp = try {
                "${InetAddress.getLocalHost().hostName} ${InetAddress.getLocalHost().hostAddress}"
            } catch (e: UnknownHostException) {
                "unknown"
            },
        )

        fun static(userId: String, clientIp: String): UserRecordProvider = static(
            UserRecord(userId = userId, clientIp = clientIp, email = null, desc = null, type = null)
        )

        fun static(user: UserRecord): UserRecordProvider = Static(user)

        fun lazy(provider: () -> UserRecord): UserRecordProvider = Lazy(provider)
    }

    class Static(private val user: UserRecord) : UserRecordProvider {
        override fun invoke(): UserRecord = user
    }

    class Lazy(provider: () -> UserRecord) : UserRecordProvider {

        private val userRecord: UserRecord by kotlin.lazy { provider() }

        override fun invoke(): UserRecord = userRecord
    }
}
