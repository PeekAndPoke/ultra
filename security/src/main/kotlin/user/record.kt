package de.peekandpoke.ultra.security.user

data class UserRecord(
    val userId: String = "anonymous",
    val clientIp: String = "unknown"
)

interface UserRecordProvider {
    operator fun invoke(): UserRecord

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val anonymous: UserRecordProvider = static("anonymous", "unknown")

        fun static(userId: String, clientIp: String): UserRecordProvider = static(UserRecord(userId, clientIp))

        fun static(user: UserRecord): UserRecordProvider = StaticUserRecordProvider(user)

        fun lazy(provider: () -> UserRecord): UserRecordProvider = LazyUserRecordProvider(provider)
    }
}

internal class StaticUserRecordProvider(private val user: UserRecord) : UserRecordProvider {
    override fun invoke(): UserRecord = user
}

internal class LazyUserRecordProvider(provider: () -> UserRecord) : UserRecordProvider {

    private val userRecord by lazy { provider() }

    override fun invoke(): UserRecord = userRecord
}
