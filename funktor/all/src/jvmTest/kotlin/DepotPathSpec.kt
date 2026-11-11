package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.cluster.FunktorClusterApiFeature
import io.peekandpoke.funktor.cluster.depot.api.DepotApi
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createTempDirectory

/**
 * A depot path from a request cannot leave its repository (ULTRA-00304). Runs on the suite's insights depot, a
 * file system repository named "insights".
 */
class DepotPathSpec : FunktorApiSpec() {

    private val api by service(FunktorClusterApiFeature::class)

    init {
        api.depot.browse { route ->

            "a path with a dot-dot segment is a bad request" {
                apiApp {
                    authenticate(superUserToken) {
                        route(DepotApi.BrowseParam(repo = "insights", path = "..")) {
                            status shouldBe HttpStatusCode.BadRequest
                        }
                        route(DepotApi.BrowseParam(repo = "insights", path = "a/../../x")) {
                            status shouldBe HttpStatusCode.BadRequest
                        }
                    }
                }
            }

            "a symlink inside the repository that points out of it is not followed" {
                val outside = createTempDirectory("depot-outside").toFile()
                File(outside, "outside-secret.txt").writeText("secret")
                val link = File(insightsDepotDir, "link-out").toPath()
                Files.deleteIfExists(link)
                Files.createSymbolicLink(link, outside.toPath())

                try {
                    apiApp {
                        authenticate(superUserToken) {
                            route(DepotApi.BrowseParam(repo = "insights", path = "link-out")) {
                                status shouldBe HttpStatusCode.BadRequest
                            }
                            route(DepotApi.BrowseParam(repo = "insights", path = "")) {
                                status shouldBe HttpStatusCode.OK
                                bodyAsText() shouldNotContain "link-out"
                            }
                        }
                    }
                } finally {
                    Files.deleteIfExists(link)
                    outside.deleteRecursively()
                }
            }
        }
    }
}
