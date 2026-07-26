package io.peekandpoke.funktor.demo.common

import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import kotlinx.serialization.Serializable

@Serializable
data class AdminUserModel(
    val id: UserId,
    val name: String,
    val email: EmailAddress,
    val isSuperUser: Boolean,
) {
    companion object {
        const val USER_TYPE = "AppUser"
    }
}
