package io.peekandpoke.funktor.demo.common

import kotlinx.serialization.Serializable

/** Frontend model of a b2b (customer-admin) tenant user. */
@Serializable
data class B2bUserModel(
    val id: String,
    val name: String,
    val email: String,
) {
    companion object {
        const val USER_TYPE = "B2bUser"
    }
}
