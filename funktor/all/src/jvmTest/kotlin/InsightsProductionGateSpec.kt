package io.peekandpoke.funktor

import io.kotest.assertions.withClue
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The insights API answers 404 wherever insights is not active (ULTRA-00225).
 *
 * The `api.*` host mounts the same API with insights off; the `insights.*` host with it on. Both share one
 * blueprint and one depot, so a record written on the insights host exists when the api host is asked for it: a
 * 404 there is the gate, not a missing file. The insights host answering 200 for the same calls is the control.
 */
class InsightsProductionGateSpec : FunktorApiSpec() {

    private val insightsApi by service(InsightsApiFeature::class)
    private val introspectionApi by service(IntrospectionApiFeature::class)

    private val wholeDepot = InsightsApiFeature.PagingParam(page = 1, epp = InsightsApi.MAX_EPP)

    /** Records one request on the insights host and returns the ref of a record that is now in the depot. */
    private suspend fun aRecordedRef(): InsightsRecordRef {
        insightsApp {
            authenticate(superUserToken) {
                request(introspectionApi.introspection.getLifecycleHooks) {
                    status shouldBe HttpStatusCode.OK
                }
            }
        }

        var found: List<InsightsRecordSummary> = emptyList()

        withTimeoutOrNull(10.seconds) {
            while (found.isEmpty()) {
                insightsApp {
                    authenticate(superUserToken) {
                        request(insightsApi.insights.listRecords, wholeDepot) {
                            status shouldBe HttpStatusCode.OK
                            found = apiResponseData<Paged<InsightsRecordSummary>>()?.items ?: emptyList()
                        }
                    }
                }

                if (found.isEmpty()) delay(25.milliseconds)
            }
        }

        return found.firstOrNull().shouldNotBeNull().ref
    }

    init {
        "where insights is off, a superuser gets 404 from every endpoint, also for a record that exists" {
            val ref = aRecordedRef()
            val recordParam = InsightsApiFeature.RecordParam(bucket = ref.bucket, file = ref.file)

            withClue("control: the insights host serves all five") {
                insightsApp {
                    authenticate(superUserToken) {
                        request(insightsApi.insights.listRecords, wholeDepot) { status shouldBe HttpStatusCode.OK }
                        request(insightsApi.insights.getRecord, recordParam) { status shouldBe HttpStatusCode.OK }
                        request(insightsApi.insights.getKontainerGraph) { status shouldBe HttpStatusCode.OK }
                        request(insightsApi.insights.getDatabaseGraph) { status shouldBe HttpStatusCode.OK }
                        request(insightsApi.insights.getServerInfo) { status shouldBe HttpStatusCode.OK }
                    }
                }
            }

            apiApp {
                authenticate(superUserToken) {
                    withClue("list") {
                        request(insightsApi.insights.listRecords, wholeDepot) { status shouldBe HttpStatusCode.NotFound }
                    }
                    withClue("the recorded record") {
                        request(insightsApi.insights.getRecord, recordParam) { status shouldBe HttpStatusCode.NotFound }
                    }
                    withClue("kontainer graph") {
                        request(insightsApi.insights.getKontainerGraph) { status shouldBe HttpStatusCode.NotFound }
                    }
                    withClue("database graph") {
                        request(insightsApi.insights.getDatabaseGraph) { status shouldBe HttpStatusCode.NotFound }
                    }
                    withClue("server info") {
                        request(insightsApi.insights.getServerInfo) { status shouldBe HttpStatusCode.NotFound }
                    }
                }
            }
        }

        "where insights is off, a caller below superuser still gets the floor's 401, not the 404" {
            val recordParam = InsightsApiFeature.RecordParam(bucket = "records-2026-01-01", file = "a.json")

            apiApp {
                anonymous {
                    request(insightsApi.insights.listRecords, wholeDepot) { status shouldBe HttpStatusCode.Unauthorized }
                    request(insightsApi.insights.getRecord, recordParam) { status shouldBe HttpStatusCode.Unauthorized }
                    request(insightsApi.insights.getKontainerGraph) { status shouldBe HttpStatusCode.Unauthorized }
                    request(insightsApi.insights.getDatabaseGraph) { status shouldBe HttpStatusCode.Unauthorized }
                    request(insightsApi.insights.getServerInfo) { status shouldBe HttpStatusCode.Unauthorized }
                }
            }
        }
    }
}
