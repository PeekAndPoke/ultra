package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.cluster.FunktorClusterApiFeature
import io.peekandpoke.funktor.cluster.depot.api.DepotApi
import io.peekandpoke.funktor.insights.InsightsRepository
import io.peekandpoke.funktor.inspect.cluster.depot.api.DepotBrowseModel
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import java.io.File

/**
 * A file's metadata reaches the depot browser through the API (ULTRA-00311). On the suite's insights depot, a file
 * system repository named "insights".
 */
class DepotMetaApiSpec : FunktorApiSpec() {

    private val api by service(FunktorClusterApiFeature::class)
    private val repository by service(InsightsRepository::class)

    init {
        afterSpec {
            File(insightsDepotDir, "meta-e2e").deleteRecursively()
            File(insightsDepotDir, ".meta/files/meta-e2e").deleteRecursively()
        }

        api.depot.browse { route ->

            "a file's metadata is served with it: what the writer gave and the subject" {
                repository.putFile(
                    subject = DepotSubjectId<Any>("customer-42"),
                    path = "meta-e2e/contract.pdf",
                    content = "pdf",
                    meta = mapOf("mime-type" to "application/pdf", "title" to "Vertrag März"),
                )

                apiApp {
                    authenticate(superUserToken) {
                        route(DepotApi.BrowseParam(repo = "insights", path = "meta-e2e/contract.pdf")) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<DepotBrowseModel>()?.meta shouldBe mapOf(
                                "mime-type" to "application/pdf",
                                "title" to "Vertrag März",
                                "depot-subject" to "customer-42",
                            )
                        }
                    }
                }
            }

            "the metadata tree is not part of what the browser lists, and cannot be browsed" {
                repository.putFile(subject = DepotSubjectId.system, path = "meta-e2e/b.txt", content = "b")

                apiApp {
                    authenticate(superUserToken) {
                        route(DepotApi.BrowseParam(repo = "insights", path = "")) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<DepotBrowseModel>()!!.children.map { it.name }.contains(".meta") shouldBe false
                            bodyAsText() shouldNotContain "\".meta\""
                        }
                        route(DepotApi.BrowseParam(repo = "insights", path = ".meta")) {
                            status shouldBe HttpStatusCode.BadRequest
                        }
                    }
                }
            }
        }
    }
}
