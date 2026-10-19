package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.insights.InsightsRepository
import io.peekandpoke.funktor.insights.api.InsightsApi
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import io.peekandpoke.funktor.insights.api.InsightsKontainerGraph
import io.peekandpoke.funktor.insights.api.InsightsRecordRef
import io.peekandpoke.funktor.insights.api.InsightsRecordSummary
import io.peekandpoke.funktor.insights.collectors.KontainerCollector
import io.peekandpoke.funktor.inspect.introspection.api.IntrospectionApiFeature
import io.peekandpoke.ultra.model.Paged
import io.peekandpoke.ultra.security.user.UserProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The `kontainer` slice of a REAL record — recorded the way `InsightsRecordingSpec` records one — as it
 * sits on disk and as the API serves it: its size, its wire shape, and its join against the kontainer graph.
 *
 * Also proves an OLD record, which carries the fat shape, is still served as written.
 */
class InsightsKontainerSliceSpec : FunktorApiSpec() {

    private val insightsApi by service(InsightsApiFeature::class)
    private val introspectionApi by service(IntrospectionApiFeature::class)
    private val repository by service(InsightsRepository::class)

    /** The writer's own rendering (`InsightsFull`), so a re-rendered record measures like the file. */
    private val prettyJson = Json { prettyPrint = true }

    private var recorded: InsightsRecordRef? = null

    private suspend fun listPage(page: Int, epp: Int): Paged<InsightsRecordSummary> {
        var paged: Paged<InsightsRecordSummary>? = null

        insightsApp {
            authenticate(superUserToken) {
                request(insightsApi.insights.listRecords, InsightsApiFeature.PagingParam(page = page, epp = epp)) {
                    status shouldBe HttpStatusCode.OK
                    paged = apiResponseData<Paged<InsightsRecordSummary>>()
                }
            }
        }

        return paged.shouldNotBeNull()
    }

    /** Every record the list shows, page by page: the depot outgrows one page once a JVM has run enough specs. */
    private suspend fun allRecords(): List<InsightsRecordSummary> {
        val pages = listPage(page = 1, epp = InsightsApi.MAX_EPP).fullPageCount.shouldNotBeNull()

        return (1..pages.toInt()).flatMap { listPage(page = it, epp = InsightsApi.MAX_EPP).items }
    }

    /** Records the request `InsightsRecordingSpec` records, once per spec, and returns its ref. */
    private suspend fun recordedRef(): InsightsRecordRef = recorded ?: run {
        // The FULL count, not the length of one page, which stops growing at MAX_EPP
        val before = listPage(page = 1, epp = 1).fullItemCount.shouldNotBeNull()

        insightsApp {
            authenticate(superUserToken) {
                request(introspectionApi.introspection.getLifecycleHooks) {
                    status shouldBe HttpStatusCode.OK
                }
            }
        }

        var newest: InsightsRecordSummary? = null

        // Done once the count has grown AND the newest file parses: a half-written one lists as an empty page
        withTimeoutOrNull(10.seconds) {
            while (newest == null) {
                val page = listPage(page = 1, epp = 1)
                newest = page.items.firstOrNull()?.takeIf { (page.fullItemCount ?: 0) > before }
                if (newest == null) delay(25.milliseconds)
            }
        }

        val found = withClue("no NEW insights record appeared within 10s (had $before) — was the slice slumberable?") {
            newest.shouldNotBeNull()
        }

        found.ref.also { recorded = it }
    }

    /** The record as the API serves it, raw. */
    private suspend fun served(ref: InsightsRecordRef): JsonObject {
        var body = ""

        insightsApp {
            authenticate(superUserToken) {
                request(insightsApi.insights.getRecord, InsightsApiFeature.RecordParam(ref.bucket, ref.file)) {
                    status shouldBe HttpStatusCode.OK
                    body = bodyAsText()
                }
            }
        }

        insightsWireSample("record-response-${ref.file}", body)

        return Json.parseToJsonElement(body).jsonObject["data"].shouldNotBeNull().jsonObject
    }

