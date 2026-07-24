package io.peekandpoke.funktor.demo.server

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.auth.model.AuthSelectOrgRequest
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.saas.model.OrgStatus
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.funktor.testing.AppSpec

/**
 * The b2b realm is the first [io.peekandpoke.funktor.auth.OrgPolicy.Required] realm, so this locks
 * the generic 0/1/n org-selection flow end to end against the seeded b2b fixtures:
 * 0 orgs → denied, 1 org → auto-selected (Success), n orgs → OrgSelectionRequired → select-org.
 *
 * Also locks the security branches of the selection step (single-use token, membership binding)
 * and the org-lifecycle rule (Suspended/Archived orgs are not sign-in-able).
 */
class B2bAuthFlowTest : AppSpec<FunktorDemoConfig>(testApp) {

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
            // NOTE: route blocks register FreeSpec containers at spec level — they cannot nest.
            // The second route is referenced directly; the request-invoke DSL works on any route.
            val selectOrgRoute = authApi.auth.selectOrg

            "a single-org b2b user is auto-selected on sign-in (Success)" {
                apiApp {
                    anonymous {
                        signInRoute(RealmParam(RealmId("b2b")), body = signIn("single@b2b.test")) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>()
                                .org?.slug shouldBe "acme"
                        }
                    }
                }
            }

            "a multi-org b2b user must pick an org on sign-in (OrgSelectionRequired)" {
                apiApp {
                    anonymous {
                        signInRoute(RealmParam(RealmId("b2b")), body = signIn("multi@b2b.test")) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.OrgSelectionRequired>()
                                .organisations.map { it.slug }.toSet() shouldBe setOf("acme", "globex")
                        }
                    }
                }
            }

            "a b2b user with no org is denied access on sign-in (Forbidden)" {
                apiApp {
                    anonymous {
                        signInRoute(RealmParam(RealmId("b2b")), body = signIn("noorg@b2b.test")) {
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }

            "org selection completes with the token, which is single-use" {
                apiApp {
                    anonymous {
                        var selectionToken = ""
                        var acmeId = ""

                        signInRoute(RealmParam(RealmId("b2b")), body = signIn("multi@b2b.test")) {
                            val required = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.OrgSelectionRequired>()

                            selectionToken = required.selectionToken
                            acmeId = required.organisations.first { it.slug == "acme" }.id
                        }

                        val select = AuthSelectOrgRequest(selectionToken = selectionToken, orgId = acmeId)

                        selectOrgRoute(RealmParam(RealmId("b2b")), body = select) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.Success>()
                                .org?.slug shouldBe "acme"
                        }

                        // Replay of the consumed token must fail (single-use).
                        selectOrgRoute(RealmParam(RealmId("b2b")), body = select) {
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }

            "org selection rejects an org the user has no membership for" {
                val initech = orgs.ensureBySlug("initech", "Initech")

                apiApp {
                    anonymous {
                        var selectionToken = ""

                        signInRoute(RealmParam(RealmId("b2b")), body = signIn("multi@b2b.test")) {
                            selectionToken = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.OrgSelectionRequired>()
                                .selectionToken
                        }

                        selectOrgRoute(
                            RealmParam(RealmId("b2b")),
                            body = AuthSelectOrgRequest(selectionToken = selectionToken, orgId = initech._key),
                        ) {
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }

            "org selection rejects an unknown org id" {
                apiApp {
                    anonymous {
                        var selectionToken = ""

                        signInRoute(RealmParam(RealmId("b2b")), body = signIn("multi@b2b.test")) {
                            selectionToken = apiResponseData<AuthSignInResponse>()
                                .shouldBeInstanceOf<AuthSignInResponse.OrgSelectionRequired>()
                                .selectionToken
                        }

                        selectOrgRoute(
                            RealmParam(RealmId("b2b")),
                            body = AuthSelectOrgRequest(selectionToken = selectionToken, orgId = "does-not-exist"),
                        ) {
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }

            "a suspended org is not sign-in-able; remaining active orgs still are" {
                try {
                    setOrgStatus("acme", OrgStatus.Suspended)

                    apiApp {
                        anonymous {
                            // The single-org user's only org is suspended → no access at all.
                            signInRoute(RealmParam(RealmId("b2b")), body = signIn("single@b2b.test")) {
                                status shouldBe HttpStatusCode.Forbidden
                            }

                            // The multi-org user has exactly one ACTIVE org left → auto-select.
                            signInRoute(RealmParam(RealmId("b2b")), body = signIn("multi@b2b.test")) {
                                status shouldBe HttpStatusCode.OK
                                apiResponseData<AuthSignInResponse>()
                                    .shouldBeInstanceOf<AuthSignInResponse.Success>()
                                    .org?.slug shouldBe "globex"
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
