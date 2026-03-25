package io.peekandpoke.funktor.cluster.locks

import io.peekandpoke.funktor.cluster.FunktorClusterUi
import io.peekandpoke.funktor.cluster.locks.api.GlobalLockEntryModel
import io.peekandpoke.funktor.cluster.renderDefault
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.datetime.formatDdMmmYyyyHhMmSs
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

class GlobalLocksListPage(ctx: Ctx<Props>) : Component<GlobalLocksListPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorClusterUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val loader = dataLoader {
        props.ui.api.globalLocks
            .listGlobalLocks()
            .map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Global Locks") }

        ui.padded.segment {
            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 { +"Global Locks" }
            }

            loader.renderDefault(this) { data ->
                renderTable(data)
            }
        }
    }

    private fun FlowContent.renderTable(locks: List<GlobalLockEntryModel>) {
        ui.striped.selectable.table Table {
            thead {
                tr {
                    th { +"Key" }
                    th { +"Acquired by server" }
                    th { +"Acquired at" }
                    th { +"Expires at" }
                }
            }
            tbody {
                locks.forEach { lock ->

                    tr {
                        td { // Key
                            +lock.key
                        }
                        td { // Server Id
                            +lock.serverId
                        }
                        td { // Last Ping
                            +lock.created.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                        }
                        td { // Expires at
                            +lock.expires.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                        }
                    }
                }
            }
        }
    }
}
