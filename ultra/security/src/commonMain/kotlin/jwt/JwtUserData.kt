package io.peekandpoke.ultra.security.jwt

import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserRecord
import kotlinx.serialization.Serializable

@Serializable
data class JwtUserData(
    val id: UserId,
    val desc: String,
    val type: String,
    val email: EmailAddress? = null,
) {
    fun toUserRecord(clientIp: String?): UserRecord = UserRecord.LoggedIn(
        userId = id,
        clientIp = clientIp,
        email = email,
        desc = desc,
        type = type,
    )
}
