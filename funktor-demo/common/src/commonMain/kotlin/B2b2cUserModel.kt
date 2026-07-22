package io.peekandpoke.funktor.demo.common

import kotlinx.serialization.Serializable

/** Frontend model of a b2b2c (end-user) tenant user. */
@Serializable
data class B2b2cUserModel(
    val id: String,
    val name: String,
    val email: String,
) {
    companion object {
        const val USER_TYPE = "B2b2cUser"
    }
}
