package io.peekandpoke.funktor.demo.common

import io.peekandpoke.ultra.security.user.UserId
import kotlinx.serialization.Serializable

/** Frontend model of a b2b (customer-admin) tenant user. */
@Serializable
data class B2bUserModel(
    val id: UserId,
    val name: String,
    val email: String,
) {
    companion object {
        const val USER_TYPE = "B2bUser"
    }
}
