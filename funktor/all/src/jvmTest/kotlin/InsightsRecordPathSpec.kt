package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import java.io.File

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

            "a name with dot-dot or a backslash is a not-found, not an error" {
                insightsApp {
                    authenticate(superUserToken) {
                        route(InsightsApiFeature.RecordParam("records-2026-01-01", "x..json")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                        route(InsightsApiFeature.RecordParam("records-2026-01-01", "a\\b.json")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                        route(InsightsApiFeature.RecordParam("..", "x.json")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }
    }
}
