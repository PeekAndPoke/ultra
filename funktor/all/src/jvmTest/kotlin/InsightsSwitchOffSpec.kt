package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.insights.InsightsRepository
import io.peekandpoke.funktor.insights.api.InsightsApi
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import io.peekandpoke.funktor.insights.api.InsightsRecordRef
import io.peekandpoke.funktor.inspect.introspection.api.IntrospectionApiFeature
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration.Companion.seconds

/**
 * With the app's one insights switch off, the insights API answers 404 on every host, and nothing records
 * (ULTRA-00225, ULTRA-00301).
 *
 * `insightsOffTestApp` is funktor-all's test app with `funktor.insights.enabled = false`: the same routes, the
 * `insights.*` host instrumented as before, the same depot. A record planted in that depot exists when the API is
 * asked for it, so the 404 is the switch, not a missing file. The suite's own app, switch on, answers 200 for the
 * same calls (`InsightsOneSwitchSpec`, `InsightsApiSpec`).
 */
class InsightsSwitchOffSpec : FunktorApiSpec(insightsOffTestApp) {

    private val insightsApi by service(InsightsApiFeature::class)
    private val introspectionApi by service(IntrospectionApiFeature::class)
    private val repository by service(InsightsRepository::class)

    private val wholeDepot = InsightsApiFeature.PagingParam(page = 1, epp = InsightsApi.MAX_EPP)

    private fun depotFiles(): Int = File(insightsDepotDir).walkTopDown().count { it.isFile }

    /** Every endpoint, on both hosts, answers a superuser 404. */
    private suspend fun assertAllNotFound(recordParam: InsightsApiFeature.RecordParam) {
        for (host in listOf("api.funktor.local", "insights.funktor.local")) {
            testApp(host = host).apply {
                authenticate(superUserToken) {
                    withClue("$host: list") {
                        request(insightsApi.insights.listRecords, wholeDepot) { status shouldBe HttpStatusCode.NotFound }
                    }
                    withClue("$host: the planted record") {
                        request(insightsApi.insights.getRecord, recordParam) { status shouldBe HttpStatusCode.NotFound }
                    }
                    withClue("$host: kontainer graph") {
                        request(insightsApi.insights.getKontainerGraph) { status shouldBe HttpStatusCode.NotFound }
                    }
                    withClue("$host: database graph") {
                        request(insightsApi.insights.getDatabaseGraph) { status shouldBe HttpStatusCode.NotFound }
                    }
                    withClue("$host: server info") {
                        request(insightsApi.insights.getServerInfo) { status shouldBe HttpStatusCode.NotFound }
                    }
                }
            }
        }
    }

    init {
        "a superuser gets 404 from every endpoint on every host, also for a record that exists" {
            val ref = InsightsRecordRef(bucket = "records-2026-11-10", file = "2026-11-10T12:00:00.000000000.json")
            val planted = File(insightsDepotDir, ref.toPath())
            repository.putFile(path = ref.toPath(), content = """{"planted": true}""")
            planted.exists() shouldBe true

            val recordParam = InsightsApiFeature.RecordParam(bucket = ref.bucket, file = ref.file)

            // The depot is the suite's: other specs count its records
            try {
                assertAllNotFound(recordParam)
            } finally {
                planted.delete()
            }
        }

        "a caller below superuser still gets the floor's 401, not the 404" {
            val recordParam = InsightsApiFeature.RecordParam(bucket = "records-2026-01-01", file = "a.json")

            insightsApp {
                anonymous {
                    request(insightsApi.insights.listRecords, wholeDepot) { status shouldBe HttpStatusCode.Unauthorized }
                    request(insightsApi.insights.getRecord, recordParam) { status shouldBe HttpStatusCode.Unauthorized }
                    request(insightsApi.insights.getKontainerGraph) { status shouldBe HttpStatusCode.Unauthorized }
                    request(insightsApi.insights.getDatabaseGraph) { status shouldBe HttpStatusCode.Unauthorized }
                    request(insightsApi.insights.getServerInfo) { status shouldBe HttpStatusCode.Unauthorized }
                }
                authenticate(regularUserToken) {
                    request(insightsApi.insights.listRecords, wholeDepot) { status shouldBe HttpStatusCode.Unauthorized }
                    request(insightsApi.insights.getRecord, recordParam) { status shouldBe HttpStatusCode.Unauthorized }
                    request(insightsApi.insights.getKontainerGraph) { status shouldBe HttpStatusCode.Unauthorized }
                    request(insightsApi.insights.getDatabaseGraph) { status shouldBe HttpStatusCode.Unauthorized }
                    request(insightsApi.insights.getServerInfo) { status shouldBe HttpStatusCode.Unauthorized }
                }
            }
        }

        "the instrumented host records nothing" {
            val before = depotFiles()

            insightsApp {
                authenticate(superUserToken) {
                    request(introspectionApi.introspection.getLifecycleHooks) { status shouldBe HttpStatusCode.OK }
                }
            }

            // Records are written after the response; give a record the time it takes the suite's own app to write one
            delay(1.seconds)

            depotFiles() shouldBe before
        }
    }
}
