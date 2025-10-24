package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthUpdateRequest {

    @Serializable
    @SerialName("set_password")
    data class SetPassword(
        override val provider: String,
        override val userId: String,
        val newPassword: String,
    ) : AuthUpdateRequest

    /** The auth provider */
    val provider: String

    /** The id of the user to make the update for */
    val userId: String
}
