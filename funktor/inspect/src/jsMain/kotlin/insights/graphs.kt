package io.peekandpoke.funktor.inspect.insights

import io.peekandpoke.funktor.insights.api.InsightsApiClient
import io.peekandpoke.funktor.insights.api.InsightsDatabaseGraph
import io.peekandpoke.funktor.insights.api.InsightsKontainerGraph
import io.peekandpoke.kraft.addons.visnetwork.VisEdge
import io.peekandpoke.kraft.addons.visnetwork.VisNetworkComponent
import io.peekandpoke.kraft.addons.visnetwork.VisNetworkData
import io.peekandpoke.kraft.addons.visnetwork.VisNetworkOptions
import io.peekandpoke.kraft.addons.visnetwork.VisNode
import io.peekandpoke.kraft.utils.jsObject
import io.peekandpoke.ultra.remote.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlin.js.Date

/**
 * The insights graphs (ULTRA-00198): what they are drawn from, the vis-network data, and the cache the tabs fetch
 * them through. Ported from the Vue GUI's `graphs.ts`: same colours, sizes, arrows, dashes and physics.
 *
 * Labels are drawn on a canvas; no node or edge carries a `title`.
 */

/** Which graph a network is: each had its own physics in the old GUI. */
internal enum class NetworkKind { Instances, Full, Database }

/**
 * One network, built once and kept by the tab while what it is drawn from stays the same: [VisNetworkComponent]
 * updates the live network whenever [data] or [options] is another object, so a rebuild per render would lay the
 * graph out again on every sort click. vis-network keeps [data]'s node and edge objects and writes ids into the edges.
 */
internal class NetworkGraph(
    val kind: NetworkKind,
    val data: VisNetworkData,
    val nodeCount: Int,
    val edgeCount: Int,
    /** Edges not drawn because one end is no node, e.g. an injection whose declared type is no service key. */
    val undrawn: Int,
) {
    val options: VisNetworkOptions = networkOptions(kind)
}

/** The old GUI's colours, by provider type. */
private val typeColours = mapOf(
    "Singleton" to "#1E90FF",
    "Prototype" to "#FFD700",
    "Dynamic" to "#8B0000",
    "SemiDynamic" to "#9400D3",
    "DynamicOverride" to "#DC143C",
)

private fun node(id: Int, label: String, value: Int, color: String? = null) = jsObject<VisNode> {
    this.id = id
    this.label = label
    this.value = value
    color?.let { this.color = it }
}

private fun edge(from: Int, to: Int, dashes: Boolean, value: Int, label: String? = null) = jsObject<VisEdge> {
    this.from = from
    this.to = to
    this.arrows = "to"
    this.dashes = dashes
    this.value = value
    label?.let { this.label = it }
}

private fun network(kind: NetworkKind, nodes: List<VisNode>, edges: List<VisEdge>, undrawn: Int) = NetworkGraph(
    kind = kind,
    data = jsObject {
        this.nodes = nodes.toTypedArray()
        this.edges = edges.toTypedArray()
    },
    nodeCount = nodes.size,
    edgeCount = edges.size,
    undrawn = undrawn,
)

/** One node per service, one edge per injected class: dashed and thinner when the injection is `Lazy`. */
internal fun kontainerNetwork(services: List<KontainerService>, kind: NetworkKind): NetworkGraph {
    val ids = LinkedHashMap<String, Int>()
    services.forEach { service -> service.cls?.let { if (it !in ids) ids[it] = ids.size + 1 } }

    val nodes = mutableListOf<VisNode>()
    val edges = mutableListOf<VisEdge>()
    var undrawn = 0

    services.forEach { service ->
        val id = service.cls?.let { ids[it] } ?: return@forEach
        val injects = service.definition?.injects ?: emptyList()
        val type = service.type ?: "?"

        nodes += node(
            id = id,
            label = "${shortName(service.cls)}\n$type",
            value = injects.sumOf { it.classes.size },
            color = typeColours[type],
        )

        injects.forEach { inject ->
            val lazy = inject.provisionType == "Lazy"
            inject.classes.forEach { cls ->
                when (val to = ids[cls]) {
                    null -> undrawn++
                    else -> edges += edge(from = id, to = to, dashes = lazy, value = if (lazy) 1 else 2)
                }
            }
        }
    }

    return network(kind, nodes, edges, undrawn)
}

