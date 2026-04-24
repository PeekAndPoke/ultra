package io.peekandpoke.funktor.rest.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.peekandpoke.funktor.core.jwtGenerator
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserProvider
import io.peekandpoke.ultra.security.user.UserRecord

/**
 * Builds a per-call [UserProvider] by dispatching on whichever [Caller] principal authentication
 * produced. Returns an anonymous [User] if no principal is present (e.g. the route is outside
 * any `authenticate { }` block).
 *
 * Pair with funktor's [jwtCaller] / [apiKeyCaller] / [anonymous] auth-config helpers.
 *
 * **Failure semantics** (inherited from the auth-provider chain): a request bearing an
 * *invalid* credential (expired JWT, revoked API key, wrong signature) falls through to
 * [Caller.AnonymousCaller] rather than producing a 401. Routes that require an authenticated
 * principal must check [User.isAnonymous] explicitly. This is the same fail-soft behaviour as
 * the previous design and is intentional: it lets a single endpoint serve both anonymous and
 * authenticated views without the chain rejecting the request before the route is reached.
 */
fun ApplicationCall.currentUserProvider(): UserProvider = UserProvider.lazy {
    val clientIp = request.origin.remoteHost
    val anonymousUser = User(
        record = UserRecord.Anonymous(clientIp = clientIp),
        permissions = UserPermissions.anonymous,
    )

    when (val caller = principal<Caller>() ?: Caller.AnonymousCaller) {

        is Caller.AnonymousCaller -> anonymousUser

        is Caller.JwtCaller -> kontainer.jwtGenerator.extractUser(clientIp, caller.payload)

        is Caller.ApiKeyCaller -> User(
            record = UserRecord.ApiKey(
                userId = caller.userId,
                clientIp = clientIp,
                keyId = caller.keyId,
                keyName = caller.keyName,
                email = caller.email,
                desc = caller.desc,
                type = caller.type,
            ),
            permissions = caller.permissions,
        )
    }
}
