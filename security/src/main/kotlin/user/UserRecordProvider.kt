package de.peekandpoke.ultra.security.user

interface UserRecordProvider {
    operator fun invoke(): UserRecord

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val anonymous: UserRecordProvider = static("anonymous", "unknown")

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
