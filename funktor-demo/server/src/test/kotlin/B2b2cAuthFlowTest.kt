package io.peekandpoke.funktor.demo.server

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.saas.model.OrgStatus
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.funktor.testing.AppSpec

/**
 * Locks the b2b2c (end-user) realm's wiring of the generic 0/1/n org-selection flow against its own
 * seeded fixtures. The deep security matrix of the selection step (single-use token, membership
 * binding, non-member/unknown org rejection) is locked once in [B2bAuthFlowTest] — both realms
 * delegate to the same shared org hooks (`saas_org_hooks.kt`), so this suite covers the
 * realm-specific wiring: user store, realm id, fixtures, and the lifecycle rule.
 */
class B2b2cAuthFlowTest : AppSpec<FunktorDemoConfig>(testApp) {

    private val authApi by service(AuthApiFeature::class)
    private val orgs by service(OrgsStorage::class)

    private fun signIn(email: String) = AuthSignInRequest.EmailAndPassword(
        provider = "email-password",
        email = email,
        password = "S3cret123!",
    )

    private suspend fun setOrgStatus(slug: String, status: OrgStatus) {
        val stored = orgs.findBySlug(slug) ?: error("org '$slug' is not seeded")
        orgs.save(stored.modify { it.copy(status = status) })
    }

    init {
        installAllFixturesBeforeSpec()

        authApi.auth.signIn { signInRoute ->

            "a single-org b2b2c end-user is auto-selected on sign-in (Success)" {
                apiApp {
                    anonymous {
                        signInRoute(RealmParam(RealmId("b2b2c")), body = signIn("single@b2b2c.test")) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>()
                                .org?.slug shouldBe "acme"
                        }
                    }
                }
            }

            "a multi-org b2b2c end-user must pick an org on sign-in (OrgSelectionRequired)" {
                apiApp {
                    anonymous {
                        signInRoute(RealmParam(RealmId("b2b2c")), body = signIn("multi@b2b2c.test")) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.OrgSelectionRequired>()
                                .organisations.map { it.slug }.toSet() shouldBe setOf("acme", "globex")
                        }
                    }
                }
            }

            "a b2b2c end-user with no org is denied access on sign-in (Forbidden)" {
                apiApp {
                    anonymous {
                        signInRoute(RealmParam(RealmId("b2b2c")), body = signIn("noorg@b2b2c.test")) {
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }

            "a b2b2c user cannot sign in through the b2b realm (separate user stores)" {
                apiApp {
                    anonymous {
                        signInRoute(RealmParam(RealmId("b2b")), body = signIn("single@b2b2c.test")) {
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }

            "a suspended org is not sign-in-able for b2b2c end-users either" {
                try {
                    setOrgStatus("acme", OrgStatus.Suspended)

                    apiApp {
                        anonymous {
                            signInRoute(RealmParam(RealmId("b2b2c")), body = signIn("single@b2b2c.test")) {
                                status shouldBe HttpStatusCode.Forbidden
                            }
                        }
                    }
                } finally {
                    setOrgStatus("acme", OrgStatus.Active)
                }
            }
        }
    }
}
