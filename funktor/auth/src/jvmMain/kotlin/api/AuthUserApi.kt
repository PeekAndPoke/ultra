package io.peekandpoke.funktor.auth.api

import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.funktorAuth
import io.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.core.user
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.acl.UserApiAccessProvider
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse

/**
 * The AUTHENTICATED self-service auth endpoints — any logged-in user managing their OWN session,
 * regardless of realm (the realm is inside the token). The whole group floors `authenticated()`;
 * per-route body checks (e.g. "userId matches the caller") stay in the handlers.
 */
class AuthUserApi : ApiRoutes("login", authFloor = { authenticated() }) {

    val setPassword = AuthApiClient.SetPassword.mount(AuthApiFeature.RealmParam::class) {
        docs {
            name = "Set Password"
        }.codeGen {
            funcName = "setPassword"
        }.handle { params, body ->
            // Let the bots wait a bit
            letTheBotsWait()

            // Check if the current user is able to do the update
            if (user.record.userId != body.userId) {
                return@handle ApiResponse.Companion.forbidden()
            }

            try {
                funktorAuth
                    .setPassword(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.badRequest(AuthSetPasswordResponse.failed)
                    .withInfo(e.message ?: "")
            }
        }
    }

    val refreshToken = AuthApiClient.RefreshToken.mount(AuthApiFeature.RealmParam::class) {
        docs {
            name = "Refresh Token"
        }.codeGen {
            funcName = "refreshToken"
        }.handle { params ->
            try {
                funktorAuth
                    .refreshToken(params.realm, user.record.userId, user.record.type, user.permissions.org)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.forbidden<AuthSignInResponse>()
                    .withInfo(e.message ?: "")
            }
        }
    }

    val getMyApiAccess = AuthApiClient.GetMyApiAccess.mount {
        docs {
            name = "My API Access"
        }.codeGen {
            funcName = "getMyApiAccess"
        }.handle {
            val provider = call.kontainer.get(UserApiAccessProvider::class)

            ApiResponse.ok(provider.describeForUser(user))
        }
    }
}
