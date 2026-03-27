package io.peekandpoke.ultra.security.user

import kotlinx.serialization.Serializable

/** Represents an authenticated or anonymous user with associated permissions. */
@Serializable
data class User(
    val record: UserRecord,
    val permissions: UserPermissions,
) {
    companion object {
        /** Singleton anonymous user with no permissions. */
        val anonymous = User(
            record = UserRecord.anonymous,
            permissions = UserPermissions.anonymous,
        )

        /** Creates a system user with the given [ip] address. */
        fun system(ip: String?) = User(
            record = UserRecord.system(ip),
            permissions = UserPermissions.system
        )
    }

    /** Returns true if this user is anonymous. */
    fun isAnonymous() = record.isAnonymous()

    /** Returns true if this user is the system user. */
    fun isSystem() = record.isSystem()
}
