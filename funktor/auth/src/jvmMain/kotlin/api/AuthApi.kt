package io.peekandpoke.funktor.auth.api

import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.funktorAuth
import io.peekandpoke.funktor.auth.model.AuthActivateActivateResponse
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import io.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthSignUpResponse
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.core.user
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.acl.UserApiAccessProvider
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Slows a request down by a random delay — evens the timing between hit/miss on public auth routes. */
internal suspend fun letTheBotsWait() {
    delay(Random.nextLong(250, 500))
}

/**
 * The PUBLIC auth endpoints — reachable by anonymous callers (sign-in, sign-up, account recovery,
 * org selection). The credential each carries (password, single-use token) IS the authorization,
 * so the whole group floors `public()`. The authenticated self-service endpoints live in the
 * separate [AuthUserApi] group (an append-only floor cannot mix `public()` with `authenticated()`).
 */
class AuthApi : ApiRoutes("login", defaultAuth = { public() }) {

    val getRealm = AuthApiClient.GetRealm.mount(RealmParam::class) {
        docs {
            name = "Get realm"
        }.codeGen {
            funcName = "getRealm"
        }.handle { params ->
            // Let the bots wait a bit
            val realm = funktorAuth.getRealmOrNull(params.realm)

            ApiResponse.okOrNotFound(
                realm?.asApiModel()
            )
        }
    }

    val signIn = AuthApiClient.SignIn.mount(RealmParam::class) {
        docs {
            name = "Sign in"
        }.codeGen {
            funcName = "signIn"
        }.handle { params, body ->
            // Let the bots wait a bit
            letTheBotsWait()

            try {
                funktorAuth
                    .signIn(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.forbidden<AuthSignInResponse>()
                    .withInfo(e.message ?: "")
            }
        }
    }

    val selectOrg = AuthApiClient.SelectOrg.mount(RealmParam::class) {
        docs {
            name = "Select organisation"
        }.codeGen {
            funcName = "selectOrg"
        }.handle { params, body ->
            // The single-use selection token is the credential here.
            letTheBotsWait()

            try {
                funktorAuth
                    .selectOrg(params.realm, body.selectionToken, body.orgId)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.forbidden<AuthSignInResponse>()
                    .withInfo(e.message ?: "")
            }
        }
    }

    val signUp = AuthApiClient.SignUp.mount(RealmParam::class) {
        docs {
            name = "Sign up"
        }.codeGen {
            funcName = "signUp"
        }.handle { params, body ->
            letTheBotsWait()

            try {
                funktorAuth
                    .signUp(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.badRequest(AuthSignUpResponse.failed)
                    .withInfo(e.message ?: "")
            }
        }
    }

    val activateAccount = AuthApiClient.ActivateAccount.mount(RealmParam::class) {
        docs {
            name = "Activate Account"
        }.codeGen {
            funcName = "activateAccount"
        }.handle { params, body ->
            letTheBotsWait()

            try {
                funktorAuth
                    .activate(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.badRequest(AuthActivateActivateResponse(success = false))
                    .withInfo(e.message ?: "")
            }
        }
    }

    val recoverAccountInitPasswordReset = AuthApiClient.RecoverAccountInitPasswordReset.mount(RealmParam::class) {
        docs {
            name = "Recover Account Init Password Reset"
        }.codeGen {
            funcName = "recoverAccountInitPasswordReset"
        }.handle { params, body ->
            // Let the bots wait a bit
            letTheBotsWait()

            try {
                funktorAuth
                    .recoverAccountInitPasswordReset(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.badRequest(AuthRecoverAccountResponse.InitPasswordReset)
                    .withInfo(e.message ?: "")
            }
        }
    }

    val recoverAccountValidatePasswordResetToken =
        AuthApiClient.RecoverAccountValidatePasswordResetToken.mount(RealmParam::class) {
            docs {
                name = "Recover Account Validate Password Reset Token"
            }.codeGen {
                funcName = "recoverAccountValidatePasswordResetToken"
            }.handle { params, body ->
                // Let the bots wait a bit
                letTheBotsWait()

                try {
                    funktorAuth
                        .recoverAccountValidatePasswordResetToken(params.realm, body)
                        .let { ApiResponse.ok(it) }
                } catch (e: AuthError) {
                    ApiResponse.badRequest(AuthRecoverAccountResponse.ValidatePasswordResetToken(success = false))
                        .withInfo(e.message ?: "")
                }
            }
        }

    val recoverAccountSetPasswordWithToken =
        AuthApiClient.RecoverAccountSetPasswordWithToken.mount(RealmParam::class) {
            docs {
                name = "Recover Account Set Password With Token"
            }.codeGen {
                funcName = "recoverAccountSetPasswordWithToken"
            }.handle { params, body ->
                // Let the bots wait a bit
                letTheBotsWait()

                try {
                    funktorAuth
                        .recoverAccountSetPasswordWithToken(params.realm, body)
                        .let { ApiResponse.ok(it) }
                } catch (e: AuthError) {
                    ApiResponse.badRequest(AuthRecoverAccountResponse.SetPasswordWithToken(success = false))
                        .withInfo(e.message ?: "")
                }
            }
        }
}

/**
 * The AUTHENTICATED self-service auth endpoints — any logged-in user managing their OWN session,
 * regardless of realm (the realm is inside the token). The whole group floors `authenticated()`;
 * per-route body checks (e.g. "userId matches the caller") stay in the handlers.
 */
class AuthUserApi : ApiRoutes("login", defaultAuth = { authenticated() }) {

    val setPassword = AuthApiClient.SetPassword.mount(RealmParam::class) {
        docs {
            name = "Set Password"
        }.codeGen {
            funcName = "setPassword"
        }.handle { params, body ->
            // Let the bots wait a bit
            letTheBotsWait()

            // Check if the current user is able to do the update
            if (user.record.userId != body.userId) {
                return@handle ApiResponse.forbidden()
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

    val refreshToken = AuthApiClient.RefreshToken.mount(RealmParam::class) {
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
