package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthActivateAccountResponse(
    val success: Boolean,
)
