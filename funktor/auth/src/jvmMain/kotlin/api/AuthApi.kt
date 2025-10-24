package de.peekandpoke.funktor.auth.api

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.funktorAuth
import de.peekandpoke.funktor.auth.model.AuthActivateResponse
import de.peekandpoke.funktor.auth.model.AuthRecoveryResponse
import de.peekandpoke.funktor.auth.model.AuthSignInResponse
import de.peekandpoke.funktor.auth.model.AuthSignUpResponse
import de.peekandpoke.funktor.auth.model.AuthUpdateResponse
import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.core.user
import de.peekandpoke.funktor.rest.ApiRoutes
import de.peekandpoke.funktor.rest.docs.codeGen
import de.peekandpoke.funktor.rest.docs.docs
import de.peekandpoke.ultra.common.remote.ApiResponse
import kotlinx.coroutines.delay
import kotlin.random.Random

class AuthApi(converter: OutgoingConverter) : ApiRoutes("login", converter) {

    val getRealm = AuthApiClient.GetRealm.mount(AuthApiFeature.RealmParam::class) {
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

    val signIn = AuthApiClient.SignIn.mount(AuthApiFeature.RealmParam::class) {
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

    val update = AuthApiClient.Update.mount(AuthApiFeature.RealmParam::class) {
        docs {
            name = "Update"
        }.codeGen {
            funcName = "update"
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
                    .update(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.badRequest(AuthUpdateResponse.failed)
                    .withInfo(e.message ?: "")
            }
        }
    }

    val recover = AuthApiClient.Recover.mount(AuthApiFeature.RealmParam::class) {
        docs {
            name = "Recover"
        }.codeGen {
            funcName = "recover"
        }.authorize {
            public()
        }.handle { params, body ->
            // Let the bots wait a bit
            letTheBotsWait()

            try {
                funktorAuth
                    .recover(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.badRequest(AuthRecoveryResponse.failed)
                    .withInfo(e.message ?: "")
            }
        }
    }

    val signUp = AuthApiClient.SignUp.mount(AuthApiFeature.RealmParam::class) {
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

    val activate = AuthApiClient.Activate.mount(AuthApiFeature.RealmParam::class) {
        docs {
            name = "Activate"
        }.codeGen {
            funcName = "activate"
        }.authorize {
            public()
        }.handle { params, body ->
            letTheBotsWait()

            try {
                funktorAuth
                    .activate(params.realm, body)
                    .let { ApiResponse.ok(it) }
            } catch (e: AuthError) {
                ApiResponse.badRequest(AuthActivateResponse(success = false))
                    .withInfo(e.message ?: "")
            }
        }
    }

    private suspend fun letTheBotsWait() {
        delay(Random.nextLong(250, 500))
    }
}
