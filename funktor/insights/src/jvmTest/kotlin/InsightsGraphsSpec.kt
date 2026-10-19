package io.peekandpoke.funktor.insights

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.funktor.insights.api.InsightsDatabaseGraph
import io.peekandpoke.funktor.insights.api.InsightsKontainerGraph
import io.peekandpoke.ultra.kontainer.ServiceProvider
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import io.peekandpoke.ultra.vault.domain.DatabaseGraphModel
import kotlin.reflect.KClass

class InsightsGraphsSpec : FreeSpec() {

    private fun InsightsKontainerGraph.service(cls: KClass<*>) = services.single { it.cls == cls.java.name }

    init {
        "the kontainer graph" - {

            "lists every service with its blueprint type and definition" {
                val graph = InsightsGraphs(KontainerFixture.blueprint()).kontainerGraph

                graph.service(KontainerFixture.Clock::class).type shouldBe "Singleton"
                graph.service(KontainerFixture.Unused::class).type shouldBe "Singleton"
                graph.service(KontainerFixture.Stamp::class).type shouldBe "Prototype"
                graph.service(KontainerFixture.Greeter::class).type shouldBe "Dynamic"
                graph.service(KontainerFixture.Wrapper::class).type shouldBe "SemiDynamic"

                val handler = graph.service(KontainerFixture.Handler::class)
                handler.type shouldBe "Dynamic"
                handler.definition.creates shouldBe KontainerFixture.Handler::class.java.name
                handler.definition.injectionType shouldBe "Dynamic"
                handler.definition.injects.map { it.name } shouldBe listOf("clock", "greeter", "later")
                handler.definition.injects.map { it.provisionType } shouldBe listOf("Direct", "Direct", "Lazy")
                handler.definition.injects[0].classes shouldBe listOf(KontainerFixture.Clock::class.java.name)
                handler.definition.overwrites shouldBe null
            }

            "is read WITHOUT the request's overrides, so who asks does not change it" {
                val request = KontainerFixture.requestKontainer()

                withClue("the request's own kontainer reports the handed-in instance as an override") {
                    request.tools.getDebugInfo().services
                        .single { it.cls.fqn == KontainerFixture.Greeter::class.java.name }
                        .type shouldBe ServiceProvider.Type.DynamicOverride
                }

                val greeter = InsightsGraphs(request.blueprint).kontainerGraph.service(KontainerFixture.Greeter::class)

                withClue("the graph shows the blueprint's definition, not the override's empty one") {
                    greeter.type shouldBe "Dynamic"
                    greeter.definition.creates shouldBe KontainerFixture.DefaultGreeter::class.java.name
                    greeter.definition.codeLocation shouldNotContain "ServiceProviderFactory"
                }
            }

            "carries an overwritten definition as a chain" {
                val greeter = InsightsGraphs(KontainerFixture.blueprintWithOverwrite()).kontainerGraph
                    .service(KontainerFixture.Greeter::class)

                greeter.definition.creates shouldBe KontainerFixture.OtherGreeter::class.java.name
                greeter.definition.overwrites.shouldNotBeNull().creates shouldBe
                        KontainerFixture.DefaultGreeter::class.java.name
            }

            "is built once per InsightsGraphs, not per call" {
                val graphs = InsightsGraphs(KontainerFixture.blueprint())

                (graphs.kontainerGraph === graphs.kontainerGraph) shouldBe true
            }
        }

        "the fingerprint" - {

            "is a SHA-256 hex string, equal for equal blueprints" {
                val fingerprint = InsightsGraphs(KontainerFixture.blueprint()).kontainerGraph.fingerprint

                fingerprint shouldMatch Regex("[0-9a-f]{64}")
                InsightsGraphs(KontainerFixture.blueprint()).kontainerGraph.fingerprint shouldBe fingerprint
            }

            "changes when only a definition changes" {
                // Same service set; only what Greeter creates differs. A fingerprint over the class names
                // alone would call these two graphs equal.
                InsightsGraphs(KontainerFixture.blueprintWithOverwrite()).kontainerGraph.fingerprint shouldNotBe
                        InsightsGraphs(KontainerFixture.blueprint()).kontainerGraph.fingerprint
            }

            "does not depend on the order the kontainer lists services in" {
                val forwards = KontainerFixture.singletons(KontainerFixture.Clock::class, KontainerFixture.Unused::class)
                val backwards = KontainerFixture.singletons(KontainerFixture.Unused::class, KontainerFixture.Clock::class)

                InsightsGraphs(forwards).kontainerGraph.fingerprint shouldBe
                        InsightsGraphs(backwards).kontainerGraph.fingerprint
            }
        }

        "the database graph mirrors the model field for field" {
            val users = DatabaseGraphModel.Repo.Id(name = "users", connection = "ArangoDB(3.12)::db")

            val model = DatabaseGraphModel(
                repos = listOf(
                    DatabaseGraphModel.Repo(
                        id = users,
                        connection = users.connection,
                        storedClasses = listOf(
                            DatabaseGraphModel.StoredClass(
                                fqn = "app.User",
                                references = listOf(
                                    DatabaseGraphModel.Reference(
                                        repo = users,
                                        fqn = "app.User",
                                        type = DatabaseGraphModel.Reference.Type.Direct,
                                    ),
                                    DatabaseGraphModel.Reference(
                                        repo = null,
                                        fqn = "app.Unstored",
                                        type = DatabaseGraphModel.Reference.Type.Direct,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            )

            val graph = InsightsGraphs(KontainerFixture.blueprint()).databaseGraph(model)

            graph shouldBe InsightsDatabaseGraph(
                repos = listOf(
                    InsightsDatabaseGraph.Repo(
                        id = InsightsDatabaseGraph.Repo.Id(name = "users", connection = "ArangoDB(3.12)::db"),
                        connection = "ArangoDB(3.12)::db",
                        storedClasses = listOf(
                            InsightsDatabaseGraph.StoredClass(
                                fqn = "app.User",
                                references = listOf(
                                    InsightsDatabaseGraph.Reference(
                                        repo = InsightsDatabaseGraph.Repo.Id(name = "users", connection = "ArangoDB(3.12)::db"),
                                        fqn = "app.User",
                                        type = "Direct",
                                    ),
                                    InsightsDatabaseGraph.Reference(repo = null, fqn = "app.Unstored", type = "Direct"),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }

        "both graphs round-trip through Slumber, which is what writes an ApiRoutes response" {
            // Not kotlinx: `SlumberRestCodec` serializes every response, and the TypeScript generator reads
            // the same SlumberConfig. A field Slumber cannot describe fails on the first request.
            val codec = Codec.default
            val kontainerGraph = InsightsGraphs(KontainerFixture.blueprintWithOverwrite()).kontainerGraph

            codec.slumber(kontainerGraph).shouldBeInstanceOf<Map<*, *>>()
            codec.awake<InsightsKontainerGraph>(codec.slumber(kontainerGraph)) shouldBe kontainerGraph

            val databaseGraph = InsightsDatabaseGraph(
                repos = listOf(
                    InsightsDatabaseGraph.Repo(
                        id = InsightsDatabaseGraph.Repo.Id("users", "c"),
                        connection = "c",
                        storedClasses = listOf(
                            InsightsDatabaseGraph.StoredClass(
                                fqn = "app.User",
                                references = listOf(InsightsDatabaseGraph.Reference(repo = null, fqn = "x", type = "Direct")),
                            ),
                        ),
                    ),
                ),
            )

            codec.awake<InsightsDatabaseGraph>(codec.slumber(databaseGraph)) shouldBe databaseGraph
        }
    }
}
