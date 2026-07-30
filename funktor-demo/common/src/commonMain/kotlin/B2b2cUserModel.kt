package io.peekandpoke.funktor.demo.common

import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import kotlinx.serialization.Serializable

/** Frontend model of a b2b2c (end-user) tenant user. */
@Serializable
data class B2b2cUserModel(
    val id: UserId,
    val name: String,
    val email: EmailAddress,
) {
    companion object {
        const val USER_TYPE = "B2b2cUser"
    }
}
