package io.peekandpoke.funktor

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.saas.api.CreateOrgRequest
import io.peekandpoke.funktor.saas.api.OrgsApi
import io.peekandpoke.funktor.saas.api.OrgsApiFeature
import io.peekandpoke.funktor.saas.api.UpdateOrgRequest
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.funktor.saas.model.BranchModel
import io.peekandpoke.funktor.saas.model.OrgModel
import io.peekandpoke.funktor.saas.model.OrgStatus
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.ultra.vault.Stored

class OrgsApiSpec : FunktorApiSpec() {

    private val api by service(OrgsApiFeature::class)
    private val orgs by service(OrgsStorage::class)

    // The `{id}` path segment now binds a `Stored<Organisation>`. The typed-route helper renders the
    // url from the param's `_key` (via the outgoing converter), so a throwaway ref addresses any key
    // without a real row — the binding does the actual lookup server-side.
    private fun orgRef(key: String): Stored<Organisation> =
        Stored(value = Organisation(slug = key, name = key), _id = "organisation/$key", _key = key)

    init {
        api.orgs.list { route ->
            "anonymous list request is unauthorized" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "super-user list includes the ensured system-default org" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<List<OrgModel>>()!!.map { it.slug } shouldContain "system-default"
                        }
                    }
                }
            }

            "authenticated non-super-user list request is unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }
        }

        api.orgs.create { route ->
            "anonymous create request is unauthorized" {
                apiApp {
                    anonymous {
                        request(route, body = CreateOrgRequest(slug = "anon-org", name = "Anon")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "super-user can create an organisation with branches" {
                apiApp {
                    authenticate(superUserToken) {
                        request(
                            route,
                            body = CreateOrgRequest(
                                slug = "acme-create",
                                name = "Acme Hotels",
                                branches = listOf(
                                    BranchModel(id = "berlin", slug = "berlin", name = "Berlin", status = OrgStatus.Active),
                                ),
                            ),
                        ) {
                            status shouldBe HttpStatusCode.OK
                            val created = apiResponseData<OrgModel>()!!
                            created.slug shouldBe "acme-create"
                            created.name shouldBe "Acme Hotels"
                            created.branches.map { it.slug } shouldContain "berlin"
                        }
                    }
                }
            }

            "creating an organisation with a duplicate slug returns conflict" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route, body = CreateOrgRequest(slug = "dup-org", name = "First")) {
                            status shouldBe HttpStatusCode.OK
                        }
                        request(route, body = CreateOrgRequest(slug = "dup-org", name = "Second")) {
                            status shouldBe HttpStatusCode.Conflict
                        }
                    }
                }
            }

            "creating an organisation with a blank slug is rejected" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route, body = CreateOrgRequest(slug = "   ", name = "Blank")) {
                            status shouldBe HttpStatusCode.BadRequest
                        }
                    }
                }
            }
        }

        api.orgs.get { route ->
            "super-user can fetch an organisation by id" {
                val seeded = orgs.create(Organisation(slug = "acme-get", name = "Acme Get"))

                apiApp {
                    authenticate(superUserToken) {
                        route(OrgsApi.IdParam(id = seeded)) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<OrgModel>()!!.slug shouldBe "acme-get"
                        }
                    }
                }
            }

            // Envelope parity: the binding's miss must 404 exactly as the old handler's okOrNotFound did.
            "fetching an unknown organisation returns not found" {
                apiApp {
                    authenticate(superUserToken) {
                        route(OrgsApi.IdParam(id = orgRef("does-not-exist"))) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "anonymous get request is unauthorized" {
                apiApp {
                    anonymous {
                        route(OrgsApi.IdParam(id = orgRef("anything"))) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }
        }

        api.orgs.update { route ->
            "super-user can update an organisation" {
                val seeded = orgs.create(Organisation(slug = "acme-update", name = "Acme Original"))

                apiApp {
                    authenticate(superUserToken) {
                        route(
                            OrgsApi.IdParam(id = seeded),
                            UpdateOrgRequest(name = "Acme Renamed", status = OrgStatus.Suspended),
                        ) {
                            status shouldBe HttpStatusCode.OK
                            val updated = apiResponseData<OrgModel>()!!
                            updated.name shouldBe "Acme Renamed"
                            updated.status shouldBe OrgStatus.Suspended
                        }
                    }
                }
            }

            "anonymous update request is unauthorized" {
                apiApp {
                    anonymous {
                        route(
                            OrgsApi.IdParam(id = orgRef("anything")),
                            UpdateOrgRequest(name = "X", status = OrgStatus.Active),
                        ) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }
        }
    }
}
