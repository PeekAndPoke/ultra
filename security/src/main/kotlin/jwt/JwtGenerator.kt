package de.peekandpoke.ultra.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.JwtParserBuilder

interface JwtGenerator {
    fun createToken(builder: JwtBuilder.() -> Unit = {}): String

    fun parseToken(token: String, builder: JwtParserBuilder.() -> Unit = {}): Jws<Claims>
}

