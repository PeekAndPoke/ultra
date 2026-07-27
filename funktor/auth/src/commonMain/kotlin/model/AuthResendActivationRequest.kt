package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthResendActivationRequest(
    val provider: String,
    val email: String,
)
