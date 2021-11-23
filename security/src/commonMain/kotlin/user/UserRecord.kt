package de.peekandpoke.ultra.security.user

import kotlinx.serialization.Serializable

@Serializable
data class UserRecord(
    val userId: String = "anonymous",
    val clientIp: String = "unknown",
    val desc: String = "n/a",
    val type: String = "n/a",
) {
    fun isAnonymous() = this == anonymous || userId == "anonymous"

    companion object {
        val anonymous = UserRecord()
    }
}
