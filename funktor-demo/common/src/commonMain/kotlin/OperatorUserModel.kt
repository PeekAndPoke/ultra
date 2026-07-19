package io.peekandpoke.funktor.demo.common

import kotlinx.serialization.Serializable

@Serializable
data class OperatorUserModel(
    val id: String,
    val name: String,
    val email: String,
    val isSuperUser: Boolean,
) {
    companion object {
        const val USER_TYPE = "OperatorUser"
    }
}
