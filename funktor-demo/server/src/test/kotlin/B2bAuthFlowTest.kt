package io.peekandpoke.funktor.demo.server

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.testing.AppSpec

/**
 * The b2b realm is the first [io.peekandpoke.funktor.auth.OrgPolicy.Required] realm, so this locks
 * the generic 0/1/n org-selection flow end to end against the seeded b2b fixtures:
 * 0 orgs → denied, 1 org → auto-selected (Success), n orgs → OrgSelectionRequired.
 */
class B2bAuthFlowTest : AppSpec<FunktorDemoConfig>(testApp) {

    private val authApi by service(AuthApiFeature::class)

    private fun signIn(email: String) = AuthSignInRequest.EmailAndPassword(
        provider = "email-password",
        email = email,
        password = "S3cret123!",
    )

    init {
        installAllFixturesBeforeSpec()

        authApi.auth.signIn { route ->

            "a single-org b2b user is auto-selected on sign-in (Success)" {
                apiApp {
                    anonymous {
                        route(RealmParam("b2b"), body = signIn("single@b2b.test")) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>()
                        }
                    }
                }
            }

            "a multi-org b2b user must pick an org on sign-in (OrgSelectionRequired)" {
                apiApp {
                    anonymous {
                        route(RealmParam("b2b"), body = signIn("multi@b2b.test")) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.OrgSelectionRequired>()
                                .organisations shouldHaveSize 2
                        }
                    }
                }
            }

            "a b2b user with no org is denied access on sign-in" {
                apiApp {
                    anonymous {
                        route(RealmParam("b2b"), body = signIn("noorg@b2b.test")) {
                            status shouldNotBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }
    }
}
