package io.peekandpoke.funktor

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.*
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthRealmModel
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpResponse
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.auth.model.bearerToken
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.messaging.senders.hrefs
import io.peekandpoke.funktor.rest.acl.UserApiAccessMatrix
import io.peekandpoke.ultra.common.decodeUriComponent
import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.security.user.UserId

class AuthApiSpec : FunktorApiSpec() {

    private val api by service(AuthApiFeature::class)

    private val existingRealm = RealmParam(realm = TestUserRealm.REALM)
    private val nonExistentRealm = RealmParam(realm = RealmId("non-existent"))
    private val provider = EmailAndPasswordAuth.ID
    private val signupEmail = "signup-${System.currentTimeMillis()}@test.com"

    init {
        api.authLogin.getRealm { route ->
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

        api.authLogin.signIn { route ->
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

        api.authLogin.signUp { route ->
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

                            // Succeeded, but deliberately WITHOUT a session: the address is unproven
                            // until the activation link is followed.
                            response.success shouldBe true
                            response.requiresActivation shouldBe true
                            response.signIn shouldBe null
                        }
                    }
                }
            }
        }

        api.authLogin.signIn { route ->
            "Sign in before activation must be refused, even with the correct password" {
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
                            // NOT a failure status: the credential check passed, so the answer is a
                            // response CASE. An unknown user or a wrong password still gets a 403, so
                            // this cannot go vacuously green if the sign-up above is renamed or
                            // reordered — which a bare "shouldBe Forbidden" assertion would.
                            status shouldBe HttpStatusCode.OK

                            apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.ActivationRequired>()
                        }
                    }
                }
            }

            "Sign in after activation must return a token" {
                apiApp {
                    anonymous {
                        // The activation token is only ever delivered by mail — there is no other way
                        // to get one, which is the point of the whole flow.
                        val token = capturedEmails.lastTo(signupEmail).shouldNotBeNull()
                            .hrefs().single()
                            .substringAfterLast("/activate/")
                            .decodeUriComponent()

                        api.authLogin.activateAccount(
                            existingRealm,
                            body = AuthActivateAccountRequest(provider = provider, token = token),
                        ) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<AuthActivateAccountResponse>()?.success shouldBe true
                        }

                        route(
                            existingRealm,
                            body = AuthSignInRequest.EmailAndPassword(
                                provider = provider,
                                email = signupEmail,
                                password = "Test1234!",
                            ),
                        ) {
                            status shouldBe HttpStatusCode.OK
                            val response = apiResponseData<AuthSignInResponse>() as? AuthSignInResponse.Success
                            response.shouldNotBeNull()
                            response.bearerToken!!.shouldNotBeBlank()
                        }
                    }
                }
            }
        }

        api.authUser.setPassword { route ->
            "Anonymous set password must be unauthorized" {
                apiApp {
                    anonymous {
                        route(
                            nonExistentRealm,
                            body = AuthSetPasswordRequest(
                                provider = provider,
                                userId = UserId("non-existent"),
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

        api.authLogin.activateAccount { route ->
            "Activate account with invalid token must return ok with success=false" {
                apiApp {
                    anonymous {
                        route(
                            existingRealm,
                            body = AuthActivateAccountRequest(provider = provider, token = "invalid-token"),
                        ) {
                            // OK, not an error: the endpoint is anonymous, so answering differently
                            // for a token that exists would let an attacker probe for live tokens.
                            status shouldBe HttpStatusCode.OK
                            val result = apiResponseData<AuthActivateAccountResponse>()
                            result.shouldNotBeNull()
                            result.success shouldBe false
                        }
                    }
                }
            }

            "Activate account in a non-existent realm must return bad request" {
                apiApp {
                    anonymous {
                        route(
                            nonExistentRealm,
                            body = AuthActivateAccountRequest(provider = provider, token = "invalid-token"),
                        ) {
                            status shouldBe HttpStatusCode.BadRequest
                        }
                    }
                }
            }
        }

        api.authLogin.recoverAccountInitPasswordReset { route ->
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

        api.authLogin.recoverAccountValidatePasswordResetToken { route ->
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

        api.authLogin.recoverAccountSetPasswordWithToken { route ->
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

        api.authUser.refreshToken { route ->
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
                            val response = apiResponseData<AuthSignInResponse>() as? AuthSignInResponse.Success
                            response.shouldNotBeNull()
                            response.bearerToken!!.shouldNotBeBlank()
                            // The response STATES the permissions now -- the client no longer decodes
                            // them out of the token, so this is the only thing checking they are right.
                            response.permissions.isSuperUser shouldBe false
                            response.expiresAt.shouldNotBeNull()
                            // A refresh must return a token for the SAME user.
                            response.userId shouldBe regularUserId
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
                            val response = apiResponseData<AuthSignInResponse>() as? AuthSignInResponse.Success
                            response.shouldNotBeNull()
                            response.bearerToken!!.shouldNotBeBlank()
                            response.permissions.isSuperUser shouldBe true
                            response.expiresAt.shouldNotBeNull()
                            // A refresh must return a token for the SAME user.
                            response.userId shouldBe superUserId
                            response.realm.shouldNotBeNull()
                            response.user.shouldNotBeNull()
                        }
                    }
                }
            }
        }

        api.authUser.getMyApiAccess { route ->
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
