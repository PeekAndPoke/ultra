package io.peekandpoke.funktor.demo.server

import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.demo.common.operator.OperatorDashboardStats
import io.peekandpoke.funktor.demo.server.operator.OperatorApiFeature
import io.peekandpoke.funktor.testing.AppSpec

/**
 * The operator console is scoped to super-users OF THE OPERATORS REALM. All realms share one JWT
 * signing key, so the guard must check the user-type claim on top of `isSuperUser` — the admin
 * realm also mints super-user tokens, and those must be rejected here (realm boundary, see
 * `20260719-cross-realm-authz-and-tests.md`).
 */
class OperatorApiTest : AppSpec<FunktorDemoConfig>(testApp) {

    private val api by service(OperatorApiFeature::class)
    private val authApi by service(AuthApiFeature::class)

    private fun signIn(email: String) = AuthSignInRequest.EmailAndPassword(
        provider = "email-password",
        email = email,
        password = "S3cret123!",
    )

    init {
        installAllFixturesBeforeSpec()

        api.operator.dashboardStats { statsRoute ->
            // Route blocks register FreeSpec containers at spec level and cannot nest, so the
            // sign-in route is referenced directly.
            val signInRoute = authApi.auth.signIn

            // Security boundary: this also proves the feature is registered and the route is
            // mounted (an unmounted route would 404, not 401).
            "operator dashboard stats rejects an anonymous caller" {
                apiApp {
                    anonymous {
                        request(statsRoute) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "an operators-realm super-user gets the dashboard stats" {
                apiApp {
                    var token = ""

                    anonymous {
                        signInRoute(RealmParam("operators"), body = signIn("karsten.john.gerber@googlemail.com")) {
                            status shouldBe HttpStatusCode.OK
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>()
                                .token.token
                        }
                    }

                    authenticate(token) {
                        request(statsRoute) {
                            status shouldBe HttpStatusCode.OK

                            val stats = apiResponseData<OperatorDashboardStats>().shouldNotBeNull()
                            stats.operators shouldBeGreaterThanOrEqual 1
                            stats.orgs shouldBeGreaterThanOrEqual 2
                            // The by-status buckets must always sum to the org total.
                            stats.orgsByStatus.values.sum() shouldBe stats.orgs
                        }
                    }
                }
            }

            "an admin-realm super-user is rejected (realm boundary)" {
                apiApp {
                    var token = ""

                    anonymous {
                        signInRoute(RealmParam("admin-user"), body = signIn("karsten.john.gerber@googlemail.com")) {
                            status shouldBe HttpStatusCode.OK
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>()
                                .token.token
                        }
                    }

                    authenticate(token) {
                        request(statsRoute) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "a b2b tenant user is rejected" {
                apiApp {
                    var token = ""

                    anonymous {
                        signInRoute(RealmParam("b2b"), body = signIn("single@b2b.test")) {
                            status shouldBe HttpStatusCode.OK
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>()
                                .token.token
                        }
                    }

                    authenticate(token) {
                        request(statsRoute) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }
        }
    }
}
