package de.peekandpoke.ultra.security.user

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val record: UserRecord,
    val permissions: UserPermissions,
) {
    companion object {
        val anonymous = User(
            record = UserRecord.anonymous,
            permissions = UserPermissions.anonymous,
        )

        fun system(ip: String?) = User(
            record = UserRecord.system(ip),
            permissions = UserPermissions.system
        )
    }

    fun isAnonymous() = record.isAnonymous()

    fun isSystem() = record.isSystem()
}
