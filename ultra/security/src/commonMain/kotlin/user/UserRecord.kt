package io.peekandpoke.ultra.security.user

import kotlinx.serialization.Serializable

/** Core identity record for a user, holding id, IP, email, description, and type. */
@Serializable
data class UserRecord(
    val userId: String = ANONYMOUS_ID,
    val clientIp: String? = null,
    val email: String? = null,
    val desc: String? = null,
    val type: String? = null,
) {
    companion object {
        /** User id for anonymous users. */
        const val ANONYMOUS_ID = "anonymous"

        /** User id for system users. */
        const val SYSTEM_ID = "system"

        /** Singleton anonymous user record. */
        val anonymous = UserRecord(
            userId = ANONYMOUS_ID,
            clientIp = null,
            email = null,
            desc = null,
            type = null,
        )

        /** Creates a system user record with the given [ip] address. */
        fun system(ip: String?) = UserRecord(
            userId = SYSTEM_ID,
            clientIp = ip,
            email = null,
            desc = null,
            type = null,
        )
    }

    /** Returns true if this record represents an anonymous user. */
    fun isAnonymous() = userId == ANONYMOUS_ID

    /** Returns true if this record represents the system user. */
    fun isSystem() = userId == SYSTEM_ID
}
