package de.peekandpoke.funktor.rest.auth

import de.peekandpoke.funktor.core.jwtGenerator
import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.ultra.security.user.User
import de.peekandpoke.ultra.security.user.UserProvider
import de.peekandpoke.ultra.security.user.UserRecord
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*

fun ApplicationCall.jwtUserProvider() = UserProvider.lazy {
    val clientIp = request.origin.remoteHost

    val anonymous = User.anonymous.copy(
        record = UserRecord.anonymous.copy(clientIp = clientIp)
    )

    val principal = principal<JWTPrincipal>() ?: return@lazy anonymous
    val payload = principal.payload

    kontainer.jwtGenerator.extractUser(clientIp, payload)
}
