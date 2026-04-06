package io.peekandpoke.funktor.rest.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.peekandpoke.funktor.core.jwtGenerator
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.ultra.security.jwt.JwtAnonymous
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserProvider
import io.peekandpoke.ultra.security.user.UserRecord

fun ApplicationCall.jwtUserProvider() = UserProvider.lazy {
    val clientIp = request.origin.remoteHost

    val anonymous = User.anonymous.copy(
        record = UserRecord.anonymous.copy(clientIp = clientIp)
    )

    val principal = principal<JWTPrincipal>() ?: return@lazy anonymous
    val payload = principal.payload

    // The JWT challenge may provide a JwtAnonymous payload for unauthenticated requests
    if (payload is JwtAnonymous) return@lazy anonymous

    kontainer.jwtGenerator.extractUser(clientIp, payload)
}