/** One node per repository, sized by its references; one edge per referenced repository, labelled with the count. */
internal fun databaseNetwork(graph: InsightsDatabaseGraph): NetworkGraph {
    fun key(id: InsightsDatabaseGraph.Repo.Id) = "${id.name}::${id.connection}"

    val ids = LinkedHashMap<String, Int>()
    graph.repos.forEach { repo -> key(repo.id).let { if (it !in ids) ids[it] = ids.size + 1 } }

    val nodes = mutableListOf<VisNode>()
    val edges = mutableListOf<VisEdge>()
    var undrawn = 0

    graph.repos.forEach { repo ->
        val id = ids.getValue(key(repo.id))
        val references = repo.storedClasses.flatMap { it.references }

        nodes += node(id = id, label = "${repo.id.name}\n${repo.id.connection}", value = references.size)

        references.mapNotNull { it.repo }.groupingBy { key(it) }.eachCount().forEach { (target, count) ->
            when (val from = ids[target]) {
                null -> undrawn++
                else -> edges += edge(from = from, to = id, dashes = false, value = count, label = "$count")
            }
        }
    }

    return network(NetworkKind.Database, nodes, edges, undrawn)
}

/**
 * The old GUI's vis.js options per graph. `improvedLayout = false` is load-bearing: a few hundred nodes take seconds.
 *
 * The per-kind values are computed OUT here: inside `jsObject<dynamic> { }` the receiver is dynamic, so a name like
 * `NetworkKind` resolves as a property of the JS object being built, `undefined`.
 */
internal fun networkOptions(kind: NetworkKind): VisNetworkOptions {
    val solverName = if (kind == NetworkKind.Full) "barnesHut" else "forceAtlas2Based"
    val barnesHutOverlap = if (kind == NetworkKind.Instances) 0.1 else 0.0
    val forceAtlasOverlap = if (kind == NetworkKind.Full) 0.0 else 0.25

    return jsObject {
        layout = jsObject<dynamic> { improvedLayout = false }
        physics = jsObject<dynamic> {
            solver = solverName
            barnesHut = jsObject<dynamic> {
                gravitationalConstant = -2000
                centralGravity = 0.3
                springLength = 95
                springConstant = 0.04
                damping = 0.09
                avoidOverlap = barnesHutOverlap
            }
            forceAtlas2Based = jsObject<dynamic> {
                gravitationalConstant = -50
                centralGravity = 0.01
                springConstant = 0.08
                springLength = 100
                damping = 0.4
                avoidOverlap = forceAtlasOverlap
            }
            maxVelocity = 50
            minVelocity = 0.75
        }
        nodes = jsObject<dynamic> {
            shape = "dot"
            scaling = jsObject<dynamic> { min = 10; max = 50 }
        }
        edges = jsObject<dynamic> {
            scaling = jsObject<dynamic> { min = 1; max = 2 }
        }
    }
}

//  Where a tab gets the graphs  ///////////////////////////////////////////////////////////////////////////////////////

/** A graph and when it was fetched, or why there is none. A failure is a value: the tab renders on without it. */
internal sealed class GraphResult<out T> {
    data class Ok<T>(val value: T, val fetchedAt: Date) : GraphResult<T>()
    data class Failed(val reason: String) : GraphResult<Nothing>()
}

/**
 * Fetches the two graph endpoints for the tabs, lazily: nothing is asked until a tab asks.
 *
 * One per [InsightsUi], so going back to the list and into another record reuses a graph already fetched. Askers
 * while a fetch is running share it. A failed fetch is not kept: the next click asks again. Never throws.
 */
internal class InsightsGraphSource(private val api: InsightsApiClient) {

    /** Fetches outlive the tab that started them: another tab may be waiting for the same one. */
    private val scope = CoroutineScope(SupervisorJob())

    private val kontainerInFlight = mutableMapOf<String?, Deferred<GraphResult<KontainerGraphData>>>()

    private var databaseInFlight: Deferred<GraphResult<InsightsDatabaseGraph>>? = null

