package io.peekandpoke.funktor.insights.collectors

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.peekandpoke.funktor.insights.InsightsCodec
import io.peekandpoke.funktor.insights.InsightsGraphs
import io.peekandpoke.funktor.insights.KontainerFixture
import io.peekandpoke.ultra.slumber.JsonUtil.toJsonElement
import io.peekandpoke.ultra.slumber.slumber
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject

/**
 * `KontainerCollector.finish` never throws, and what it keeps when a part fails.
 *
 * A collector that throws drops the WHOLE record (`InsightsFull.finish` runs them all before it writes),
 * so each part is guarded on its own: a graph that cannot be built must not take the request's services
 * with it, and services that cannot be read must not take the fingerprint.
 */
class KontainerCollectorFinishSpec : FreeSpec() {

    /** Runs [collector] against a real `ApplicationCall` and returns its slice for assertion out here. */
    private fun finish(collector: KontainerCollector): KontainerCollector.Data {
        var captured: KontainerCollector.Data? = null

        testApplication {
            routing {
                get("/probe") {
                    captured = collector.finish(call)
                    call.respondText("ok")
                }
            }

            client.get("/probe")
        }

        return captured ?: error("the probe route never ran")
    }

    private fun KontainerCollector.Data.has(cls: Class<*>) = services.any { it.cls == cls.name }

    init {
        "finish" - {

            "records the request's services and the fingerprint of the graph they join" {
                val request = KontainerFixture.requestKontainer()
                val graphs = InsightsGraphs(request.blueprint)

                val data = finish(KontainerCollector(request, graphs))

                data.graphFingerprint shouldBe graphs.kontainerGraph.fingerprint
                data.has(KontainerFixture.Handler::class.java) shouldBe true
            }

            "writes exactly graphFingerprint and services -- no counters (maintainer, 2026-10-19)" {
                // numOld/numTotal counted TRACKED kontainers and tracking is never enabled: always 0. Slumbered
                // with the writer's codec, because writing is what the slice undergoes in production.
                val request = KontainerFixture.requestKontainer()
                val data = finish(KontainerCollector(request, InsightsGraphs(request.blueprint)))

                InsightsCodec().slumber(data).toJsonElement().jsonObject.keys shouldBe setOf("graphFingerprint", "services")
            }

            "a graph that cannot be built leaves the fingerprint null and the services in place" {
                val request = KontainerFixture.requestKontainer()
                val broken = InsightsGraphs(KontainerFixture.brokenBlueprint())

                val data = finish(KontainerCollector(request, broken))

                data.graphFingerprint shouldBe null

                withClue("one try around both would have dropped the request's services with the graph") {
                    data.has(KontainerFixture.Handler::class.java) shouldBe true
                    data.has(KontainerFixture.Greeter::class.java) shouldBe true
                }

                withClue("the graphFingerprint KEY marks the v2 shape, so a null value must be written, not omitted") {
                    val written = InsightsCodec().slumber(data).toJsonElement().jsonObject

                    written.containsKey("graphFingerprint") shouldBe true
                    written["graphFingerprint"] shouldBe JsonNull
                }
            }
        }

        "collect" - {

            // `finish` cannot be made to fail at reading providers with a real kontainer, so this branch
            // is driven through the function `finish` delegates to.

            "services that cannot be read leave an empty list and keep the fingerprint" {
                val data = KontainerCollector.collect(
                    graphFingerprint = { "f".repeat(64) },
                    providers = { error("the providers could not be read") },
                )

                data.services.shouldBeEmpty()
                data.graphFingerprint shouldBe "f".repeat(64)
            }
        }
    }
}
