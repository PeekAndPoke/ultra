package de.peekandpoke.ultra.security.jwt

import de.peekandpoke.ultra.security.user.UserRecord
import kotlinx.serialization.Serializable

@Serializable
data class JwtUserData(
    val id: String,
    val desc: String,
    val type: String,
    val email: String? = null,
) {
    fun toUserRecord(clientIp: String?) = UserRecord(
        userId = id,
        clientIp = clientIp,
        email = email,
        desc = desc,
        type = type,
    )
}
