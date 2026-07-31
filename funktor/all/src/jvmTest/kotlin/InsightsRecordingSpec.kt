package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.insights.HeaderLogging
import io.peekandpoke.funktor.insights.api.InsightsApi
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import io.peekandpoke.funktor.insights.api.InsightsRecord
import io.peekandpoke.funktor.insights.api.InsightsRecordSummary
import io.peekandpoke.funktor.insights.collectors.RequestCollector
import io.peekandpoke.funktor.inspect.introspection.api.IntrospectionApiFeature
import io.peekandpoke.ultra.model.Paged
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The insights API against a depot that actually holds records.
 *
 * `InsightsApiSpec` proves who may call these endpoints. This one proves they return something: it
 * drives an ordinary request through the app so insights records it, then reads that record back over
 * HTTP. Nothing else exercises the write path and the read path together, and the two were changed
 * independently — the writer stores the headline at the top level, the reader expects it there, and
 * only a round trip says whether they agree.
 *
 * Runs on the `insights.*` host: the same API mounted a second time with recording enabled, so the rest
 * of this module's specs do not pay for records nobody reads. See `insightsApp`.
 */
class InsightsRecordingSpec : FunktorApiSpec() {

    private val insightsApi by service(InsightsApiFeature::class)
    private val introspectionApi by service(IntrospectionApiFeature::class)

    init {
        val wholeDepot = InsightsApiFeature.PagingParam(page = 1, epp = InsightsApi.MAX_EPP)

        /**
         * Calls a plain endpoint so insights records it, then waits for that record to show up.
         *
         * The write is asynchronous — `finish` hands off to `launch(Dispatchers.IO) { delay(1) }` after
         * the response is sent — so there is no point at which the test could simply assume the file is
         * on disk. Polling the list endpoint is also the honest way to wait: it asks exactly the
         * question the assertions go on to ask.
         */
        suspend fun countRecords(): Int {
            var count = 0

            insightsApp {
                authenticate(superUserToken) {
                    request(insightsApi.insights.listRecords, wholeDepot) {
                        status shouldBe HttpStatusCode.OK
                        count = apiResponseData<Paged<InsightsRecordSummary>>()?.items?.size ?: 0
                    }
                }
            }

            return count
        }

        suspend fun recordOneRequest(): List<InsightsRecordSummary> {
            // Wait for THIS call's record, not merely for a non-empty depot. Polling `isEmpty()` returned
            // immediately once an earlier test had recorded anything, so the assertions ran against a
            // stale record and the pending write landed later — during the next test, where it read as
            // "the insights endpoints recorded themselves".
            val before = countRecords()

            insightsApp {
                authenticate(superUserToken) {
                    request(introspectionApi.introspection.getLifecycleHooks) {
                        status shouldBe HttpStatusCode.OK
                    }
                }
            }

            var found: List<InsightsRecordSummary> = emptyList()

            withTimeoutOrNull(10.seconds) {
                while (found.size <= before) {
                    insightsApp {
                        authenticate(superUserToken) {
                            request(insightsApi.insights.listRecords, wholeDepot) {
                                status shouldBe HttpStatusCode.OK
                                found = apiResponseData<Paged<InsightsRecordSummary>>()?.items ?: emptyList()
                            }
                        }
                    }

                    if (found.size <= before) delay(25.milliseconds)
                }
            }

            withClue("no NEW insights record appeared within 10s (had $before) — was it recorded?") {
                (found.size > before) shouldBe true
            }

            return found
        }

        "a recorded request is listed with its headline populated" {
            val summary = recordOneRequest().first()

            // These columns are what a list view is FOR, and they are read straight off the top level
            // of the record rather than dug out of the request slice. The old 200-path assertion was
            // `shouldNotBe null` against an empty depot; nothing here can pass on an empty list.
            summary.method shouldBe "GET"
            summary.path.shouldNotBeNull()
            summary.status shouldBe HttpStatusCode.OK.value
            summary.durationMs.shouldNotBeNull()
        }

        "a recorded request loads in full, and its credential header is redacted" {
            val summary = recordOneRequest().first()

            insightsApp {
                authenticate(superUserToken) {
                    val param = InsightsApiFeature.RecordParam(
                        bucket = summary.ref.bucket,
                        file = summary.ref.file,
                    )

                    request(insightsApi.insights.getRecord, param) {
                        status shouldBe HttpStatusCode.OK

                        val body = bodyAsText()

                        // The point of the round trip: a real record, serialised over HTTP with its
                        // open-envelope slices intact. `getRecord`'s 200 branch had never run before.
                        val record = apiResponseData<InsightsRecord>().shouldNotBeNull()
                        record.collectors.map { it.key } shouldContain RequestCollector.KEY

                        val request = record.collectors.first { it.key == RequestCollector.KEY }
                        val headers = request.data.jsonObject["headers"].shouldNotBeNull().jsonObject

                        // Header names keep whatever casing the client sent, so match case-insensitively
                        // rather than pinning ktor's behaviour.
                        val authorization = headers.entries
                            .first { it.key.equals("authorization", ignoreCase = true) }
                            .value.jsonArray.map { it.jsonPrimitive.content }

                        // End to end, not merely in `HeaderLoggingSpec`: the policy is consulted by the
                        // collector, survives the write, and is what a superuser actually receives.
                        authorization shouldBe listOf(HeaderLogging.REDACTED)

                        // and no fragment of the token leaked through any other slice
                        body shouldNotContain superUserToken
                    }
                }
            }
        }

        "the list endpoint returns a Paged envelope over the wire, with a usable total" {
            recordOneRequest()

            insightsApp {
                authenticate(superUserToken) {
                    request(insightsApi.insights.listRecords, InsightsApiFeature.PagingParam(page = 1, epp = 1)) {
                        status shouldBe HttpStatusCode.OK

                        val paged = apiResponseData<Paged<InsightsRecordSummary>>().shouldNotBeNull()

                        // epp=1, so a bare List could not tell a table whether more exist. This is what
                        // Paged buys, and it must survive Slumber to the client rather than only exist
                        // server-side.
                        paged.items.size shouldBe 1
                        paged.page shouldBe 1
                        paged.epp shouldBe 1
                        (paged.fullItemCount!! >= 1L) shouldBe true
                        paged.fullPageCount shouldBe paged.fullItemCount
                    }
                }
            }
        }

        "an oversized epp is clamped by the handler, not honoured" {
            recordOneRequest()

            insightsApp {
                authenticate(superUserToken) {
                    // Without `coerceIn(1, MAX_EPP)` this asks the loader to open 99 999 records — mean
                    // size 236 KB — on a handler thread. Unit tests cannot reach the handler body, so
                    // this is the only place the clamp is actually exercised.
                    request(insightsApi.insights.listRecords, InsightsApiFeature.PagingParam(page = 1, epp = 99_999)) {
                        status shouldBe HttpStatusCode.OK
                        val rows = apiResponseData<Paged<InsightsRecordSummary>>().shouldNotBeNull().items
                        (rows.size <= InsightsApi.MAX_EPP) shouldBe true
                    }

                    // epp=0 must not mean "an empty page forever"
                    request(insightsApi.insights.listRecords, InsightsApiFeature.PagingParam(page = 1, epp = 0)) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<Paged<InsightsRecordSummary>>().shouldNotBeNull().items.size shouldBe 1
                    }

                    // and a nonsense page is an empty page, not page one
                    request(insightsApi.insights.listRecords, InsightsApiFeature.PagingParam(page = Int.MAX_VALUE, epp = 20)) {
                        status shouldBe HttpStatusCode.OK
                        apiResponseData<Paged<InsightsRecordSummary>>().shouldNotBeNull().items shouldBe emptyList()
                    }
                }
            }
        }

