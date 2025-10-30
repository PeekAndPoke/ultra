package de.peekandpoke.funktor.auth

import de.peekandpoke.ultra.security.jwt.JwtConfig

data class AuthConfig(
    val jwt: JwtConfig? = null,
)