    private suspend fun kontainerGraph(): InsightsKontainerGraph {
        var body = ""

        // Deliberately the OTHER host: the graph belongs to the blueprint, not to a mount
        apiApp {
            authenticate(superUserToken) {
                request(insightsApi.insights.getKontainerGraph) {
                    status shouldBe HttpStatusCode.OK
                    body = bodyAsText()
                }
            }
        }

        return Json.decodeFromJsonElement(
            InsightsKontainerGraph.serializer(),
            Json.parseToJsonElement(body).jsonObject["data"].shouldNotBeNull(),
        )
    }

    private fun JsonObject.kontainerSlice(): JsonObject = this["collectors"].shouldNotBeNull().jsonArray
        .map { it.jsonObject }
        .single { it["key"]!!.jsonPrimitive.content == KontainerCollector.KEY }["data"].shouldNotBeNull().jsonObject

    private fun JsonElement.keys(): Set<String> = jsonObject.keys

    init {
        "measure: the record and its kontainer slice, as written to disk" {
            val ref = recordedRef()
            val file = File(insightsDepotDir, ref.toPath())
            val root = Json.parseToJsonElement(file.readText()).jsonObject

            insightsWireSample("record-${ref.file}", file.readText())

            val withoutSlice = JsonObject(
                root + ("collectors" to JsonArray(
                    root["collectors"]!!.jsonArray.filter {
                        it.jsonObject["key"]!!.jsonPrimitive.content != KontainerCollector.KEY
                    }
                ))
            )

            // BYTES throughout, as the file is measured: a char count drifts on any non-ASCII path or name
            fun JsonElement.prettyBytes() = prettyJson.encodeToString(JsonElement.serializer(), this).toByteArray().size

            val rendered = root.prettyBytes()
            val share = rendered - withoutSlice.prettyBytes()
            val slice = root.kontainerSlice()
            val services = slice["services"].shouldNotBeNull().jsonArray
            val instances = services.sumOf { it.jsonObject["instances"]!!.jsonArray.size }
            val sliceCompact = slice.toString().toByteArray().size

            println(
                "[insights-size] record=${file.length()} rendered=$rendered kontainerSliceShare=$share " +
                        "(${"%.1f".format(100.0 * share / rendered)}%) " +
                        "sliceCompact=$sliceCompact services=${services.size} instances=$instances"
            )

            withClue("the file is the writer's pretty JSON, so re-rendering it measures the same bytes") {
                rendered.toLong() shouldBe file.length()
            }

            // Measured on this very record (2026-10-19): the fat slice was 115 737 compact bytes for 77 instances,
            // ~1 500 per instance, because it carried all 178 definitions; the slim one is 19 461 for 78, ~250
            // per instance. Per instance, so the guard grows with the app and ignores the other slices.
            withClue("the definitions are back in the record: $sliceCompact compact bytes for $instances instances") {
                (sliceCompact < 600 * instances) shouldBe true
            }
        }

        "the slim slice on the wire: {numOld, numTotal, graphFingerprint, services}" {
            val slice = served(recordedRef()).kontainerSlice()

            slice.keys shouldContainExactlyInAnyOrder listOf("numOld", "numTotal", "graphFingerprint", "services")
            (slice["numOld"].shouldNotBeNull().jsonPrimitive.long >= 0) shouldBe true
            (slice["numTotal"].shouldNotBeNull().jsonPrimitive.long >= 0) shouldBe true

            val services = slice["services"].shouldNotBeNull().jsonArray
            services.shouldNotBeEmpty()

            val now = System.currentTimeMillis()

            services.forEach { raw ->
                withClue(raw.toString().take(200)) {
                    raw.keys() shouldContainExactlyInAnyOrder listOf("cls", "type", "instances")
                    // a flat string: the fat shape wrapped every class name as {"fqn": ...}
                    (raw.jsonObject["cls"] as? JsonPrimitive)?.isString shouldBe true
                    (raw.jsonObject["type"] as? JsonPrimitive)?.isString shouldBe true

                    val instances = raw.jsonObject["instances"].shouldNotBeNull().jsonArray
                    instances.shouldNotBeEmpty()

                    instances.forEach { instance ->
                        instance.keys() shouldContainExactlyInAnyOrder listOf("cls", "createdAt")
                        (instance.jsonObject["cls"] as? JsonPrimitive)?.isString shouldBe true

                        // an object, never a bare number: a reader took bare numbers for epoch SECONDS (D2)
                        val createdAt = instance.jsonObject["createdAt"].shouldNotBeNull()
                        (createdAt as? JsonObject)?.keys shouldBe setOf("ts", "timezone", "human")

                        // epoch MILLIS: a seconds value would be ~1000x too small to land in this window
                        val ts = createdAt.jsonObject["ts"].shouldNotBeNull().jsonPrimitive.long
                        (ts > now - 2.hours.inWholeMilliseconds && ts < now + 1.minutes.inWholeMilliseconds) shouldBe true
                    }
                }
            }
        }

        "the API serves the slice exactly as the file holds it" {
            val ref = recordedRef()
            val onDisk = Json.parseToJsonElement(File(insightsDepotDir, ref.toPath()).readText()).jsonObject

            served(ref).kontainerSlice() shouldBe onDisk.kontainerSlice()
        }

        "its services join the kontainer graph, against the very fingerprint the graph reports" {
            val slice = served(recordedRef()).kontainerSlice()
            val graph = kontainerGraph()

            slice["graphFingerprint"].shouldNotBeNull().jsonPrimitive.content shouldBe graph.fingerprint

            val graphClasses = graph.services.map { it.cls }.toSet()
            val sliceServices = slice["services"]!!.jsonArray.map { it.jsonObject }

            sliceServices.forEach { service ->
                graphClasses shouldContain service["cls"]!!.jsonPrimitive.content
            }

            withClue("the record keeps what only the request knows: UserProvider was handed in, so it is an override here") {
                val userProvider = sliceServices.single { it["cls"]!!.jsonPrimitive.content == UserProvider::class.java.name }
                userProvider["type"]!!.jsonPrimitive.content shouldBe "DynamicOverride"

                graph.services.single { it.cls == UserProvider::class.java.name }.type shouldBe "Dynamic"
            }
        }

        "an OLD record, in the fat shape, is still served exactly as it was written" {
            // A real record written before the slimming, trimmed to three services and nothing else. Frozen
            // here rather than rebuilt from today's DebugInfo, so it keeps testing the shape that is on disk.
            val ref = InsightsRecordRef(bucket = "records-2026-10-01", file = "2026-10-01T12:00:00.000000000.json")
            val stored = Json.parseToJsonElement(FAT_RECORD).jsonObject

            repository.putFile(path = ref.toPath(), content = FAT_RECORD)

            try {
                val record = served(ref)

                record["method"]!!.jsonPrimitive.content shouldBe "GET"
                record["path"]!!.jsonPrimitive.content shouldBe "/_/funktor/introspection/lifecycle-hooks"
                record["status"]!!.jsonPrimitive.long shouldBe 200L

                val slice = record.kontainerSlice()

                withClue("nothing reshapes an old slice on the way out: the frontend reads both shapes itself") {
                    slice shouldBe stored.kontainerSlice()
                }

                withClue("and it is unmistakably the fat shape, which a reader tells apart by `info`") {
                    slice.keys shouldBe setOf("numOld", "numTotal", "info")
                    slice["info"]!!.jsonObject["services"]!!.jsonArray.size shouldBe 3
                }

                allRecords().map { it.ref } shouldContain ref
            } finally {
                repository.removeFile(ref.toPath()) shouldNotBe false
            }
        }
    }

