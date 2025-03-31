package de.peekandpoke.funktor.cluster.depot

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.funktor.cluster.renderDefault
import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.vdom.VDom
import depot.api.DepotRepositoryModel
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
        val ui: FunktorClusterUi,
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
                            props.ui.navTo(evt) {
                                depot.browse(repo = repo.name, path = "")
                            }
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
