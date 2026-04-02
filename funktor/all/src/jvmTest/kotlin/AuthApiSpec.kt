package io.peekandpoke.funktor

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.http.*
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateActivateResponse
import io.peekandpoke.funktor.auth.model.AuthRealmModel
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpResponse
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth

class AuthApiSpec : FunktorApiSpec() {

    private val api by service(AuthApiFeature::class)

    private val existingRealm = RealmParam(realm = TestUserRealm.REALM)
    private val nonExistentRealm = RealmParam(realm = "non-existent")
    private val provider = EmailAndPasswordAuth.ID
    private val signupEmail = "signup-${System.currentTimeMillis()}@test.com"

    init {
        api.auth.getRealm { route ->
            "Getting an existing realm must return realm details" {
                apiApp {
                    anonymous {
                        route(existingRealm) {
                            status shouldBe HttpStatusCode.OK
                            val realm = apiResponseData<AuthRealmModel>()
                            realm.shouldNotBeNull()
                            realm.id shouldBe TestUserRealm.REALM
                            realm.providers.shouldNotBeEmpty()
                        }
                    }
                }
            }

            "Getting a non-existent realm must return not found" {
                apiApp {
                    anonymous {
                        route(nonExistentRealm) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }

        api.auth.signIn { route ->
            "Sign in with non-existent realm must return forbidden" {
                apiApp {
                    anonymous {
                        route(
                            nonExistentRealm,
                            body = AuthSignInRequest.EmailAndPassword(
                                provider = provider,
                                email = "test@test.com",
                                password = "password",
                            ),
                        ) {
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }

            "Sign in with invalid credentials must return forbidden" {
                apiApp {
                    anonymous {
                        route(
                            existingRealm,
                            body = AuthSignInRequest.EmailAndPassword(
                                provider = provider,
                                email = "nonexistent@test.com",
                                password = "wrong-password",
                            ),
                        ) {
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }
        }

        api.auth.signUp { route ->
            "Sign up with non-existent realm must return bad request" {
                apiApp {
                    anonymous {
                        route(
                            nonExistentRealm,
                            body = AuthSignUpRequest.EmailAndPassword(
                                provider = provider,
                                email = "signup@test.com",
                                password = "Test1234!",
                            ),
                        ) {
                            status shouldBe HttpStatusCode.BadRequest
                        }
                    }
                }
            }

            "Sign up with valid data must succeed" {
                apiApp {
                    anonymous {
                        route(
                            existingRealm,
                            body = AuthSignUpRequest.EmailAndPassword(
                                provider = provider,
                                email = signupEmail,
                                password = "Test1234!",
                                displayName = "Test User",
                            ),
                        ) {
                            status shouldBe HttpStatusCode.OK
                            val response = apiResponseData<AuthSignUpResponse>()
                            response.shouldNotBeNull()
                        }
                    }
                }
            }
        }

        api.auth.signIn { route ->
            "Sign in after sign up must return a token" {
                apiApp {
                    anonymous {
                        route(
                            existingRealm,
                            body = AuthSignInRequest.EmailAndPassword(
                                provider = provider,
                                email = signupEmail,
                                password = "Test1234!",
                            ),
                        ) {
                            status shouldBe HttpStatusCode.OK
                            val response = apiResponseData<AuthSignInResponse>()
                            response.shouldNotBeNull()
                            response.token.token.shouldNotBeBlank()
                        }
                    }
                }
            }
        }

        api.auth.setPassword { route ->
            "Set password with non-existent realm must return forbidden" {
                apiApp {
                    anonymous {
                        route(
                            nonExistentRealm,
                            body = AuthSetPasswordRequest(
                                provider = provider,
                                userId = "non-existent",
                                currentPassword = "old",
                                newPassword = "new",
                            ),
                        ) {
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }
        }

        api.auth.activateAccount { route ->
            "Activate account with invalid token must return ok with success=false" {
                apiApp {
                    anonymous {
                        route(
                            nonExistentRealm,
                            body = AuthActivateAccountRequest(token = "invalid-token"),
                        ) {
                            status shouldBe HttpStatusCode.OK
                            val result = apiResponseData<AuthActivateActivateResponse>()
                            result.shouldNotBeNull()
                            result.success shouldBe false
                        }
                    }
                }
            }
        }

        api.auth.recoverAccountInitPasswordReset { route ->
            "Recover account init with non-existent realm must return bad request" {
                apiApp {
                    anonymous {
                        route(
                            nonExistentRealm,
                            body = AuthRecoverAccountRequest.InitPasswordReset(
                                provider = provider,
                                email = "test@test.com",
                            ),
                        ) {
                            status shouldBe HttpStatusCode.BadRequest
                        }
                    }
                }
            }
        }

        api.auth.recoverAccountValidatePasswordResetToken { route ->
            "Validate reset token with non-existent realm must return bad request" {
                apiApp {
                    anonymous {
                        route(
                            nonExistentRealm,
                            body = AuthRecoverAccountRequest.ValidatePasswordResetToken(
                                provider = provider,
                                token = "invalid-token",
                            ),
                        ) {
                            status shouldBe HttpStatusCode.BadRequest
                        }
                    }
                }
            }
        }

        api.auth.recoverAccountSetPasswordWithToken { route ->
            "Set password with token on non-existent realm must return bad request" {
                apiApp {
                    anonymous {
                        route(
                            nonExistentRealm,
                            body = AuthRecoverAccountRequest.SetPasswordWithToken(
                                provider = provider,
                                token = "invalid-token",
                                password = "new-password",
                            ),
                        ) {
                            status shouldBe HttpStatusCode.BadRequest
                        }
                    }
                }
            }
        }
    }
}
