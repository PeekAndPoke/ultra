package de.peekandpoke.funktor.auth.api

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import de.peekandpoke.funktor.auth.funktorAuth
import de.peekandpoke.funktor.auth.model.AuthActivateActivateResponse
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import de.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import de.peekandpoke.funktor.auth.model.AuthSignInResponse
import de.peekandpoke.funktor.auth.model.AuthSignUpResponse
import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.core.user
import de.peekandpoke.funktor.rest.ApiRoutes
import de.peekandpoke.funktor.rest.docs.codeGen
import de.peekandpoke.funktor.rest.docs.docs
import de.peekandpoke.ultra.common.remote.ApiResponse
import kotlinx.coroutines.delay
import kotlin.random.Random

class AuthApi(converter: OutgoingConverter) : ApiRoutes("login", converter) {

    val getRealm = AuthApiClient.GetRealm.mount(RealmParam::class) {
        docs {
            name = "Get realm"
        }.codeGen {
            funcName = "getRealm"
        }.authorize {
            public()
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
        }.authorize {
            public()
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

    val setPassword = AuthApiClient.SetPassword.mount(RealmParam::class) {
        docs {
            name = "Set Password"
        }.codeGen {
            funcName = "setPassword"
        }.authorize {
            public()
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

    val signUp = AuthApiClient.SignUp.mount(RealmParam::class) {
        docs {
            name = "Sign up"
        }.codeGen {
            funcName = "signUp"
        }.authorize {
            public()
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
        }.authorize {
            public()
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
        }.authorize {
            public()
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
            }.authorize {
                public()
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
            }.authorize {
                public()
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

    private suspend fun letTheBotsWait() {
        delay(Random.nextLong(250, 500))
    }
}
