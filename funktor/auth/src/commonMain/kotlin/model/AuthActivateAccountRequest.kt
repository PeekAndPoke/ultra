package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthActivateAccountRequest(
    val token: String,
)
