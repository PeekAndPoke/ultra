package io.peekandpoke.funktor.inspect.insights

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.key
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.TBODY
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.span
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.serialization.json.JsonElement
import kotlin.js.Date

@Suppress("FunctionName")
internal fun Tag.KontainerTab(data: JsonElement, ui: InsightsUi) =
    comp(KontainerTab.Props(data, ui)) { KontainerTab(it) }

/**
 * The `kontainer` slice: which services this request's kontainer instantiated, and the service graph (ULTRA-00198,
 * as the Vue `KontainerTab`).
 *
 * A `full` record (before 2026-10-19) carries every definition; a `slim` one only the instantiated services and
 * the fingerprint of the graph it was written against. Its definitions come from the kontainer graph endpoint,
 * joined on `cls`, fetched on the first graph click and cached by fingerprint. A mismatch is said, not hidden.
 *
 * A click on a column header sorts by it, again reverses. Without the graph the tab still renders.
 */
internal class KontainerTab(ctx: Ctx<Props>) : Component<KontainerTab.Props>(ctx) {

    data class Props(val data: JsonElement, val ui: InsightsUi)

    private enum class Column(val label: String) {
        Service("Service"),
        Type("Type"),
        Instances("Instances"),
        Injects("Injects"),
        Definition("Definition & overwrites"),
    }

    private data class Sort(val column: Column, val descending: Boolean)

    private var graph: KontainerGraphData? by value(null)
    private var fetchedAt: Date? by value(null)
    private var graphProblem: String? by value(null)
    private var loading: Boolean by value(false)
    private var shown: NetworkKind? by value(null)
    private var sortBy: Sort? by value(null)

    private fun toggle(slice: KontainerSlice, which: NetworkKind) {
        shown = if (shown == which) null else which

        if (shown == null || slice.shape == KontainerShape.Full || graph != null || loading) return

        loading = true
        graphProblem = null

        launch {
            when (val result = props.ui.graphs.kontainer(slice.graphFingerprint)) {
                is GraphResult.Ok -> {
                    graph = result.value
                    fetchedAt = result.fetchedAt
                }

                is GraphResult.Failed -> graphProblem = "The kontainer graph is not available: ${result.reason}"
            }
            loading = false
        }
    }

    override fun VDom.render() {
        val slice = readKontainer(props.data) ?: return jsonView(props.data)

        factList(summary(slice))
        note(
            "A Singleton is process state, shared by every request: created at boot or by whichever request first " +
                "needed it. Every other type belongs to this request's kontainer. The creation time only says " +
                "whether this request was the one that created the instance."
        )
        ui.hidden.divider()

        graphs(slice)
        ui.hidden.divider()

        if (slice.shape == KontainerShape.Slim && graph == null) {
            note("Injects and definitions are not part of this record. Show a graph to load them from the server; the table fills in too.")
        }

        table(sorted(services(slice)))

        rawSlice(
            props.data,
            if (slice.shape == KontainerShape.Full) "Raw slice -- large, every definition inline" else "Raw slice",
        )
    }

    private fun summary(slice: KontainerSlice): List<Fact> {
        val instantiated = slice.services.filter { it.instances.isNotEmpty() }
        val shared = instantiated.count { it.isProcessWide }
        val own = "${instantiated.size} instantiated: ${instantiated.size - shared} this request's own, " +
            "$shared process-wide singletons"

        return listOfNotNull(
            Fact(
                "Record shape",
                when (slice.shape) {
                    KontainerShape.Full -> "full -- every definition inline (written before 2026-10-19)"
                    KontainerShape.Slim -> "slim -- instantiated services only; definitions come from the kontainer graph"
                },
            ),
            // The old bar's young / old / total; never measured, funktor never switches kontainer tracking on
            if (slice.shape == KontainerShape.Full) {
                Fact("Kontainers (young / old / total)", "${counts(slice)} -- not measured, tracking is never enabled")
            } else {
                null
            },
            Fact("Services", if (slice.shape == KontainerShape.Full) "${slice.services.size} defined, $own" else own),
            if (slice.shape == KontainerShape.Slim) {
                Fact("Graph fingerprint", slice.graphFingerprint ?: "none -- the server could not build the graph")
            } else {
                null
            },
        )
    }

    private fun counts(slice: KontainerSlice): String {
        val young = if (slice.numTotal == null || slice.numOld == null) "?" else (slice.numTotal - slice.numOld).formatNumber()

        return "$young / ${slice.numOld?.formatNumber() ?: "?"} / ${slice.numTotal?.formatNumber() ?: "?"}"
    }

    /** A `slim` record's services joined with their definitions once the graph is in. */
    private fun services(slice: KontainerSlice): List<KontainerService> {
        val loaded = graph
        return if (slice.shape == KontainerShape.Full || loaded == null) slice.services else slice.joinGraph(loaded, false)
    }

