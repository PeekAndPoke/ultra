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

        val system = User(
            record = UserRecord.system,
            permissions = UserPermissions.system
        )
    }

    fun isAnonymous() = record.isAnonymous()

    fun isSystem() = record.isSystem()
}
