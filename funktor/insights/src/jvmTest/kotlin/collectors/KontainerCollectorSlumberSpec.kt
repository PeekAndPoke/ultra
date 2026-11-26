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
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.slumber.JsonUtil.toJsonElement
import io.peekandpoke.ultra.slumber.slumber
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

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

    private fun slice(kontainer: Kontainer = KontainerFixture.requestKontainer()): KontainerCollector.Data =
        KontainerCollector.Data.of(
            graphFingerprint = "f".repeat(64),
            providers = kontainer.tools.factory.getAllProviders(),
        )

    @Suppress("UNCHECKED_CAST")
    private fun slumbered(data: KontainerCollector.Data): Map<String, Any?> =
        codec.slumber(data).shouldBeInstanceOf<Map<String, Any?>>()

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.services(): List<Map<String, Any?>> = this["services"] as List<Map<String, Any?>>

    private fun Map<String, Any?>.service(cls: KClass<*>): Map<String, Any?> =
        services().single { it["cls"] == cls.java.name }

    init {
        "the slice slumbers, to exactly its two top-level fields -- no numOld/numTotal since 2026-10-19" {
            val map = slumbered(slice())

            map.keys shouldContainExactlyInAnyOrder listOf("graphFingerprint", "services")
            map["graphFingerprint"] shouldBe "f".repeat(64)
        }

        "only services with creations are written, each as cls, type, count and both creation times" {
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
                withClue("${service["cls"]}: no definition (they made the slice 86.7% of a record), no instance list") {
                    service.keys shouldContainExactlyInAnyOrder
                            listOf("cls", "type", "count", "firstCreatedAt", "lastCreatedAt")
                }
            }
        }

        "class names are flat strings, not {fqn} objects" {
            val services = slumbered(slice()).services()

            services.forEach { service ->
                service["cls"].shouldBeInstanceOf<String>()
            }

            slumbered(slice()).service(KontainerFixture.Handler::class)["cls"] shouldBe
                    KontainerFixture.Handler::class.java.name
        }

        "count is the provider's count, and both times are {ts, timezone, human}, ts in epoch MILLIS" {
            // The reader that took a bare number for epoch SECONDS would put a millis value in the year 58,000
            // (finding D2). The object form is what it reads correctly, so it is what must be written.
            val kontainer = KontainerFixture.requestKontainer()

            kontainer.get(KontainerFixture.Stamp::class)
            Thread.sleep(5)
            kontainer.get(KontainerFixture.Stamp::class)
            Thread.sleep(5)
            kontainer.get(KontainerFixture.Stamp::class)

            val stamp = slumbered(slice(kontainer)).service(KontainerFixture.Stamp::class)

            // From the kontainer itself, not from the slice under test: an expected value read back off the
            // slice would agree with any unit or field the collector got wrong.
            val expected = kontainer.tools.factory.getProvider(KontainerFixture.Stamp::class).created

            stamp["type"] shouldBe "Prototype"
            stamp["count"] shouldBe 3L

            listOf("firstCreatedAt" to expected.firstAt, "lastCreatedAt" to expected.lastAt).forEach { (key, at) ->
                withClue(key) {
                    @Suppress("UNCHECKED_CAST")
                    val written = stamp[key].shouldBeInstanceOf<Map<String, Any?>>()

                    written.keys shouldContainExactlyInAnyOrder listOf("ts", "timezone", "human")
                    written["ts"] shouldBe at.shouldNotBeNull().toEpochMilli()
                    written["timezone"] shouldBe "UTC"
                }
            }

            withClue("three creations 5ms apart, so the first and the last time differ") {
                (expected.firstAt!! < expected.lastAt!!) shouldBe true
            }
        }

        "an instance handed in at creation is DynamicOverride, keyed by the base type it replaces, counted once" {
            val greeter = slumbered(slice()).service(KontainerFixture.Greeter::class)

            greeter["type"] shouldBe "DynamicOverride"
            greeter["count"] shouldBe 1L
        }

        "a service from a record written before the counters awakes as one without creations" {
            val old = mapOf(
                "graphFingerprint" to "f".repeat(64),
                "services" to listOf(
                    mapOf(
                        "cls" to KontainerFixture.Handler::class.java.name,
                        "type" to "Dynamic",
                        "instances" to listOf(
                            mapOf(
                                "cls" to KontainerFixture.Handler::class.java.name,
                                "createdAt" to mapOf("ts" to 1792413528458L, "timezone" to "UTC", "human" to "x"),
                            )
                        ),
                    )
                ),
            )

            val awoken = codec.awake(typeOf<KontainerCollector.Data>(), old)
                .shouldBeInstanceOf<KontainerCollector.Data>()

            val service = awoken.services.single()

            service.cls shouldBe KontainerFixture.Handler::class.java.name
            service.type shouldBe "Dynamic"
            service.count shouldBe 0L
            service.firstCreatedAt shouldBe null
            service.lastCreatedAt shouldBe null
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
