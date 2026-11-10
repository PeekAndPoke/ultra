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
            // The router keeps this page for prev / next and only hands it the new record
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
                    !response.isSuccess() -> State.Failed(response.refusal())
                    // Null on a 2xx is what a missing record is
                    record == null -> State.Failed("This record no longer exists.")
                    else -> State.Loaded(record)
                }
            } catch (e: Throwable) {
                State.Failed(e.message ?: e.toString())
            }

            if (props.ref == ref) {
                state = next
                activeKey = (next as? State.Loaded)?.record?.collectors?.firstOrNull()?.key
            }
        }
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Insights", props.ref.file) }

        insightsStyles()

        ui.padded.segment {
            div(classes = "funktor-insights") {
                toolbar((state as? State.Loaded)?.record)

                when (val current = state) {
                    is State.Loading -> ui.basic.segment { ui.active.inline.loader {} }
                    is State.Failed -> problem(current.reason)
                    is State.Loaded -> record(current.record)
                }
            }
        }
    }

    private fun FlowContent.toolbar(record: InsightsRecord?) {
        ui.basic.segment {
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

    private fun FlowContent.record(record: InsightsRecord) {
        fun slice(key: String): JsonElement? = record.collectors.firstOrNull { it.key == key }?.data

        factList(overview(record, ::slice))
        note(
            "Neighbours are within the same day folder, so the oldest record of a day reports no previous even " +
                "when earlier days exist."
        )

        readVault(slice("vault"))?.let { vault ->
            ui.dividing.header H4 { icon.database(); +"Database" }
            statStrip(vault.cells())
        }

        readRuntime(slice("runtime"))?.let { runtime ->
            ui.dividing.header H4 { icon.microchip(); +"Runtime" }
            statStrip(runtime.cells())
        }

        if (record.collectors.isEmpty()) {
            ui.basic.segment { +"This record collected nothing -- it was recorded in BRIEF mode." }
            return
        }

        tabs(record.collectors)
    }

    private fun overview(record: InsightsRecord, slice: (String) -> JsonElement?): List<Fact> {
        // The reason phrase is in the response slice, not the headline
        val response = readResponse(slice("response"))
        val reason = response?.takeIf { it.status == record.status }?.statusDescription
        val build = readBuildInfo(slice("app-config"))

        return listOf(
            Fact("Request", listOfNotNull(record.method, record.path).joinToString(" ").ifEmpty { null }),
            Fact("Status", record.status?.let { if (reason == null) "$it" else "$it $reason" }, toneForStatus(record.status)),
            Fact("Duration", record.durationMs?.let { "${it.toFixed(1)} ms" }),
            Fact("Recorded", record.recordedAt?.let { Date(it.toEpochMillis()).toLocaleString() }),
        ) + (build?.facts() ?: emptyList()) // The record's OWN build, maybe older than the server's
    }

    private fun FlowContent.tabs(collectors: List<InsightsCollectorSlice>) {
        ui.top.attached.tabular.menu {
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

        ui.bottom.attached.segment {
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