        "a recorded record does not contain the application's signing key" {
            // The proof that the redaction is REACHED, not merely correct. ConfigRedaction has its own
            // unit spec, but removing the call from AppConfigCollector left every suite green — the same
            // "policy never invoked" trap that made the first CollectorRedactionSpec vacuous.
            //
            // The key is read from the running app's own config rather than pasted here, so the test
            // cannot rot into asserting against a string nothing uses.
            val signingKey = kontainer.get(FunktorAllTestConfig::class)
                .funktor.auth.jwt?.keys?.firstOrNull()?.secret?.value
                ?: error("the test app has no JWT signing key configured — this test would be vacuous")

            val summary = recordOneRequest().first()

            insightsApp {
                authenticate(superUserToken) {
                    val param = InsightsApiFeature.RecordParam(summary.ref.bucket, summary.ref.file)

                    request(insightsApi.insights.getRecord, param) {
                        status shouldBe HttpStatusCode.OK

                        // the whole record, app-config slice included, as a superuser receives it
                        bodyAsText() shouldNotContain signingKey
                    }
                }
            }
        }

        "the insights endpoints do not record themselves" {
            // `.noInsights()` on both routes. Without it a superuser opening the panel fills the depot
            // with records of themselves reading it — each one carrying their own Authorization header,
            // and each one making the next page of the list longer.
            recordOneRequest()

            val before = countRecords()

            // give an errant record the same chance to land that a real one gets
            delay(500.milliseconds)

            val after = countRecords()

            withClue("a saturated page cannot show growth, so the comparison below would be vacuous") {
                (before < wholeDepot.epp) shouldBe true
            }

            withClue("listing records must not itself produce a record") {
                after shouldBe before
            }
        }
    }
}
