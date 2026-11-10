package io.peekandpoke.funktor.inspect.insights

import io.peekandpoke.funktor.insights.api.InsightsDatabaseGraph
import io.peekandpoke.kraft.addons.visnetwork.VisNetwork
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.key
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.serialization.json.JsonElement
import kotlin.js.Date

@Suppress("FunctionName")
internal fun Tag.VaultTab(data: JsonElement, ui: InsightsUi) = comp(VaultTab.Props(data, ui)) { VaultTab(it) }

/**
 * The `vault` slice: the database queries this request ran (ULTRA-00198, as the Vue `VaultTab`).
 *
 * Values are shown except where the server withheld them (ULTRA-00171): the backend decides, this tab only says
 * so. A record from before `VaultCollector.Data` (`legacy`) has its text hidden.
 *
 * **No raw fallback**, unlike every other tab: an unreadable slice may be a pre-collector one, written whole with
 * the values of every repository, the withholding ones too.
 *
 * The database graph is the server's schema, not the record's: fetched on the first click of the button or of
 * "Database Graph Data", once per [InsightsUi].
 */
internal class VaultTab(ctx: Ctx<Props>) : Component<VaultTab.Props>(ctx) {

    data class Props(val data: JsonElement, val ui: InsightsUi)

    private var graph: InsightsDatabaseGraph? by value(null)
    /** [graph] as indented JSON, encoded once when it arrives. */
    private var graphText: String? = null
    private var fetchedAt: Date? by value(null)
    private var graphProblem: String? by value(null)
    private var loading: Boolean by value(false)
    private var showGraph: Boolean by value(false)
    private var showData: Boolean by value(false)

    /** The network drawn from [graph], built once: see [NetworkGraph]. */
    private var drawn: Pair<InsightsDatabaseGraph, NetworkGraph>? = null

    private fun network(loaded: InsightsDatabaseGraph): NetworkGraph =
        drawn?.takeIf { it.first === loaded }?.second ?: databaseNetwork(loaded).also { drawn = loaded to it }

    private fun loadGraph() {
        if (graph != null || loading) return

        loading = true
        graphProblem = null

        launch {
            when (val result = props.ui.graphs.database()) {
                is GraphResult.Ok -> {
                    graphText = prettyJson.encodeToString(InsightsDatabaseGraph.serializer(), result.value)
                    graph = result.value
                    fetchedAt = result.fetchedAt
                }

                is GraphResult.Failed -> graphProblem = "The database graph is not available: ${result.reason}"
            }
            loading = false
        }
    }

    override fun VDom.render() {
        val slice = readVault(props.data)

        if (slice == null) {
            notice(
                "This database slice could not be read. The raw view is suppressed: an unreadable slice may be an " +
                    "old one that holds the values of repositories that withhold them now."
            )
            return
        }

        statStrip(slice.cells())
        ui.hidden.divider()

        // Keyed: the graph section grows and shrinks, and must not shift the queries' Prism blocks
        div { key = "graph"; graphSection() }

        if (slice.entries.isEmpty()) {
            ui.basic.segment { key = "no-queries"; +"No database queries ran for this request." }
        }

        slice.entries.forEachIndexed { index, entry -> query(index, entry) }

        div { key = "graph-data"; graphData() }
    }

    private fun FlowContent.graphSection() {
        ui.red.given(showGraph) { active }.button {
            onClick {
                showGraph = !showGraph
                if (showGraph) loadGraph()
            }
            +"Show Database Graph"
        }

        note(
            "The database graph is the server's schema" +
                (fetchedAt?.let { " as fetched at ${it.toLocaleString()}" } ?: "") +
                ", not part of this record. It is fetched once per page."
        )

        if (loading) ui.basic.segment { +"Loading the database graph..." }
        graphProblem?.let { problem(it) }

        val loaded = graph
        if (showGraph && loaded != null) networkGraph(network(loaded))
    }

    private fun FlowContent.query(index: Int, entry: VaultEntry) {
        ui.segment {
            key = "query-$index"
            ui.small.header {
                +"Query #${index + 1} took ${nsToMs(entry.totalNs)}"
                ui.sub.header { +(entry.connection ?: "unknown connection") }
            }

            statStrip(
                listOf(
                    StatCell("Results", "${(entry.count ?: 0.0).formatNumber()} of ${entry.totalCount?.formatNumber() ?: "n/a"}"),
                    StatCell("Bind vars", entry.varsCount?.formatNumber() ?: "unknown"),
                    StatCell("Serializer", entry.serializer.format()),
                    StatCell("Query", entry.query_.format()),
                    StatCell("Iterator", entry.iterator.format()),
                    StatCell("Deserializer", entry.deserializer.format()),
                    StatCell("Explain", entry.explain.format()),
                )
            )

            if (entry.legacy) {
                notice(
                    "Query text is hidden: this record predates the collector that guarantees bind values are kept " +
                        "out of it."
                )
            } else {
                codeBlock(
                    text = entry.query,
                    language = queryLanguage(entry),
                    emptyText = emptyQueryText(entry),
                    wrap = true,
                )
            }

            val values = entry.values

            when {
                entry.valuesWithheld -> notice(
                    "Values withheld: this repository holds credentials, so the server recorded only that the " +
                        "query had ${entry.varsCount?.formatNumber() ?: "some"} bind " +
                        (if (entry.varsCount == 1.0) "variable." else "variables.")
                )

                values != null && values.isNotEmpty() ->
                    codeBlock(text = valuesText(values), language = "json", wrap = true)
            }

            entry.plan?.let { plan ->
                collapsed("Explained") {
                    codeBlock(text = plan, language = if (plan.trimStart().startsWith("{")) "json" else null, wrap = true)
                }
            }
        }
    }

    private fun FlowContent.graphData() {
        ui.basic.given(showData) { active }.button {
            onClick {
                showData = !showData
                if (showData) loadGraph()
            }
            +"Database Graph Data"
        }

        if (!showData) return

        val text = graphText
        val reason = graphProblem

        when {
            graph != null && text != null -> codeBlock(text = text, language = "json")

            reason != null -> problem(reason)
            else -> ui.basic.segment { +"Loading the database graph..." }
        }
    }

    /** The Prism language of a query's text: AQL for Arango, JSON for Monko's printed filter. */
    private fun queryLanguage(entry: VaultEntry): String? = when (entry.queryLanguage) {
        "aql", "aql-raw" -> "aql"
        "mongo-bound", "json" -> "json"
        else -> null
    }

    private fun emptyQueryText(entry: VaultEntry): String {
        val language = entry.queryLanguage ?: "unknown language"

        return when {
            entry.queryLanguage == "json" ->
                "No query text recorded: written before 2026-10-19, when Monko still put the bind values into its " +
                    "query text, so the server withheld it and did not count the bind variables."

            entry.valuesWithheld ->
                "No query text recorded ($language): this repository withholds its values, and the text may hold " +
                    "them as literals."

            else ->
                "No query text recorded ($language). Either the query failed before it was printed, or the record " +
                    "is from before 2026-11-10, when the server withheld text that could hold values."
        }
    }
}

/** A drawn graph with its counts; a failure to draw stays inside the graph's box. */
internal fun FlowContent.networkGraph(graph: NetworkGraph) {
    note(
        "${graph.nodeCount} nodes, ${graph.edgeCount} edges" +
            (if (graph.undrawn > 0) ", ${graph.undrawn} not drawn: their target is no node in this graph" else "") +
            "."
    )

    VisNetwork(data = graph.data, options = graph.options, height = "600px")
}
