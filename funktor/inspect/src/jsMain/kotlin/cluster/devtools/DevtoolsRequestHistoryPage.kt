package io.peekandpoke.funktor.inspect.cluster.devtools

import io.peekandpoke.funktor.inspect.cluster.FunktorInspectClusterUi
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.common.roundWithPrecision
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.window
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlin.js.Date

class DevtoolsRequestHistoryPage(ctx: Ctx<Props>) : Component<DevtoolsRequestHistoryPage.Props>(ctx) {

    data class Props(
        val ui: FunktorInspectClusterUi,
    )

    // //  STATE  ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private val history by subscribingTo(DevtoolsState.RequestHistory)

    // //  IMPL   ///////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle {
            listOf("DevTools", "Request History")
        }

        ui.header H2 {
            +"Request history"
        }

        ui.basic.segment {
            ui.celled.striped.selectable.table Table {
                thead {
                    tr {
                        th { }
                        th { +"Timestamp" }
                        th { +"Status" }
                        th { +"Method" }
                        th { +"Request" }
                        th { +"Time ms" }
                        th { +"Responding Server" }
                        th { }
                    }
                }

                tbody {
                    history
                        .sortedBy { it.ts }
                        .reversed()
                        .forEachIndexed { idx, it ->

                            val insightUrl = it.detailsUrl

                            tr {
                                insightUrl?.let {
                                    onClick {
                                        window.open(insightUrl, "_blank")?.focus()
                                    }
                                }

                                td { +"${idx + 1}." }
                                td { +Date(it.ts * 1000).toISOString() }
                                td {
                                    when {
                                        it.status.isSuccess() -> ui.green.label { +"${it.status.value}" }
                                        it.status.isRedirect() -> ui.yellow.label { +"${it.status.value}" }
                                        it.status.isClientError() -> ui.purple.label { +"${it.status.value}" }
                                        it.status.isServerError() -> ui.red.label { +"${it.status.value}" }
                                        else -> ui.basic.label { +"${it.status.value}" }
                                    }
                                }
                                td { ui.label { +it.method } }
                                td { +it.url }
                                td { +(it.durationMs?.roundWithPrecision(2)?.toString() ?: "n/a") }
                                td { +it.server }
                                td {
                                    insightUrl?.let {
                                        ui.icon.button A {
                                            target = "_blank"
                                            href = insightUrl

                                            icon.search()
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}