    /** Successful answers, by the fingerprint the SERVER sent. */
    private val kontainerByFingerprint = mutableMapOf<String, GraphResult.Ok<KontainerGraphData>>()

    /** Fingerprints a record named that were asked for once already, so a redeployed server is not asked again. */
    private val asked = mutableMapOf<String?, GraphResult.Ok<KontainerGraphData>>()

    private var latestKontainer: GraphResult.Ok<KontainerGraphData>? = null

    private var database: GraphResult.Ok<InsightsDatabaseGraph>? = null

    /**
     * The kontainer graph for a record written against [fingerprint] (null: the record names none).
     *
     * A fingerprint not yet seen is fetched ONCE: after a redeploy the server serves another graph. If it still
     * answers another fingerprint, that answer is kept for [fingerprint]. A record naming none takes the latest.
     */
    suspend fun kontainer(fingerprint: String?): GraphResult<KontainerGraphData> {
        val known = if (fingerprint == null) latestKontainer else kontainerByFingerprint[fingerprint]
        known?.let { return it }
        asked[fingerprint]?.let { return it }

        val pending = kontainerInFlight.getOrPut(fingerprint) {
            scope.async {
                fetch({ api.getKontainerGraph().first() }) { it.toGraphData() }.also { result ->
                    if (result is GraphResult.Ok) {
                        result.value.fingerprint?.let { kontainerByFingerprint[it] = result }
                        latestKontainer = result
                        asked[fingerprint] = result
                    }
                }
            }
        }

        return pending.await().also {
            if (kontainerInFlight[fingerprint] === pending) kontainerInFlight.remove(fingerprint)
        }
    }

    /** The database graph, fetched once: it has no fingerprint, nothing joins it. */
    suspend fun database(): GraphResult<InsightsDatabaseGraph> {
        database?.let { return it }

        val pending = databaseInFlight ?: scope.async {
            fetch({ api.getDatabaseGraph().first() }) { it }.also { if (it is GraphResult.Ok) database = it }
        }.also { databaseInFlight = it }

        return pending.await().also { if (databaseInFlight === pending) databaseInFlight = null }
    }

    private suspend fun <R, T> fetch(load: suspend () -> ApiResponse<R>, map: (R) -> T): GraphResult<T> = try {
        val response = load()
        val data = response.data

        when {
            !response.isSuccess() -> GraphResult.Failed(response.refusal(INSIGHTS_OFF))
            data == null -> GraphResult.Failed("The server sent no graph.")
            else -> GraphResult.Ok(map(data), Date())
        }
    } catch (e: Throwable) {
        // No envelope at all: a network failure, or an older server without the endpoint
        GraphResult.Failed(e.message ?: e.toString())
    }
}

/** The API's answer to every call while insights are off; for a record, also a missing one. */
internal const val INSIGHTS_OFF = "Insights are not active on this server: it answers 404 to every insights call."

/** [refusal], saying what a 404 means here: [meaning]. */
internal fun ApiResponse<*>.refusal(meaning: String): String =
    if (status.value == 404) "$meaning (${refusal()})" else refusal()

/** Why a response is not a success, for a notice. */
internal fun ApiResponse<*>.refusal(): String {
    val texts = messages?.map { it.text }?.filter { it.isNotBlank() }?.joinToString("; ")

    return "${status.value} ${status.description}${if (texts.isNullOrEmpty()) "" else " -- $texts"}"
}

/** The typed endpoint answer as the reader's shape, so a joined service looks like a read one. */
private fun InsightsKontainerGraph.toGraphData() = KontainerGraphData(
    fingerprint = fingerprint,
    services = services.map { service ->
        KontainerService(
            cls = service.cls,
            type = service.type,
            definition = service.definition.toDefinition(),
            instances = emptyList(),
        )
    },
)

private fun InsightsKontainerGraph.Definition.toDefinition(): KontainerDefinition = KontainerDefinition(
    creates = creates,
    injectionType = injectionType,
    injects = injects.map { KontainerInjection(name = it.name, classes = it.classes, provisionType = it.provisionType) },
    codeLocation = codeLocation,
    overwrites = overwrites?.toDefinition(),
)
