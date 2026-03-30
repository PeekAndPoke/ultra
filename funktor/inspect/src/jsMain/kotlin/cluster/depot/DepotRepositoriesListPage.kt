package io.peekandpoke.funktor.inspect.cluster.depot

import depot.api.DepotRepositoryModel
import io.peekandpoke.funktor.inspect.cluster.FunktorInspectClusterUi
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
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

class DepotRepositoriesListPage(ctx: Ctx<Props>) : Component<DepotRepositoriesListPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorInspectClusterUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    val loader = dataLoader {
        props.ui.api.depot.listRepositories().map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Depot", "Repositories") }

        ui.padded.segment {
            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 { +"Depot repositories" }
            }

            loader.renderDefault(this) {
                renderTable(it)
            }
        }
    }

    private fun FlowContent.renderTable(repos: List<DepotRepositoryModel>) {
        ui.striped.selectable.table Table {
            thead {
                tr {
                    th {
                        +"Name"
                    }
                    th {
                        +"Type"
                    }
                    th {
                        +"Location"
                    }
                }
            }
            tbody {
                repos.forEach { repo ->
                    tr {
                        onClick { evt ->
                            router.navToUri(
                                evt = evt,
                                route = props.ui.routes.depot.browse(repo = repo.name, path = ""),
                            )
                        }

                        td { // Name
                            +repo.name
                        }

                        td { // Type
                            +repo.type
                        }

                        td { // Location
                            +repo.location
                        }
                    }
                }
            }
        }
    }
}
