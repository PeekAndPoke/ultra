package io.peekandpoke.funktor.inspect.cluster.locks

import io.peekandpoke.funktor.inspect.cluster.FunktorInspectClusterUi
import io.peekandpoke.funktor.inspect.cluster.locks.api.ServerBeaconModel
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.datetime.MpInstant
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

class ServerBeaconsListPage(ctx: Ctx<Props>) : Component<ServerBeaconsListPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorInspectClusterUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val loader = dataLoader {
        props.ui.api.globalLocks
            .listServerBeacons()
            .map {
                it.data!! to MpInstant.now()
            }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Server Beacons") }

        ui.padded.segment {
            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 {
                    +"Server Beacons at ${
                        (loader.value()?.second ?: MpInstant.now()).atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                    }"
                }
            }

            loader.renderDefault(this) { data ->
                renderTable(data)
            }
        }
    }

    private fun FlowContent.renderTable(data: Pair<List<ServerBeaconModel>, MpInstant>) {
        val beacons = data.first
        val lastResponse = data.second

        ui.striped.selectable.table Table {
            thead {
                tr {
                    th {
                        +"State"
                    }
                    th {
                        +"Server Id"
                    }
                    th {
                        +"Server Version"
                    }
                    th {
                        +"Last Ping"
                    }
                }
            }
            tbody {
                beacons.forEach { beacon ->

                    tr {
                        td { // Last Ping

                            val lastPing = beacon.lastPing
                            val diff = (lastResponse - lastPing).inWholeMilliseconds

                            when {
                                diff < 10_000.0 -> {
                                    ui.green.label {
                                        +"$diff ms ago"
                                    }
                                }

                                diff < 30_000.0 -> {
                                    ui.yellow.label {
                                        +"$diff ms ago"
                                    }
                                }

                                else -> {
                                    ui.red.label {
                                        +"$diff ms ago"
                                    }
                                }
                            }
                        }

                        td { // Server Id
                            +beacon.serverId
                        }

                        td { // Server Version
                            +beacon.serverVersion
                        }

                        td { // Last Ping
                            +beacon.lastPing.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                        }
                    }
                }
            }
        }
    }
}
