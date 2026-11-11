package io.peekandpoke.funktor.demo.server

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.auth.model.bearerToken
import io.peekandpoke.funktor.demo.server.api.showcase.ClusterShowcaseRepoParams
import io.peekandpoke.funktor.demo.server.api.showcase.ShowcaseApiFeature
import io.peekandpoke.funktor.testing.AppSpec

/**
 * The demo's depot listings are for superusers only: they name the repositories' server locations and their files,
 * insights records included (ULTRA-00304).
 */
class ShowcaseDepotAuthTest : AppSpec<FunktorDemoConfig>(testApp) {

    private val api by service(ShowcaseApiFeature::class)
    private val authApi by service(AuthApiFeature::class)

    private fun signIn(email: String) = AuthSignInRequest.EmailAndPassword(
        provider = "email-password",
        email = email,
        password = "S3cret123!",
    )

    init {
        installAllFixturesBeforeSpec()

        api.clusterAdmin.getDepotRepos { reposRoute ->
            val signInRoute = authApi.authLogin.signIn

            "the depot repositories are refused to an anonymous caller" {
                apiApp {
                    anonymous {
                        request(reposRoute) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "the depot repositories are refused to a user who is no superuser" {
                apiApp {
                    var token = ""

                    anonymous {
                        signInRoute(RealmParam(RealmId("b2b")), body = signIn("single@b2b.test")) {
                            status shouldBe HttpStatusCode.OK
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>()
                                .bearerToken
                        }
                    }

                    authenticate(token) {
                        request(reposRoute) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "a superuser gets the depot repositories" {
                apiApp {
                    var token = ""

                    anonymous {
                        signInRoute(RealmParam(RealmId("admin-user")), body = signIn("karsten.john.gerber@googlemail.com")) {
                            status shouldBe HttpStatusCode.OK
                            token = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>()
                                .bearerToken
                        }
                    }

                    authenticate(token) {
                        request(reposRoute) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.clusterAdmin.getDepotFiles { filesRoute ->
            "the depot files are refused to an anonymous caller" {
                apiApp {
                    anonymous {
                        filesRoute(ClusterShowcaseRepoParams(repo = "insights")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }
        }
    }
}
