package io.peekandpoke.funktor.inspect.insights

import io.peekandpoke.funktor.insights.api.InsightsCollectorSlice
import io.peekandpoke.funktor.insights.api.InsightsRecord
import io.peekandpoke.funktor.insights.api.InsightsRecordRef
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.routing.href
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.key
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.first
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.serialization.json.JsonElement
import kotlin.js.Date

/**
 * One record: the overview, then a tab per collector slice.
 *
 * The headline is repeated from the list because a `BRIEF` record has no slices at all; reached by link, the page
 * would otherwise not tell it from a record that collected nothing. Prev and next stay within the record's day.
 */
class InsightsDetailPage(ctx: Ctx<Props>) : Component<InsightsDetailPage.Props>(ctx) {

    data class Props(val ui: InsightsUi, val ref: InsightsRecordRef)

    private sealed interface State {
        data object Loading : State
        data class Loaded(val record: InsightsRecord) : State
        data class Failed(val reason: String) : State
    }

    private var state: State by value(State.Loading)
    private var activeKey: String? by value(null)

    init {
        load(props.ref)

        lifecycle {
            // For a host that renders the page with a new record in place; the router mounts one per path
            onNextProps { new, old -> if (new.ref != old.ref) load(new.ref) }
        }
    }

    private fun load(ref: InsightsRecordRef) {
        state = State.Loading

        launch {
            val next = try {
                val response = props.ui.api.getRecord(ref).first()
                val record = response.data

                when {
                    !response.isSuccess() -> State.Failed(
                        response.refusal("This record does not exist, or insights are not active on this server.")
                    )
                    record == null -> State.Failed("The server sent no record.")
                    else -> State.Loaded(record)
                }
            } catch (e: Throwable) {
                State.Failed(e.message ?: e.toString())
            }

            if (props.ref == ref) {
                state = next
                // The overview first, when the record has one; else the first collector
                activeKey = (next as? State.Loaded)?.record?.let { loaded ->
                    if (hasOverview(loaded)) OVERVIEW_TAB else loaded.collectors.firstOrNull()?.key
                }
            }
        }
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Insights", props.ref.file) }

        insightsStyles()

        div(classes = "funktor-insights") {
            // The record's head in a segment; its tabs below, framed by their own box only
            ui.segment {
                toolbar((state as? State.Loaded)?.record)

                when (val current = state) {
                    is State.Loading -> ui.basic.segment { ui.active.inline.loader {} }
                    is State.Failed -> problem(current.reason)
                    is State.Loaded -> head(current.record)
                }
            }

            (state as? State.Loaded)?.record
                ?.takeIf { it.collectors.isNotEmpty() }
                ?.let { tabs(it) }
        }
    }

    private fun FlowContent.toolbar(record: InsightsRecord?) {
        // No segment around the buttons: its padding indented them from the content below
        div(classes = "insights-toolbar") {
            ui.basic.button A {
                href(props.ui.routes.list())
                icon.list()
                +"Back to list"
            }

            neighbour(record?.previous, "Previous") { icon.arrow_left() }
            neighbour(record?.next, "Next") { icon.arrow_right() }
        }
    }

    private fun FlowContent.neighbour(ref: InsightsRecordRef?, label: String, icon: FlowContent.() -> Unit) {
        when (ref) {
            null -> ui.basic.disabled.button { icon(); +label }
            else -> ui.basic.button A {
                href(props.ui.routes.detail(ref))
                icon()
                +label
            }
        }
    }

    /** The record's head: its facts, or why there is nothing more. */
    private fun FlowContent.head(record: InsightsRecord) {
        fun slice(key: String): JsonElement? = record.collectors.firstOrNull { it.key == key }?.data

        factList(overview(record, ::slice))

        if (record.collectors.isEmpty()) {
            ui.basic.segment { +"This record collected nothing -- it was recorded in BRIEF mode." }
        }
    }

    /** Whether [record] has figures for the Overview tab: the database and runtime strips. */
    private fun hasOverview(record: InsightsRecord): Boolean =
        record.collectors.any { it.key == "vault" || it.key == "runtime" }

    /** The Overview tab: the database and runtime figures at a glance. */
    private fun FlowContent.overviewTab(record: InsightsRecord) {
        fun slice(key: String): JsonElement? = record.collectors.firstOrNull { it.key == key }?.data

        readVault(slice("vault"))?.let { vault ->
            ui.dividing.header H4 { icon.database(); +"Database" }
            statStrip(vault.cells())
        }

        readRuntime(slice("runtime"))?.let { runtime ->
            ui.dividing.header H4 { icon.microchip(); +"Runtime" }
            statStrip(runtime.cells())
        }
    }

    private fun overview(record: InsightsRecord, slice: (String) -> JsonElement?): List<Fact> {
        // The reason phrase is in the response slice, not the headline
        val response = readResponse(slice("response"))
        val reason = response?.takeIf { it.status == record.status }?.statusDescription
        val build = readBuildInfo(slice("app-config"))

        return listOf(
            Fact("Request", listOfNotNull(record.method, record.path).joinToString(" ").ifEmpty { null }, full = true),
            Fact("Status", record.status?.let { if (reason == null) "$it" else "$it $reason" }, toneForStatus(record.status)),
            Fact("Duration", record.durationMs?.let { "${it.toFixed(1)} ms" }),
            Fact("Recorded", record.recordedAt?.let { Date(it.toEpochMillis()).toLocaleString() }),
        ) + (build?.facts() ?: emptyList()) // The record's OWN build, maybe older than the server's
    }

    private fun FlowContent.tabs(record: InsightsRecord) {
        val collectors = record.collectors

        ui.top.attached.tabular.menu {
            if (hasOverview(record)) {
                noui.given(activeKey == OVERVIEW_TAB) { active }.item A {
                    key = OVERVIEW_TAB
                    onClick { activeKey = OVERVIEW_TAB }
                    icon.tachometer_alternate()
                    +"Overview"
                }
            }

            collectors.forEach { slice ->
                val tab = insightsTabs[slice.key]

                noui.given(slice.key == activeKey) { active }.item A {
                    key = slice.key
                    onClick { activeKey = slice.key }
                    tab?.icon?.invoke(this)
                    // An unknown key shows its raw key: the honest label
                    +(tab?.label ?: slice.key)
                }
            }
        }

        ui.bottom.attached.with("insights-tab-body").segment {
            if (activeKey == OVERVIEW_TAB) {
                div {
                    key = "${props.ref.toPath()}/$OVERVIEW_TAB"
                    overviewTab(record)
                }
                return@segment
            }

            val active = collectors.firstOrNull { it.key == activeKey } ?: return@segment

            div {
                // A tab's state belongs to its record and slice: a new record starts it fresh
                key = "${props.ref.toPath()}/${active.key}"

                when (val tab = insightsTabs[active.key]) {
                    // An app-defined collector, or one without a tab yet
                    null -> jsonView(active.data)
                    else -> tab.render(this, active.data, props.ui)
                }
            }
        }
    }
}

/** The Overview tab's key; `#` keeps it apart from every collector key. */
private const val OVERVIEW_TAB = "#overview"