    private fun FlowContent.graphs(slice: KontainerSlice) {
        ui.green.given(shown == NetworkKind.Instances) { active }.button {
            onClick { toggle(slice, NetworkKind.Instances) }
            +"Show instances Graph"
        }
        ui.red.given(shown == NetworkKind.Full) { active }.button {
            onClick { toggle(slice, NetworkKind.Full) }
            +"Show Full Graph"
        }

        if (loading) ui.basic.segment { +"Loading the kontainer graph..." }
        graphProblem?.let { problem(it) }

        val loaded = graph
        val at = fetchedAt?.toLocaleString() ?: "n/a"

        when (loaded?.let { slice.matches(it) }) {
            GraphMatch.Other -> notice(
                "This record was written against another kontainer graph (fingerprint ${slice.graphFingerprint}). " +
                    "Asked for it at $at, the server answered ${loaded.fingerprint ?: "none"}: the definitions and " +
                    "edges shown are that graph's, not this record's. Expected after almost any deploy: the " +
                    "fingerprint covers line numbers."
            )

            GraphMatch.Unknown -> notice(
                "This record names no kontainer graph -- the server could not build one when it was written -- so " +
                    "whether the server's graph, fetched at $at, matches it cannot be verified. The definitions and " +
                    "edges shown are that graph's."
            )

            else -> Unit
        }

        val which = shown ?: return
        val network = when {
            slice.shape == KontainerShape.Full -> kontainerNetwork(
                if (which == NetworkKind.Instances) slice.services.filter { it.instances.isNotEmpty() } else slice.services,
                which,
            )

            loaded != null -> kontainerNetwork(slice.joinGraph(loaded, which == NetworkKind.Full), which)
            else -> null
        }

        // Keyed by the graph shown: switching graphs draws a new network rather than morphing the old one
        network?.let { div { key = which.name; networkGraph(it) } }
    }

    private fun sortKey(service: KontainerService, column: Column): Comparable<*> = when (column) {
        Column.Service -> service.cls ?: ""
        Column.Type -> service.type ?: ""
        Column.Instances -> service.instances.size
        Column.Injects -> service.definition?.injects?.size ?: 0
        // What the cell shows, not the hidden FQN: sorting by package would look unsorted
        Column.Definition -> service.definition?.let { "${it.injectionType ?: "?"} - ${shortName(it.creates)}" } ?: ""
    }

    /** Stable, so equal keys keep the reader's order: instantiated first, then by name. */
    private fun sorted(services: List<KontainerService>): List<KontainerService> {
        val by = sortBy ?: return services

        @Suppress("UNCHECKED_CAST")
        val comparator = compareBy<KontainerService> { sortKey(it, by.column) as Comparable<Any> }

        return services.sortedWith(if (by.descending) comparator.reversed() else comparator)
    }

    private fun sortOn(column: Column) {
        val by = sortBy
        sortBy = Sort(column, descending = by?.column == column && !by.descending)
    }

    private fun FlowContent.table(services: List<KontainerService>) {
        ui.very.compact.celled.sortable.table Table {
            thead {
                tr {
                    Column.entries.forEach { column ->
                        val by = sortBy
                        val sorted = when {
                            by?.column != column -> ""
                            by.descending -> " descending sorted"
                            else -> " ascending sorted"
                        }

                        th(classes = sorted.trim()) {
                            onClick { sortOn(column) }
                            +column.label
                        }
                    }
                }
            }
            tbody {
                services.forEach { service -> row(service) }
            }
        }
    }

    private fun TBODY.row(service: KontainerService) {
        tr {
            key = service.cls ?: ""

            td(classes = "insights-mono insights-path") { +(service.cls ?: "n/a") }
            td {
                ui.basic.label { +(service.type ?: "n/a") }
                if (service.instances.isNotEmpty()) {
                    div { muted(if (service.isProcessWide) "process-wide" else "this request's") }
                }
            }
            td(classes = "insights-mono") {
                if (service.instances.isEmpty()) muted("-")
                service.instances.forEach { instance ->
                    div {
                        span {
                            title = instance.cls ?: ""
                            +shortName(instance.cls)
                        }
                        +" "
                        muted(instance.createdAtMillis?.let { Date(it).toLocaleString() } ?: "n/a")
                    }
                }
            }
            td(classes = "insights-mono") {
                val injects = service.definition?.injects ?: emptyList()
                if (injects.isEmpty()) muted("-")
                injects.forEach { inject ->
                    div {
                        +"${inject.name ?: "?"}: "
                        span {
                            title = inject.classes.joinToString(", ")
                            muted(inject.classes.joinToString(", ") { shortName(it) }.ifEmpty { "n/a" })
                        }
                        // Lazy injection is why a singleton can hold a request-scoped service
                        if (inject.provisionType == "Lazy") ui.mini.basic.label { +"Lazy" }
                    }
                }
            }
            td(classes = "insights-mono") {
                val definition = service.definition
                if (definition == null) muted("-")
                definition?.chain()?.forEachIndexed { index, def ->
                    div {
                        if (index > 0) muted("Overwrites -> ")
                        +"${def.injectionType ?: "?"} - "
                        span {
                            title = def.creates ?: ""
                            +shortName(def.creates)
                        }
                        def.codeLocation?.let { location -> div { muted(location) } }
                    }
                }
            }
        }
    }
}