    private companion object {
        /** Trimmed from `records-2026-10-19/2026-10-19T14:21:28.864097350.json`, written by the fat collector. */
        val FAT_RECORD = """
{
  "ts": {
    "ts": 1792419688864,
    "timezone": "UTC",
    "human": "2026-10-19T14:21:28.864097350Z[UTC]"
  },
  "date": "2026-10-19T14:21:28.864097350",
  "startedNs": 785491918777290,
  "endedNs": 785491925954566,
  "method": "GET",
  "uri": "/_/funktor/introspection/lifecycle-hooks",
  "status": 200,
  "collectors": [
    {
      "key": "kontainer",
      "data": {
        "numOld": 0,
        "numTotal": 0,
        "info": {
          "services": [
            {
              "cls": {
                "fqn": "io.peekandpoke.ultra.security.password.PasswordHasher"
              },
              "type": "Singleton",
              "definition": {
                "creates": {
                  "fqn": "io.peekandpoke.ultra.security.password.CompoundPasswordHasher"
                },
                "injectionType": "Singleton",
                "injects": [],
                "codeLocation": {
                  "location": "io.peekandpoke.ultra.security.Index_jvmKt.Ultra_Security${'$'}lambda${'$'}0(index_jvm.kt:49)"
                },
                "overwrites": null
              },
              "instances": []
            },
            {
              "cls": {
                "fqn": "io.peekandpoke.ultra.security.user.UserProvider"
              },
              "type": "DynamicOverride",
              "definition": {
                "creates": {
                  "fqn": "io.peekandpoke.ultra.security.user.UserProvider${'$'}Lazy"
                },
                "injectionType": "Dynamic",
                "injects": [],
                "codeLocation": {
                  "location": "io.peekandpoke.ultra.kontainer.ServiceProviderFactory.<init>(ServiceProviderFactory.kt:26)"
                },
                "overwrites": {
                  "creates": {
                    "fqn": "io.peekandpoke.ultra.security.user.UserProvider"
                  },
                  "injectionType": "Dynamic",
                  "injects": [],
                  "codeLocation": {
                    "location": "io.peekandpoke.ultra.security.Index_jvmKt.Ultra_Security${'$'}lambda${'$'}0(index_jvm.kt:26)"
                  },
                  "overwrites": null
                }
              },
              "instances": [
                {
                  "createdAt": {
                    "ts": 1792412488863,
                    "timezone": "UTC",
                    "human": "2026-10-19T12:21:28.863898199Z"
                  },
                  "cls": {
                    "fqn": "io.peekandpoke.ultra.security.user.UserProvider${'$'}Lazy"
                  }
                }
              ]
            },
            {
              "cls": {
                "fqn": "io.peekandpoke.funktor.insights.InsightsRepository"
              },
              "type": "Dynamic",
              "definition": {
                "creates": {
                  "fqn": "io.peekandpoke.funktor.insights.InsightsFileRepository"
                },
                "injectionType": "Dynamic",
                "injects": [],
                "codeLocation": {
                  "location": "io.peekandpoke.funktor.Index_testJvmKt.createBlueprint${'$'}lambda${'$'}0(index_testJvm.kt:94)"
                },
                "overwrites": {
                  "creates": {
                    "fqn": "io.peekandpoke.funktor.insights.InsightsFileRepository"
                  },
                  "injectionType": "Dynamic",
                  "injects": [],
                  "codeLocation": {
                    "location": "io.peekandpoke.funktor.insights.Insights_moduleKt.Funktor_Insights${'$'}lambda${'$'}0(insights_module.kt:113)"
                  },
                  "overwrites": null
                }
              },
              "instances": [
                {
                  "createdAt": {
                    "ts": 1792412488864,
                    "timezone": "UTC",
                    "human": "2026-10-19T12:21:28.864064539Z"
                  },
                  "cls": {
                    "fqn": "io.peekandpoke.funktor.insights.InsightsFileRepository"
                  }
                }
              ]
            }
          ]
        }
      }
    }
  ]
}
        """.trimIndent()
    }
}
