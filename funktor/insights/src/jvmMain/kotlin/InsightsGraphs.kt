package io.peekandpoke.funktor.insights

import io.peekandpoke.funktor.insights.api.InsightsDatabaseGraph
import io.peekandpoke.funktor.insights.api.InsightsKontainerGraph
import io.peekandpoke.ultra.common.sha256
import io.peekandpoke.ultra.common.toHex
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.domain.DebugInfo
import io.peekandpoke.ultra.vault.domain.DatabaseGraphModel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Builds the two static graphs the insights API serves.
 *
 * A singleton that injects only the blueprint — itself a singleton — so [kontainerGraph] is built once
 * per blueprint and shared by every request. Injecting anything dynamic would make it one per request.
 */
class InsightsGraphs(private val blueprint: KontainerBlueprint) {

    private companion object {
        val json = Json

        fun fingerprintOf(services: List<InsightsKontainerGraph.Service>): String =
            json.encodeToString(ListSerializer(InsightsKontainerGraph.Service.serializer()), services)
                .sha256()
                .toHex()
    }

    /**
     * The blueprint's service graph, built on first use.
     *
     * Read from a kontainer created WITHOUT dynamic overrides. A request's kontainer swaps some services
     * for instances handed in at creation (`UserProvider`, `InsightsConfig`), which it reports as
     * `DynamicOverride` with an empty definition — a graph read from it would depend on who asked.
     * Sorted, so equal blueprints give equal fingerprints whatever order the kontainer lists them in.
     */
    val kontainerGraph: InsightsKontainerGraph by lazy {
        val services = blueprint.create().tools.getDebugInfo().services
            .map { it.toGraph() }
            .sortedBy { it.cls }

        InsightsKontainerGraph(fingerprint = fingerprintOf(services), services = services)
    }

    /** [model] as the wire DTO. */
    fun databaseGraph(model: DatabaseGraphModel): InsightsDatabaseGraph = InsightsDatabaseGraph(
        repos = model.repos.map { repo ->
            InsightsDatabaseGraph.Repo(
                id = repo.id.toGraph(),
                connection = repo.connection,
                storedClasses = repo.storedClasses.map { stored ->
                    InsightsDatabaseGraph.StoredClass(
                        fqn = stored.fqn,
                        references = stored.references.map { ref ->
                            InsightsDatabaseGraph.Reference(repo = ref.repo?.toGraph(), fqn = ref.fqn, type = ref.type.name)
                        },
                    )
                },
            )
        },
    )

    private fun DatabaseGraphModel.Repo.Id.toGraph() = InsightsDatabaseGraph.Repo.Id(name = name, connection = connection)

    private fun DebugInfo.ServiceDebugInfo.toGraph() = InsightsKontainerGraph.Service(
        cls = cls.fqn,
        type = type.name,
        definition = definition.toGraph(),
    )

    private fun DebugInfo.ServiceDefinitionInfo.toGraph(): InsightsKontainerGraph.Definition =
        InsightsKontainerGraph.Definition(
            creates = creates.fqn,
            injectionType = injectionType.name,
            injects = injects.map { param ->
                InsightsKontainerGraph.Injection(
                    name = param.name,
                    provisionType = param.provisionType.name,
                    classes = param.classes.map { it.fqn }.sorted(),
                )
            },
            codeLocation = codeLocation.location,
            overwrites = overwrites?.toGraph(),
        )
}
