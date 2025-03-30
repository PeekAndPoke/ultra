package de.peekandpoke.ktorfx.cluster.locks

import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ktorfx.cluster.KtorFxClusterUi
import de.peekandpoke.ktorfx.cluster.locks.api.GlobalLockEntryModel
import de.peekandpoke.ktorfx.cluster.renderDefault
import de.peekandpoke.ultra.common.datetime.formatDdMmmYyyyHhMmSs
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
        val ui: KtorFxClusterUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val loader = dataLoader {
        props.ui.api.globalLocks
            .listGlobalLocks()
            .map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("KtorFx", "Global Locks") }

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
