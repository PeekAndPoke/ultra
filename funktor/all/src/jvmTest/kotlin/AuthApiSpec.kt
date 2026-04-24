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
import io.peekandpoke.funktor.rest.acl.UserApiAccessMatrix
import io.peekandpoke.ultra.remote.ApiAccessLevel

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
            "Anonymous set password must be unauthorized" {
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
                            status shouldBe HttpStatusCode.Unauthorized
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

        api.auth.refreshToken { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(existingRealm) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Garbage bearer token must fall through to anonymous and be unauthorized" {
                apiApp {
                    authenticate("not-a-real-jwt-or-key") {
                        route(existingRealm) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Structurally-valid JWT with a bad signature must be treated as anonymous and unauthorized" {
                // A real JWT in shape (header.payload.signature) but signed with a different key —
                // the verifier in jwtCaller's default validate must reject it, the request then
                // falls through to AnonymousCaller, and refreshToken denies anonymous.
                val forgedJwt =
                    "eyJhbGciOiJIUzUxMiJ9." +
                            "eyJpc3MiOiJmYWtlIiwic3ViIjoiYXR0YWNrZXIifQ." +
                            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"

                apiApp {
                    authenticate(forgedJwt) {
                        route(existingRealm) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Empty bearer token must fall through to anonymous and be unauthorized" {
                apiApp {
                    authenticate("") {
                        route(existingRealm) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Regular user request must return a refreshed token with user data" {
                apiApp {
                    authenticate(regularUserToken) {
                        route(existingRealm) {
                            status shouldBe HttpStatusCode.OK
                            val response = apiResponseData<AuthSignInResponse>()
                            response.shouldNotBeNull()
                            response.token.token.shouldNotBeBlank()
                            response.token.permissionsNs.shouldNotBeBlank()
                            response.realm.shouldNotBeNull()
                            response.user.shouldNotBeNull()
                        }
                    }
                }
            }

            "Super user request must return a refreshed token with user data" {
                apiApp {
                    authenticate(superUserToken) {
                        route(existingRealm) {
                            status shouldBe HttpStatusCode.OK
                            val response = apiResponseData<AuthSignInResponse>()
                            response.shouldNotBeNull()
                            response.token.token.shouldNotBeBlank()
                            response.token.permissionsNs.shouldNotBeBlank()
                            response.realm.shouldNotBeNull()
                            response.user.shouldNotBeNull()
                        }
                    }
                }
            }
        }

        api.auth.getMyApiAccess { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Regular user request must return access matrix with only non-denied entries" {
                apiApp {
                    authenticate(regularUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                            val matrix = apiResponseData<UserApiAccessMatrix>()
                            matrix.shouldNotBeNull()
                            matrix.entries.shouldNotBeEmpty()
                            // Denied entries are filtered out server-side
                            matrix.entries.none { it.level == ApiAccessLevel.Denied } shouldBe true
                        }
                    }
                }
            }

            "Super user request must return full access matrix with all Granted" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                            val matrix = apiResponseData<UserApiAccessMatrix>()
                            matrix.shouldNotBeNull()
                            matrix.entries.shouldNotBeEmpty()
                            matrix.entries.forEach { entry ->
                                entry.level shouldBe ApiAccessLevel.Granted
                            }
                        }
                    }
                }
            }
        }
    }
}
