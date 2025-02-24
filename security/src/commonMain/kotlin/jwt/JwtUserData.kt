package de.peekandpoke.ultra.security.jwt

import kotlinx.serialization.Serializable

@Serializable
data class JwtUserData(
    val id: String,
    val desc: String,
    val type: String,
    val email: String? = null,
)
