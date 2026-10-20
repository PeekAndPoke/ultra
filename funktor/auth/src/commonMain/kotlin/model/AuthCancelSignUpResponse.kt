package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

/** `success = false` for an unknown, used or expired token, or an account that is already activated. */
@Serializable
data class AuthCancelSignUpResponse(
    val success: Boolean,
)
