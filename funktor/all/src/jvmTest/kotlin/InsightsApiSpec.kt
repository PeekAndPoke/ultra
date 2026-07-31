package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import io.peekandpoke.funktor.insights.api.InsightsRecordSummary

/**
 * End-to-end proof that insights data is reachable only by a superuser.
 *
 * The route-level spec in `funktor/insights` asserts the same decisions through `estimateAccess`; this
 * one puts a real HTTP request through the real auth chain, which is the only thing that proves the
 * chain is actually wired to the routes rather than merely declared on them.
 */
class InsightsApiSpec : FunktorApiSpec() {

    private val api by service(InsightsApiFeature::class)

    init {
        api.insights.listRecords { route ->

            val firstPage = InsightsApiFeature.PagingParam()

            "anonymous list request is unauthorized" {
                apiApp {
                    anonymous {
                        route(firstPage) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "authenticated non-super-user list request is unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        route(firstPage) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "super-user may list records" {
                apiApp {
                    authenticate(superUserToken) {
                        route(firstPage) {
                            status shouldBe HttpStatusCode.OK
                            // The depot may legitimately be empty in a fresh test app; what matters is
                            // that the call is answered rather than refused.
                            apiResponseData<List<InsightsRecordSummary>>() shouldNotBe null
                        }
                    }
                }
            }

            "the response carries no credential material" {
                apiApp {
                    authenticate(superUserToken) {
                        route(firstPage) {
                            status shouldBe HttpStatusCode.OK
                            // A summary must never echo the token that fetched it, nor any Set-Cookie.
                            bodyAsText() shouldNotContain superUserToken
                        }
                    }
                }
            }
        }

        api.insights.getRecord { route ->

            val someRecord = InsightsApiFeature.RecordParam(
                bucket = "records-2026-01-01",
                file = "does-not-exist.json",
            )

            "anonymous record request is unauthorized" {
                apiApp {
                    anonymous {
                        route(someRecord) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "authenticated non-super-user record request is unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        route(someRecord) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "a missing record is a not-found for a super-user, not an error" {
                apiApp {
                    authenticate(superUserToken) {
                        route(someRecord) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "an unauthorized caller learns nothing about which records exist" {
                // Two different paths must be refused identically, so the status cannot be used to
                // probe for which insight files are present.
                apiApp {
                    anonymous {
                        route(InsightsApiFeature.RecordParam("records-2026-01-01", "a.json")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                        route(InsightsApiFeature.RecordParam("records-2026-01-02", "b.json")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }
        }
    }
}
