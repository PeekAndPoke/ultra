package de.peekandpoke.funktor.cluster.depot

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.funktor.cluster.depot.api.DepotBrowseModel
import de.peekandpoke.funktor.cluster.depot.api.DepotItemModel
import de.peekandpoke.funktor.cluster.renderDefault
import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.addons.routing.urlParam
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.datetime.formatDdMmmYyyyHhMmSs
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

class DepotBrowsePage(ctx: Ctx<Props>) : Component<DepotBrowsePage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorClusterUi,
        val repo: String,
        val path: String,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val path: String by urlParam(props.ui.router, "path", props.path) {
        loader.reload()
    }

    val loader = dataLoader {
        props.ui.api.depot
            .browse(repo = props.repo, path = path)
            .map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Depot", path) }

        ui.padded.segment {
            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            loader.renderDefault(this) { data ->

                div {
                    ui.header H2 { +"Depot Browser ${data.repo.name}" }
                }

                ui.message {
                    +data.item.depotUri.uri
                }

                data.meta?.let { meta ->
                    ui.relaxed.divided.horizontal.list {
                        noui.item {
                            noui.content {
                                noui.header { +"MimeType" }
                                noui.description { +(meta.mimeType ?: "-") }
                            }
                        }
                        noui.item {
                            noui.content {
                                noui.header { +"Visibility" }
                                noui.description { +(meta.visibility.name) }
                            }
                        }
                        noui.item {
                            noui.content {
                                noui.header { +"Organisation Id" }
                                noui.description { +(meta.organisationId ?: "-") }
                            }
                        }
                        noui.item {
                            noui.content {
                                noui.header { +"Organisation Name" }
                                noui.description { +(meta.organisationName ?: "-") }
                            }
                        }
                        noui.item {
                            noui.content {
                                noui.header { +"Owner Id" }
                                noui.description { +(meta.ownerId ?: "-") }
                            }
                        }
                    }
                }

                when (val item = data.item) {
                    is DepotItemModel.File -> renderFile(item)
                    is DepotItemModel.Folder -> renderTable(data)
                }
            }
        }
    }

    private fun FlowContent.renderFile(file: DepotItemModel.File) {

        ui.divider()

        ui.header {
            +"File: ${file.name}"
        }

        ui.blue.button {
            icon.download()
            +"Download"
        }
    }

    private fun FlowContent.renderTable(content: DepotBrowseModel) {
        ui.striped.selectable.table Table {
            thead {
                tr {
                    th { +"" }
                    th { +"Name" }
                    th { +"Size" }
                    th { +"Last modified" }
                }
            }
            tbody {
                tr {
                    onClick { evt ->
                        props.ui.navTo(evt) {
                            when (content.isRoot) {
                                true -> depot.listRepositories()
                                false -> depot.browse(repo = props.repo, path = content.parentPath)
                            }
                        }
                    }

                    td("collapsing") { // Icon
                        icon.folder()
                    }
                    td {// Name
                        +".."
                    }
                    td { // Size
                    }
                    td { // Last Modified
                    }
                }

                content.children.forEach { item ->
                    tr {
                        onClick { evt ->
                            props.ui.navTo(evt) {
                                depot.browse(repo = props.repo, path = item.path)
                            }
                        }

                        td("collapsing") {
                            when (item) {
                                is DepotItemModel.Folder -> icon.folder()
                                is DepotItemModel.File -> icon.file()
                            }
                        }
                        td {
                            +item.name
                        }
                        td {
                            item.size?.let { +"$it bytes" }
                        }
                        td {
                            item.lastModifiedAt?.let { +it.atSystemDefaultZone().formatDdMmmYyyyHhMmSs() }
                        }
                    }
                }
            }
        }
    }
}
