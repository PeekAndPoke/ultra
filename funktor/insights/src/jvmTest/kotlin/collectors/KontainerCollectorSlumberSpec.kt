package io.peekandpoke.funktor.insights.collectors

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.funktor.insights.CollectorData
import io.peekandpoke.funktor.insights.InsightsCodec
import io.peekandpoke.funktor.insights.InsightsData
import io.peekandpoke.funktor.insights.KontainerFixture
import io.peekandpoke.ultra.slumber.JsonUtil.toJsonElement
import io.peekandpoke.ultra.slumber.slumber
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import kotlin.reflect.KClass

/**
 * The slim `kontainer` slice survives the one operation it undergoes in production: slumbering.
 *
 * It is slumbered inside `launch(Dispatchers.IO)` after the response has gone out, so a slice Slumber
 * cannot write does not fail a request — it silently drops the WHOLE record. That is how `vault.entries`
 * was `[]` in all 1193 depot records (see `VaultCollectorSlumberSpec`). Uses [InsightsCodec], the codec
 * the writer is given, not a stand-in.
 */
class KontainerCollectorSlumberSpec : FreeSpec() {

    private val codec = InsightsCodec()

    private fun slice(): KontainerCollector.Data = KontainerCollector.Data.of(
        numOld = 1,
        numTotal = 3,
        graphFingerprint = "f".repeat(64),
        providers = KontainerFixture.requestKontainer().tools.factory.getAllProviders(),
    )

    @Suppress("UNCHECKED_CAST")
    private fun slumbered(data: KontainerCollector.Data): Map<String, Any?> =
        codec.slumber(data).shouldBeInstanceOf<Map<String, Any?>>()

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.services(): List<Map<String, Any?>> = this["services"] as List<Map<String, Any?>>

    private fun Map<String, Any?>.service(cls: KClass<*>): Map<String, Any?> =
        services().single { it["cls"] == cls.java.name }

    init {
        "the slice slumbers, to exactly the four top-level fields" {
            val map = slumbered(slice())

            map.keys shouldContainExactlyInAnyOrder listOf("numOld", "numTotal", "graphFingerprint", "services")
            map["numOld"] shouldBe 1
            map["numTotal"] shouldBe 3
            map["graphFingerprint"] shouldBe "f".repeat(64)
        }

        "only services with instances are written, and none carries a definition" {
            val services = slumbered(slice()).services()

            services.map { it["cls"] }.toSet().let { written ->
                withClue("instantiated in the request: Handler, the Clock it injects, the Greeter handed in") {
                    (KontainerFixture.Handler::class.java.name in written) shouldBe true
                    (KontainerFixture.Clock::class.java.name in written) shouldBe true
                    (KontainerFixture.Greeter::class.java.name in written) shouldBe true
                }
                withClue("never instantiated, so it is the static graph's business, not the record's") {
                    (KontainerFixture.Unused::class.java.name in written) shouldBe false
                }
            }

            services.forEach { service ->
                withClue("${service["cls"]}: the definitions are what made the slice 86.7% of a record") {
                    service.keys shouldContainExactlyInAnyOrder listOf("cls", "type", "instances")
                }
            }
        }

        "class names are flat strings, not {fqn} objects" {
            val services = slumbered(slice()).services()

            services.forEach { service ->
                service["cls"].shouldBeInstanceOf<String>()

                @Suppress("UNCHECKED_CAST")
                (service["instances"] as List<Map<String, Any?>>).forEach { it["cls"].shouldBeInstanceOf<String>() }
            }

            @Suppress("UNCHECKED_CAST")
            val handler = (slumbered(slice()).service(KontainerFixture.Handler::class)["instances"] as List<Map<String, Any?>>)
            handler.single()["cls"] shouldBe KontainerFixture.Handler::class.java.name
        }

        "createdAt is {ts, timezone, human}, ts in epoch MILLIS" {
            // The reader that took a bare number for epoch SECONDS would put a millis value in the year 58,000
            // (finding D2). The object form is what it reads correctly, so it is what must be written.
            val data = slice()
            val expected = data.services.single { it.cls == KontainerFixture.Handler::class.java.name }
                .instances.single().createdAt

            @Suppress("UNCHECKED_CAST")
            val instance = (slumbered(data).service(KontainerFixture.Handler::class)["instances"] as List<Map<String, Any?>>)
                .single()

            @Suppress("UNCHECKED_CAST")
            val createdAt = instance["createdAt"].shouldBeInstanceOf<Map<String, Any?>>()

            createdAt.keys shouldContainExactlyInAnyOrder listOf("ts", "timezone", "human")
            createdAt["ts"] shouldBe expected.toEpochMilli()
            createdAt["timezone"] shouldBe "UTC"
        }

        "an instance handed in at creation is DynamicOverride, keyed by the base type it replaces" {
            val greeter = slumbered(slice()).service(KontainerFixture.Greeter::class)

            greeter["type"] shouldBe "DynamicOverride"

            @Suppress("UNCHECKED_CAST")
            val instance = (greeter["instances"] as List<Map<String, Any?>>).single()
            instance["cls"] shouldBe KontainerFixture.RequestGreeter::class.java.name
        }

        "the whole record, assembled the way InsightsFull writes it, carries the slice intact" {
            // Mirrors `InsightsFull.finish`: slumber the slice, wrap it, slumber the record, render JSON.
            val data = slice()
            val sliceMap = slumbered(data)

            val record = InsightsData(
                ts = LocalDateTime.now(),
                date = "2026-10-19",
                startedNs = 1,
                endedNs = 2,
                collectors = listOf(CollectorData(KontainerCollector.KEY, sliceMap)),
            )

            val json = codec.slumber(record).toJsonElement().jsonObject
            val written = json["collectors"].shouldNotBeNull().jsonArray.single().jsonObject

            written["key"] shouldBe JsonPrimitive(KontainerCollector.KEY)

            val writtenSlice = written["data"].shouldBeInstanceOf<JsonObject>()
            writtenSlice["services"].shouldNotBeNull().jsonArray.size shouldBe data.services.size
            writtenSlice["graphFingerprint"].shouldNotBeNull().jsonPrimitive.content shouldBe "f".repeat(64)
        }
    }
}
