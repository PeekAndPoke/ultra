package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.insights.api.InsightsApi
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createTempDirectory

/**
 * The record endpoint reads records only, `records-<date>/<name>.json`, and answers 404 for anything else in the
 * insights depot, never 500 (ULTRA-00304).
 */
class InsightsRecordPathSpec : FunktorApiSpec() {

    private val api by service(InsightsApiFeature::class)

    private fun put(path: String, content: String = "{}") {
        File(insightsDepotDir, path).apply { parentFile.mkdirs() }.writeText(content)
    }

    init {
        // Other specs count the records in the shared depot: what this one plants goes again
        afterSpec {
            listOf("records-2026-01-02", "records-2026-01-03", "records-2026-01-07", "elsewhere", "etc")
                .forEach { File(insightsDepotDir, it).deleteRecursively() }
        }

        api.insights.getRecord { route ->

            "a record is found" {
                put("records-2026-01-02/2026-01-02T10:00:00.json")

                insightsApp {
                    authenticate(superUserToken) {
                        route(InsightsApiFeature.RecordParam("records-2026-01-02", "2026-01-02T10:00:00.json")) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }

            "a JSON file outside a records folder is not a record" {
                put("elsewhere/notes.json")

                insightsApp {
                    authenticate(superUserToken) {
                        route(InsightsApiFeature.RecordParam("elsewhere", "notes.json")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "a file in a records folder that is not .json is not a record" {
                put("records-2026-01-03/notes.txt")

                insightsApp {
                    authenticate(superUserToken) {
                        route(InsightsApiFeature.RecordParam("records-2026-01-03", "notes.txt")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "a file that exists but is no record is a not-found: a backslash name, an absolute folder, a NUL" {
                // Real files, so only the record rule can make these a 404
                put("records-2026-01-07/a\\b.json")
                put("etc/passwd.json")

                insightsApp {
                    authenticate(superUserToken) {
                        route(InsightsApiFeature.RecordParam("records-2026-01-07", "a\\b.json")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                        route(InsightsApiFeature.RecordParam("/etc", "passwd.json")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                        route(InsightsApiFeature.RecordParam("records-2026-01-07", "a${Char(0)}.json")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }

            "two dots inside a name are allowed: only a dot-dot segment is refused" {
                put("records-2026-01-07/x..json")

                insightsApp {
                    authenticate(superUserToken) {
                        route(InsightsApiFeature.RecordParam("records-2026-01-07", "x..json")) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.insights.getRecord { route ->

            "a symlink in a records folder that points out of the depot is a not-found, not an error" {
                val outside = createTempDirectory("insights-outside").toFile()
                File(outside, "x.json").writeText("{}")
                val link = File(insightsDepotDir, "records-2026-01-05/link.json").apply { parentFile.mkdirs() }.toPath()

                try {
                    Files.deleteIfExists(link)
                    Files.createSymbolicLink(link, File(outside, "x.json").toPath())

                    insightsApp {
                        authenticate(superUserToken) {
                            route(InsightsApiFeature.RecordParam("records-2026-01-05", "link.json")) {
                                status shouldBe HttpStatusCode.NotFound
                            }
                        }
                    }
                } finally {
                    Files.deleteIfExists(link)
                    outside.deleteRecursively()
                }
            }
        }

        api.insights.listRecords { route ->

            "the list shows records only, and a refused entry does not fail it" {
                // A far-future day: newest first, so these take the first slots of page 1 however full the depot is
                val day = File(insightsDepotDir, "records-2999-12-31")
                put("records-2999-12-31/2999-12-31T10:00:00.json", """{"method":"GET","uri":"/listed"}""")
                put("records-2999-12-31/notes.txt")
                val outside = createTempDirectory("insights-outside").toFile()
                File(outside, "y.json").writeText("{}")
                val link = File(day, "2999-12-31T11:00:00.json").toPath()

                try {
                    Files.deleteIfExists(link)
                    Files.createSymbolicLink(link, File(outside, "y.json").toPath())

                    insightsApp {
                        authenticate(superUserToken) {
                            route(InsightsApiFeature.PagingParam(page = 1, epp = InsightsApi.MAX_EPP)) {
                                status shouldBe HttpStatusCode.OK
                                bodyAsText() shouldContain "/listed"
                                bodyAsText() shouldNotContain "notes.txt"
                            }
                        }
                    }
                } finally {
                    // Other specs count the records
                    Files.deleteIfExists(link)
                    day.deleteRecursively()
                    outside.deleteRecursively()
                }
            }
        }
    }
}
