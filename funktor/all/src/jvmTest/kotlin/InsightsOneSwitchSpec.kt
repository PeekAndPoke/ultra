package io.peekandpoke.funktor

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.insights.api.InsightsApi
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import io.peekandpoke.funktor.insights.api.InsightsRecordRef
import io.peekandpoke.funktor.insights.api.InsightsRecordSummary
import io.peekandpoke.funktor.inspect.introspection.api.IntrospectionApiFeature
import io.peekandpoke.ultra.model.Paged
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * One insights switch for the whole app (ULTRA-00301). The suite's app has it on: the `insights.*` host
 * instruments its routes and records; the `api.*` host does not instrument, records nothing, and still serves the
 * insights API. A GUI on another host than the recording one reads the same records.
 */
class InsightsOneSwitchSpec : FunktorApiSpec() {

    private val insightsApi by service(InsightsApiFeature::class)
    private val introspectionApi by service(IntrospectionApiFeature::class)

    private val wholeDepot = InsightsApiFeature.PagingParam(page = 1, epp = InsightsApi.MAX_EPP)

    private fun depotFiles(): Int = File(insightsDepotDir).walkTopDown().count { it.isFile }

    /** The records the API lists on [host]. */
    private suspend fun listOn(host: String): List<InsightsRecordSummary> {
        var items: List<InsightsRecordSummary> = emptyList()

        testApp(host = host).apply {
            authenticate(superUserToken) {
                request(insightsApi.insights.listRecords, wholeDepot) {
                    status shouldBe HttpStatusCode.OK
                    items = apiResponseData<Paged<InsightsRecordSummary>>()?.items ?: emptyList()
                }
            }
        }

        return items
    }

    init {
        "a record written through the insights host is served by the API on the api host" {
            val before = listOn("api.funktor.local").map { it.ref }.toSet()

            insightsApp {
                authenticate(superUserToken) {
                    request(introspectionApi.introspection.getLifecycleHooks) { status shouldBe HttpStatusCode.OK }
                }
            }

            var recorded: InsightsRecordRef? = null

            withTimeoutOrNull(10.seconds) {
                while (recorded == null) {
                    recorded = listOn("api.funktor.local").map { it.ref }.firstOrNull { it !in before }
                    if (recorded == null) delay(25.milliseconds)
                }
            }

            val ref = recorded.shouldNotBeNull()

            apiApp {
                authenticate(superUserToken) {
                    request(insightsApi.insights.getRecord, InsightsApiFeature.RecordParam(ref.bucket, ref.file)) {
                        status shouldBe HttpStatusCode.OK
                    }
                    request(insightsApi.insights.getKontainerGraph) { status shouldBe HttpStatusCode.OK }
                    request(insightsApi.insights.getDatabaseGraph) { status shouldBe HttpStatusCode.OK }
                    request(insightsApi.insights.getServerInfo) { status shouldBe HttpStatusCode.OK }
                }
            }

            listOn("insights.funktor.local").map { it.ref } shouldContain ref
        }

        "the api host, not instrumented, records nothing" {
            val before = depotFiles()

            apiApp {
                authenticate(superUserToken) {
                    request(introspectionApi.introspection.getLifecycleHooks) { status shouldBe HttpStatusCode.OK }
                }
            }

            // Records are written after the response; give one the time the insights host takes to write it
            delay(1.seconds)

            depotFiles() shouldBe before
        }
    }
}
