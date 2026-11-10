package io.peekandpoke.funktor.inspect.insights

import io.peekandpoke.funktor.insights.api.InsightsRecordSummary
import io.peekandpoke.funktor.insights.api.InsightsServerInfo
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.key
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.model.Paged
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.first
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlin.js.Date
import kotlin.math.ceil
import kotlin.math.max

/**
 * The record list, where the deleted insights bar's job landed: status and timing for every request.
 *
 * The header shows the RUNNING server's git version, application id and environment, as the bar did. `path` is a
 * path, never a URL with its query string; it is attacker-controlled and rendered as text.
 */
class InsightsListPage(ctx: Ctx<Props>) : Component<InsightsListPage.Props>(ctx) {

    data class Props(val ui: InsightsUi)

    private sealed interface State {
        data object Loading : State
        data class Loaded(val paged: Paged<InsightsRecordSummary>) : State
        data class Failed(val reason: String) : State
    }

    private var page: Int by value(1)
    private var state: State by value(State.Loading)
    private var server: List<Fact> by value(emptyList())

    init {
        load()
        loadServer()
    }

    private fun load() {
        val wanted = page
        state = State.Loading

        launch {
            val next = try {
                val response = props.ui.api.listRecords(page = wanted, epp = props.ui.epp).first()
                val data = response.data

                when {
                    !response.isSuccess() -> State.Failed(response.refusal(INSIGHTS_OFF))
                    data == null -> State.Failed("The server sent no records.")
                    else -> State.Loaded(data)
                }
            } catch (e: Throwable) {
                // No envelope at all: a network failure, or a proxy's error page
                State.Failed(e.message ?: e.toString())
            }

            if (page == wanted) state = next
        }
    }

    private fun loadServer() {
        launch {
            try {
                val response: ApiResponse<InsightsServerInfo> = props.ui.api.getServerInfo().first()
                val info = response.data
                if (response.isSuccess() && info != null) {
                    server = serverBuildInfo(info.git, info.gitDesc, info.applicationId, info.environment).facts()
                }
            } catch (_: Throwable) {
                // The header is a convenience: the list works without it
            }
        }
    }

    private fun goTo(next: Int) {
        page = next
        load()
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Insights") }

        insightsStyles()

        ui.padded.segment {
            div(classes = "funktor-insights") {
                ui.dividing.header H2 {
                    icon.chart_bar_outline()
                    +"Recorded requests"
                }

                if (server.isNotEmpty()) {
                    ui.small.header { icon.code_branch(); +"Server" }
                    factList(server)
                }

                when (val current = state) {
                    is State.Loading -> ui.basic.segment { ui.active.inline.loader {} }
                    is State.Failed -> {
                        problem(current.reason)
                        ui.basic.button {
                            onClick { load() }
                            icon.redo()
                            +"Retry"
                        }
                        if (page > 1) {
                            ui.basic.button {
                                onClick { goTo(page - 1) }
                                icon.arrow_left()
                                +"Previous"
                            }
                        }
                    }

                    is State.Loaded -> {
                        if (current.paged.items.isEmpty()) ui.basic.segment { +"No records" } else records(current.paged.items)
                        pager(current.paged)
                    }
                }
            }
        }
    }

    private fun FlowContent.records(items: List<InsightsRecordSummary>) {
        ui.very.compact.selectable.celled.with("insights-records").table Table {

            thead {
                tr {
                    th { +"Recorded" }
                    th { +"Method" }
                    th { +"Path" }
                    th { +"Status" }
                    th { +"Duration" }
                }
            }
            tbody {
                items.forEach { summary ->
                    tr {
                        key = summary.ref.toPath()
                        onClick { event -> router.navToUri(evt = event, route = props.ui.routes.detail(summary.ref)) }

                        td(classes = "insights-mono") {
                            +(summary.recordedAt?.let { Date(it.toEpochMillis()).toLocaleString() } ?: "n/a")
                        }
                        td(classes = "insights-mono") { +(summary.method ?: "n/a") }
                        td(classes = "insights-mono insights-path") { +(summary.path ?: "n/a") }
                        td { toneLabel(toneForStatus(summary.status), summary.status?.toString() ?: "n/a") }
                        td {
                            // The old bar's bands, minus its fourth: yellow above 150 ms, red above 300
                            toneLabel(
                                toneAbove(summary.durationMs, warn = 150.0, error = 300.0),
                                summary.durationMs?.let { "${it.toFixed(1)} ms" } ?: "n/a",
                            )
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.pager(paged: Paged<InsightsRecordSummary>) {
        // The server's coerced page size, not ours: epp is clamped to 200
        val epp = max(1, paged.epp)
        val total = paged.fullItemCount
        // Not `items.size >= epp`: the server skips a half-written record but still consumes its slot
        val hasNext = if (total == null) paged.items.size >= epp else page.toLong() * epp < total

        ui.basic.segment {
            ui.basic.given(page <= 1) { disabled }.button {
                onClick { if (page > 1) goTo(page - 1) }
                icon.arrow_left()
                +"Previous"
            }

            +"Page $page"
            total?.let { +" of ${max(1L, ceil(it.toDouble() / epp).toLong())}" }
            +" "

            ui.basic.given(!hasNext) { disabled }.button {
                onClick { if (hasNext) goTo(page + 1) }
                +"Next"
                icon.arrow_right()
            }
        }
    }
}
