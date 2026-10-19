package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.insights.InsightsGraphs
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import io.peekandpoke.funktor.insights.api.InsightsDatabaseGraph
import io.peekandpoke.funktor.insights.api.InsightsKontainerGraph
import io.peekandpoke.funktor.insights.collectors.KontainerCollector
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.ultra.security.user.UserProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The two graph endpoints over real HTTP: who may read them, and what their JSON looks like on the wire.
 *
 * Asserted on the RAW body, not on a decoded DTO. The server writes responses with Slumber while
 * clients read them with kotlinx or by hand, so the JSON is the contract — inferring it from the Kotlin
 * types is how TAB-SPECS went wrong.
 */
class InsightsGraphsApiSpec : FunktorApiSpec() {

    private val api by service(InsightsApiFeature::class)

    /** Strict on purpose: an unknown key or a missing field in what Slumber wrote fails the decode. */
    private val strictJson = Json

    private val knownTypes = setOf("Singleton", "SemiDynamic", "Dynamic", "Prototype")

    private fun String.data(): JsonElement = Json.parseToJsonElement(this).jsonObject["data"].shouldNotBeNull()

    private fun JsonElement.keys(): Set<String> = jsonObject.keys

    init {
        "the floor: neither graph reaches anyone below superuser" - {

            suspend fun <R> refusedBelowSuperuser(name: String, route: ApiRoute.Plain<R>) {

                "$name: anonymous gets the floor's 401 and no graph" {
                    apiApp {
                        anonymous {
                            request(route) {
                                status shouldBe HttpStatusCode.Unauthorized

                                val body = bodyAsText()
                                body.data() shouldBe JsonNull
                                // a class every graph names; it must not leak through an error body either
                                body shouldNotContain KontainerCollector::class.java.name
                            }
                        }
                    }
                }

                "$name: an authenticated non-superuser gets the floor's 401 and no graph" {
                    apiApp {
                        authenticate(regularUserToken) {
                            request(route) {
                                status shouldBe HttpStatusCode.Unauthorized

                                val body = bodyAsText()
                                body.data() shouldBe JsonNull
                                body shouldNotContain KontainerCollector::class.java.name
                            }
                        }
                    }
                }
            }

            refusedBelowSuperuser("kontainer graph", api.insights.getKontainerGraph)
            refusedBelowSuperuser("database graph", api.insights.getDatabaseGraph)

            "the answer is the very one the record endpoints give: same status, same refusal" {
                var graphBody = ""
                var recordsBody = ""

                apiApp {
                    authenticate(regularUserToken) {
                        request(api.insights.getKontainerGraph) {
                            status shouldBe HttpStatusCode.Unauthorized
                            graphBody = bodyAsText()
                        }
                        request(api.insights.listRecords, InsightsApiFeature.PagingParam()) {
                            status shouldBe HttpStatusCode.Unauthorized
                            recordsBody = bodyAsText()
                        }
                    }
                }

                insightsWireSample("refusal-kontainer-graph.json", graphBody)

                fun JsonElement.refusal() = jsonObject.filterKeys { it != "insights" }.let { JsonObject(it) }
                    .toString()
                    // The refusal names the URI it refused; apart from that the two must be identical
                    .replace(Regex("/_/funktor/insights/[a-z/]+(\\?[^' ]*)?"), "<uri>")

                Json.parseToJsonElement(graphBody).refusal() shouldBe Json.parseToJsonElement(recordsBody).refusal()
            }
        }

        "GET kontainer graph, as a superuser" - {

            suspend fun fetch(): String {
                var body = ""

                apiApp {
                    authenticate(superUserToken) {
                        request(api.insights.getKontainerGraph) {
                            status shouldBe HttpStatusCode.OK
                            body = bodyAsText()
                        }
                    }
                }

                return body
            }

            "the wire shape: {fingerprint, services[{cls, type, definition}]}" {
                val body = fetch().also { insightsWireSample("kontainer-graph.json", it) }
                val data = body.data()

                data.keys() shouldContainExactlyInAnyOrder listOf("fingerprint", "services")
                data.jsonObject["fingerprint"].shouldNotBeNull().jsonPrimitive.content shouldMatch Regex("[0-9a-f]{64}")

                val services = data.jsonObject["services"].shouldNotBeNull().jsonArray
                services.shouldNotBeEmpty()

                services.forEach { raw ->
                    val service = raw.jsonObject
                    val cls = service["cls"].shouldNotBeNull().jsonPrimitive

                    withClue(cls.content) {
                        service.keys shouldContainExactlyInAnyOrder listOf("cls", "type", "definition")
                        cls.isString shouldBe true
                        (service["type"].shouldNotBeNull().jsonPrimitive.content in knownTypes) shouldBe true

                        var definition: JsonElement = service["definition"].shouldNotBeNull()

                        // walk the overwrites chain: every link has the same five keys, the last one is null
                        while (definition != JsonNull) {
                            definition.keys() shouldContainExactlyInAnyOrder
                                    listOf("creates", "injectionType", "injects", "codeLocation", "overwrites")
                            definition.jsonObject["creates"].shouldNotBeNull().jsonPrimitive.isString shouldBe true
                            definition.jsonObject["codeLocation"].shouldNotBeNull().jsonPrimitive.isString shouldBe true

                            definition.jsonObject["injects"].shouldNotBeNull().jsonArray.forEach { inject ->
                                inject.keys() shouldContainExactlyInAnyOrder listOf("name", "provisionType", "classes")
                                (inject.jsonObject["provisionType"]!!.jsonPrimitive.content in setOf("Direct", "Lazy")) shouldBe true
                                inject.jsonObject["classes"].shouldNotBeNull().jsonArray.forEach {
                                    (it as JsonPrimitive).isString shouldBe true
                                }
                            }

                            definition = definition.jsonObject["overwrites"].shouldNotBeNull()
                        }
                    }
                }

                withClue("ordered by cls, so a reader can binary-search and the fingerprint is stable") {
                    val names = services.map { it.jsonObject["cls"]!!.jsonPrimitive.content }
                    names shouldBe names.sorted()
                }
            }

            "it decodes strictly into the typed DTO a kotlinx client would use" {
                val graph = strictJson.decodeFromJsonElement(InsightsKontainerGraph.serializer(), fetch().data())

                graph.services.shouldNotBeEmpty()
            }

            "it is the BLUEPRINT's graph, not the asking request's" {
                // The request asking for it had a UserProvider handed in, which its own kontainer reports as
                // `DynamicOverride` with an empty definition. The graph must not depend on who asked.
                val graph = strictJson.decodeFromJsonElement(InsightsKontainerGraph.serializer(), fetch().data())
                val userProvider = graph.services.single { it.cls == UserProvider::class.java.name }

                userProvider.type shouldBe "Dynamic"
                userProvider.definition.codeLocation shouldNotContain "ServiceProviderFactory"
            }

            "InsightsGraphs is a true singleton, so the graph is built once per blueprint" {
                // Injecting anything dynamic would silently downgrade it to SemiDynamic: rebuilt per request.
                val graph = strictJson.decodeFromJsonElement(InsightsKontainerGraph.serializer(), fetch().data())

                graph.services.single { it.cls == InsightsGraphs::class.java.name }.type shouldBe "Singleton"
            }

            "two requests get the same fingerprint" {
                val first = fetch().data().jsonObject["fingerprint"]
                val second = fetch().data().jsonObject["fingerprint"]

                first shouldBe second
            }
        }

        "GET database graph, as a superuser" - {

            "the wire shape: {repos[{id{name, connection}, storedClasses[{fqn, references[{repo, fqn}]}]}]}" {
                var body = ""

                apiApp {
                    authenticate(superUserToken) {
                        request(api.insights.getDatabaseGraph) {
                            status shouldBe HttpStatusCode.OK
                            body = bodyAsText()
                        }
                    }
                }

                insightsWireSample("database-graph.json", body)

                val data = body.data()
                data.keys() shouldBe setOf("repos")

                val repos = data.jsonObject["repos"].shouldNotBeNull().jsonArray
                repos.shouldNotBeEmpty()

                repos.forEach { raw ->
                    val repo = raw.jsonObject
                    val id = repo["id"].shouldNotBeNull()

                    withClue(id.toString()) {
                        // no top-level `connection` and no reference `type`: the model's dead fields are dropped
                        repo.keys shouldContainExactlyInAnyOrder listOf("id", "storedClasses")
                        id.keys() shouldContainExactlyInAnyOrder listOf("name", "connection")
                        // this app is Karango-only; the two-backend case is InsightsDatabaseGraphBackendsSpec
                        id.jsonObject["connection"].shouldNotBeNull().jsonPrimitive.content shouldMatch Regex("ArangoDB\\(.+\\)::.+")

                        repo["storedClasses"].shouldNotBeNull().jsonArray.forEach { stored ->
                            stored.keys() shouldContainExactlyInAnyOrder listOf("fqn", "references")
                            stored.jsonObject["references"].shouldNotBeNull().jsonArray.forEach { ref ->
                                ref.keys() shouldContainExactlyInAnyOrder listOf("repo", "fqn")
                            }
                        }
                    }
                }

                strictJson.decodeFromJsonElement(InsightsDatabaseGraph.serializer(), data).repos.shouldNotBeEmpty()
            }
        }
    }
}
