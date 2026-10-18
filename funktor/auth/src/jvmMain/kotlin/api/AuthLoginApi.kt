package io.peekandpoke.funktor.auth.api

import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.funktorAuth
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import io.peekandpoke.funktor.auth.model.AuthResendActivationResponse
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthSignUpResponse
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse

/**
 * The PUBLIC auth endpoints — reachable by anonymous callers (sign-in, sign-up, account recovery,
 * org selection). The credential each carries (password, single-use token) IS the authorization,
 * so the whole group floors `public()`. The authenticated self-service endpoints live in the
 * separate [AuthUserApi] group (an append-only floor cannot mix `public()` with `authenticated()`).
 */
class AuthLoginApi : ApiRoutes("login", authFloor = { public() }) {

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
                ApiResponse.badRequest(AuthActivateAccountResponse(success = false))
                    .withInfo(e.message ?: "")
            }
        }
    }

    val resendActivation = AuthApiClient.ResendActivation.mount(RealmParam::class) {
        docs {
            name = "Resend Activation"
        }.codeGen {
            funcName = "resendActivation"
        }.handle { params, body ->
            letTheBotsWait()

            try {
                funktorAuth
                    .resendActivation(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.badRequest(AuthResendActivationResponse(sent = false))
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

