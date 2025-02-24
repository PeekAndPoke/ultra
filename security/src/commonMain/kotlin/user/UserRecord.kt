package de.peekandpoke.ultra.security.user

import kotlinx.serialization.Serializable

@Serializable
data class UserRecord(
    val userId: String = ANONYMOUS_ID,
    val clientIp: String? = null,
    val email: String? = null,
    val desc: String? = null,
    val type: String? = null,
) {
    companion object {
        const val ANONYMOUS_ID = "anonymous"
        const val SYSTEM_ID = "system"

        val anonymous = UserRecord(
            userId = ANONYMOUS_ID,
            clientIp = null,
            email = null,
            desc = null,
            type = null,
        )

        val system = UserRecord(
            userId = SYSTEM_ID,
            clientIp = null,
            email = null,
            desc = null,
            type = null,
        )
    }

    fun isAnonymous() = this == anonymous || userId == ANONYMOUS_ID
    fun isSystem() = this == system || userId == SYSTEM_ID
}
